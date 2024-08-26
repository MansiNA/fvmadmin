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
@Table(name = "SQL_CONFIGURATION")
public class Configuration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME")
    private String name="";

    @Column(name = "USER_NAME")
    private String userName="";

    private String password="";

    private String db_Url="";

    @Column(name = "IS_MONITORING")
    private Integer isMonitoring;

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
