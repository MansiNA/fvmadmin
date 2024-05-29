package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.service.ConfigurationService;
import com.example.application.service.MessageStatus;
import com.jcraft.jsch.KeyPairGenXEC;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@PageTitle("Verwaltung hängende Nachrichten")
@Route(value = "hangingmassages", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class HangingMessagesView extends VerticalLayout {

    @Autowired
    JdbcTemplate jdbcTemplate;
    private ComboBox<Configuration> comboBox;
    CountDownLatch latch=new CountDownLatch(0);
    LogView lv = new LogView();
    TextArea textArea = new TextArea();
    Button executeBtn = new Button("Ausführen");
    Button checkBtn = new Button("Check");
    MessageStatus status;
    HorizontalLayout hl = new HorizontalLayout();
    private static final Set<SerializableConsumer<String>> subscribers = new HashSet<>();
    private static final Set<SerializableConsumer<String>> start_subscribers = new HashSet<>();
    private static final ExecutorService notifierThread = Executors.newSingleThreadExecutor();
    private static final ExecutorService startnotifierThread = Executors.newSingleThreadExecutor();
    private SerializableConsumer<String> subscriber;

    private void updateSubscription() {
        UI ui = getUI().orElse(null);

        // Subscribe if checkbox is checked and view is attached
        if ( ui != null) {
            if (subscriber != null) {
                // Already subscribed
                return;
            }

            //subscriber = message -> ui.access(() -> Notification.show(message));
            //subscriber = message -> ui.access((()->  lv.addLogMessage(message)));

            subscriber = message -> {
                if (message=="Start")
                {
                    ui.access((()->  lv.addLogMessage(message)));
                    ui.access((()->  textArea.setEnabled(false)));
                    ui.access((()->  executeBtn.setEnabled(false)));
                    ui.access((()->  checkBtn.setEnabled(false)));
                    ui.access((()->  textArea.setValue("")));
                    ui.access((()->  textArea.setValue(status.getMessages())));

                }
                else if (message=="Fertig") {
                    ui.access((() -> lv.addLogMessage(message)));
                    ui.access((() -> textArea.setEnabled(true)));
                    ui.access((() -> executeBtn.setEnabled(true)));
                    ui.access((() -> checkBtn.setEnabled(true)));
                }
                else {
                    ui.access((()->  lv.addLogMessage(message)));
                }
                                    };


            synchronized (subscribers) {
                subscribers.add(subscriber);
            }
        } else {
            if (subscriber == null) {
                // Already unsubscribed
                return;
            }

            synchronized (subscribers) {
                subscribers.remove(subscriber);
            }
            subscriber = null;
        }
    }



    private static void notifySubscribers(String message) {
        Set<SerializableConsumer<String>> subscribersSnapshot;
        synchronized (subscribers) {
            subscribersSnapshot = new HashSet<>(subscribers);
        }

        for (SerializableConsumer<String> subscriber : subscribersSnapshot) {
            notifierThread.execute(
                    () -> {
                        try {
                            subscriber.accept(message);


                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
            );
        }
    }



    public HangingMessagesView(MessageStatus status, ConfigurationService conf_service) {

        addAttachListener(event -> updateSubscription());
        addDetachListener(event -> updateSubscription());

        Button makeChanges = new Button(
                "Notify subscribers",
                clickEvent -> notifySubscribers("This is a notification triggerd by the button")
        );

        makeChanges.addClickListener(clickEvent -> {
            System.out.println("Notifier gedrückt!");
        });

//        add( makeChanges);

        this.status=status;

        comboBox = new ComboBox<>("Verbindung");
        List<Configuration> configList = conf_service.findMessageConfigurations();

        comboBox.setItems(configList);
        comboBox.setItemLabelGenerator(Configuration::getName);

       // comboBox.setValue(conf_service.findAllConfigurations().stream().findFirst().get());

        comboBox.setValue(configList.get(1) );


        add(new H2("Bearbeitung hängende Nachrichten"));

        String yourContent ="In dem folgenden Textfeld können Nachrichten vom Typ \"incoming\" eingegeben werden, die von der eKP erneut versendet werden sollen. <br />" +
                "Der Check-Button überprüft, ob die Nachricht-IDs valide sind.<br />" +
                "Mit dem Ausführen-Button werden die eingegebenen Nachrichten erneut versendet.<br />";
        Html html = new Html("<text>" + yourContent + "</text>");
        add(html);


        //int charLimit = 1400;


        textArea.setLabel("NachrichtID-Extern:");
        //textArea.setMaxLength(charLimit);
        textArea.setWidth("600px");
       // textArea.setMaxLength(charLimit);
        textArea.setValueChangeMode(ValueChangeMode.EAGER);
        /*textArea.addValueChangeListener(e -> {
            e.getSource()
                    .setHelperText(e.getValue().length() + "/" + charLimit);
        });*/
        textArea.setValue("");
        textArea.addValueChangeListener( e -> {
            //System.out.println("IDs wurden eingegeben");
            executeBtn.setEnabled(true);
            checkBtn.setEnabled(true);
        });


        UI ui = UI.getCurrent();

        executeBtn.addClickListener(clickEvent -> {
            System.out.println("Button Ausführen clicked");
            //start();
            notifySubscribers("Start");
            status.setStatus(true);
            status.setMessages(textArea.getValue());
         //   textArea.setEnabled(false);
           // executeBtn.setVisible(false);
           // checkBtn.setVisible(false);
//            executeBtn.setEnabled(false);
//            checkBtn.setEnabled(false);

         //   lv.addLogMessage("Verarbeitung gestartet...");

            String[] lines = textArea.getValue().split("\\n");

            latch = new CountDownLatch(lines.length);

            for (String line : lines) {

              /*  try {
                    Thread.sleep(2000); //5 Sekunden warten
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }*/

                // Hintergrundaufgabe erstellen
                Runnable task = () -> {
                    // Hintergrundaufgabe ausführen, die möglicherweise längere Zeit in Anspruch nimmt
                    resendMessage(line,latch);
                  //  resendMessage(line);

                    // Benutzeroberfläche aktualisieren
                    ui.access(() -> {
                        // Hier können Sie auf die Benutzeroberfläche zugreifen und sie aktualisieren
                        //lv.addLogMessage("beendet: " + line);
                     //   notifySubscribers("NachrichtID beendet: " + line);

                        if (latch.getCount() == 0 && ! status.getStatus())
                        {
                           notifySubscribers("Fertig");
                           //fertig("Job ist fertig geworden!");
                        }

                    });
                };

                // Hintergrundaufgabe im Hintergrund-Thread ausführen
                new Thread(task).start();

            }

           /* // Warten Sie auf das Ende aller Tasks
            try {
                latch.await();
                status.setStatus(false);
                textArea.setValue("");
                textArea.setEnabled(true);
                hl.setVisible(true);
                lv.addLogMessage("fertig...");
                System.out.println("Alle Tasks sind beendet!");
            } catch (InterruptedException e) {
                // Handle die Exception
            }*/

        });

        checkBtn.addClickListener(clickEvent -> {
            System.out.println("Button Check clicked");
            //start();
            notifySubscribers("Start");
            status.setStatus(true);
            status.setMessages(textArea.getValue());

            String[] lines = textArea.getValue().split("\\n");

            latch = new CountDownLatch(lines.length);

            for (String line : lines) {


                // Hintergrundaufgabe erstellen
                Runnable task = () -> {
                    // Hintergrundaufgabe ausführen, die möglicherweise längere Zeit in Anspruch nimmt
                    checkMessage(line,latch);
                    //  resendMessage(line);

                    // Benutzeroberfläche aktualisieren
                    ui.access(() -> {
                        // Hier können Sie auf die Benutzeroberfläche zugreifen und sie aktualisieren
                        //lv.addLogMessage("beendet: " + line);
                        //   notifySubscribers("NachrichtID beendet: " + line);

                        if (latch.getCount() == 0 && ! status.getStatus())
                        {
                            notifySubscribers("Fertig");
                            //fertig("Job ist fertig geworden!");
                        }

                    });
                };

                // Hintergrundaufgabe im Hintergrund-Thread ausführen
                new Thread(task).start();

            }

        });

        checkBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        executeBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY,ButtonVariant.LUMO_ERROR);

        executeBtn.setEnabled(false);
        checkBtn.setEnabled(false);

        hl.add(checkBtn,executeBtn);

        //lv.addLogMessage("");

        if(status.getStatus())
        {
            textArea.setEnabled(false);
            textArea.setValue(status.getMessages());
            checkBtn.setEnabled(false);
            executeBtn.setEnabled(false);
            lv.addLogMessage("Job läuft aktuell noch...");
        }

        add(comboBox,textArea,hl,lv);


    }

    private void fertig(String message) {
        textArea.setEnabled(true);
        executeBtn.setEnabled(true);
        checkBtn.setEnabled(true);
        lv.addLogMessage(message);
        textArea.setValue("");
    }

    private void start() {
        textArea.setEnabled(false);
        executeBtn.setEnabled(false);
        checkBtn.setEnabled(false);
        notifySubscribers("Start");
    }

    private ComponentEventListener<ClickEvent<Button>> refresher() {
        System.out.println("Huhu");
        return null;
    }

    private void checkMessage(String line,CountDownLatch latch ) {
        System.out.println("Check NachrichtenID: " + line );

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));
        try {

            jdbcTemplate.setDataSource(ds);

            Connection connection = DataSourceUtils.getConnection((jdbcTemplate.getDataSource()));
            CallableStatement statement = connection.prepareCall("{call ekp.HH_MESSAGE_RESEND(?,?,?) }");
            statement.setString(1,line);
            statement.setString(2,"check");
            statement.registerOutParameter(3, Types.VARCHAR);

            statement.executeUpdate();

            var xx = statement.getNString((3));
            //jdbcTemplate.execute("call ekp.HH_MESSAGE_RESEND('" + line + "')");
            notifySubscribers(line + " => " + xx );

            latch.countDown();
            if (latch.getCount() == 0)
            {
                System.out.println("FERTIG!");
                status.setStatus(false);
                // lv.addLogMessage("fertig");
            }

        } catch (Exception e) {
            latch.countDown();
            status.setStatus(false);
            System.out.println("Exception in HangingMessagesView.resendMessage: " + e.getMessage());
            notifySubscribers("Fehler: " + e.getMessage());
        }



        /*try{
            Random random = new Random();
            int randomNumber = random.nextInt(20) + 1;
            Thread.sleep(randomNumber * 1000); //1-20 Sekunden warten
            System.out.println("Verarbeite NachrichtenID: " + line );
            latch.countDown();
            if (latch.getCount() == 0)
            {
                System.out.println("FERTIG!");
                status.setStatus(false);
               // lv.addLogMessage("fertig");
            }
        }
        catch(InterruptedException e){
            throw new RuntimeException(e);
        }*/



    }

    private void resendMessage(String line,CountDownLatch latch ) {
        System.out.println("Verarbeite NachrichtenID: " + line );
        //Ausführen des HH_MESSAGE_RESEND('NachrichtIDExtern');
        // exec HH_MESSAGE_RESEND('StageMsg1681032256039954439fb-ca07-4546-9fd1-a601006876ab');
        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));
        try {

            jdbcTemplate.setDataSource(ds);

            Connection connection = DataSourceUtils.getConnection((jdbcTemplate.getDataSource()));
            CallableStatement statement = connection.prepareCall("{call ekp.HH_MESSAGE_RESEND(?,?,?) }");
            statement.setString(1,line);
            statement.setString(2,"go");
            statement.registerOutParameter(3, Types.VARCHAR);

            statement.executeUpdate();

            var xx = statement.getNString((3));
            //jdbcTemplate.execute("call ekp.HH_MESSAGE_RESEND('" + line + "')");
            notifySubscribers(line + " => " + xx );

            latch.countDown();
            if (latch.getCount() == 0)
            {
                System.out.println("FERTIG!");
                status.setStatus(false);
                // lv.addLogMessage("fertig");
            }

        } catch (Exception e) {
            latch.countDown();
            status.setStatus(false);
            System.out.println("Exception in HangingMessagesView.resendMessage: " + e.getMessage());
            notifySubscribers("Fehler: " + e.getMessage());
        }



        /*try{
            Random random = new Random();
            int randomNumber = random.nextInt(20) + 1;
            Thread.sleep(randomNumber * 1000); //1-20 Sekunden warten
            System.out.println("Verarbeite NachrichtenID: " + line );
            latch.countDown();
            if (latch.getCount() == 0)
            {
                System.out.println("FERTIG!");
                status.setStatus(false);
               // lv.addLogMessage("fertig");
            }
        }
        catch(InterruptedException e){
            throw new RuntimeException(e);
        }*/



    }

    private String infoText() {
        return String.format("Nachrichten werden erneut verarbeitet");
    }

}

