package com.illbethere.web.dto;

import com.illbethere.domain.Favorite;
import com.illbethere.domain.FavoriteSource;
import com.illbethere.web.dto.LocationDtos.LocationResponse;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class FavoriteDtos {

    private FavoriteDtos() {
    }

    public record FavoriteResponse(
            Long id,
            LocationResponse location,
            FavoriteSource source,
            Instant createdAt,
            Instant lastActivityAt
    ) {
        public static FavoriteResponse from(Favorite favorite) {
            return new FavoriteResponse(
                    favorite.getId(),
                    LocationResponse.from(favorite.getLocation()),
                    favorite.getSource(),
                    favorite.getCreatedAt(),
                    favorite.getLastActivityAt());
        }
    }

    public record AddFavoriteRequest(@NotNull Long locationId) {
    }
}
