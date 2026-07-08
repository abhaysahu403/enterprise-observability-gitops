package com.enterprise.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TicketCommentRequest {

    @NotNull(message = "Author id is required")
    private Long authorId;

    @NotBlank(message = "Author name is required")
    private String authorName;

    @NotBlank(message = "Comment text is required")
    private String comment;

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
