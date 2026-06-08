package br.com.fiap.cityorbit.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "TB_LGPD_CONSENT")
public class ConsentRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false)
    private boolean granted;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false, length = 500)
    private String purposes;

    @Column(nullable = false, length = 45)
    private String ipAddress;

    protected ConsentRecordEntity() {}

    public ConsentRecordEntity(String username, boolean granted,
                               String purposes, String ipAddress) {
        this.username  = username;
        this.granted   = granted;
        this.purposes  = purposes;
        this.ipAddress = ipAddress;
        this.timestamp = Instant.now();
    }

    public Long    getId()        { return id; }
    public String  getUsername()  { return username; }
    public boolean isGranted()    { return granted; }
    public Instant getTimestamp() { return timestamp; }
    public String  getPurposes()  { return purposes; }
    public String  getIpAddress() { return ipAddress; }
}
