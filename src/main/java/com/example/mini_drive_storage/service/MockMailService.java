package com.example.mini_drive_storage.service;

import com.example.mini_drive_storage.enums.PermissionLevel;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class MockMailService implements EmailService {
    @Override
    public void sendShareNotification(String toEmail, String itemName, PermissionLevel permissionLevel) {
        System.out.println(
                "[MOCK EMAIL] Send to: " + toEmail +
                        ", Item: " + itemName +
                        ", Permission: " + permissionLevel
        );
    }
}
