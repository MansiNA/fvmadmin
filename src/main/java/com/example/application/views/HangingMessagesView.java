package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.service.ConfigurationService;
import com.example.application.service.MessageStatus;
import com.jcraft.jsch.KeyPairGenXEC;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.security.RolesAllowed;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;


@PageTitle("Verwaltung hängende Nachrichten")
@Route(value = "hangingmassages", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class HangingMessagesView extends VerticalLayout {

    @Autowired
    JdbcTemplate jdbcTemplate;
    private ComboBox<Configuration> comboBox;
    CountDownLatch latch=new CountDownLatch(0);

    MessageStatus status;
    public HangingMessagesView(MessageStatus status, ConfigurationService conf_service) {
        this.status=status;

        comboBox = new ComboBox<>("Verbindung");
        comboBox.setItems(conf_service.findMessageConfigurations());
        comboBox.setItemLabelGenerator(com.example.application.data.entity.Configuration::get_Message_Connection);

        //    comboBox.setValue(service.findAllConfigurations().stream().findFirst().get());
        comboBox.setPlaceholder("auswählen");

        LogView lv = new LogView();

        add(new H2("Bearbeitung hängende Nachrichten"));

        String yourContent ="In dem folgenden Textfeld können Nachrichten vom Typ \"incoming\" eingegeben werden, die von der eKP erneut versendet werden sollen. <br />" +
                "Der Check-Button überprüft, ob die Nachricht-IDs valide sind.<br />" +
                "Mit dem Ausführen-Button werden die eingegebenen Nachrichten erneut versendet.<br />";
        Html html = new Html("<text>" + yourContent + "</text>");
        add(html);
        HorizontalLayout hl = new HorizontalLayout();

        //int charLimit = 1400;

        TextArea textArea = new TextArea();
        textArea.setLabel("NachrichtID-Extern:");
        //textArea.setMaxLength(charLimit);
        textArea.setWidth("500px");
       // textArea.setMaxLength(charLimit);
        textArea.setValueChangeMode(ValueChangeMode.EAGER);
        /*textArea.addValueChangeListener(e -> {
            e.getSource()
                    .setHelperText(e.getValue().length() + "/" + charLimit);
        });*/
        textArea.setValue("");


        Button executeBtn = new Button("Ausführen");
        Button checkBtn = new Button("Check");
        UI ui = UI.getCurrent();

        executeBtn.addClickListener(clickEvent -> {
            System.out.println("Button Ausführen clicked");
            status.setStatus(true);
            status.setMessages(textArea.getValue());
            textArea.setEnabled(false);
            executeBtn.setVisible(false);
            checkBtn.setVisible(false);
            lv.addLogMessage("Verarbeitung gestartet...");

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
                        lv.addLogMessage("beendet: " + line);


                        if (latch.getCount() == 0 && ! status.getStatus())
                        {
                            textArea.setEnabled(true);
                            executeBtn.setVisible(true);
                            checkBtn.setVisible(true);
                            lv.addLogMessage("fertig");
                            textArea.setValue("");
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



        checkBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        executeBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);


        hl.add(checkBtn,executeBtn);

        //lv.addLogMessage("");

        if(status.getStatus())
        {
            textArea.setEnabled(false);
            textArea.setValue(status.getMessages());
            hl.setVisible(false);
            lv.addLogMessage("ongoing");
        }

        add(comboBox,textArea,hl,lv);


    }

    private void resendMessage(String line,CountDownLatch latch ) {
        //private void resendMessage(String line) {

        //Check. ob NachrichtIDextern in Metadaten vorhanden ist:

        //Ausführen des HH_MESSAGE_RESEND('NachrichtIDExtern');
        // exec HH_MESSAGE_RESEND('StageMsg1681032256039954439fb-ca07-4546-9fd1-a601006876ab');
        try{
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
        }



    }

    private String infoText() {
        return String.format("Nachrichten werden erneut verarbeitet");
    }

}

