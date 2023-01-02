package com.example.application.data.service;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.Mailbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.ArrayList;
import java.util.List;

public class DataService {

    @Autowired
    static
    JdbcTemplate jdbcTemplate;
    public static List<Mailbox> mygetMailboxes(Configuration conf) {


        List<Mailbox> mailboxen = new ArrayList<Mailbox>();

        /*Mailbox p = new Mailbox();
        p.setCourt_ID("K1101");
        p.setQuantifier(1);
        p.setTyp("eVvD");
        p.setName("Amtsgericht Hamburg");
        p.setUser_ID("safe-sp1-147676755-90078127");
        p.setKonvertierungsdienste("1");
        mailboxen.add(p);

        Mailbox t = new Mailbox();
        t.setName("Landgericht Bremerhaven");

        t.setCourt_ID("K1221");
        t.setQuantifier(0);
        t.setTyp("ZENVG");
        t.setUser_ID("safe-sp1-1qw3eq3drqw3dr-wef127");
        t.setKonvertierungsdienste("0");
        mailboxen.add(t);*/


        String sql = "select name,court_id,quantifier, user_id,typ,konvertierungsdienste from EKP.MAILBOX_CONFIG";

        System.out.println("Abfrage EKP.Mailbox_Config");

        DriverManagerDataSource ds = new DriverManagerDataSource();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(conf.getPassword());

        //ds.setUrl("jdbc:oracle:thin:@37.120.189.200:1521:xe");
        //ds.setUsername("SYSTEM");
        //ds.setPassword("Michael123");

        try {

            jdbcTemplate.setDataSource(ds);

            mailboxen = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Mailbox.class));



            System.out.println("MAILBOX_CONFIG eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }

        return mailboxen;
    }

}


