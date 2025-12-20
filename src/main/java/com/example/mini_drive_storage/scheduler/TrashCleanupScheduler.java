package com.example.mini_drive_storage.scheduler;

import com.example.mini_drive_storage.entity.Items;
import com.example.mini_drive_storage.repo.ItemRepo;
import com.example.mini_drive_storage.service.ItemCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class TrashCleanupScheduler {

    private final ItemRepo itemRepo;
    private final ItemCleanupService itemCleanupService;
    private final Executor executor;

    public TrashCleanupScheduler(
            ItemRepo itemRepo,
            ItemCleanupService itemCleanupService,
            @Qualifier("cleanupExecutor") Executor executor
    ) {
        this.itemRepo = itemRepo;
        this.itemCleanupService = itemCleanupService;
        this.executor = executor;
    }

    @Value("${trash.retention-days}")
    private int retentionDays;

    @Scheduled(cron = "${trash.cleanup.cron}")
    public void trashCleanup() {
        Instant expiredTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        List<Items> expiredItems = itemRepo.findExpiredRootItems(expiredTime);

        log.info("Found {} expired items to cleanup", expiredItems.size());

        for (Items item : expiredItems) {
            CompletableFuture.runAsync(
                    () -> itemCleanupService.hardDeleteRecursive(item),
                    executor
            );
        }
    }
}
