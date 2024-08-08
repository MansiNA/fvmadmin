package com.example.application.utils;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.PrePersist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.Properties;

@Component
public class EMailVersenden {
    @Value("${mail.smtp.host}")
    private String smtpHost;

    @Value("${mail.smtp.port}")
    private int smtpPort;

    @Value("${mail.smtp.auth}")
    private boolean smtpAuth;

    @Value("${mail.smtp.starttls.enable}")
    private boolean starttlsEnable;

    @Value("${mail.username}")
    private  String absender;

    @Value("${mail.password}")
    private String password;


    public void versendeEMail(String betreff, String inhalt, String empfaenger, File attachment) throws MessagingException {
        System.out.println("username"+ absender);

        Properties properties = new Properties();
        properties.setProperty("mail.smtp.host", smtpHost);
        properties.setProperty("mail.smtp.port", String.valueOf(smtpPort));
        properties.setProperty("mail.smtp.auth", String.valueOf(smtpAuth));
        properties.setProperty("mail.smtp.starttls.enable", String.valueOf(starttlsEnable));
        properties.setProperty("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
//        properties.setProperty("mail.smtp.host", smtpHost);
//        properties.setProperty("mail.smtp.port", String.valueOf(smtpPort));
//        properties.setProperty("mail.smtp.auth", String.valueOf(smtpAuth));
//        properties.setProperty("mail.smtp.starttls.enable", String.valueOf(starttlsEnable));

        Session session = Session.getInstance(properties, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(absender, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(absender));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(empfaenger));
        message.setSubject(betreff, "ISO-8859-1");

        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(inhalt);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);

        if (attachment != null && attachment.exists()) {
            messageBodyPart = new MimeBodyPart();
            DataSource source = new FileDataSource(attachment);
            messageBodyPart.setDataHandler(new javax.activation.DataHandler(source));
            messageBodyPart.setFileName(attachment.getName());
            multipart.addBodyPart(messageBodyPart);
        }

        message.setContent(multipart);

        Transport.send(message);
        System.out.println("send email...............");
    }

    public void versendeEMail( String betreff, String inhalt, String empfaenger) throws MessagingException, AddressException {
        System.out.println("username"+ absender);
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", smtpHost);
        Session session = Session.getDefaultInstance( properties );
        MimeMessage message = new MimeMessage( session );
        message.setFrom( new InternetAddress( absender ) );
        message.addRecipient( Message.RecipientType.TO, new InternetAddress( empfaenger ) );
        message.setSubject( betreff, "ISO-8859-1" );
        message.setText( inhalt, "UTF-8" );
        Transport.send( message );
    }
}
