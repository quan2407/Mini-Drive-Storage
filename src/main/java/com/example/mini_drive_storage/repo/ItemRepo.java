package com.example.mini_drive_storage.repo;

import com.example.mini_drive_storage.entity.Items;
import com.example.mini_drive_storage.entity.Users;
import com.example.mini_drive_storage.enums.ItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ItemRepo extends JpaRepository<Items, UUID> {
    List<Items> findAllByParent_Id(UUID parentId);
    List<Items> findAllByParent(Items parent);

    List<Items> findByParent(Items folder);

    @Query(
            value = """
    SELECT DISTINCT i.*
    FROM items i
    LEFT JOIN file_permission fp ON fp.item_id = i.id
    WHERE
    (i.owner_id = :userId OR fp.user_id = :userId)
    AND (:parentId IS NULL OR i.parent_id = :parentId)
    AND (:q IS NULL OR i.name ILIKE CONCAT('%', :q, '%'))
    AND (
        :mimeType IS NULL
        OR i.type = :itemType
        OR i.mime_type = :mimeType
    )
    AND (:fromSize IS NULL OR i.size >= :fromSize)
    AND (:toSize IS NULL OR i.size <= :toSize)
    AND i.deleted_at IS NULL
    """,
            nativeQuery = true
    )
    List<Items> search(
            @Param("userId") UUID userId,
            @Param("q") String q,
            @Param("mimeType") String mimeType,
            @Param("itemType") String itemType,
            @Param("parentId") UUID parentId,
            @Param("fromSize") Long fromSize,
            @Param("toSize") Long toSize
    );

}
