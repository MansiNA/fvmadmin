package com.example.application.data.entity;

import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotEmpty;

@Entity
public class Film extends AbstractEntity  {

    @NotEmpty
    private Integer Film_ID=0;

    @NotEmpty
    private String Film_Name= "";;

    public Integer getFilm_ID() {
        return Film_ID;
    }

    public void setFilm_ID(Integer film_ID) {
        Film_ID = film_ID;
    }


    public String getFilm_Name() {
        return Film_Name;
    }

    public void setFilm_Name(String film_Name) {
        Film_Name = film_Name;
    }
}
