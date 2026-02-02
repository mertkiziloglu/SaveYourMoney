package com.hackathon.dbconnection.repository;

import com.hackathon.dbconnection.model.Order;
import com.hackathon.dbconnection.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUser(User user);

    List<Order> findByStatus(String status);

    @Query("SELECT o FROM Order o WHERE o.orderDate BETWEEN :start AND :end ORDER BY o.orderDate DESC")
    List<Order> findOrdersBetween(Instant start, Instant end);

    @Query("SELECT o FROM Order o JOIN FETCH o.user WHERE o.totalAmount > :minAmount")
    List<Order> findLargeOrders(Double minAmount);

    @Query("SELECT o FROM Order o JOIN FETCH o.user u WHERE u.active = true AND o.status = :status")
    List<Order> findActiveUserOrders(String status);
}
