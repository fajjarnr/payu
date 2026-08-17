package id.payu.support.domain.model;

import id.payu.support.domain.CompletionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Domain model representing Agent Training progress.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentTraining {

    private Long id;
    private Long agentId;
    private String agentName;
    private Long trainingModuleId;
    private String moduleCode;
    private String moduleTitle;

    @Builder.Default
    private CompletionStatus status = CompletionStatus.NOT_STARTED;

    private Integer score;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
