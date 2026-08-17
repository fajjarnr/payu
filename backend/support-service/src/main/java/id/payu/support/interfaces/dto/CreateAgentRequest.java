package id.payu.support.interfaces.dto;

import id.payu.support.domain.AgentLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateAgentRequest(
    @NotBlank String employeeId,
    @NotBlank String name,
    @NotBlank @Email String email,
    @NotBlank String department,
    AgentLevel level
) {}
