package com.example.application.utils;


import com.example.application.data.entity.MailboxShutdown;
import com.example.application.data.service.MailboxService;
import com.example.application.data.service.ProtokollService;
import com.example.application.views.MailboxWatcher;
import com.example.application.views.MainLayout;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.Mailbox;
import com.example.application.data.service.ConfigurationService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.example.application.Application.globalList;

@Component
public class MailboxWatchdogJobExecutor implements Job {

    private MailboxService mailboxService;
    private ProtokollService protokollService;
    private ConfigurationService configurationService;
    private Configuration configuration;
    private final List<MailboxShutdown> affectedMailboxes = Collections.synchronizedList(new ArrayList<>());
    public static boolean stopJob = false;
    private static final Logger logger = LoggerFactory.getLogger(MailboxWatchdogJobExecutor.class);


    //private final ApplicationContextStorage applicationContextStorage;


   // public MailboxWatchdogJobExecutor(ApplicationContextStorage applicationContextStorage) {
   //     this.applicationContextStorage = applicationContextStorage;
   // }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("Executing MailboxWatchdogJobExecutor...");

        mailboxService = SpringContextHolder.getBean(MailboxService.class);
        protokollService = SpringContextHolder.getBean(ProtokollService.class);
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
        logger.info("Executing checkAndUpdateMailboxStatus");
        logger.info("Value of watchdog stopJob: " + stopJob);
        if (stopJob) {
            return; // Exit if the job is stopped
        }

        int inVerarbeitung = Integer.parseInt(mailbox.getAktuell_in_eKP_verarbeitet()); // Current "In Verarbeitung" value
        int maxMessageCount = Integer.parseInt(mailbox.getMAX_MESSAGE_COUNT()); // Maximum allowed message count

        logger.info("-----------Check MB " + mailbox.getNAME() + "---------------");

        if (inVerarbeitung > maxMessageCount && mailbox.getQUANTIFIER()==0)
        {
            logger.info("Mailbox {} allready disabled...", mailbox.getNAME());
            return;
        }

        logger.info("Mailbox {} (with maxMessageCount {}) has active Messages: {}", mailbox.getNAME(), maxMessageCount, inVerarbeitung);

        MailboxShutdown mb = new MailboxShutdown();
        mb.setMailboxId(mailbox.getCOURT_ID());

        boolean exists = globalList.stream().anyMatch(m -> mb.getMailboxId().equals(m.getMailboxId()));

        //logger.info("MB exists in globalList? " + exists);
        //logger.info("globalList has {} entries.", globalList.stream().count());

        // Check if mailbox needs to be disabled


        if (inVerarbeitung > maxMessageCount && mailbox.getQUANTIFIER()==1 && !exists) {
            logger.info("Shutdown mailbox {} due to exceeding max message count!", mailbox.getNAME());

            String result = mailboxService.updateMessageBox(mailbox,"0", configuration);
            if(result.equals("Ok")) {

               // affectedMailboxes.add(mb);
                globalList.add(mb);

       //         applicationContextStorage.getGlobalList().add(mb);
                protokollService.logAction("watchdog" ,configuration.getName(), mailbox.getUSER_ID()+" wurde ausgeschaltet", "max_message_count "+inVerarbeitung+" of "+maxMessageCount+" exceeded");
                logger.info("Add Mailbox to globalList. Entries now:" + globalList.stream().count());

            } else {
                logger.error("Error Disabling mailbox "+ mailbox.getNAME()+": " + result );
            }
        } else {
            logger.info("Mailbox {} below max message count...", mailbox.getNAME());

            if (mailbox.getQUANTIFIER()==1)
            {
                logger.info("Mailbox {} already active...", mailbox.getNAME());
                return;
            }

            if (exists) {
                logger.info("Mailbox stopped by watchdog => switch back to active");
                String result = mailboxService.updateMessageBox(mailbox,"1", configuration);
                if(result.equals("Ok")) {
                    logger.info("Mailbox {} enabled successfully.", mailbox.getNAME());
                    protokollService.logAction("watchdog" ,configuration.getName(), mailbox.getUSER_ID()+" wurde eingschaltet", "max_message_count "+inVerarbeitung+" of "+maxMessageCount+" exceeded");
                    //remove Mailbox from internal list
                    Iterator<MailboxShutdown> iterator = globalList.iterator();
                    while (iterator.hasNext()){
                        MailboxShutdown mbElement = iterator.next();
                        if (mbElement.getMailboxId().equals(mailbox.getCOURT_ID()))
                        {
                            iterator.remove();
                        }
                    }


                } else {
                    logger.error("Error Disabling mailbox "+ mailbox.getNAME()+": " + result );
                }
            }
            else  {

                logger.info("Mailbox not stopped by watchdog => skip switch back");
            }


        }
    }
}
