package br.com.fiap.cityorbit.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "TB_INCIDENT_EVENT")
public class IncidentEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String phase;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 100)
    private String performedBy;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 2000)
    private String notes;

    protected IncidentEventEntity() {}

    public IncidentEventEntity(String phase, String description,
                               String performedBy, Instant timestamp, String notes) {
        this.phase       = phase;
        this.description = description;
        this.performedBy = performedBy;
        this.timestamp   = timestamp;
        this.notes       = notes;
    }

    public Long    getId()          { return id; }
    public String  getPhase()       { return phase; }
    public String  getDescription() { return description; }
    public String  getPerformedBy() { return performedBy; }
    public Instant getTimestamp()   { return timestamp; }
    public String  getNotes()       { return notes; }
}
