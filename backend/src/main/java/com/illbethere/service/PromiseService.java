package com.illbethere.service;

import com.illbethere.calendar.GoogleCalendarService;
import com.illbethere.config.AppProperties;
import com.illbethere.domain.AppUser;
import com.illbethere.domain.AttendancePromise;
import com.illbethere.domain.Location;
import com.illbethere.domain.PromiseStatus;
import com.illbethere.repo.PromiseRepository;
import com.illbethere.web.dto.PromiseDtos.CreatePromiseRequest;
import com.illbethere.web.dto.PromiseDtos.DaySlotsResponse;
import com.illbethere.web.dto.PromiseDtos.PersonResponse;
import com.illbethere.web.dto.PromiseDtos.PromiseResponse;
import com.illbethere.web.dto.PromiseDtos.SlotCount;
import com.illbethere.web.dto.PromiseDtos.SlotDetailsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PromiseService {

    private static final DateTimeFormatter SLOT = DateTimeFormatter.ofPattern("HH:mm");

    private final PromiseRepository promiseRepository;
    private final LocationService locationService;
    private final FavoriteService favoriteService;
    private final GoogleCalendarService googleCalendarService;
    private final AppProperties properties;

    public PromiseService(
            PromiseRepository promiseRepository,
            LocationService locationService,
            FavoriteService favoriteService,
            GoogleCalendarService googleCalendarService,
            AppProperties properties) {
        this.promiseRepository = promiseRepository;
        this.locationService = locationService;
        this.favoriteService = favoriteService;
        this.googleCalendarService = googleCalendarService;
        this.properties = properties;
    }

    public DaySlotsResponse daySlots(Long locationId, String date, AppUser currentUser) {
        locationService.get(locationId);
        ZoneId zone = zone();
        LocalDate day = LocalDate.parse(date);
        Instant from = day.atStartOfDay(zone).toInstant();
        Instant to = day.plusDays(1).atStartOfDay(zone).toInstant();
        List<AttendancePromise> promises = promiseRepository.findActiveInRange(
                locationId, from, to, PromiseStatus.ACTIVE);

        Map<Instant, Long> counts = promises.stream()
                .collect(Collectors.groupingBy(AttendancePromise::getSlotStart, Collectors.counting()));
        Long myId = currentUser != null ? currentUser.getId() : null;
        Map<Instant, Boolean> mine = promises.stream()
                .filter(p -> myId != null && p.getUser().getId().equals(myId))
                .collect(Collectors.toMap(AttendancePromise::getSlotStart, p -> true, (a, b) -> true));

        Instant now = Instant.now();
        List<SlotCount> slots = new ArrayList<>();
        for (int minute = 6 * 60; minute < 24 * 60; minute += 30) {
            LocalTime start = LocalTime.of(minute / 60, minute % 60);
            Instant slotStart = day.atTime(start).atZone(zone).toInstant();
            if (slotStart.isBefore(now)) {
                continue;
            }
            String startLabel = start.format(SLOT);
            String endLabel = start.plusMinutes(30).format(SLOT);
            slots.add(new SlotCount(
                    startLabel,
                    endLabel,
                    counts.getOrDefault(slotStart, 0L),
                    mine.getOrDefault(slotStart, false)));
        }
        return new DaySlotsResponse(date, slots);
    }

    public SlotDetailsResponse slotDetails(Long locationId, String date, String slot) {
        locationService.get(locationId);
        Instant slotStart = parseSlot(date, slot);
        Instant slotEnd = slotStart.plusSeconds(30 * 60);
        List<AttendancePromise> promises = promiseRepository.findActiveInRange(
                locationId, slotStart, slotEnd, PromiseStatus.ACTIVE);
        List<PersonResponse> people = promises.stream()
                .map(p -> new PersonResponse(
                        p.getUser().getName() != null ? p.getUser().getName() : "Пользователь",
                        p.getUser().getAvatarUrl()))
                .toList();
        LocalTime start = LocalTime.parse(slot);
        return new SlotDetailsResponse(start.format(SLOT), start.plusMinutes(30).format(SLOT), people);
    }

    @Transactional
    public PromiseResponse create(CreatePromiseRequest request, AppUser user) {
        Location location = locationService.get(request.locationId());
        Instant slotStart = parseSlot(request.date(), request.slot());
        if (slotStart.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Нельзя записаться на слот в прошлом");
        }
        promiseRepository.findByUserIdAndLocationIdAndSlotStartAndStatus(
                        user.getId(), location.getId(), slotStart, PromiseStatus.ACTIVE)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Вы уже обещали прийти в этот слот");
                });

        AttendancePromise promise = new AttendancePromise();
        promise.setUser(user);
        promise.setLocation(location);
        promise.setSlotStart(slotStart);
        promise.setStatus(PromiseStatus.ACTIVE);
        promise.setCreatedAt(Instant.now());
        GoogleCalendarService.WriteResult calendar = googleCalendarService.createEvent(user, location, slotStart);
        promise.setGoogleEventId(calendar.eventId());
        AttendancePromise saved = promiseRepository.save(promise);
        favoriteService.touchFromPromise(location, user);
        return toResponse(saved, calendar.warning());
    }

    @Transactional
    public void cancel(Long promiseId, AppUser user) {
        AttendancePromise promise = promiseRepository.findById(promiseId)
                .orElseThrow(() -> new IllegalArgumentException("Обещание не найдено"));
        if (!promise.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Можно отменить только своё обещание");
        }
        if (promise.getStatus() == PromiseStatus.CANCELLED) {
            return;
        }
        googleCalendarService.deleteEvent(user, promise.getGoogleEventId());
        promise.setStatus(PromiseStatus.CANCELLED);
        promiseRepository.save(promise);
    }

    public List<PromiseResponse> myPromises(AppUser user) {
        return promiseRepository.findByUserIdAndStatusOrderBySlotStartAsc(user.getId(), PromiseStatus.ACTIVE)
                .stream()
                .map(p -> toResponse(p, null))
                .toList();
    }

    public Long findActiveId(AppUser user, Long locationId, String date, String slot) {
        return promiseRepository.findByUserIdAndLocationIdAndSlotStartAndStatus(
                        user.getId(), locationId, parseSlot(date, slot), PromiseStatus.ACTIVE)
                .map(AttendancePromise::getId)
                .orElse(null);
    }

    private PromiseResponse toResponse(AttendancePromise promise, String calendarWarning) {
        ZonedDateTime start = promise.getSlotStart().atZone(zone());
        return new PromiseResponse(
                promise.getId(),
                promise.getLocation().getId(),
                promise.getLocation().getName(),
                start.toLocalDate().toString(),
                start.toLocalTime().format(SLOT),
                promise.getGoogleEventId(),
                calendarWarning);
    }

    private Instant parseSlot(String date, String slot) {
        LocalDate day = LocalDate.parse(date);
        LocalTime time = LocalTime.parse(slot);
        if (time.getMinute() % 30 != 0 || time.getSecond() != 0) {
            throw new IllegalArgumentException("Слот должен начинаться на 00 или 30 минут");
        }
        return day.atTime(time).atZone(zone()).toInstant();
    }

    private ZoneId zone() {
        return ZoneId.of(properties.getTimezone());
    }
}
