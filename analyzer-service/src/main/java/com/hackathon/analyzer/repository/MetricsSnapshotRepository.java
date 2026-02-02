package com.hackathon.analyzer.repository;

import com.hackathon.analyzer.model.MetricsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MetricsSnapshotRepository extends JpaRepository<MetricsSnapshot, Long> {

    List<MetricsSnapshot> findByServiceName(String serviceName);

    List<MetricsSnapshot> findByServiceNameAndTimestampAfter(String serviceName, Instant since);

    @Query("SELECT m FROM MetricsSnapshot m WHERE m.serviceName = :serviceName ORDER BY m.timestamp DESC")
    List<MetricsSnapshot> findRecentMetrics(String serviceName);

    void deleteByTimestampBefore(Instant before);
}
