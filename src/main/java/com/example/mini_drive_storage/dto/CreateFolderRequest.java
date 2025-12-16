package com.example.mini_drive_storage.dto;

import com.example.mini_drive_storage.enums.ItemType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CreateFolderRequest {
    private String name;
    private UUID parentId;
    private ItemType type;
}
