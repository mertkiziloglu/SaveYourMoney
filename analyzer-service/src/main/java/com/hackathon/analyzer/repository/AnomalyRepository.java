package com.hackathon.analyzer.repository;

import com.hackathon.analyzer.model.Anomaly;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, Long> {

    List<Anomaly> findByServiceNameOrderByDetectedAtDesc(String serviceName, Pageable pageable);

    List<Anomaly> findByServiceNameAndResolvedFalse(String serviceName);

    List<Anomaly> findByResolvedFalse();

    List<Anomaly> findByDetectedAtAfter(LocalDateTime dateTime);

    long countByServiceNameAndResolvedFalse(String serviceName);

    @Query("SELECT a FROM Anomaly a WHERE a.serviceName = :serviceName AND a.detectedAt BETWEEN :startDate AND :endDate ORDER BY a.detectedAt DESC")
    List<Anomaly> findByServiceNameAndDateRange(
        @Param("serviceName") String serviceName,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT COUNT(a) FROM Anomaly a WHERE a.resolved = false")
    long countActiveAnomalies();
}
