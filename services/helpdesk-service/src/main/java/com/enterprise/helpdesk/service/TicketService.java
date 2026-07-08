package com.enterprise.helpdesk.service;

import com.enterprise.helpdesk.dto.*;
import com.enterprise.helpdesk.entity.*;
import com.enterprise.helpdesk.repository.TicketCommentRepository;
import com.enterprise.helpdesk.repository.TicketRepository;
import com.enterprise.shared.dto.PageResponse;
import com.enterprise.shared.exception.BusinessException;
import com.enterprise.shared.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    // SLA response windows per priority level
    private static final Map<TicketPriority, Duration> SLA_WINDOWS = Map.of(
            TicketPriority.CRITICAL, Duration.ofHours(4),
            TicketPriority.HIGH, Duration.ofHours(24),
            TicketPriority.MEDIUM, Duration.ofHours(72),
            TicketPriority.LOW, Duration.ofHours(120)
    );

    private final TicketRepository ticketRepository;
    private final TicketCommentRepository commentRepository;

    public TicketService(TicketRepository ticketRepository, TicketCommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public TicketResponse create(TicketRequest request) {
        TicketCategory category = parseCategory(request.getCategory());
        TicketPriority priority = parsePriority(request.getPriority());

        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setCategory(category);
        ticket.setPriority(priority);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setRaisedByEmployeeId(request.getRaisedByEmployeeId());
        ticket.setRaisedByName(request.getRaisedByName());
        ticket.setSlaDueAt(Instant.now().plus(SLA_WINDOWS.get(priority)));
        ticket.setTicketNumber(generateTicketNumber());

        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket raised: id={} number={} priority={} slaDue={}",
                saved.getId(), saved.getTicketNumber(), priority, saved.getSlaDueAt());
        return TicketResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(Long id) {
        return TicketResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> list(String status, Long assignedTo, Long raisedBy, Pageable pageable) {
        Page<Ticket> page;
        if (status != null) {
            page = ticketRepository.findByStatus(parseStatus(status), pageable);
        } else if (assignedTo != null) {
            page = ticketRepository.findByAssignedToEmployeeId(assignedTo, pageable);
        } else if (raisedBy != null) {
            page = ticketRepository.findByRaisedByEmployeeId(raisedBy, pageable);
        } else {
            page = ticketRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(TicketResponse::from));
    }

    @Transactional
    public TicketResponse assign(Long id, TicketAssignRequest request) {
        Ticket ticket = findOrThrow(id);
        ticket.setAssignedToEmployeeId(request.getAssigneeId());
        ticket.setAssignedToName(request.getAssigneeName());
        if (ticket.getStatus() == TicketStatus.OPEN || ticket.getStatus() == TicketStatus.REOPENED) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }
        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket assigned: id={} assignee={}", saved.getId(), request.getAssigneeName());
        return TicketResponse.from(saved);
    }

    @Transactional
    public TicketResponse updateStatus(Long id, String status) {
        Ticket ticket = findOrThrow(id);
        TicketStatus newStatus = parseStatus(status);

        if (newStatus == TicketStatus.RESOLVED) {
            throw new BusinessException("USE_RESOLVE_ENDPOINT", "Use the /resolve endpoint to resolve a ticket");
        }

        ticket.setStatus(newStatus);
        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket status updated: id={} newStatus={}", saved.getId(), newStatus);
        return TicketResponse.from(saved);
    }

    @Transactional
    public TicketResponse resolve(Long id, TicketResolutionRequest request) {
        Ticket ticket = findOrThrow(id);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessException("INVALID_STATE", "Cannot resolve a closed ticket");
        }
        ticket.setResolution(request.getResolution());
        ticket.setResolvedAt(Instant.now());
        ticket.setStatus(TicketStatus.RESOLVED);
        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket resolved: id={} slaBreached={}", saved.getId(),
                saved.getResolvedAt().isAfter(saved.getSlaDueAt()));
        return TicketResponse.from(saved);
    }

    @Transactional
    public TicketResponse reopen(Long id) {
        Ticket ticket = findOrThrow(id);
        if (ticket.getStatus() != TicketStatus.RESOLVED && ticket.getStatus() != TicketStatus.CLOSED) {
            throw new BusinessException("INVALID_STATE", "Only resolved or closed tickets can be reopened");
        }
        ticket.setStatus(TicketStatus.REOPENED);
        ticket.setResolvedAt(null);
        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket reopened: id={}", saved.getId());
        return TicketResponse.from(saved);
    }

    @Transactional
    public TicketCommentResponse addComment(Long ticketId, TicketCommentRequest request) {
        Ticket ticket = findOrThrow(ticketId);

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthorId(request.getAuthorId());
        comment.setAuthorName(request.getAuthorName());
        comment.setComment(request.getComment());

        TicketComment saved = commentRepository.save(comment);
        log.info("Comment added to ticket: ticketId={} authorId={}", ticketId, request.getAuthorId());
        return TicketCommentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<TicketCommentResponse> getComments(Long ticketId) {
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(TicketCommentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> slaBreached() {
        List<TicketStatus> closed = List.of(TicketStatus.RESOLVED, TicketStatus.CLOSED);
        return ticketRepository.findByStatusNotInAndSlaDueAtBefore(closed, Instant.now()).stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> statusReport() {
        return Map.of(
                "OPEN", ticketRepository.countByStatus(TicketStatus.OPEN),
                "ASSIGNED", ticketRepository.countByStatus(TicketStatus.ASSIGNED),
                "IN_PROGRESS", ticketRepository.countByStatus(TicketStatus.IN_PROGRESS),
                "RESOLVED", ticketRepository.countByStatus(TicketStatus.RESOLVED),
                "CLOSED", ticketRepository.countByStatus(TicketStatus.CLOSED),
                "REOPENED", ticketRepository.countByStatus(TicketStatus.REOPENED)
        );
    }

    private Ticket findOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", id));
    }

    private TicketCategory parseCategory(String value) {
        try {
            return TicketCategory.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_CATEGORY", "Unknown ticket category: " + value);
        }
    }

    private TicketPriority parsePriority(String value) {
        try {
            return TicketPriority.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_PRIORITY", "Unknown ticket priority: " + value);
        }
    }

    private TicketStatus parseStatus(String value) {
        try {
            return TicketStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("INVALID_STATUS", "Unknown ticket status: " + value);
        }
    }

    private String generateTicketNumber() {
        long next = ticketRepository.count() + 1;
        return String.format("TKT-%06d", next);
    }
}
