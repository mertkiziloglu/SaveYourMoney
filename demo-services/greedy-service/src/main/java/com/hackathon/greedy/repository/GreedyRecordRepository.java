package com.hackathon.greedy.repository;

import com.hackathon.greedy.model.GreedyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GreedyRecordRepository extends JpaRepository<GreedyRecord, Long> {

    @Query("SELECT r FROM GreedyRecord r ORDER BY r.createdAt DESC")
    List<GreedyRecord> findLatestRecords();

    long countByCreatedAtGreaterThan(Long timestamp);
}
