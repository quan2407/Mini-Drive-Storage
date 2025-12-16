package com.example.mini_drive_storage.dto;

import com.example.mini_drive_storage.entity.Items;
import com.example.mini_drive_storage.enums.ItemType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class ItemResponseDto {
    private UUID id;
    private String name;
    private ItemType type;
    private Long size;
    private String mimeType;
    private UUID parentId;
    private Instant createdDate;

    public static ItemResponseDto from(Items item) {
        return new ItemResponseDto(
                item.getId(),
                item.getName(),
                item.getType(),
                item.getSize(),
                item.getMimeType(),
                item.getParent() != null ? item.getParent().getId() : null,
                item.getCreatedDate()
        );
    }
}
