package com.example.mini_drive_storage.utils;

import com.example.mini_drive_storage.entity.Users;
import com.example.mini_drive_storage.exception.NotFoundException;
import com.example.mini_drive_storage.repo.UserRepo;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@AllArgsConstructor
public class CurrentUserUtils {

    private final UserRepo userRepo;

    public Users getCurrentUser() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new NotFoundException("User is not authenticated");
        }

        String email = authentication.getName();

        return userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new NotFoundException("User not found with email: " + email));
    }

    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
