package com.devpilot.aiagent.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public class ApplyChangesRequest {

    // It separates two actions:
    // - Planning: the Coding Agent generates a CodingResult.
    // - Applying: the user reviews that result and submits it with approved: true

    @NotNull
    @Valid
    private CodingResult proposal;

    @NotNull
    private Boolean approved;

    public ApplyChangesRequest() {
    }

    public CodingResult getProposal() {
        return proposal;
    }

    public void setProposal(CodingResult proposal) {
        this.proposal = proposal;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }
}