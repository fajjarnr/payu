package id.payu.support.dto;

import id.payu.support.domain.SupportAgent.AgentLevel;
import java.time.LocalDateTime;

public record AgentResponse(
    Long id,
    String employeeId,
    String name,
    String email,
    String department,
    AgentLevel level,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
