package br.com.fiap.cityorbit.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "TB_INCIDENT_NOTIFICATION")
public class IncidentNotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String incidentPhase;

    @Column(nullable = false, length = 50)
    private String channel;

    @Column(nullable = false, length = 200)
    private String recipient;

    @Column(nullable = false, length = 3000)
    private String message;

    @Column(nullable = false)
    private Instant sentAt;

    @Column(nullable = false)
    private boolean delivered;

    @Column(length = 500)
    private String deliveryNote;

    protected IncidentNotificationEntity() {}

    public IncidentNotificationEntity(String incidentPhase, String channel,
                                      String recipient, String message,
                                      boolean delivered, String deliveryNote) {
        this.incidentPhase = incidentPhase;
        this.channel       = channel;
        this.recipient     = recipient;
        this.message       = message;
        this.delivered     = delivered;
        this.deliveryNote  = deliveryNote;
        this.sentAt        = Instant.now();
    }

    public Long    getId()            { return id; }
    public String  getIncidentPhase() { return incidentPhase; }
    public String  getChannel()       { return channel; }
    public String  getRecipient()     { return recipient; }
    public String  getMessage()       { return message; }
    public Instant getSentAt()        { return sentAt; }
    public boolean isDelivered()      { return delivered; }
    public String  getDeliveryNote()  { return deliveryNote; }
}
