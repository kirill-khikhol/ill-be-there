package com.illbethere.service;

import com.illbethere.domain.AppUser;
import com.illbethere.domain.Favorite;
import com.illbethere.domain.FavoriteSource;
import com.illbethere.domain.Location;
import com.illbethere.repo.FavoriteRepository;
import com.illbethere.web.dto.FavoriteDtos.FavoriteResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final LocationService locationService;

    public FavoriteService(FavoriteRepository favoriteRepository, LocationService locationService) {
        this.favoriteRepository = favoriteRepository;
        this.locationService = locationService;
    }

    @Transactional(readOnly = true)
    public List<FavoriteResponse> list(AppUser user) {
        return favoriteRepository.findByUserIdOrderByActivity(user.getId()).stream()
                .map(FavoriteResponse::from)
                .toList();
    }

    @Transactional
    public FavoriteResponse addManual(Long locationId, AppUser user) {
        return upsert(locationService.get(locationId), user, FavoriteSource.MANUAL);
    }

    @Transactional
    public void touchFromPromise(Location location, AppUser user) {
        upsert(location, user, FavoriteSource.PROMISE);
    }

    @Transactional
    public void remove(Long locationId, AppUser user) {
        if (!favoriteRepository.existsByUserIdAndLocationId(user.getId(), locationId)) {
            throw new IllegalArgumentException("Точки нет в избранном");
        }
        favoriteRepository.deleteByUserIdAndLocationId(user.getId(), locationId);
    }

    public boolean isFavorite(Long locationId, AppUser user) {
        return favoriteRepository.existsByUserIdAndLocationId(user.getId(), locationId);
    }

    private FavoriteResponse upsert(Location location, AppUser user, FavoriteSource incoming) {
        Instant now = Instant.now();
        Favorite favorite = favoriteRepository.findByUserIdAndLocationId(user.getId(), location.getId())
                .orElseGet(() -> {
                    Favorite created = new Favorite();
                    created.setUser(user);
                    created.setLocation(location);
                    created.setSource(incoming);
                    created.setCreatedAt(now);
                    return created;
                });
        if (favorite.getSource() == FavoriteSource.MANUAL && incoming == FavoriteSource.PROMISE) {
            favorite.setSource(FavoriteSource.PROMISE);
        }
        favorite.setLastActivityAt(now);
        return FavoriteResponse.from(favoriteRepository.save(favorite));
    }
}
