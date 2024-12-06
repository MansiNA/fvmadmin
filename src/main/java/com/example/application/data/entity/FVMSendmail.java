package com.example.application.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "FVMADM_SENDMAIL")
public class FVMSendmail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ENTRYTYP")
    private String entryTyp="";

    @Column(name = "VALUE")
    private String value="";
}
