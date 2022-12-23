package com.example.application.views;

import com.example.application.DownloadLinksArea;
import com.example.application.data.entity.ValueBlob;
import com.example.application.uils.DateiZippen;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
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
  //  private TextArea infotext = new TextArea();

    private Span content = new Span();

    private RawHtml rawHtmlml = new RawHtml();
    VerticalLayout infoBox = new VerticalLayout();
    File uploadFolder = getUploadFolder();
    private final VerticalLayout live_content;
    private final VerticalLayout archive_content;

    private final VerticalLayout main_content;

    private Integer NachrichtID=576757;
    DownloadLinksArea linksArea = new DownloadLinksArea(uploadFolder);
    public LobHandler lobHandler;

    private final Tab live;
    private final Tab archive;

    public MessageExportView() {

        live = new Tab("Live");
        archive = new Tab("Archive");


        Tabs tabs = new Tabs(live, archive);
       // tabs.setAutoselect(false);
        tabs.addSelectedChangeListener(
                event -> setContent(event.getSelectedTab()));
        add(tabs);

        live_content = new VerticalLayout();
        archive_content = new VerticalLayout();

        archive_content.add(new H6("Archive Nachrichten-Export"));

        live_content.add(new H6("Message-Export"));


        textField.setLabel("NachrichtIdIntern:");
        //textField.setValue("MessageIdIntern");
        textField.setClearButtonVisible(true);

        live_content.add(textField);


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

            NachrichtID=576757;
            // Start background task
            CompletableFuture.runAsync(() -> {

                // Do some long running task
                try {
                    System.out.println("Hole Dateien für NachrichtenID: " + textField.getValue() );

                    //NachrichtID=Integer.getInteger(textField.getValue());

                    exportMessage(NachrichtID);



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


                    String csvData= "";

                    String InfoFile="c:/tmp/messages/"+ NachrichtID.toString() +"/eKP_Metadata.html";

                    Path filePath = Path.of(InfoFile);

                    try {
                        System.out.println("Read InfoFile >" + InfoFile);
                        byte[] bytes = Files.readAllBytes(Paths.get(filePath.toUri()));
                        csvData = new String (bytes);
                    } catch (IOException e) {
                        System.out.println("InfoFile >" + InfoFile +"< nicht gefunden:" + e.getMessage());
                        //handle exception
                    }

                   // content.removeAll();
                   // content.add(new Html("<span>" + csvData + "</span>"));

                    //ui.add(content);

                //    infotext.setValue(csvData);
                //    infotext.setWidth("100%");
                //    infotext.setMaxHeight("500px");

                    rawHtmlml.setHtml(csvData);
                    infoBox.setVisible(true);
                    info.setVisible(false);

                    ZipMessage("c:/tmp/messages/", NachrichtID);

                });
            });



        });

        HorizontalLayout horizontalLayout = new HorizontalLayout(button, info);
        horizontalLayout.setAlignItems(Alignment.BASELINE);
        live_content.add(horizontalLayout);

        spinner.setIndeterminate(true);
        spinner.setVisible(false);
        live_content.add(spinner);


       // UploadArea uploadArea = new UploadArea(uploadFolder);


        // add(uploadArea, linksArea);
        live_content.add(linksArea);
       // add(infotext);


        infoBox.add("Dateiübersicht:");
        infoBox.add(rawHtmlml);
        infoBox.setVisible(false);

        live_content.add(infoBox);

        main_content = new VerticalLayout();
        main_content.setSpacing(false);
        main_content.add(live_content);


        add(tabs,main_content);

    }

    private void setContent(Tab tab) {
        main_content.removeAll();
        if (tab == null) {
            return;
        }
        if (tab.equals(live)) {
            main_content.add(live_content);
        } else if (tab.equals(archive)) {
            main_content.add(archive_content);
        } else {
            main_content.add(new Paragraph("This is the Shipping tab"));
        }
    }

    private class RawHtml extends Div {
        public void setHtml(String html) {
            getElement().setProperty("innerHTML", html);
        }
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




       // ZipMessage(targetfolder, nachrichtid);



    }

    private void ZipMessage(String targetfolder, Integer nachrichtid){
           DateiZippen dateiZippen = new DateiZippen();
           dateiZippen.createZipOfFolder(targetfolder + nachrichtid);
    }

    private String infoText() {
        return String.format("Exportiere NachrichtID: %s ", textField.getValue());
    }

}
