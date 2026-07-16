package com.melih.omsslcm.repository;

import com.melih.omsslcm.domain.OrderHistory;
import com.melih.omsslcm.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderHistoryRepository extends JpaRepository<OrderHistory, String> {

    List<OrderHistory> findByStatus(OrderStatus status);
}
