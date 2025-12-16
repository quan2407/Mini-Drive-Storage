package com.example.mini_drive_storage.repo;

import com.example.mini_drive_storage.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;
@Repository
public interface UserRepo extends JpaRepository<Users, UUID> {

    Optional<Users> findByEmail(String email);
}
