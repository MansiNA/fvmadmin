package com.example.application.views;

import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class LogView extends VerticalLayout {

    public LogView() {
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
    }
}
