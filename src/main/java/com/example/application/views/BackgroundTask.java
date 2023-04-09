package com.example.application.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Paragraph;

public class BackgroundTask implements Runnable {

    String Message="";
    Paragraph info_text;
    UI ui;
    public BackgroundTask(UI ui, Paragraph info,String message) {
        Message=message;
        info_text=info;
        this.ui=ui;
    }

    @Override
    public void run() {

        try{

            Thread.sleep(5000); //5 Sekunden warten
            System.out.println("Verarbeite NachrichtenID: " + Message );
            ui.access(
                    () -> {
                       info_text.setText("Verarbeite: " + Message);
                       ui.push();
                    }

            );

        }
        catch(InterruptedException e){
            throw new RuntimeException(e);
        }

    }


}
