package com.example.smd.repositories;

import com.example.smd.entities.Session_Material_Block;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionMaterialBlockRepository extends JpaRepository<Session_Material_Block, UUID> {
    boolean existsBySession_SessionIdAndMaterial_MaterialIdAndBlock_BlockId(UUID sessionId, UUID materialId, UUID blockId);

    @EntityGraph(attributePaths = {"session", "material", "block"})
    List<Session_Material_Block> findAllBySession_SessionId(UUID sessionId);

    @EntityGraph(attributePaths = {"session", "material", "block"})
    List<Session_Material_Block> findAllBySession_SessionIdIn(List<UUID> sessionIds);

    @Modifying
    int deleteBySession_SessionId(UUID sessionId);
}
