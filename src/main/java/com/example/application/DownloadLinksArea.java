package com.example.application;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.server.StreamResource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.helger.commons.io.file.FileOperations.deleteFile;

public class DownloadLinksArea extends VerticalLayout {

    private final File uploadFolder;

    public DownloadLinksArea(File uploadFolder) {
        this.uploadFolder = uploadFolder;
        refreshFileLinks();
        setMargin(true);
    }

    public void refreshFileLinks() {
        removeAll();

       // if(uploadFolder.listFiles().length != 0){
       //     add(new H4("Exportierte Nachrichten:"));
       // }

        List<Anchor> anchor_list = new ArrayList<Anchor>();
        Grid<Anchor> anchorGrid = new Grid(Anchor.class,false);

        for (File file : uploadFolder.listFiles()) {
           // addLinkToFile(file);

            anchor_list.add(getAnchor(file));
        }

        anchorGrid.setItems(anchor_list);
//anchorGrid.addClassNames("py-0","px-0");


        //anchorGrid.addColumn(Anchor::getHref ).setHeader("Link");

     //   anchorGrid.addColumn(createAnchorRenderer()).setHeader("Link").setAutoWidth(true).setFlexGrow(0);
        anchorGrid.addColumn(Anchor::getText).setHeader("Filename").setAutoWidth(true).setFlexGrow(0);;
     //   anchorGrid.addColumn(Anchor::getTitle).setHeader("Erstellt").setWidth("80px");
      //  anchorGrid.sets
        anchorGrid.addColumn(createAnchorRenderer());


        anchorGrid.addComponentColumn(item -> new Button("Delete File", click -> {
            try {
                delFile(item.getText());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            anchor_list.remove(item);
            refreshFileLinks();
        }));

        anchorGrid.addThemeVariants(GridVariant.LUMO_COMPACT);
      //  anchorGrid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        anchorGrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        anchorGrid.setHeight("150px");
        anchorGrid.setWidth("450px");
        add(anchorGrid);
        this.addClassName("downloadArea");
        this.setSpacing(false);
    }

    private boolean delFile(String file) throws IOException {


        String path = uploadFolder.getAbsolutePath();
        File myFile=new File(path+"/"+file);
        Notification.show("Delete file " + myFile.getCanonicalPath() );
        if (file != null) {
           deleteFile(myFile);

        }
        return false;

    }

    private static Renderer<Anchor> createAnchorRenderer() {
        return LitRenderer.<Anchor> of(
                "<a router-ignore=\"\" href=\"${item.pictureUrl}\" download>Download </a>")
                .withProperty("pictureUrl", Anchor::getHref)
                .withProperty("created", Anchor::getTitle)
                ;
    }


    private Anchor getAnchor(File file){
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


       // Anchor link = new Anchor(streamResource, String.format("%s | %s | (%d KB)", file.getName(), creationDate.getDate() + "/" +  (creationDate.getMonth() + 1) + "/" +  (creationDate.getYear() + 1900) + " " + creationDate.getHours() + ":" +  creationDate.getMinutes() , (int) file.length() / 1024));
        Anchor link = new Anchor(streamResource, String.format("%s", file.getName()));
        link.getElement().setAttribute("Title",creationDate.toString());
        link.getElement().setAttribute("download", true);
        link.getElement().setVisible(false);
        add(link);
        return link;
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


        Anchor link = new Anchor(streamResource, String.format("%s | %s | (%d KB)", file.getName(), creationDate.getDate() + "/" +  (creationDate.getMonth() + 1) + "/" +  (creationDate.getYear() + 1900) + " " + creationDate.getHours() + ":" +  creationDate.getMinutes() , (int) file.length() / 1024));
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
