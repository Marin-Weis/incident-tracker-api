package dev.marinweis.incidents.dto;

import dev.marinweis.incidents.model.IncidentPriority;
import dev.marinweis.incidents.model.IncidentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public final class IncidentDtos {
    private IncidentDtos() {}

    public record CreateIncidentRequest(
            @NotBlank @Size(min = 5, max = 160) String title,
            @NotBlank @Size(min = 10, max = 5000) String description,
            @NotNull IncidentPriority priority
    ) {}

    public record UpdateIncidentRequest(
            @Size(min = 5, max = 160) String title,
            @Size(min = 10, max = 5000) String description,
            IncidentStatus status,
            IncidentPriority priority,
            String assigneeUsername
    ) {}

    public record CreateCommentRequest(
            @NotBlank @Size(min = 2, max = 2000) String body
    ) {}

    public record CommentResponse(
            Long id,
            String authorUsername,
            String body,
            OffsetDateTime createdAt
    ) {}

    public record IncidentSummaryResponse(
            Long id,
            String title,
            IncidentStatus status,
            IncidentPriority priority,
            String reporterUsername,
            String assigneeUsername,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    public record IncidentDetailResponse(
            Long id,
            String title,
            String description,
            IncidentStatus status,
            IncidentPriority priority,
            String reporterUsername,
            String assigneeUsername,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            List<CommentResponse> comments
    ) {}

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}

    public record IncidentStatsResponse(
            long total,
            Map<IncidentStatus, Long> byStatus,
            Map<IncidentPriority, Long> byPriority
    ) {}
}
