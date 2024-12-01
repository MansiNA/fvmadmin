package com.example.application.utils;


import com.example.application.data.entity.MailboxShutdown;
import com.example.application.data.service.MailboxService;
import com.example.application.data.service.ProtokollService;
import com.example.application.views.MailboxWatcher;
import com.example.application.views.MainLayout;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.vaadin.flow.component.UI;
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
import static com.example.application.Application.mailboxen;

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
        logger.debug("Execute MailboxWatchdogJobExecutor...");

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
        logger.debug("Finished executing MailboxWatchdogJobExecutor...");

    }
    private void executeJob(Configuration configuration) {
        if (configuration == null) {
            logger.error("Configuration is null. Exiting MailboxWatchdogJobExecutor.");
            return;
        }

        mailboxen = mailboxService.getMailboxes(configuration);

        if (mailboxen == null || mailboxen.isEmpty()) {
            logger.error("No mailboxes found for " + configuration.getUserName());
            return;
        }

        for (Mailbox mailbox : mailboxen) {
            try {
                int x = checkAndUpdateMailboxStatus(mailbox);
                if (x==1) //If mailbox set offline
                {
                    mailboxen.stream().filter(m -> m.getUSER_ID() == mailbox.getUSER_ID()).findFirst().ifPresent(m -> m.setQUANTIFIER(0));
                    //mailboxen = mailboxService.getMailboxes(configuration);
                    MailboxWatcher.notifySubscribers("Update grid");
                }
                if (x==2) //If mailbox set online
                {
                    mailboxen.stream().filter(m -> m.getUSER_ID() == mailbox.getUSER_ID()).findFirst().ifPresent(m -> m.setQUANTIFIER(1));
                    //mailboxen = mailboxService.getMailboxes(configuration);
                    MailboxWatcher.notifySubscribers("Update grid");
                }

            } catch (Exception e) {
                logger.error("Error processing mailbox {}: {}", mailbox.getNAME(), e.getMessage());
            }
        }

        MailboxWatcher.notifySubscribers("Update grid");

    }

    private int checkAndUpdateMailboxStatus(Mailbox mailbox) {
        logger.info("Executing checkAndUpdateMailboxStatus");
        logger.debug("Value of watchdog stopJob: " + stopJob);
        if (stopJob) {
            return 0; // Exit if the job is stopped
        }

        int inVerarbeitung = Integer.parseInt(mailbox.getAktuell_in_eKP_verarbeitet()); // Current "In Verarbeitung" value
        int maxMessageCount = Integer.parseInt(mailbox.getMAX_MESSAGE_COUNT()); // Maximum allowed message count
        boolean isDisabled = mailbox.getQUANTIFIER() == 0;
        logger.info("-----------Check MB " + mailbox.getNAME() + "---------------");
        logger.info("Mailbox {} (with maxMessageCount {}) has active Messages: {}", mailbox.getNAME(), maxMessageCount, inVerarbeitung);

        MailboxShutdown mailboxShutdown = new MailboxShutdown();
        mailboxShutdown.setMailboxId(mailbox.getCOURT_ID());
        boolean exists = globalList.stream().anyMatch(m -> mailboxShutdown.getMailboxId().equals(m.getMailboxId()));
        //logger.info("MB exists in globalList? " + exists);
        //logger.info("globalList has {} entries.", globalList.stream().count());

        if (inVerarbeitung > maxMessageCount) {
            if (!isDisabled) {
                disableMailbox(mailbox, inVerarbeitung, maxMessageCount, mailboxShutdown);
                return 1;


            } else {
                logger.info("Mailbox {} is already disabled.", mailbox.getNAME());
                return 0;
            }
        }

        if (inVerarbeitung <= maxMessageCount && exists)  //Schwellwert unterschritten und war nicht von Watchdog disabled
        {
            if (isDisabled) {
                enableMailbox(mailbox,inVerarbeitung,maxMessageCount);
                return 2;
            } else {
                logger.info("Mailbox {} is already active.", mailbox.getNAME());
            }
        }
        else
        {
            if(!isDisabled)
            {
                logger.info("Mailbox {} is already enabled", mailbox.getNAME());
            }
            else
            {
                logger.info("Mailbox {} was not stopped by watchdog, skipping reactivation.", mailbox.getNAME());
            }

        }


        return 0;
    }

    private void disableMailbox(Mailbox mailbox, int inVerarbeitung, int maxMessageCount, MailboxShutdown mailboxShutdown) {
        logger.info("disableMailbox: Shutdown mailbox {} due to exceeding max message count!", mailbox.getNAME());

        String result = mailboxService.updateMessageBox(mailbox,"0", configuration);
        if(result.equals("Ok")) {
            // affectedMailboxes.add(mb);
            globalList.add(mailboxShutdown);
           // MailboxWatcher.notifySubscribers(mailbox.getUSER_ID() +",, 0,,"+configuration.getId());
            MailboxWatcher.notifySubscribers("Update grid");
            //         applicationContextStorage.getGlobalList().add(mb);
            protokollService.logAction("watchdog" ,configuration.getName(), mailbox.getUSER_ID()+" wurde ausgeschaltet", "active messages " + inVerarbeitung + " exceeded " + maxMessageCount);
            logger.info("Add Mailbox to globalList. Entries now:" + globalList.stream().count());

        } else {
            logger.error("Error Disabling mailbox "+ mailbox.getNAME()+": " + result );
        }
    }

    private void enableMailbox(Mailbox mailbox, int inVerarbeitung, int maxMessageCount) {
        logger.info("Mailbox {} below max message count...", mailbox.getNAME());

        logger.info("Mailbox stopped by watchdog => switch back to active");
            String result = mailboxService.updateMessageBox(mailbox,"1", configuration);
            if(result.equals("Ok")) {
                logger.info("Mailbox {} enabled successfully.", mailbox.getNAME());
             //   MailboxWatcher.notifySubscribers(mailbox.getUSER_ID() +",, 1,,"+configuration.getId());
                MailboxWatcher.notifySubscribers("Update grid");
                protokollService.logAction("watchdog" ,configuration.getName(), mailbox.getUSER_ID()+" wurde eingschaltet", "active messages " + inVerarbeitung + " below " + maxMessageCount);
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
                logger.error("Failed to enable mailbox {}: {}", mailbox.getNAME(), result);
            }
        }


    }

