package com.example.application.data.service;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.Metadaten;
import com.example.application.data.repository.MetadatenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MetadatenService {

    private final MetadatenRepository metadatenRepository;
    @Autowired
    static
    JdbcTemplate jdbcTemplate;

    public MetadatenService(MetadatenRepository metadatenRepository) {
        this.metadatenRepository = metadatenRepository;
    }

    public List<Metadaten> findAllMetadaten(Configuration conf, String stringFilter) {

        List<Metadaten> metadaten = new ArrayList<Metadaten>();

        String sql = "select * from EKP.METADATEN";

        System.out.println("Abfrage EKP.Metadaten");

        DriverManagerDataSource ds = new DriverManagerDataSource();

        //ds.setUrl(conf.getDb_Url());
        //ds.setUsername(conf.getUserName());
        //ds.setPassword(conf.getPassword());

        ds.setUrl("jdbc:oracle:thin:@37.120.189.200:1521:xe");
        ds.setUsername("SYSTEM");
        ds.setPassword("Michael123");

        try {

            jdbcTemplate.setDataSource(ds);

            metadaten = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Metadaten.class));



            System.out.println("Metadaten eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }

        return metadaten;


    }

}
