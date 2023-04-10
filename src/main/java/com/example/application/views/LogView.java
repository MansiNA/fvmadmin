package com.example.application.views;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LogView extends VerticalLayout {

 /*   public LogView() {
        setDefaultHorizontalComponentAlignment(Alignment.STRETCH);
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        setHeightFull();
    }

    public void addLogMessage(String message) {
        Label logLabel = new Label(message);
        logLabel.getStyle().set("white-space", "pre-wrap"); // ermöglicht Zeilenumbrüche
        logLabel.getStyle().set("font-family", "monospace"); // für gleichmäßige Schriftart
        add(logLabel);
        setFlexGrow(1, logLabel); // um den Platz innerhalb des Layouts zu füllen
        getElement().executeJs("window.scrollTo(0, document.body.scrollHeight);"); // scrollt zur letzten Zeile
    }*/

    private final TextArea logArea;

    public LogView() {
        logArea = new TextArea();
        logArea.setReadOnly(true);
        logArea.getStyle().set("maxHeight", "250px");
        logArea.getStyle().set("overflow", "auto");
        logArea.setWidthFull();
        logArea.addClassName("logArea");
        //logArea.getStyle().set("border-style","solid");
        add(logArea);
    }

    public void addLogMessage(String message) {
        LocalTime currentTime = LocalTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String formattedTime = currentTime.format(formatter);

      //  logArea.setValue(logArea.getValue() + "\n" + formattedTime + ": " + message);
          logArea.setValue(formattedTime + ": " + message + "\n" + logArea.getValue() );
     //   logArea.scrollIntoView();
        logArea.focus();
     //   logArea.setAutoselect(true);
        scrollToBottom();
    }
    private void scrollToBottom() {
        logArea.getElement().executeJs("this.scrollTop = this.scrollHeight;");
    }
    public void clearLog() {
        logArea.clear();
    }


}
