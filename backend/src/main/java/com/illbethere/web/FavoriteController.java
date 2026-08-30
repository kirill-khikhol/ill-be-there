package com.illbethere.web;

import com.illbethere.service.FavoriteService;
import com.illbethere.web.dto.FavoriteDtos.AddFavoriteRequest;
import com.illbethere.web.dto.FavoriteDtos.FavoriteResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public List<FavoriteResponse> list() {
        return favoriteService.list(CurrentUser.require());
    }

    @PostMapping
    public FavoriteResponse add(@Valid @RequestBody AddFavoriteRequest request) {
        return favoriteService.addManual(request.locationId(), CurrentUser.require());
    }

    @DeleteMapping("/{locationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long locationId) {
        favoriteService.remove(locationId, CurrentUser.require());
    }
}
