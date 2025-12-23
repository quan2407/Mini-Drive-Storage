package com.example.mini_drive_storage.service;

import com.example.mini_drive_storage.entity.FilePermission;
import com.example.mini_drive_storage.entity.Items;
import com.example.mini_drive_storage.entity.Users;
import com.example.mini_drive_storage.enums.ItemType;
import com.example.mini_drive_storage.enums.PermissionLevel;
import com.example.mini_drive_storage.repo.FilePermissionRepo;
import com.example.mini_drive_storage.repo.ItemRepo;
import com.example.mini_drive_storage.repo.UserRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class MockDataService {
    private final UserRepo userRepo;
    private final ItemRepo itemRepo;
    private final FilePermissionRepo filePermissionRepo;
    private final PasswordEncoder passwordEncoder;

    private static final int USER_COUNT = 10;
    private static final int TOTAL_FILE_COUNT = 10_000;

    public void generateSystemMockData() {
        List<Users> users = createUsers();
        List<Items> allFiles = createFilesForUsers(users);
        randomShareFiles(allFiles,users);
    }

    private List<Users> createUsers() {
        List<Users> users = new ArrayList<>();
        for (int i = 0; i <= USER_COUNT; i++) {
            Users user = Users.builder()
                    .email("mock_user_" + i + "@test.com")
                    .password(passwordEncoder.encode("123456"))
                    .createdDate(Instant.now())
                    .build();
            users.add(userRepo.save(user));
        }
        return users;
    }
    private List<Items> createFilesForUsers(List<Users> users) {
        List<Items> allFiles = new ArrayList<>();
        int filePerUser = TOTAL_FILE_COUNT/ users.size();
        for (Users user : users) {
            for (int i = 1; i < filePerUser; i++) {
                Items file = Items.builder()
                        .name("file_" + UUID.randomUUID() + ".txt")
                        .type(ItemType.FILE)
                        .size(1024L)
                        .mimeType("text/plain")
                        .owner(user)
                        .path(createFakeFileOnDisk(user))
                        .createdDate(Instant.now())
                        .build();
                allFiles.add(itemRepo.save(file));
            }
        }
        return allFiles;
    }

    private String createFakeFileOnDisk(Users user) {
        try {
            Path path = Path.of("mock-storage", user.getId().toString());
            Files.createDirectories(path);

            Path file = path.resolve(UUID.randomUUID().toString() + ".txt");
            Files.writeString(file,"mock data");
            return file.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void randomShareFiles(List<Items> allFiles, List<Users> users) {
        Collections.shuffle(allFiles);
        int shareCount = (int) (allFiles.size() * 0.1);

        Random random = new Random();
        for (int i = 0; i < shareCount; i++) {
            Items file = allFiles.get(i);
            Users targetUser;
            do {
                targetUser = users.get(random.nextInt(users.size()));
            } while (targetUser.getId().equals(file.getOwner().getId()));
            PermissionLevel level = random.nextBoolean()
                    ? PermissionLevel.VIEW
                    : PermissionLevel.EDIT;
            FilePermission permission = FilePermission.builder()
                    .item(file)
                    .sharedToUser(targetUser)
                    .permissionLevel(level)
                    .createdDate(Instant.now())
                    .build();
            filePermissionRepo.save(permission);
        }
    }

}
