package dev.marinweis.incidents.service;

import dev.marinweis.incidents.dto.IncidentDtos.*;
import dev.marinweis.incidents.exception.ResourceNotFoundException;
import dev.marinweis.incidents.model.*;
import dev.marinweis.incidents.repository.IncidentCommentRepository;
import dev.marinweis.incidents.repository.IncidentRepository;
import dev.marinweis.incidents.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentCommentRepository commentRepository;
    private final UserRepository userRepository;

    public IncidentService(
            IncidentRepository incidentRepository,
            IncidentCommentRepository commentRepository,
            UserRepository userRepository
    ) {
        this.incidentRepository = incidentRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<IncidentSummaryResponse> list(
            IncidentStatus status,
            IncidentPriority priority,
            String query,
            Pageable pageable
    ) {
        Specification<Incident> spec = Specification
                .where(IncidentSpecifications.hasStatus(status))
                .and(IncidentSpecifications.hasPriority(priority))
                .and(IncidentSpecifications.matchesQuery(query));

        Page<Incident> page = incidentRepository.findAll(spec, pageable);
        List<IncidentSummaryResponse> content = page.getContent().stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public IncidentDetailResponse getById(Long id) {
        Incident incident = findIncident(id);
        List<CommentResponse> comments = commentRepository.findByIncidentIdOrderByCreatedAtAsc(id)
                .stream()
                .map(this::toCommentDto)
                .toList();
        return toDetail(incident, comments);
    }

    @Transactional
    public IncidentDetailResponse create(CreateIncidentRequest request, String reporterUsername) {
        User reporter = findUser(reporterUsername);
        Incident incident = incidentRepository.save(
                new Incident(request.title(), request.description(), request.priority(), reporter)
        );
        return toDetail(incident, List.of());
    }

    @Transactional
    public IncidentDetailResponse update(Long id, UpdateIncidentRequest request, String actorUsername) {
        Incident incident = findIncident(id);
        User actor = findUser(actorUsername);

        boolean isAdmin = actor.getRole() == Role.ADMIN;
        boolean isReporter = incident.getReporter().getId().equals(actor.getId());
        boolean isAssignee = incident.getAssignee() != null && incident.getAssignee().getId().equals(actor.getId());
        if (!isAdmin && !isReporter && !isAssignee) {
            throw new AccessDeniedException("Only admins, the reporter, or the assignee can update this incident");
        }

        if (request.title() != null) {
            incident.setTitle(request.title());
        }
        if (request.description() != null) {
            incident.setDescription(request.description());
        }
        if (request.status() != null) {
            incident.setStatus(request.status());
        }
        if (request.priority() != null) {
            incident.setPriority(request.priority());
        }
        if (request.assigneeUsername() != null) {
            if (request.assigneeUsername().isBlank()) {
                incident.setAssignee(null);
            } else {
                incident.setAssignee(findUser(request.assigneeUsername()));
            }
        }

        Incident saved = incidentRepository.save(incident);
        List<CommentResponse> comments = commentRepository.findByIncidentIdOrderByCreatedAtAsc(id)
                .stream()
                .map(this::toCommentDto)
                .toList();
        return toDetail(saved, comments);
    }

    @Transactional
    public void delete(Long id) {
        Incident incident = findIncident(id);
        incidentRepository.delete(incident);
    }

    @Transactional
    public CommentResponse addComment(Long incidentId, CreateCommentRequest request, String authorUsername) {
        Incident incident = findIncident(incidentId);
        User author = findUser(authorUsername);
        IncidentComment saved = commentRepository.save(new IncidentComment(incident, author, request.body()));
        return toCommentDto(saved);
    }

    @Transactional(readOnly = true)
    public IncidentStatsResponse stats() {
        Map<IncidentStatus, Long> byStatus = new EnumMap<>(IncidentStatus.class);
        for (IncidentStatus s : IncidentStatus.values()) {
            byStatus.put(s, incidentRepository.countByStatus(s));
        }
        Map<IncidentPriority, Long> byPriority = new EnumMap<>(IncidentPriority.class);
        for (IncidentPriority p : IncidentPriority.values()) {
            byPriority.put(p, incidentRepository.countByPriority(p));
        }
        return new IncidentStatsResponse(incidentRepository.count(), byStatus, byPriority);
    }

    private Incident findIncident(Long id) {
        return incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + id));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    private IncidentSummaryResponse toSummary(Incident i) {
        return new IncidentSummaryResponse(
                i.getId(),
                i.getTitle(),
                i.getStatus(),
                i.getPriority(),
                i.getReporter().getUsername(),
                i.getAssignee() != null ? i.getAssignee().getUsername() : null,
                i.getCreatedAt(),
                i.getUpdatedAt()
        );
    }

    private IncidentDetailResponse toDetail(Incident i, List<CommentResponse> comments) {
        return new IncidentDetailResponse(
                i.getId(),
                i.getTitle(),
                i.getDescription(),
                i.getStatus(),
                i.getPriority(),
                i.getReporter().getUsername(),
                i.getAssignee() != null ? i.getAssignee().getUsername() : null,
                i.getCreatedAt(),
                i.getUpdatedAt(),
                comments
        );
    }

    private CommentResponse toCommentDto(IncidentComment c) {
        return new CommentResponse(c.getId(), c.getAuthor().getUsername(), c.getBody(), c.getCreatedAt());
    }
}
