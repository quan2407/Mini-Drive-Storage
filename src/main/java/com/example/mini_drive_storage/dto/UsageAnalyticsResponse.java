package com.example.mini_drive_storage.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsageAnalyticsResponse {
    private long totalFiles;
    private long totalSize;
}
