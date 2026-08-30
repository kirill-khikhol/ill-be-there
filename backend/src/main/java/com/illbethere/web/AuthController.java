package com.illbethere.web;

import com.illbethere.config.AppProperties;
import com.illbethere.domain.AppUser;
import com.illbethere.service.PromiseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AppProperties properties;
    private final PromiseService promiseService;

    public AuthController(AppProperties properties, PromiseService promiseService) {
        this.properties = properties;
        this.promiseService = promiseService;
    }

    @GetMapping("/me")
    public Map<String, Object> me() {
        AppUser user = CurrentUser.require();
        return Map.of(
                "id", user.getId(),
                "name", user.getName() != null ? user.getName() : "",
                "email", user.getEmail() != null ? user.getEmail() : "",
                "avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "",
                "hasCalendarAccess", user.hasCalendarToken(),
                "promises", promiseService.myPromises(user));
    }

    @GetMapping("/auth/config")
    public Map<String, Object> authConfig() {
        return Map.of(
                "googleEnabled", properties.isGoogleConfigured(),
                "hasClientId", properties.getGoogle().getClientId() != null
                        && !properties.getGoogle().getClientId().isBlank(),
                "hasClientSecret", properties.getGoogle().getClientSecret() != null
                        && !properties.getGoogle().getClientSecret().isBlank(),
                "clientIdHint", properties.googleClientIdHint(),
                "loginUrl", "/oauth2/authorization/google");
    }
}
