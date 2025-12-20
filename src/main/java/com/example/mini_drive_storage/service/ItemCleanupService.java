package com.example.mini_drive_storage.service;

import com.example.mini_drive_storage.entity.Items;
import com.example.mini_drive_storage.enums.ItemType;
import com.example.mini_drive_storage.repo.FilePermissionRepo;
import com.example.mini_drive_storage.repo.ItemRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemCleanupService {
    private final ItemRepo itemRepo;
    private final FilePermissionRepo filePermissionRepo;

    @Transactional
    public void hardDeleteRecursive(Items item) {
        if (item.getType() == ItemType.FOLDER) {
            List<Items> children = itemRepo.findByParent(item);
            for (Items child : children) {
                hardDeleteRecursive(child);
            }
        }
        if (item.getType() == ItemType.FILE && item.getPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(item.getPath()));
            } catch (Exception e) {
                log.error("Failed to delete file " + e.getMessage());
            }
        }
        filePermissionRepo.deleteByItem(item);
        itemRepo.delete(item);
        log.info("Deleted file " + item.getId());
    }
}
