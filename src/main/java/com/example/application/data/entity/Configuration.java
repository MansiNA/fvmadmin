package com.example.application.data.entity;

import javax.persistence.Entity;
import javax.validation.constraints.NotEmpty;

@Entity
public class Configuration extends AbstractEntity{

    public String getLand() {
        return Land;
    }

    public void setLand(String land) {
        Land = land;
    }

    public String getUmgebung() {
        return Umgebung;
    }

    public void setUmgebung(String umgebung) {
        Umgebung = umgebung;
    }

    @NotEmpty
    private String Land="";

    @NotEmpty
    private String Umgebung="";
    @NotEmpty
    private String userName="";
    @NotEmpty
    private String password="";

    private String db_Url="";

    public Configuration() {

    }
    public Configuration(String userName, String password, String db_Url) {
        this.userName = userName;
        this.password = password;
        this.db_Url = db_Url;
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

    public void setDb_Url(String db_Url) {
        this.db_Url = db_Url;
    }
}
