package com.example.application.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Base64;

@Getter
@Setter
@Entity
@Table(name = "FVMADM_SERVER_CONFIG")
public class ServerConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "HOST_NAME")
    private String hostName="";

    @Column(name = "SSH_PORT")
    private String sshPort="";

    @Column(name = "PATH_LIST")
    private String pathList="";

    @Column(name = "USERNAME")
    private String userName="";

    @Column(name = "SSH_KEY")
    private String sshKey="";

    public static String encodePassword(String plainTextPassword) {
        return Base64.getUrlEncoder().encodeToString(plainTextPassword.getBytes());
    }

    // Method to decode a password from URL-safe Base64
    public static String decodePassword(String encodedPassword) {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedPassword);
        return new String(decodedBytes);
    }
}
