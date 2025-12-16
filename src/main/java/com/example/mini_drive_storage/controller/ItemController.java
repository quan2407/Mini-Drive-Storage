package com.example.mini_drive_storage.controller;

import com.example.mini_drive_storage.dto.ItemResponseDto;
import com.example.mini_drive_storage.entity.Items;
import com.example.mini_drive_storage.service.ItemService;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class ItemController {
    private ItemService itemService;

    @PostMapping(
            value = "/files",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<ItemResponseDto>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "parentId", required = false) UUID parentId
    ) {
        List<ItemResponseDto> items =  itemService.uploadFiles(files,parentId);
        return ResponseEntity.ok(items);
    }

}
