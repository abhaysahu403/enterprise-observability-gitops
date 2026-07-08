package com.enterprise.helpdesk.controller;

import com.enterprise.helpdesk.dto.*;
import com.enterprise.helpdesk.service.TicketService;
import com.enterprise.shared.dto.ApiResponse;
import com.enterprise.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Help Desk", description = "Ticket raising, assignment, comments, resolution, SLA tracking, reports")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @Operation(summary = "Raise a new support ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> create(@Valid @RequestBody TicketRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Ticket raised", ticketService.create(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get ticket details including comments")
    public ResponseEntity<ApiResponse<TicketResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List tickets with optional status/assignee/raiser filters")
    public ResponseEntity<ApiResponse<PageResponse<TicketResponse>>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long assignedTo,
            @RequestParam(required = false) Long raisedBy,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.list(status, assignedTo, raisedBy, pageable)));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "Assign a ticket to a support agent")
    public ResponseEntity<ApiResponse<TicketResponse>> assign(@PathVariable Long id,
                                                                @Valid @RequestBody TicketAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ticket assigned", ticketService.assign(id, request)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update ticket status (e.g. IN_PROGRESS)")
    public ResponseEntity<ApiResponse<TicketResponse>> updateStatus(@PathVariable Long id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", ticketService.updateStatus(id, status)));
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Resolve a ticket with a resolution note")
    public ResponseEntity<ApiResponse<TicketResponse>> resolve(@PathVariable Long id,
                                                                 @Valid @RequestBody TicketResolutionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Ticket resolved", ticketService.resolve(id, request)));
    }

    @PutMapping("/{id}/reopen")
    @Operation(summary = "Reopen a resolved or closed ticket")
    public ResponseEntity<ApiResponse<TicketResponse>> reopen(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Ticket reopened", ticketService.reopen(id)));
    }

    @PostMapping("/{id}/comments")
    @Operation(summary = "Add a comment to a ticket")
    public ResponseEntity<ApiResponse<TicketCommentResponse>> addComment(@PathVariable Long id,
                                                                           @Valid @RequestBody TicketCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Comment added", ticketService.addComment(id, request)));
    }

    @GetMapping("/{id}/comments")
    @Operation(summary = "List comments for a ticket")
    public ResponseEntity<ApiResponse<List<TicketCommentResponse>>> getComments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.getComments(id)));
    }

    @GetMapping("/reports/sla-breached")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'MANAGER')")
    @Operation(summary = "List tickets that have breached their SLA and are still open")
    public ResponseEntity<ApiResponse<List<TicketResponse>>> slaBreached() {
        return ResponseEntity.ok(ApiResponse.success(ticketService.slaBreached()));
    }

    @GetMapping("/reports/status-summary")
    @Operation(summary = "Count of tickets per status")
    public ResponseEntity<ApiResponse<Map<String, Long>>> statusReport() {
        return ResponseEntity.ok(ApiResponse.success(ticketService.statusReport()));
    }
}
