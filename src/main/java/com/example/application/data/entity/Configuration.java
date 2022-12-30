package com.example.application.data.entity;

import javax.persistence.Entity;
import javax.validation.constraints.NotEmpty;

@Entity
public class Configuration extends AbstractEntity{

    @NotEmpty
    private String land="";

    @NotEmpty
    private String umgebung="";
    @NotEmpty
    private String userName="";
    @NotEmpty
    private String password="";
    @NotEmpty
    private String db_Url="";

    public Configuration() {

    }


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDb_Url() {
        return db_Url;
    }
    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public String getUmgebung() {
        return umgebung;
    }

    public Configuration(String land, String umgebung, String userName, String password, String db_Url) {
        this.land = land;
        this.umgebung = umgebung;
        this.userName = userName;
        this.password = password;
        this.db_Url = db_Url;
    }

    public void setUmgebung(String umgebung) {
        this.umgebung = umgebung;
    }
    public void setDb_Url(String db_Url) {
        this.db_Url = db_Url;
    }
}
