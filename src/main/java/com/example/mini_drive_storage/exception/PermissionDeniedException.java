package com.example.mini_drive_storage.exception;

public class PermissionDeniedException extends BusinessException {
    public PermissionDeniedException(String message) {
        super(message);
    }
}
