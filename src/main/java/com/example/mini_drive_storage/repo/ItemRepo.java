package com.example.mini_drive_storage.repo;

import com.example.mini_drive_storage.entity.Items;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItemRepo extends JpaRepository<Items, UUID> {
    List<Items> findAllByParent_Id(UUID parentId);
    List<Items> findAllByParent(Items parent);
}
