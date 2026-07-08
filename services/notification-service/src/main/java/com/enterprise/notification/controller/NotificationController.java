package com.enterprise.notification.controller;

import com.enterprise.notification.dto.NotificationResponse;
import com.enterprise.notification.dto.NotificationSendRequest;
import com.enterprise.notification.service.NotificationService;
import com.enterprise.shared.dto.ApiResponse;
import com.enterprise.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Mock email/Teams/SMS notifications, history, retry, status")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/send")
    @Operation(summary = "Send a notification over email, Teams, or SMS (mocked delivery)")
    public ResponseEntity<ApiResponse<NotificationResponse>> send(@Valid @RequestBody NotificationSendRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Notification queued", notificationService.send(request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a notification by id, including delivery status")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getById(id)));
    }

    @PutMapping("/{id}/retry")
    @Operation(summary = "Manually retry a failed/retrying notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> retry(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Retry attempted", notificationService.retry(id)));
    }

    @GetMapping("/history")
    @Operation(summary = "Notification history, optionally filtered by employee or status")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> history(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.history(employeeId, status, pageable)));
    }

    @GetMapping("/reports/status-summary")
    @Operation(summary = "Count of notifications per delivery status")
    public ResponseEntity<ApiResponse<Map<String, Long>>> statusSummary() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.statusSummary()));
    }
}
