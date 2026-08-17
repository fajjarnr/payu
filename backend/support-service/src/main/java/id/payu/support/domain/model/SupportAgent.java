package id.payu.support.domain.model;

import id.payu.support.domain.AgentLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Domain model representing a Support Agent.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportAgent {

    private Long id;
    private String employeeId;
    private String name;
    private String email;
    private String department;

    @Builder.Default
    private AgentLevel level = AgentLevel.JUNIOR;

    @Builder.Default
    private boolean active = true;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
