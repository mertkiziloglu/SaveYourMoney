package com.hackathon.dbconnection.repository;

import com.hackathon.dbconnection.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findByActive(Boolean active);

    List<User> findByEmailContaining(String email);

    @Query("SELECT u FROM User u WHERE u.createdAt > :since ORDER BY u.createdAt DESC")
    List<User> findRecentUsers(Instant since);

    @Query("SELECT u FROM User u WHERE u.lastLoginAt IS NULL OR u.lastLoginAt < :before")
    List<User> findInactiveUsers(Instant before);
}
