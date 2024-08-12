package com.example.application.service;

import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender emailSender;


    public EmailServiceImpl(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    public void sendSimpleMessage(String to, String subject, String text){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("m.quaschny@dataport.de");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }

    @Override
    public void sendAttachMessage(String to, String subject, String text, String attachement) {
        MimeMessagePreparator preparator = mimeMessage -> {
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mimeMessage.setFrom(new InternetAddress("noreplay@dataport.de"));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(text);

            //String attachment = "test.csv";
            try{

                FileSystemResource file = new FileSystemResource(new File(attachement));
                if (file.exists()){
                    MimeMessageHelper helper= new MimeMessageHelper(mimeMessage,true);
                    helper.addAttachment(file.getFilename(), file);
                    helper.setText("",true);
                } else {
                    System.out.println("File not Found: " + attachement);
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }

        };

        emailSender.send (preparator);
    }

    public void sendAttachMessage(String to, String subject, String text, String attachmentFileName, ByteArrayResource byteArrayResource) {
        MimeMessagePreparator preparator = mimeMessage -> {
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
            mimeMessage.setFrom(new InternetAddress("noreplay@dataport.de"));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(text);

            try {
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.addAttachment(attachmentFileName, byteArrayResource);
                helper.setText(text, true);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        };

        emailSender.send(preparator);
    }


}
