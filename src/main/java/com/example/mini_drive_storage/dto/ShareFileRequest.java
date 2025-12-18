package com.example.mini_drive_storage.dto;

import com.example.mini_drive_storage.enums.PermissionLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShareFileRequest {
    @Email(message = "email doesn't match type")
    @NotNull(message = "email is not null")
    private String email;
    @NotNull(message = "permission is not null")
    private PermissionLevel permission;
}
