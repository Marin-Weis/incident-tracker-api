package dev.marinweis.incidents.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "incident_comments")
public class IncidentComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected IncidentComment() {}

    public IncidentComment(Incident incident, User author, String body) {
        this.incident = incident;
        this.author = author;
        this.body = body;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Incident getIncident() { return incident; }
    public User getAuthor() { return author; }
    public String getBody() { return body; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
