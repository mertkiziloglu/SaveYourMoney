package com.hackathon.memoryleaker.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CachedData {
    private String id;
    private byte[] largeData;  // This will consume memory
    private Instant createdAt;
    private String metadata;

    public long getSizeInBytes() {
        return largeData != null ? largeData.length : 0;
    }
}
