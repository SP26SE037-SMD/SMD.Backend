package com.example.smd.repositories;

import com.example.smd.entities.Options;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FeedbackOptionRepository extends JpaRepository<Options, UUID> {
    List<Options> findAllByOrderByOptionNoAsc();
}
