package com.example.application.views;

import com.example.application.DownloadLinksArea;
import com.example.application.UploadArea;
import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.ElaFavoriten;
import com.example.application.data.service.ConfigurationService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


@PageTitle("Upload ELA-Favoriten | by DBUSS GmbH")
@Route(value = "ela-upload", layout= MainLayout.class)
public class ElaFavoritenView extends VerticalLayout {
    @Autowired
    JdbcTemplate jdbcTemplate;

    private ComboBox<Configuration> comboBox;
    Button button = new Button("Refresh");

    private ConfigurationService service;

    public ElaFavoritenView(ConfigurationService service){

        this.service = service;

        comboBox = new ComboBox<>("Ziel-Datenbank");
        comboBox.setItems(service.findMessageConfigurations());
        comboBox.setItemLabelGenerator(Configuration::get_Message_Connection);

        comboBox.setValue(service.findAllConfigurations().stream().findFirst().get());
        comboBox.setPlaceholder("Select Database");
        //comboBox.addValueChangeListener(e -> textField.setValue(String.valueOf(e.getValue())));

        HorizontalLayout hl = new HorizontalLayout();
        hl.add(comboBox);
        hl.setAlignItems(FlexComponent.Alignment.BASELINE);
        setSizeFull();
        add(hl);


        add(new H2("Upload neuer ELA-Favoriten Excel Datei"));

        File uploadFolder = getUploadFolder();
        UploadArea uploadArea = new UploadArea(uploadFolder);
        DownloadLinksArea linksArea = new DownloadLinksArea(uploadFolder);

        uploadArea.getUploadField().addSucceededListener(e -> {
            uploadArea.hideErrorField();
            linksArea.refreshFileLinks();
        });

        add(uploadArea);

        Button button = new Button("Button");
        button.addClickListener(clickEvent -> {
            try {
                upload();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        add(button);

    }


//    DriverManagerDataSource ds = new DriverManagerDataSource();
//    Configuration conf;
//    conf = comboBox.getValue();
//
//        ds.setUrl(conf.getDb_Url());
//        ds.setUsername(conf.getUserName());
//        ds.setPassword(conf.getPassword());
//
//        try {
//
//        jdbcTemplate.setDataSource(ds);
//
//        mailboxen = jdbcTemplate.query(
//                sql,
//                new BeanPropertyRowMapper(Mailbox.class));
//
//
//
//        System.out.println("MAILBOX_CONFIG eingelesen");
//
//    } catch (Exception e) {
//        System.out.println("Exception: " + e.getMessage());
//    }


private void upload() throws SQLException, IOException, ClassNotFoundException {


    System.out.println("Excel import ");

    Class.forName ("oracle.jdbc.OracleDriver");
    Connection conn = DriverManager.getConnection("jdbc:oracle:thin:@//37.120.189.200:1521/xe", "SYSTEM", "Michael123");
    PreparedStatement sql_statement = null;
    String jdbc_insert_sql = "INSERT INTO EKP.ELA_FAVORITEN"  + "(ID,BENUTZER_KENNUNG,NUTZER_ID,NAME,VORNAME,ORT,PLZ,STRASSE,HAUSNUMMER,ORGANISATION,VERSION) VALUES"  + "(?,?,?,?,?,?,?,?,?,?,?)";
    sql_statement = conn.prepareStatement(jdbc_insert_sql);
    /* We should now load excel objects and loop through the worksheet data */
    FileInputStream input_document = new FileInputStream(new File("C:\\tmp\\ELA_FAVORITEN.XLS"));
    /* Load workbook */
    HSSFWorkbook my_xls_workbook = new HSSFWorkbook(input_document);
    /* Load worksheet */
    HSSFSheet my_worksheet = my_xls_workbook.getSheetAt(0);
    // we loop through and insert data
    Iterator<Row> rowIterator = my_worksheet.iterator();

    List<ElaFavoriten> elaFavoritenListe = new ArrayList<ElaFavoriten>();

    Integer RowNumber=0;

    while(rowIterator.hasNext())
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

                elaFavoriten.setID(checkCellNumeric(cell, RowNumber,"ID"));

                cell = cellIterator.next();

                elaFavoriten.setBENUTZER_KENNUNG(checkCellString(cell, RowNumber,"Benutzer-Kennung"));

                cell = cellIterator.next();

                elaFavoriten.setNUTZER_ID(checkCellString(cell, RowNumber,"Nutzer-ID"));

                cell = cellIterator.next();

                elaFavoriten.setNAME(checkCellString(cell, RowNumber,"Name"));

                cell = cellIterator.next();

                elaFavoriten.setVORNAME(checkCellString(cell, RowNumber,"Vorname"));

                cell = cellIterator.next();

                elaFavoriten.setORT(checkCellString(cell, RowNumber,"Ort"));

                cell = cellIterator.next();

                elaFavoriten.setPLZ(checkCellString(cell, RowNumber,"PLZ"));

                cell = cellIterator.next();

                elaFavoriten.setSTRASSE(checkCellString(cell, RowNumber,"Strasse"));

                cell = cellIterator.next();

                elaFavoriten.setHAUSNUMMER(checkCellString(cell, RowNumber,"Hausnummer"));

                cell = cellIterator.next();

                elaFavoriten.setORGANISATION(checkCellString(cell, RowNumber,"Organisation"));

                cell = cellIterator.next();

                elaFavoriten.setVERSION(checkCellNumeric(cell, RowNumber,"Version"));

                elaFavoritenListe.add(elaFavoriten);

            }

    }

    System.out.println("Anzahl Zeilen im Excel: " + elaFavoritenListe.size());

    try {
    DriverManagerDataSource ds = new DriverManagerDataSource();
//    ds.setUrl("jdbc:oracle:thin:@//37.120.189.200:1521/xe");
//    ds.setUsername("SYSTEM");
//    ds.setPassword("Michael123");

        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(conf.getPassword());

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

    } catch (Exception e) {
        System.out.println("Exception: " + e.getMessage());
    }

    /* Close input stream */
            input_document.close();
    /* Close prepared statement */
            sql_statement.close();
    /* COMMIT transaction */

//            conn.commit();
    /* Close connection */
            conn.close();

}

    private String checkCellString(Cell cell, Integer zeile, String spalte) {

        if (cell.getCellType()!=Cell.CELL_TYPE_STRING && !cell.getStringCellValue().isEmpty())
        {
            System.out.println("Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte nicht gelesen werden, da ExcelTyp Numeric!");
            return "";
        }
        else
        {
            if (cell.getStringCellValue().isEmpty())
            {
                System.out.println("Info: Zeile " + zeile.toString() + ", Spalte " + spalte + " ist leer");
            }
            return  cell.getStringCellValue();

        }

    }


    private Integer checkCellNumeric(Cell cell, Integer zeile, String spalte) {

        if (cell.getCellType()!=Cell.CELL_TYPE_NUMERIC)
        {
            System.out.println("Zeile " + zeile.toString() + ", Spalte " + spalte + " konnte nicht gelesen werden, da ExcelTyp nicht numerisch!");
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
