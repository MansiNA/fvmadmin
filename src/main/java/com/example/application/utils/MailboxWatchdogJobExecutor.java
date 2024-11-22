package com.example.application.utils;

import com.example.application.data.service.MailboxService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import org.springframework.stereotype.Component;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.Mailbox;
import com.example.application.data.service.ConfigurationService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class MailboxWatchdogJobExecutor implements Job {

    private MailboxService mailboxService;
    private ConfigurationService configurationService;
    private Configuration configuration;
    private static final Logger logger = LoggerFactory.getLogger(MailboxWatchdogJobExecutor.class);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Executing MailboxWatchdogJobExecutor...");

        mailboxService = SpringContextHolder.getBean(MailboxService.class);
        configurationService = SpringContextHolder.getBean(ConfigurationService.class);
        String jobDefinitionString = context.getMergedJobDataMap().getString("configuration");

        try {
            configuration = JobDefinitionUtils.deserializeJobConfDefinition(jobDefinitionString);
            executeJob(configuration);
        } catch (JsonProcessingException e) {
            throw new JobExecutionException("Error deserializing job definition", e);
        }
        logger.info("Finished executing MailboxWatchdogJobExecutor.");

    }
    private void executeJob(Configuration configuration) {
        if (configuration == null) {
            logger.error("Configuration is null. Exiting MailboxWatchdogJobExecutor.");
            return;
        }

        List<Mailbox> mailboxes = mailboxService.getMailboxes(configuration);
        if (mailboxes == null || mailboxes.isEmpty()) {
            logger.info("No mailboxes found for "+configuration.getUserName());
            return;
        }


        for (Mailbox mailbox : mailboxes) {
            try {
                checkAndUpdateMailboxStatus(mailbox);
            } catch (Exception e) {
                logger.error("Error processing mailbox {}: {}", mailbox.getNAME(), e.getMessage());
            }
        }


    }

    private void checkAndUpdateMailboxStatus(Mailbox mailbox) {
        int inVerarbeitung = Integer.parseInt(mailbox.getAktuell_in_eKP_verarbeitet()); // Current "In Verarbeitung" value
        int maxMessageCount = Integer.parseInt(mailbox.getMAX_MESSAGE_COUNT()); // Maximum allowed message count

        // Check if mailbox needs to be disabled
        if (inVerarbeitung > maxMessageCount) {
            String result = mailboxService.updateMessageBox(mailbox,"1", configuration);
            if(result.equals("Ok")) {
                logger.info("Disabling mailbox {} due to exceeding max message count.", mailbox.getNAME());
            } else {
                logger.error("Error Disabling mailbox "+ mailbox.getNAME()+": " + result );
            }
        } else {
            logger.info("Re-enabling mailbox {} as processing count is within limits.", mailbox.getNAME());
        }
    }
}
