package com.example.application.views;

import com.example.application.DownloadLinksArea;
import com.example.application.data.entity.ValueBlob;
import com.example.application.uils.DateiZippen;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;



@PageTitle("MessageExport")
@Route(value = "messageexport", layout= MainLayout.class)
public class MessageExportView extends VerticalLayout {

    @Autowired
    JdbcTemplate jdbcTemplate;
    private TextField textField = new TextField();
    ProgressBar spinner = new ProgressBar();
    TextArea infotext = new TextArea();
    File uploadFolder = getUploadFolder();
    DownloadLinksArea linksArea = new DownloadLinksArea(uploadFolder);
    public LobHandler lobHandler;

    public MessageExportView() {
        add(new H1("Message-Export"));


        textField.setLabel("NachrichtIdIntern:");
        //textField.setValue("MessageIdIntern");
        textField.setClearButtonVisible(true);

        add(textField);
        add(infotext);

        Button button = new Button("Start Export");
        Paragraph info = new Paragraph(infoText());
        info.setVisible(false);
        button.addClickListener(clickEvent -> {
            info.setVisible(true);
            spinner.setVisible(true);
            info.setText("Hole Daten aus DB für Nachricht-ID " + textField.getValue() );


            UI ui = UI.getCurrent();

            // Instruct client side to poll for changes and show spinner
            ui.setPollInterval(500);
            spinner.setVisible(true);

            // Start background task
            CompletableFuture.runAsync(() -> {

                // Do some long running task
                try {
                    System.out.println("Hole Dateien für NachrichtenID: " + textField.getValue() );
                    exportMessage(576757);



                    //Thread.sleep(2000); //2 Sekunden warten
                    Thread.sleep(20); //2 Sekunden warten

                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }

                // Need to use access() when running from background thread
                ui.access(() -> {
                    // Stop polling and hide spinner
              //      ui.setPollInterval(-1);
                    spinner.setVisible(false);
                    linksArea.refreshFileLinks();


                    String csvData= "";;

                    Path filePath = Path.of("c:/tmp/messages/eKP_Metadata.html");

                    try {

                        byte[] bytes = Files.readAllBytes(Paths.get(filePath.toUri()));
                        csvData = new String (bytes);
                    } catch (IOException e) {
                        //handle exception
                    }


                    infotext.setValue(csvData);
                    infotext.setWidth("100%");
                    infotext.setMaxHeight("500px");





                    info.setVisible(false);
                });
            });



        });

        HorizontalLayout horizontalLayout = new HorizontalLayout(button, info);
        horizontalLayout.setAlignItems(Alignment.BASELINE);
        add(horizontalLayout);

        spinner.setIndeterminate(true);
        spinner.setVisible(false);
        add(spinner);


       // UploadArea uploadArea = new UploadArea(uploadFolder);


        // add(uploadArea, linksArea);
        add(linksArea);
    }

    private static File getUploadFolder() {
        File folder = new File("c:\\tmp\\messages");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    private void exportMessage(int nachrichtid) throws IOException {

        System.out.println("Exportiere: " + nachrichtid);

        String sql = "select a.Name NAME,a.Relativerpfad PFAD,v.Value BVAL from ekp.anhang a inner join ekp.value_blob v on a.INHALT=v.ID  and nachrichtidintern= " + nachrichtid;

        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl("jdbc:oracle:thin:@37.120.189.200:1521:xe");
        ds.setUsername("SYSTEM");
        ds.setPassword("Michael123");


        jdbcTemplate.setDataSource(ds);


        LobHandler lobHandler = new DefaultLobHandler();
        List<ValueBlob> values = new ArrayList<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        for (Map row : rows) {
            ValueBlob obj = new ValueBlob();

            obj.setName((String) row.get("NAME"));
            obj.setPfad((String) row.get("PFAD"));
            byte[]  content = (byte[]) row.get("BVAL");
            obj.setBlob(content);
            values.add(obj);
        }


        // In values stehen jetzt die Dateien mit namen und Pfad
        // Schreiben in Dateisystem:


        //values.forEach(x->System.out.println(x.getName()));
        String targetfolder="c:\\tmp\\messages\\";

        for(ValueBlob val : values)
        {
            String MyFile= targetfolder + nachrichtid + "\\"+ val.getPfad() +  val.getName();
            //String MyFile= "c:\\tmp\\tmp_exportMS\\"+ val.getPfad() +  val.getName();
            System.out.println("Schreibe File: " + MyFile);

            File outputFile = new File(MyFile);

            if( val.getBlob() != null && val.getBlob().length != 0 ) {

                FileUtils.writeByteArrayToFile(outputFile, val.getBlob());
            }


        }





        DateiZippen dateiZippen = new DateiZippen();
        dateiZippen.createZipOfFolder(targetfolder + nachrichtid);


    }

    private String infoText() {
        return String.format("Exportiere NachrichtID: %s ", textField.getValue());
    }

}
