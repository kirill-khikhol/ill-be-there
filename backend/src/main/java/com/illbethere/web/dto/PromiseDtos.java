package com.illbethere.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public final class PromiseDtos {

    private PromiseDtos() {
    }

    public record SlotCount(String start, String end, long count, boolean mine) {
    }

    public record DaySlotsResponse(String date, List<SlotCount> slots) {
    }

    public record PersonResponse(String name, String avatarUrl) {
    }

    public record SlotDetailsResponse(String start, String end, List<PersonResponse> people) {
    }

    public record CreatePromiseRequest(
            @NotNull Long locationId,
            @NotBlank String date,
            @NotBlank String slot
    ) {
    }

    public record PromiseResponse(
            Long id,
            Long locationId,
            String locationName,
            String date,
            String slot,
            String googleEventId,
            String calendarWarning
    ) {
    }
}
