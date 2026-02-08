package com.hackathon.greedy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "greedy_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GreedyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payload", length = 4096)
    private String payload;

    @Column(name = "hash_result", length = 256)
    private String hashResult;

    @Column(name = "computation_time_ms")
    private Long computationTimeMs;

    @Column(name = "created_at")
    private Long createdAt;
}
