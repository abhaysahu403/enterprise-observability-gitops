package com.enterprise.helpdesk.dto;

import jakarta.validation.constraints.NotBlank;

public class TicketResolutionRequest {

    @NotBlank(message = "Resolution text is required")
    private String resolution;

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
}
