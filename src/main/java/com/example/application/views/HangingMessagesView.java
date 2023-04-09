package com.example.application.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import javax.annotation.security.RolesAllowed;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@PageTitle("Verwaltung hängende Nachrichten")
@Route(value = "hangingmassages", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class HangingMessagesView extends VerticalLayout {


    public HangingMessagesView() {

        LogView lv = new LogView();

        add(new H2("Bearbeitung hängende Nachrichten"));

        int charLimit = 1400;

        TextArea textArea = new TextArea();
        textArea.setLabel("NachrichtID-Extern");
        textArea.setMaxLength(charLimit);
        textArea.setWidth("600px");
       // textArea.setMaxLength(charLimit);
        textArea.setValueChangeMode(ValueChangeMode.EAGER);
        textArea.addValueChangeListener(e -> {
            e.getSource()
                    .setHelperText(e.getValue().length() + "/" + charLimit);
        });
        textArea.setValue("Great job. This is excellent!");


        Button exeuteBtn = new Button("Ausführen");
        UI ui = UI.getCurrent();

        exeuteBtn.addClickListener(clickEvent -> {
            System.out.println("Nachrichten erneut verarbeiten geclicked");

            String[] lines = textArea.getValue().split("\\n");

            for (String line : lines) {

                try{

                    Thread.sleep(2000); //5 Sekunden warten
                }
                catch(InterruptedException e){
                    throw new RuntimeException(e);
                }

        //        System.out.println("Verarbeite Nachricht: " + line);


                // Hintergrundaufgabe erstellen
                Runnable task = () -> {
                    // Hintergrundaufgabe ausführen, die möglicherweise längere Zeit in Anspruch nimmt
                    resendMessage(line);

                    // Benutzeroberfläche aktualisieren
                    ui.access(() -> {
                        // Hier können Sie auf die Benutzeroberfläche zugreifen und sie aktualisieren
                        lv.addLogMessage("Verarbeite Nachricht: " + line);
                    });
                };

                // Hintergrundaufgabe im Hintergrund-Thread ausführen
                new Thread(task).start();

            }

        });

        exeuteBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY,
                ButtonVariant.LUMO_ERROR);

        //lv.addLogMessage("");

        add(textArea,exeuteBtn,lv);


    }

    private void resendMessage(String line) {

        //Check. ob NachrichtIDextern in Metadaten vorhanden ist:

        //Ausführen des HH_MESSAGE_RESEND('NachrichtIDExtern');
        // exec HH_MESSAGE_RESEND('StageMsg1681032256039954439fb-ca07-4546-9fd1-a601006876ab');
        try{

            Thread.sleep(5000); //5 Sekunden warten
            System.out.println("Verarbeite NachrichtenID: " + line );

        }
        catch(InterruptedException e){
            throw new RuntimeException(e);
        }



    }

    private String infoText() {
        return String.format("Nachrichten werden erneut verarbeitet");
    }

}

