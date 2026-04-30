package com.example.smd.repositories;

import com.example.smd.entities.Document;
import com.example.smd.entities.Regulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {
    @Query("SELECT d FROM Document d WHERE " +
            "(:majorId IS NULL OR d.major.majorId = :majorId) AND " +
            "(:status IS NULL OR d.status = :status)")
    List<Document> findAllWithFilters(@Param("majorId") UUID majorId,
                                      @Param("status") String status);
}
