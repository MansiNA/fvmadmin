package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.ElaFavoriten;
import com.example.application.data.service.ConfigurationService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;


@PageTitle("Upload ELA-Favoriten | by DBUSS GmbH")
@Route(value = "ela-upload", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class ElaFavoritenView extends VerticalLayout {
    @Autowired
    JdbcTemplate jdbcTemplate;
    ProgressBar spinner = new ProgressBar();
    private ComboBox<Configuration> comboBox;

    private ComboBox<String> targetDB;

    Button button = new Button("Hochladen");
    Button countRows = new Button("Count Rows");
    private ConfigurationService service;

    TextArea detailsText = new TextArea();

    File uploadFolder = getUploadFolder();
//    UploadArea uploadArea = new UploadArea(uploadFolder);

    MemoryBuffer memoryBuffer = new MemoryBuffer();
    Upload singleFileUpload = new Upload(memoryBuffer);

    String fileName="";
    long contentLength=0;
    String mimeType="";
    InputStream fileData;
    HorizontalLayout errorBadge;

    Article article=new Article();
    Div textArea=new Div();

    Details details = new Details();


    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
//    MessageList list = new MessageList();
Icon icon;
    String ret="ok";
  //  private TextField textField = new TextField();

    public ElaFavoritenView(ConfigurationService service){

        this.service = service;

        comboBox = new ComboBox<>("Ziel-Datenbank");
        comboBox.setItems(service.findMessageConfigurations());
        comboBox.setItemLabelGenerator(Configuration::getName);

        comboBox.setValue(service.findAllConfigurations().stream().findFirst().get());
      //  comboBox.setPlaceholder("Select Database");
        //comboBox.addValueChangeListener(e -> textField.setValue(String.valueOf(e.getValue())));

        targetDB = new ComboBox<>("Ziel Tabelle");
        targetDB.setItems("ELA_FAVORITEN_NEU","ELA_FAVORITEN");
        targetDB.setValue("ELA_FAVORITEN_NEU");
        targetDB.setWidth("250px");


        textArea.setWidthFull();
        //textArea.setHeight("200px");
        //textArea.setLabel("Info");
    //    textArea.setValue("wait for File");
//        textArea.setReadOnly((Boolean.TRUE));
//        textArea.getStyle().set("background-color", "#fcfcfa");
//        textArea.getStyle().set("border", "1px solid #eee");

        //    textArea.getStyle().set("border", "none");


        article=new Article();
        article.setText("Warten auf Datei");
        textArea.add(article);

        HorizontalLayout hl = new HorizontalLayout();
        hl.add(comboBox,targetDB);
        hl.setAlignItems(FlexComponent.Alignment.BASELINE);
        setSizeFull();
        add(hl);

        button.setEnabled(false);

        singleFileUpload.setWidth("600px");

        singleFileUpload.addSucceededListener(event -> {
            // Get information about the uploaded file
            fileData = memoryBuffer.getInputStream();
            fileName = event.getFileName();
            contentLength = event.getContentLength();
            mimeType = event.getMIMEType();
            button.setEnabled(true);
            textArea.setText("Warten auf Button \"Hochladen\"");
            detailsText.setValue("Weitere Ladeinformationen bzgl. >>" + fileName + "<<");
            // Do something with the file data
            // processFile(fileData, fileName, contentLength, mimeType);
        });



        Instant yesterday = LocalDateTime.now(ZoneOffset.UTC).minusDays(1).toInstant(ZoneOffset.UTC);
        MessageListItem message1 = new MessageListItem("Linsey, could you check if the details with the order are okay?", yesterday, "Matt Mambo");
//        message1.setUserColorIndex(1);
        message1.addThemeNames("current-user");
        message1.setUserColorIndex(2);

        spinner.setIndeterminate(true);
        spinner.setVisible(false);

     //   add(new H2("Upload neuer ELA-Favoriten Excel Datei"));


      //  DownloadLinksArea linksArea = new DownloadLinksArea(uploadFolder);


        button.setWidth("180px");
        button.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        countRows.addClickListener(clickEvent -> countRows());

        button.addClickListener(clickEvent -> {
            try {
                upload();

                singleFileUpload.clearFileList();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        HorizontalLayout horl = new HorizontalLayout();
        //horl.setWidthFull();
        horl.setWidth("600px");
     //   horl.setJustifyContentMode(JustifyContentMode.START);

       VerticalLayout verl = new VerticalLayout();
       verl.add(button,spinner);

        horl.add(singleFileUpload,verl,countRows);
        horl.setAlignItems(Alignment.CENTER);

        icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
        icon.getStyle().set("width", "var(--lumo-icon-size-s)");
        icon.getStyle().set("height", "var(--lumo-icon-size-s)");

        detailsText.setWidthFull();
        detailsText.setHeight("300px");
        details = new Details("Details",detailsText);
        details.setOpened(false);
        details.setWidthFull();


        add(horl,textArea,details);

    }



private void upload() throws SQLException, IOException, ClassNotFoundException, InterruptedException {

    if(fileName.isEmpty() || fileName.length()==0)
    {
        article=new Article();
        article.setText(LocalDateTime.now().format(formatter) + ": Error: Keine Datei angegeben!");
        textArea.add(article);
        return;
    }

    if(!mimeType.contains("application/vnd.ms-excel"))
    {
        article=new Article();
        article.setText(LocalDateTime.now().format(formatter) + ": Error: ungültiges Dateiformat!");
        textArea.add(article);
        return;
    }

    System.out.println("Excel import: "+  fileName + " => Mime-Type: " + mimeType  + " Größe " + contentLength + " Byte");
    textArea.setText(LocalDateTime.now().format(formatter) + ": Info: Verarbeite Datei: " + fileName + " (" + contentLength + " Byte)");


 //   FileInputStream input_document = new FileInputStream(new File("C:\\tmp\\ELA_FAVORITEN.XLS"));
    /* Load workbook */
    button.setEnabled(false);
    //spinner.setValue(0.5);
    spinner.setVisible(true);

    HSSFWorkbook my_xls_workbook = new HSSFWorkbook(fileData);
//    HSSFWorkbook my_xls_workbook = new HSSFWorkbook(input_document);
    /* Load worksheet */
    HSSFSheet my_worksheet = my_xls_workbook.getSheetAt(0);
    // we loop through and insert data
    Iterator<Row> rowIterator = my_worksheet.iterator();

    List<ElaFavoriten> elaFavoritenListe = new ArrayList<ElaFavoriten>();

    Integer RowNumber=0;
    Boolean isError=false;

    while(rowIterator.hasNext() && !isError)
    {
            ElaFavoriten elaFavoriten = new ElaFavoriten();
            Row row = rowIterator.next();
            RowNumber++;
         //   System.out.println("Zeile:" + RowNumber.toString());

            Iterator<Cell> cellIterator = row.cellIterator();
            while(cellIterator.hasNext()) {

                if(RowNumber==1) //Überschrift nicht betrachten
                {
                    break;
                }

                Cell cell = cellIterator.next();

                try {
                    elaFavoriten.setID(checkCellNumeric(cell, RowNumber,"ID"));
                }
                catch(Exception e)
                {
                    article=new Article();
                    article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte ID nicht vorhanden!");
                    textArea.add(article);
                    isError=true;
                    break;
                }


                try {
                    cell = cellIterator.next();
                    elaFavoriten.setBENUTZER_KENNUNG(checkCellString(cell, RowNumber,"Benutzer-Kennung"));
                }
                catch(Exception e)
                {
                    article=new Article();
                    article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Benutzer-Kennung nicht vorhanden!");
                    textArea.add(article);
                    isError=true;
                    break;
                }



                try {
                    cell = cellIterator.next();
                    elaFavoriten.setNUTZER_ID(checkCellString(cell, RowNumber,"Nutzer-ID"));
                }
                catch(Exception e)
                {
                    article=new Article();
                    article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Nutzer-ID nicht vorhanden!");
                    textArea.add(article);
                    isError=true;
                    break;
                }



                try {
                    cell = cellIterator.next();
                    elaFavoriten.setNAME(checkCellString(cell, RowNumber,"Name"));
                }
                catch(Exception e)
                {
                    article=new Article();
                    article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Name nicht vorhanden!");
                    textArea.add(article);
                    isError=true;
                    break;
                }



                try {
                    cell = cellIterator.next();
                    elaFavoriten.setVORNAME(checkCellString(cell, RowNumber,"Vorname"));
                }
                catch(Exception e)
                {
                    article=new Article();
                    article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Vorname nicht vorhanden!");
                    textArea.add(article);
                    isError=true;
                    break;
                }


                try {
                    cell = cellIterator.next();
                    elaFavoriten.setORT(checkCellString(cell, RowNumber,"Ort"));
                }
                catch(Exception e)
                {
                    article=new Article();
                    article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Ort nicht vorhanden!");
                    textArea.add(article);
                    isError=true;
                    break;
                }

                try {
                    cell = cellIterator.next();
                    elaFavoriten.setPLZ(checkCellString(cell, RowNumber,"PLZ"));
                }
                catch(Exception e)
                {
                    article=new Article();
                    article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte PLZ nicht vorhanden!");
                    textArea.add(article);
                    isError=true;
                    break;
                }

                try {
                    cell = cellIterator.next();
                    elaFavoriten.setSTRASSE(checkCellString(cell, RowNumber,"Strasse"));
                }
                catch(Exception e)
                {
                    article=new Article();
                    article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Strasse nicht vorhanden!");
                    textArea.add(article);
                    isError=true;
                    break;
                }

                try {
                    cell = cellIterator.next();
                    elaFavoriten.setHAUSNUMMER(checkCellString(cell, RowNumber,"Hausnummer"));
                }
                catch(Exception e)
                {
                    article=new Article();
                    article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Hausnumer nicht vorhanden!");
                    textArea.add(article);
                    isError=true;
                    break;
                }

                try {
                    cell = cellIterator.next();
                    elaFavoriten.setORGANISATION(checkCellString(cell, RowNumber,"Organisation"));
                }
                catch(Exception e)
                {
                    article=new Article();
                    article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Organisation nicht vorhanden!");
                    textArea.add(article);
                    isError=true;
                    break;
                }


                try {
                    cell = cellIterator.next();
                    elaFavoriten.setVERSION(checkCellNumeric(cell, RowNumber,"Version"));
                }
                catch(Exception e)
                {
                    article=new Article();
                    article.setText(LocalDateTime.now().format(formatter) + ": Error: Zeile " + RowNumber.toString() + ", Spalte Version nicht vorhanden!");
                    textArea.add(article);
                    isError=true;
                    break;
                }


                elaFavoritenListe.add(elaFavoriten);

            }

    }

    if(isError)
    {
    //    button.setEnabled(true);
        spinner.setVisible(false);
        fileName="";
        return;
    }

    //textArea.setValue(textArea.getValue() + "\n" + Instant.now() + ": Start Upload to DB");
    article=new Article();
    article.add(LocalDateTime.now().format(formatter) + ": Info: Anzahl Zeilen: " + elaFavoritenListe.size());
    textArea.add(article);
    article=new Article();
    article.add(LocalDateTime.now().format(formatter) + ": Info: Start Upload to DB");
    textArea.add(article);

    System.out.println("Anzahl Zeilen im Excel: " + elaFavoritenListe.size());

    UI ui = UI.getCurrent();
    // Instruct client side to poll for changes and show spinner
    ui.setPollInterval(500);





    /* Close input stream */
//    input_document.close();

    CompletableFuture.runAsync(() -> {

        // Do some long running task
        try {
           ret = write2DB(elaFavoritenListe);


            Thread.sleep(20); //2 Sekunden warten


        } catch (InterruptedException | SQLException | ClassNotFoundException e) {
            e.printStackTrace();
            spinner.setVisible(false);
        }

        // Need to use access() when running from background thread
        ui.access(() -> {
            // Stop polling and hide spinner
            ui.setPollInterval(-1);
//            spinner.setValue(1);
//            button.setEnabled(true);
            spinner.setVisible(false);
            if(!ret.equals("ok")){
                System.out.println("Fehlgeschlagen! " );
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Error: Upload to DB fehlgeschlagen: " + ret);
                textArea.add(article);

            }
            else {
                article=new Article();
                article.setText(LocalDateTime.now().format(formatter) + ": Info: Ende Upload to DB erfolgreich");
                textArea.add(article);
            }
            fileName="";
        });
    });



}


private void countRows()
{
    String jdbc_sql ="select count(*) from EKP.ELA_FAVORITEN_NEU";

    try {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));

        jdbcTemplate.setDataSource(ds);
        int result = jdbcTemplate.queryForObject(jdbc_sql, Integer.class);

        article=new Article();
        article.setText(LocalDateTime.now().format(formatter) + ": Info: Anzahl Zeilen in DB-Table " + result);
        textArea.add(article);


    } catch (Exception e) {
        //   textArea.setValue(textArea.getValue() + "\nFehler beim Speichern in DB!");
        System.out.println("Exception: " + e.getMessage());
       // return e.getMessage();
    }

}


    private String write2DB(List<ElaFavoriten> elaFavoritenListe) throws ClassNotFoundException, SQLException {

        Class.forName ("oracle.jdbc.OracleDriver");
   //     Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//37.120.189.200:1521/xe", "SYSTEM", "Michael123");
   //     PreparedStatement sql_statement = null;
        String jdbc_insert_sql = "INSERT INTO EKP.ELA_FAVORITEN"  + "(ID,BENUTZER_KENNUNG,NUTZER_ID,NAME,VORNAME,ORT,PLZ,STRASSE,HAUSNUMMER,ORGANISATION,VERSION) VALUES"  + "(?,?,?,?,?,?,?,?,?,?,?)";
//        sql_statement = conn.prepareStatement(jdbc_insert_sql);

        try {
            DriverManagerDataSource ds = new DriverManagerDataSource();
//    ds.setUrl("jdbc:oracle:thin:@//37.120.189.200:1521/xe");
//    ds.setUsername("SYSTEM");
//    ds.setPassword("Michael123");

            Configuration conf;
            conf = comboBox.getValue();

            ds.setUrl(conf.getDb_Url());
            ds.setUsername(conf.getUserName());
            ds.setPassword(Configuration.decodePassword(conf.getPassword()));

            jdbcTemplate.setDataSource(ds);
            jdbcTemplate.batchUpdate("INSERT INTO EKP.ELA_FAVORITEN_NEU (ID,BENUTZER_KENNUNG,NUTZER_ID,NAME,VORNAME,ORT,PLZ,STRASSE,HAUSNUMMER,ORGANISATION,VERSION) " +
                            "VALUES (?, ?, ?,?, ?, ?,?, ?, ?, ?, ?)",
                    elaFavoritenListe,
                    100,
                    (PreparedStatement ps, ElaFavoriten elaFavoriten1) -> {
                        ps.setInt(1, elaFavoriten1.getID());
                        ps.setString(2, elaFavoriten1.getBENUTZER_KENNUNG());
                        ps.setString(3, elaFavoriten1.getNUTZER_ID());
                        ps.setString(4, elaFavoriten1.getNAME());
                        ps.setString(5, elaFavoriten1.getVORNAME());
                        ps.setString(6, elaFavoriten1.getORT());
                        ps.setString(7, elaFavoriten1.getPLZ());
                        ps.setString(8, elaFavoriten1.getSTRASSE());
                        ps.setString(9, elaFavoriten1.getHAUSNUMMER());
                        ps.setString(10, elaFavoriten1.getORGANISATION());
                        ps.setInt(11, elaFavoriten1.getVERSION());

                    });
         //   textArea.setValue(textArea.getValue() + "\nIn DB gespeichert.");
        } catch (Exception e) {
         //   textArea.setValue(textArea.getValue() + "\nFehler beim Speichern in DB!");
            System.out.println("Exception: " + e.getMessage());
            return e.getMessage();
        }


        /* Close prepared statement */
   //     sql_statement.close();
        /* COMMIT transaction */

//            conn.commit();
        /* Close connection */
//        conn.close();

        return "ok";

    }

    private String checkCellString(Cell cell, Integer zeile, String spalte) {

        try{


        if (cell.getCellType()!=Cell.CELL_TYPE_STRING && !cell.getStringCellValue().isEmpty())
        {
            System.out.println("Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte nicht gelesen werden, da ExcelTyp Numeric!");
            detailsText.setValue(detailsText.getValue() + "\nZeile " + zeile.toString() + ", Spalte " + spalte + "  konnte nicht gelesen werden, da ExcelTyp Numeric!");
            return "";
        }
        else
        {
            if (cell.getStringCellValue().isEmpty())
            {
                //System.out.println("Info: Zeile " + zeile.toString() + ", Spalte " + spalte + " ist leer");
                detailsText.setValue(detailsText.getValue() + "\nZeile " + zeile.toString() + ", Spalte " + spalte + " ist leer");
            }
            return  cell.getStringCellValue();

        }
        }
        catch(Exception e) {
            System.out.println("Exception" + e.getMessage());
            detailsText.setValue(detailsText.getValue() + "\nZeile " + zeile.toString() + ", Spalte " + spalte + "  konnte nicht gelesen werden, da ExcelTyp Numeric!");
            return "";
        }
    }


    private Integer checkCellNumeric(Cell cell, Integer zeile, String spalte) {

        if (cell.getCellType()!=Cell.CELL_TYPE_NUMERIC)
        {
            System.out.println("Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte nicht gelesen werden, da ExcelTyp nicht numerisch!");
       //     textArea.setValue(textArea.getValue() + "\n" + LocalDateTime.now().format(formatter) + ": Error: Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte nicht gelesen werden, da ExcelTyp nicht Numeric!");
            return 0;
        }
        else
        {
            return  (int) cell.getNumericCellValue();
        }

    }

    private static File getUploadFolder() {
        File folder = new File("uploaded-files");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }
}
