package br.com.fiap.cityorbit.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String type = "Bearer";
    private String username;
    private String role;
    private long   expiresIn;

    public AuthResponse(String token, String username, String role, long expiresIn) {
        this.token     = token;
        this.username  = username;
        this.role      = role;
        this.expiresIn = expiresIn;
    }
}
