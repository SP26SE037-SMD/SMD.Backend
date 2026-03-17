package com.example.smd.repositories;

import com.example.smd.entities.Syllabus_Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyllabusSourceRepository extends JpaRepository<Syllabus_Source, UUID> {

    // Lấy danh sách trung gian dựa trên Syllabus ID
    List<Syllabus_Source> findBySyllabus_SyllabusId(UUID syllabusId);

    // Tìm một bản ghi cụ thể để xóa
    Optional<Syllabus_Source> findBySyllabus_SyllabusIdAndSource_SourceId(UUID syllabusId, UUID sourceId);

    // Kiểm tra xem Source đã tồn tại trong Syllabus chưa (tránh add trùng)
    boolean existsBySyllabus_SyllabusIdAndSource_SourceId(UUID syllabusId, UUID sourceId);
}