package com.example.application.views;

import com.example.application.DownloadLinksArea;
import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.ValueBlob;
import com.example.application.data.service.ConfigurationService;
import com.example.application.utils.DateiZippen;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;

import javax.sql.DataSource;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@PageTitle("MessageExport")
@Route(value = "messageexport", layout= MainLayout.class)
@RolesAllowed({"ADMIN","FVM"})
public class MessageExportView extends VerticalLayout {

    @Autowired
    JdbcTemplate jdbcTemplate;
    private TextField textField = new TextField();

    ProgressBar spinner = new ProgressBar();
  //  private TextArea infotext = new TextArea();

    private Span content = new Span();

    private RawHtml rawHtmlml = new RawHtml();
    //VerticalLayout infoBox = new VerticalLayout();
    Details details = new Details("Dateiübersicht", rawHtmlml);
   // File uploadFolder;
    private final VerticalLayout live_content;
    private final VerticalLayout archive_content;

    private final VerticalLayout main_content;

    private String NachrichtID;
  //  DownloadLinksArea linksArea;
    public LobHandler lobHandler;

    private  ComboBox<Configuration> comboBox;
    private final Tab live;
    private final Tab archive;
    private ConfigurationService service;
    Integer ret=0;
    public MessageExportView(ConfigurationService service) {

        this.service=service;

//        System.out.println("Message Export Path: " + exportPath);
//
//        uploadFolder = new File(exportPath);
//       // uploadFolder = getUploadFolder();
//        if (!uploadFolder.exists() || !uploadFolder.isDirectory()) {
//            Notification.show("Error: Export directory does not exist.", 5000, Notification.Position.MIDDLE);
//        }

        live = new Tab("Prod");
        archive = new Tab("Archive");

        Tabs tabs = new Tabs(live, archive);
       // tabs.setAutoselect(false);
        tabs.addSelectedChangeListener(
                event -> setContent(event.getSelectedTab()));
        add(tabs);

        live_content = new VerticalLayout();
        archive_content = new VerticalLayout();

        archive_content.add(new H6("Archive Nachrichten-Export"));

        textField.setLabel("Nachricht-ID");
//        textField.setValue("576757");
        textField.setClearButtonVisible(true);

//        ComboBox<String> comboBox = new ComboBox<>("Verbindung");
//        comboBox.setAllowCustomValue(true);
//        comboBox.setItems("Chrome", "Edge", "Firefox", "Safari");

        comboBox = new ComboBox<>("Verbindung");
        List<Configuration> configList = service.findMessageConfigurations();
        if (configList != null && !configList.isEmpty()) {
            comboBox.setItems(configList);
            comboBox.setItemLabelGenerator(Configuration::getName);
        }

        Button button = new Button("Download");
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

           // NachrichtID=576757;
          //  NachrichtID= Integer.valueOf(textField.getValue());
            NachrichtID= textField.getValue();
            // Start background task
            CompletableFuture.runAsync(() -> {

                // Do some long running task
                try {
                    System.out.println("Hole Dateien für NachrichtenID: " + textField.getValue() );

                    //NachrichtID=Integer.getInteger(textField.getValue());

                    ret = exportMessage(NachrichtID);
                    if(ret==1){
                        System.out.println("Fehlgeschlagen! " );
                    }



                    //Thread.sleep(2000); //2 Sekunden warten
                    Thread.sleep(20); //2 Sekunden warten

                } catch (InterruptedException | IOException e) {
                    e.printStackTrace();
                }

                // Need to use access() when running from background thread
                ui.access(() -> {
                    // Stop polling and hide spinner
                    ui.setPollInterval(-1);
                    spinner.setVisible(false);

                    if (ret!=0)
                    {
                        System.out.println("Keine Dateien gefunden!");
                        info.setText("Keine Dateien zu dieser ID gefunden!");
                        spinner.setVisible(false);
                        details.setVisible(false);

                        return;
                    }

                    String csvData= "";

                    //String InfoFile="c:/tmp/messages/"+ NachrichtID.toString() +"/eKP_ZIP_Protokoll.html";


                    String InfoFile= NachrichtID.toString() +"/eKP_ZIP_Protokoll.html";

                    System.out.println("Suche ekp_ZIP_Protokoll.html unter " + InfoFile );

                    Path filePath = Path.of(InfoFile);

                    try {
                        System.out.println("Read InfoFile >" + InfoFile);
                        byte[] bytes = Files.readAllBytes(Paths.get(filePath.toUri()));
                        csvData = new String (bytes);
                    } catch (IOException e) {

                        System.out.println("InfoFile >" + InfoFile +"< nicht gefunden:" + e.getMessage());
                        //handle exception
                    }


                    rawHtmlml.setHtml(csvData);
                    //infoBox.setVisible(true);
                    details.setVisible(true);
                    info.setVisible(false);

                 //   ZipMessage("c:/tmp/messages/", NachrichtID);
                    prepareZipAndDownloadZipFile(NachrichtID);
                  //  downloadZipFile(NachrichtID);
                //    linksArea.refreshFileLinks();
                });
            });

        });

        HorizontalLayout buttonLayout = new HorizontalLayout(comboBox,textField,button, info);
        buttonLayout.setAlignItems(Alignment.BASELINE);

        VerticalLayout vertical = new VerticalLayout();
        vertical.setSpacing(false);
        vertical.setAlignItems(Alignment.START);
        vertical.add(buttonLayout);


        spinner.setWidth("800px");
        vertical.add(spinner);

     //   vertical.add(linksArea);

     //   live_content.add(new H6("Message-Export"));
    //    live_content.add(textField);
        live_content.add(vertical);

        spinner.setIndeterminate(true);
        spinner.setVisible(false);

   //     live_content.add(linksArea);

      //  infoBox.add("Dateiübersicht:");
      //  infoBox.add(rawHtmlml);
      //  infoBox.setVisible(false);

       // live_content.add(infoBox);


        details.setOpened(false);
        details.setVisible(false);

        live_content.add(details);

        main_content = new VerticalLayout();
        main_content.setSpacing(false);
        main_content.add(live_content);


        add(tabs,main_content);

    }

    private void downloadZipFile(String nachrichtid) {
        Path zipFilePath = Paths.get(nachrichtid + ".zip");

        if (!Files.exists(zipFilePath)) {
            System.out.println("Zip file not found: " + zipFilePath);
            return;
        } else {
            System.out.println("Zip file is found: " + zipFilePath);
        }
        String zipFileName = nachrichtid + ".zip";
        StreamResource resource = new StreamResource(zipFileName, () -> {
            try {
                return new ByteArrayInputStream(Files.readAllBytes(zipFilePath));
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        });

        Anchor anchor = new Anchor(resource, "Download " + zipFileName);
        anchor.getElement().setAttribute("download", true);
        anchor.setVisible(true);
        UI.getCurrent().add(anchor);

        UI.getCurrent().getPage().executeJs("arguments[0].click();", anchor.getElement());
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
        Anchor anchor = new Anchor(streamResource, "");
        anchor.getElement().setAttribute("download", true);
        anchor.getElement().getStyle().set("display", "none");
        add(anchor);
        UI.getCurrent().getPage().executeJs("arguments[0].click()", anchor);
        return anchor;
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

    private void read_parameter() {

     //   System.out.println("Export Path: " + exportPath);
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


//    private File getUploadFolder() {
//        //File folder = new File("c:\\tmp\\messages");
//
//        File folder = new File(exportPath);
//
//        if (!folder.exists()) {
//            folder.mkdirs();
//        }
//        return folder;
//    }

    private Integer exportMessage(String nachrichtid) throws IOException {

        Integer intNachrichtID;
        System.out.println("Exportiere: " + nachrichtid);

        try{
            intNachrichtID= Integer.valueOf(nachrichtid);
        }
        catch(Exception ex){
            intNachrichtID=-99;
        }

      //  String sql = "select a.Name NAME,a.Relativerpfad PFAD,v.Value BVAL from ekp.anhang a inner join ekp.value_blob v on a.INHALT=v.ID  and nachrichtidintern= " + nachrichtid;

        String sql = "select a.Name NAME,a.Relativerpfad PFAD,v.Value BVAL from ekp.anhang a inner join ekp.value_blob v on a.INHALT=v.ID inner join ekp.metadaten m on a.nachrichtidintern=m.nachrichtidintern  where m.nachrichtidintern=" + intNachrichtID  + " or m.Nachrichtidextern= '" + nachrichtid +"'";

        System.out.println("SQL: " + sql);

//        DriverManagerDataSource ds = new DriverManagerDataSource();
//        Configuration conf;
//        conf=comboBox.getValue();
//        ds.setUrl(conf.getDb_Url());
//        ds.setUsername(conf.getUserName());
//        ds.setPassword(Configuration.decodePassword(conf.getPassword()));
//
//        //ds.setUrl("jdbc:oracle:thin:@37.120.189.200:1521:xe");
//        //ds.setUsername("SYSTEM");
//        //ds.setPassword("Michael123");
//
//
//        jdbcTemplate.setDataSource(ds);

        getJdbcTemplateWithDBConnetion(comboBox.getValue());

        LobHandler lobHandler = new DefaultLobHandler();
        List<ValueBlob> values = new ArrayList<>();
        List<Map<String, Object>> rows=null;
        try {
            rows = jdbcTemplate.queryForList(sql);
        }
        catch(Exception e) {
            System.out.println("Exception: " + e.getMessage());
        } finally {
            connectionClose(jdbcTemplate);
        }



        if(rows == null || rows.isEmpty()){
            return 1;
        }

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

        String targetfolder="";
        //values.forEach(x->System.out.println(x.getName()));

        targetfolder="";


        for(ValueBlob val : values)
        {
            String MyFile= targetfolder + nachrichtid + val.getPfad() +  val.getName();
            //String MyFile= "c:\\tmp\\tmp_exportMS\\"+ val.getPfad() +  val.getName();
            System.out.println("Schreibe File: " + MyFile);

            File outputFile = new File(MyFile);

            if( val.getBlob() != null && val.getBlob().length != 0 ) {

                FileUtils.writeByteArrayToFile(outputFile, val.getBlob());
            }


        }

        return 0;

    }

    private void prepareZipAndDownloadZipFile(String nachrichtid){
           DateiZippen dateiZippen = new DateiZippen();
       //    dateiZippen.createZipInMemory(nachrichtid);
         //  dateiZippen.createZipOfFolder(nachrichtid);

        ByteArrayInputStream zipInputStream = dateiZippen.createZipInMemory(nachrichtid);

        if (zipInputStream != null) {
            StreamResource streamResource = new StreamResource(nachrichtid + ".zip", () -> zipInputStream);
            Anchor anchor = new Anchor(streamResource, "");
            anchor.getElement().setAttribute("download", true);
            anchor.getElement().getStyle().set("display", "none");
            add(anchor);

            // Trigger the download
            UI.getCurrent().getPage().executeJs("arguments[0].click()", anchor);
        } else {
            // Handle the case where zipInputStream is null or empty
            Notification.show("Failed to create ZIP file.", 3000, Notification.Position.MIDDLE);
        }
        deleteSourceFolder(nachrichtid);
    }
    private void deleteSourceFolder(String folderName) {
        File sourceFolder = new File(folderName);
        try {
            if (sourceFolder.exists()) {
                FileUtils.deleteDirectory(sourceFolder);
                System.out.println("Deleted folder: " + folderName);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Notification.show("Failed to delete source folder.", 3000, Notification.Position.MIDDLE);
        }
    }
    private String infoText() {
        return String.format("Exportiere NachrichtID: %s ", textField.getValue());
    }
    public JdbcTemplate getJdbcTemplateWithDBConnetion(com.example.application.data.entity.Configuration conf) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(com.example.application.data.entity.Configuration.decodePassword(conf.getPassword()));
        try {
            jdbcTemplate.setDataSource(ds);
        } catch (Exception e) {
            e.getMessage();
        }
        return null;
    }

    public void connectionClose(JdbcTemplate jdbcTemplate) {
        Connection connection = null;
        DataSource dataSource = null;
        try {
            jdbcTemplate.getDataSource().getConnection().close();
//            connection = jdbcTemplate.getDataSource().getConnection();
//            dataSource = jdbcTemplate.getDataSource();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();

                    if (dataSource instanceof HikariDataSource) {
                        ((HikariDataSource) dataSource).close();
                    }

                } catch (SQLException e) {

                    e.printStackTrace();
                }
            }
        }
    }
}
