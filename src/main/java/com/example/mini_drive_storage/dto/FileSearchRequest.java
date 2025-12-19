package com.example.mini_drive_storage.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class FileSearchRequest {
    private String q;
    private String type; // FILE, FOLDER, hoặc mime
    private UUID parentId;
    private Long fromSize;
    private Long toSize;
}

