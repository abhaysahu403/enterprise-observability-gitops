package com.enterprise.helpdesk.dto;

import com.enterprise.helpdesk.entity.TicketComment;

import java.time.Instant;

public class TicketCommentResponse {
    private Long id;
    private Long authorId;
    private String authorName;
    private String comment;
    private Instant createdAt;

    public static TicketCommentResponse from(TicketComment c) {
        TicketCommentResponse dto = new TicketCommentResponse();
        dto.id = c.getId();
        dto.authorId = c.getAuthorId();
        dto.authorName = c.getAuthorName();
        dto.comment = c.getComment();
        dto.createdAt = c.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getAuthorId() { return authorId; }
    public String getAuthorName() { return authorName; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }
}
