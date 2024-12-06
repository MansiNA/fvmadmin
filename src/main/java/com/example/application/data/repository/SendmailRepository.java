package com.example.application.data.repository;

import com.example.application.data.entity.FVMSendmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SendmailRepository extends JpaRepository<FVMSendmail, Long>, JpaSpecificationExecutor<FVMSendmail> {
}
