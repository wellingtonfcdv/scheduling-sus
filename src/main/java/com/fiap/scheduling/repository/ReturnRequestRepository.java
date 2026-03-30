package com.fiap.scheduling.repository;

import com.fiap.scheduling.entity.ReturnRequest;
import com.fiap.scheduling.enums.ReturnRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {
    List<ReturnRequest> findByStatusOrderByPriorityDescRequestDateAsc(ReturnRequestStatus status);
}
