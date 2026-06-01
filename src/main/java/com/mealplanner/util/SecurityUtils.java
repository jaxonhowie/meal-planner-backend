package com.mealplanner.util;

import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Long uid() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
