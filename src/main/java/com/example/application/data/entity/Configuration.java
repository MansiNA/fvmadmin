package com.example.application.data.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Base64;

@Getter
@Setter
@Entity
@Table(name = "Configuration")
public class Configuration extends AbstractEntity{

    @NotEmpty
    @Column(name = "NAME")
    private String name="";
    @NotEmpty
    @Column(name = "USER_NAME")
    private String userName="";
    @NotEmpty
    private String password="";
    @NotEmpty
    private String db_Url="";
    @PrePersist
    @PreUpdate
    private void encryptPassword() {
        this.password = encodePassword(this.password);
    }

    // Method to encode a password to Base64
    // Method to encode a password to URL-safe Base64
    public static String encodePassword(String plainTextPassword) {
        return Base64.getUrlEncoder().encodeToString(plainTextPassword.getBytes());
    }

    // Method to decode a password from URL-safe Base64
    public static String decodePassword(String encodedPassword) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedPassword);
        return new String(decodedBytes);
    }
}
