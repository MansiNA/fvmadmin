package com.example.application.utils;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LogPannel extends VerticalLayout {

    private TextArea textArea;

    public LogPannel() {
        textArea = new TextArea();
        textArea.setWidth("100%");
        textArea.setHeight("300px");

        add(textArea);
    }

    public void logMessage2(String level, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String formattedMessage = String.format("[%s] [%s] %s<br>", timestamp, level, message);

        // Set color based on the log level
        switch (level.toUpperCase()) {
            case "INFO":
                formattedMessage = "<span style=\"color: green;\">" + formattedMessage + "</span>";
                break;
            case "ERROR":
                formattedMessage = "<span style=\"color: red;\">" + formattedMessage + "</span>";
                break;
            // Add more cases for other log levels if needed

            default:
                break;
        }

        // Append the formatted message with a line break to the existing content
        textArea.getElement().executeJs("arguments[0].content.innerHTML += arguments[1];", textArea.getElement(), formattedMessage);

        // Add a line break
        textArea.getElement().executeJs("arguments[0].content.innerHTML += '<br>';", textArea.getElement());

        // Scroll to the bottom
        UI.getCurrent().getPage().executeJs("arguments[0].content.scrollTop = arguments[0].content.scrollHeight;", textArea.getElement());
    }

    public void logMessage(String level, String message) {
        textArea.setValue(textArea.getValue() + level + " " + message + "\n");
        UI.getCurrent().getPage().executeJs("arguments[0].scrollTop = arguments[0].scrollHeight;", textArea.getElement());
    }

    public void logMessage3(String level, String message) {
        Span span = new Span();
        span.getElement().setProperty("innerHTML", getStyledMessage(level, message));
        Div logEntry = new Div(span);
        textArea.getElement().appendChild(logEntry.getElement());

        // Scroll to the bottom after appending the message
        UI.getCurrent().getPage().executeJs("arguments[0].scrollTop = arguments[0].scrollHeight;", textArea.getElement());
    }

    private String getStyledMessage(String level, String message) {
        String color;

        // Set color based on log level
        switch (level) {
            case "INFO":
                color = "green";
                break;
            case "WARN":
                color = "orange";
                break;
            case "ERROR":
                color = "red";
                break;
            default:
                color = "black";
        }

        // Apply styling using HTML
        return "<span style='color: " + color + ";'><strong>" + level + "</strong> " + message + "</span><br>";
    }
}