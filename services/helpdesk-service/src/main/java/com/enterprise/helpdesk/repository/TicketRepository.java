package com.enterprise.helpdesk.repository;

import com.enterprise.helpdesk.entity.Ticket;
import com.enterprise.helpdesk.entity.TicketPriority;
import com.enterprise.helpdesk.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    Page<Ticket> findByAssignedToEmployeeId(Long employeeId, Pageable pageable);

    Page<Ticket> findByRaisedByEmployeeId(Long employeeId, Pageable pageable);

    List<Ticket> findByStatusNotInAndSlaDueAtBefore(List<TicketStatus> closedStatuses, Instant now);

    long countByStatus(TicketStatus status);

    long countByPriority(TicketPriority priority);
}
