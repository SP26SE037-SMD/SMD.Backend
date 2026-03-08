package com.example.smd.repositories;

import com.example.smd.entities.CLO_PLO_Mapping;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CloPloMappingRepository extends JpaRepository<CLO_PLO_Mapping, UUID> {

    @EntityGraph(attributePaths = {"clo", "plo"})
    List<CLO_PLO_Mapping> findByClo_Subject_SubjectId(UUID subjectId);

    // Lấy chi tiết các PLO mà một CLO đang map tới
    @EntityGraph(attributePaths = {"clo", "plo"})
    List<CLO_PLO_Mapping> findByClo_CloId(UUID cloId);

    // Lấy chi tiết các CLO đang đóng góp cho một PLO cụ thể
    @EntityGraph(attributePaths = {"clo", "plo"})
    List<CLO_PLO_Mapping> findByPlo_PloId(UUID ploId);

    // Kiểm tra trùng lặp mapping (Hàm này trả về boolean nên không cần EntityGraph)
    boolean existsByClo_CloIdAndPlo_PloId(UUID cloId, UUID ploId);
}
