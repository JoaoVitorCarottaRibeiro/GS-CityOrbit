package br.com.fiap.cityorbit.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "TB_MFA_SECRET")
public class MfaSecretEntity {

    @Id
    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 200)
    private String secret;

    @Column(nullable = false)
    private boolean enabled;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected MfaSecretEntity() {}

    public MfaSecretEntity(String username, String secret, boolean enabled) {
        this.username  = username;
        this.secret    = secret;
        this.enabled   = enabled;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String  getUsername()  { return username; }
    public String  getSecret()    { return secret; }
    public boolean isEnabled()    { return enabled; }
    public Instant getCreatedAt() { return createdAt; }

    public void setEnabled(boolean enabled) {
        this.enabled   = enabled;
        this.updatedAt = Instant.now();
    }

    public void setSecret(String secret) {
        this.secret    = secret;
        this.updatedAt = Instant.now();
    }
}
