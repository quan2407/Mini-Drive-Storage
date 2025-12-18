package com.example.mini_drive_storage.dto;

import com.example.mini_drive_storage.entity.FilePermission;
import com.example.mini_drive_storage.entity.Items;
import com.example.mini_drive_storage.enums.ItemType;
import com.example.mini_drive_storage.enums.PermissionLevel;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class SharedItemResponseDto {

    private UUID id;
    private String name;
    private ItemType type;
    private Long size;
    private String mimeType;
    private UUID parentId;
    private Instant createdDate;
    private PermissionLevel permission;
    private boolean inherited;
    public static SharedItemResponseDto from(FilePermission fp) {
        Items item = fp.getItem();

        return SharedItemResponseDto.builder()
                .id(item.getId())
                .name(item.getName())
                .type(item.getType())
                .size(item.getSize())
                .mimeType(item.getMimeType())
                .parentId(item.getParent() != null ? item.getParent().getId() : null)
                .createdDate(item.getCreatedDate())
                .permission(fp.getPermissionLevel())
                .inherited(fp.isInherited())
                .build();
    }

}

