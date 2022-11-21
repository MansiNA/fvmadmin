package com.example.application;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DownloadLinksArea extends VerticalLayout {

    private final File uploadFolder;

    public DownloadLinksArea(File uploadFolder) {
        this.uploadFolder = uploadFolder;
        refreshFileLinks();
        setMargin(true);
    }

    public void refreshFileLinks() {
        removeAll();
        add(new H4("Download Links:"));

        for (File file : uploadFolder.listFiles()) {
            addLinkToFile(file);
        }
    }

    private void addLinkToFile(File file) {
        StreamResource streamResource = new StreamResource(file.getName(), () -> getStream(file));
        BasicFileAttributes attr;
        Date creationDate;
        Path filepath = Paths.get(String.valueOf(file.getAbsoluteFile()));
        try {
            attr =  Files.readAttributes(filepath, BasicFileAttributes.class);
            System.out.println("creationTime: " + attr.creationTime());
            System.out.println("lastAccessTime: " + attr.lastAccessTime());
            System.out.println("lastModifiedTime: " + attr.lastModifiedTime());

            creationDate = new Date(attr.creationTime().to(TimeUnit.MILLISECONDS));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        Anchor link = new Anchor(streamResource, String.format("%s %s (%d KB)", file.getName(), creationDate.getDate() + "/" +  (creationDate.getMonth() + 1) + "/" +  (creationDate.getYear() + 1900) + " " + creationDate.getHours() + ":" +  creationDate.getMinutes() , (int) file.length() / 1024));
        link.getElement().setAttribute("download", true);
        add(link);
    }

    private InputStream getStream(File file) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return stream;
    }
}
