package dev.marinweis.incidents.controller;

import dev.marinweis.incidents.dto.IncidentDtos.*;
import dev.marinweis.incidents.model.IncidentPriority;
import dev.marinweis.incidents.model.IncidentStatus;
import dev.marinweis.incidents.service.IncidentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/incidents")
@Tag(name = "Incidents", description = "Incident CRUD, filtering, pagination, comments, and statistics")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    @Operation(summary = "List incidents with pagination and optional status, priority, or keyword filters")
    public PageResponse<IncidentSummaryResponse> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentPriority priority,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int clampedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(Math.max(page, 0), clampedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return incidentService.list(status, priority, q, pageable);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get aggregate incident counts grouped by status and priority")
    public IncidentStatsResponse stats() {
        return incidentService.stats();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full incident details including comments")
    public IncidentDetailResponse getById(@PathVariable Long id) {
        return incidentService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new technical incident")
    public IncidentDetailResponse create(
            @Valid @RequestBody CreateIncidentRequest request,
            Authentication authentication
    ) {
        return incidentService.create(request, authentication.getName());
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update an incident (allowed for admins, reporter, or assignee)")
    public IncidentDetailResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentRequest request,
            Authentication authentication
    ) {
        return incidentService.update(id, request, authentication.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete an incident (ADMIN role required)")
    public void delete(@PathVariable Long id) {
        incidentService.delete(id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a comment to an incident")
    public CommentResponse addComment(
            @PathVariable Long id,
            @Valid @RequestBody CreateCommentRequest request,
            Authentication authentication
    ) {
        return incidentService.addComment(id, request, authentication.getName());
    }
}
