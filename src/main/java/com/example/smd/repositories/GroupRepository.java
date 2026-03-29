package com.example.smd.repositories;

import com.example.smd.entities.Group;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    // Kiểm tra tồn tại theo group code
    boolean existsByGroupCode(String groupCode);

    Optional<Group> findByGroupCode(String groupCode);

    // Tìm kiếm Group theo group code (không phân biệt hoa thường)
    @Query("SELECT c FROM Group c WHERE LOWER(c.groupCode) LIKE LOWER(CONCAT('%', :groupCode, '%'))")
    Page<Group> findByGroupCodeContaining(@Param("groupCode") String groupCode, Pageable pageable);

    // Tìm kiếm Group theo group name (không phân biệt hoa thường)
    @Query("SELECT c FROM Group c WHERE LOWER(c.groupName) LIKE LOWER(CONCAT('%', :groupName, '%'))")
    Page<Group> findByGroupNameContaining(@Param("groupName") String groupName, Pageable pageable);

        // Tìm kiếm Group theo code hoặc name (không phân biệt hoa thường)
        @Query("SELECT c FROM Group c WHERE LOWER(c.groupCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.groupName) LIKE LOWER(CONCAT('%', :search, '%'))")
        Page<Group> findByCodeOrNameContaining(@Param("search") String search, Pageable pageable);

    // Lấy tất cả Group với phân trang
    Page<Group> findAll(Pageable pageable);

    // Tìm kiếm Group theo type (Elective/Group)
    @Query("SELECT c FROM Group c WHERE LOWER(c.type) = LOWER(:type)")
    Page<Group> findByType(@Param("type") String type, Pageable pageable);

        // Tìm kiếm Group theo type và group code
        @Query("SELECT c FROM Group c WHERE LOWER(c.type) = LOWER(:type) " +
            "AND LOWER(c.groupCode) LIKE LOWER(CONCAT('%', :groupCode, '%'))")
        Page<Group> findByTypeAndGroupCodeContaining(
            @Param("type") String type,
            @Param("groupCode") String groupCode,
            Pageable pageable
        );

        // Tìm kiếm Group theo type và group name
        @Query("SELECT c FROM Group c WHERE LOWER(c.type) = LOWER(:type) " +
            "AND LOWER(c.groupName) LIKE LOWER(CONCAT('%', :groupName, '%'))")
        Page<Group> findByTypeAndGroupNameContaining(
            @Param("type") String type,
            @Param("groupName") String groupName,
            Pageable pageable
        );

        // Tìm kiếm Group theo type và keyword (code hoặc name)
        @Query("SELECT c FROM Group c WHERE LOWER(c.type) = LOWER(:type) AND (" +
            "LOWER(c.groupCode) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(c.groupName) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<Group> findByTypeAndCodeOrNameContaining(
            @Param("type") String type,
            @Param("search") String search,
            Pageable pageable
        );
}
