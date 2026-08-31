package com.illbethere.web;

import com.illbethere.domain.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static AppUser optional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUser user) {
            return user;
        }
        return null;
    }

    public static AppUser require() {
        AppUser user = optional();
        if (user == null) {
            throw new UnauthorizedException("unauthorized");
        }
        return user;
    }
}
