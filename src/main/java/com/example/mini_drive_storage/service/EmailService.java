package com.example.mini_drive_storage.service;

import com.example.mini_drive_storage.enums.PermissionLevel;

public interface EmailService {
    void sendShareNotification(String toEmail, String itemName, PermissionLevel permissionLevel);
}
