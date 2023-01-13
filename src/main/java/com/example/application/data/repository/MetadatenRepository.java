package com.example.application.data.repository;

import com.example.application.data.entity.Metadaten;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MetadatenRepository  extends JpaRepository<Metadaten, UUID>{


        @Query("select c from Metadaten c " +
                "where lower(c.NACHRICHTIDINTERN) = :searchTerm " +
                "or lower(c.NACHRICHTIDEXTERN) like lower(concat('%', :searchTerm, '%'))")
        List<Metadaten> search(@Param("searchTerm") String searchTerm);


    }
