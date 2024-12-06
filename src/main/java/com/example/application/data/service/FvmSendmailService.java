package com.example.application.data.service;

import com.example.application.data.entity.FVMSendmail;
import com.example.application.data.repository.SendmailRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FvmSendmailService {

    private final SendmailRepository repository;


    public FvmSendmailService(SendmailRepository repository) {
        this.repository = repository;
    }

    public List<FVMSendmail> findAll() {
        List<FVMSendmail> fvmSendmails = repository.findAll();
        System.out.println("fvmSendmails size = " + fvmSendmails.size());
        fvmSendmails.forEach(f -> System.out.println(f.toString())); // Log details of each item
        return fvmSendmails;
    }

    public FVMSendmail findById(Long entryTyp) {
        Optional<FVMSendmail> sendmail = repository.findById(entryTyp);
        return sendmail.orElse(null);
    }

    public FVMSendmail save(FVMSendmail sendmail) {
        return repository.save(sendmail);
    }

    public void delete(Long sendmailId) {
        if (sendmailId == null) {
            System.err.println("ID is null!");
            return;
        }

        repository.deleteById(sendmailId);
    }
}
