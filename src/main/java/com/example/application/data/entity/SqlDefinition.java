package com.example.application.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "SQLDEFINITION") // Replace "YOUR_TABLE_NAME" with the actual table name
public class SqlDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "PID")
    private Long pid;

    @Column(name = "SQL", length = 4000)
    private String sql;

    @Column(name = "BESCHREIBUNG", length = 4000)
    private String beschreibung;

    @Column(name = "NAME", length = 200)
    private String name;

    @Column(name = "ACCESS_ROLES", length = 200)
    private String accessRoles;

}
