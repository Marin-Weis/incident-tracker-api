package dev.marinweis.incidents.service;

import dev.marinweis.incidents.model.Incident;
import dev.marinweis.incidents.model.IncidentPriority;
import dev.marinweis.incidents.model.IncidentStatus;
import org.springframework.data.jpa.domain.Specification;

public final class IncidentSpecifications {

    private IncidentSpecifications() {}

    public static Specification<Incident> hasStatus(IncidentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Incident> hasPriority(IncidentPriority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Incident> matchesQuery(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return null;
            }
            String pattern = "%" + q.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }
}
