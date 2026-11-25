package dev.marinweis.incidents.repository;

import dev.marinweis.incidents.model.Incident;
import dev.marinweis.incidents.model.IncidentPriority;
import dev.marinweis.incidents.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IncidentRepository extends JpaRepository<Incident, Long>, JpaSpecificationExecutor<Incident> {
    long countByStatus(IncidentStatus status);
    long countByPriority(IncidentPriority priority);
}
