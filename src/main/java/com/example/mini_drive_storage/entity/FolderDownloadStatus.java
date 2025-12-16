package com.example.mini_drive_storage.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FolderDownloadStatus {
    private String status;
    private String zipPath;
}
