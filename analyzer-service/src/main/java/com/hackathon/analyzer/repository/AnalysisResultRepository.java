package com.hackathon.analyzer.repository;

import com.hackathon.analyzer.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, Long> {

    List<AnalysisResult> findByServiceNameOrderByAnalysisTimestampDesc(String serviceName);

    Optional<AnalysisResult> findFirstByServiceNameOrderByAnalysisTimestampDesc(String serviceName);
}
