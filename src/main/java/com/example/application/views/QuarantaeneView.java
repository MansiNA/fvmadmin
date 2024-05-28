package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.Quarantine;
import com.example.application.data.entity.TableInfo;
import com.example.application.data.service.ConfigurationService;
import com.example.application.security.ApplicationUserRole;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.compress.utils.Lists;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.example.application.security.ApplicationUserRole.PF_ADMIN;
import static org.apache.xmlbeans.impl.store.Public2.getStream;

@PageTitle("Quarantäne Verwaltung")
@Route(value = "quarantaene", layout= MainLayout.class)
@RolesAllowed({"ADMIN"})
public class QuarantaeneView extends VerticalLayout {

    @Autowired
    JdbcTemplate jdbcTemplate;
    private ConfigurationService service;
    static ComboBox<Configuration> comboBox;

    Button button = new Button("Refresh");
    Integer ret = 0;
    Grid<Quarantine> qgrid = new Grid<>(Quarantine.class, false);

    List<Quarantine> lq = new ArrayList<>();
    private String exportPath;
    private Anchor anchor = new Anchor(getStreamResource("quaran.xls", "default content"), "click to download");
    private Button smallButton = new Button("Export");
    ComboBox FehlertypCB;


    public QuarantaeneView(@Value("${csv_exportPath}") String p_exportPath, ConfigurationService service) {
        this.service = service;
        this.exportPath=p_exportPath;

        Paragraph paragraph = new Paragraph("Hier erfolgt eine Auflistung von aktuellen EGVP-E Quarantäne-Nachrichten");
        paragraph.setMaxHeight("400px");

        comboBox = new ComboBox<>("Verbindung");
        comboBox.setItems(service.findMessageConfigurations());
        comboBox.setItemLabelGenerator(Configuration::getName);

        comboBox.setValue(service.findAllConfigurations().stream().findFirst().get());

        List<String> errorList = List.of("MESSAGE_INCOMPLETE", "CHECK_FILENAMES", "SERVER_NOT_REACHABLE","RECEIVERID_NOT_FOUND");

        FehlertypCB = new ComboBox<>("Fehlertyp");
        FehlertypCB.setItems(errorList);
        FehlertypCB.setWidth("200px");

        anchor.getElement().setAttribute("download",true);
        anchor.setEnabled(false);


        qgrid.addColumn(createNachrichtIDRenderer()).setKey("ID").setHeader("Nachricht-ID").setAutoWidth(true).setSortable(true).setResizable(true).setComparator(Quarantine::getID).setFooter("Anzahl Einträge: 0");
        qgrid.addColumn(Quarantine::getEXCEPTIONCODE).setHeader("Exception-Code").setAutoWidth(true).setResizable(true).setSortable(true);
        qgrid.addColumn(createDateRenderer()).setHeader("Date").setAutoWidth(true).setSortable(true).setResizable(true).setComparator(Quarantine::getENTRANCEDATE);

//        qgrid.addColumn(Quarantine::getID).setHeader("Nachricht Extern-ID").setAutoWidth(true).setResizable(true).setSortable(true).setResizable(true);
//        qgrid.addColumn(Quarantine::getNACHRICHTIDINTERN).setHeader("Nachricht-ID Intern").setAutoWidth(true).setResizable(true).setSortable(true).setResizable(true);

//        qgrid.addColumn(Quarantine::getENTRANCEDATE).setHeader("Entrance-Date").setAutoWidth(true).setResizable(true).setSortable(true);
//        qgrid.addColumn(Quarantine::getCREATIONDATE).setHeader("Creation-Date").setAutoWidth(true).setResizable(true).setSortable(true);
        qgrid.addColumn(Quarantine::getPOBOX).setHeader("POSTBOX").setAutoWidth(true).setResizable(true).setSortable(true);

        qgrid.addColumn(createReceiverRenderer()).setHeader("Receiver").setAutoWidth(true).setSortable(true).setResizable(true);
        qgrid.addColumn(createSenderRenderer()).setHeader("Sender").setAutoWidth(true).setSortable(true).setResizable(true);

        //qgrid.addColumn(Quarantine::getRECEIVERID).setHeader("Receiver-ID").setAutoWidth(true).setResizable(true).setSortable(true);
        //qgrid.addColumn(Quarantine::getRECEIVERNAME).setHeader("Receiver-Name").setAutoWidth(true).setResizable(true).setSortable(true);
        //qgrid.addColumn(Quarantine::getSENDERID).setHeader("Sender-ID").setAutoWidth(true).setResizable(true).setSortable(true);
        //qgrid.addColumn(Quarantine::getSENDERNAME).setHeader("Sender-Name").setAutoWidth(true).setResizable(true).setSortable(true);
     //   qgrid.addColumn(Quarantine::getART).setHeader("ART").setAutoWidth(true).setResizable(true).setSortable(true);
        qgrid.addColumn(Quarantine::getFEHLERTAG).setHeader("Im FH").setAutoWidth(true).setResizable(true).setSortable(true);
      //  qgrid.addColumn(Quarantine::getVERARBEITET).setHeader("Verarbeitet").setAutoWidth(true).setResizable(true).setSortable(true);
      //  qgrid.addColumn(Quarantine::getLOESCHTAG).setHeader("Löschtag").setAutoWidth(true).setResizable(true).setSortable(true);

        qgrid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        qgrid.setColumnReorderingAllowed(true);

        HorizontalLayout hl = new HorizontalLayout();
        hl.add(comboBox,button);
        hl.setAlignItems(Alignment.BASELINE);
        setSizeFull();
      //  add(comboBox, button, paragraph, qgrid);
        add(hl,FehlertypCB,smallButton,anchor, qgrid);

        button.addClickListener(clickEvent -> {

        //    Notification.show("hole Daten...",2000, Notification.Position.TOP_END);
            qgrid.setItems();

            UI ui = UI.getCurrent();

            // Instruct client side to poll for changes and show spinner
            ui.setPollInterval(500);
            // Start background task
            CompletableFuture.runAsync(() -> {

                // Do some long running task
                try {
                    System.out.println("Hole Quarantäne Infos");

                    lq = getQuarantaene(ui);

                    //Thread.sleep(2000); //2 Sekunden warten
                    Thread.sleep(20); //2 Sekunden warten

                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Need to use access() when running from background thread
                ui.access(() -> {
                    // Stop polling and hide spinner
                    ui.setPollInterval(-1);

                    if (ret != 0) {
                        System.out.println("Keine Quarantäne Infos gefunden!");

                        return;
                    }
                    qgrid.setItems(lq);

                    refreshGrid();

                });
            });


        });


        smallButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        smallButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        smallButton.addClickListener(clickEvent -> {
            Notification.show("Exportiere Liste ");
            //System.out.println("aktuelle_SQL:" + aktuelle_SQL);

            String sql = "select EXCEPTIONCODE, ID, RECEIVERID, receivername, SENDERID, Sendername, entrancedate, creationdate  from egvp.quarantine@egvp q";

            try {

                if (!FehlertypCB.isEmpty()){
                    sql = sql + " where EXCEPTIONCODE='" + FehlertypCB.getValue() + "' and q.CREATIONDATE > sysdate-5";
                }
                else {
                    sql = sql + " where q.CREATIONDATE > sysdate-5";
                }

                System.out.println("aktuelle_SQL:" + sql);

                generateExcel(exportPath + "quarantaene_export.xls",sql);

                File file= new File(exportPath + "quarantaene_export.xls");
                StreamResource streamResource = new StreamResource(file.getName(),()->getStream(file));

                anchor.setHref(streamResource);
                //anchor = new Anchor(streamResource, String.format("%s (%d KB)", file.getName(), (int) file.length() / 1024));

                anchor.setEnabled(true);
                smallButton.setVisible(false);
                //      download("c:\\tmp\\" + aktuelle_Tabelle + ".xls");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        });

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

    private static void generateExcel(String file, String query) throws IOException {
        Configuration conf;
        conf = comboBox.getValue();

        try {
            //String url="jdbc:oracle:thin:@37.120.189.200:1521:xe";
            //String user="SYSTEM";
            //String password="Michael123";

            Class.forName("oracle.jdbc.driver.OracleDriver");

            //    Connection conn=DriverManager.getConnection(url, user, password);
            Connection conn= DriverManager.getConnection(conf.getDb_Url(), conf.getUserName(), conf.getPassword());

            //   DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());


            PreparedStatement stmt=null;
            //Workbook
            HSSFWorkbook workBook=new HSSFWorkbook();
            HSSFSheet sheet1=null;

            //Cell
            Cell c=null;

            CellStyle cs=workBook.createCellStyle();
            HSSFFont f =workBook.createFont();
            f.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
            f.setFontHeightInPoints((short) 12);
            cs.setFont(f);


            sheet1=workBook.createSheet("Sheet1");


            // String query="select  EMPNO, ENAME, JOB, MGR, HIREDATE, SAL, COMM, DEPTNO, WORK_CITY, WORK_COUNTRY from APEX_040000.WWV_DEMO_EMP";
            System.out.println("Query: " + query);
            stmt=conn.prepareStatement(query);
            ResultSet rs=stmt.executeQuery();

            ResultSetMetaData metaData=rs.getMetaData();
            int colCount=metaData.getColumnCount();

            LinkedHashMap<Integer, TableInfo> hashMap=new LinkedHashMap<Integer, TableInfo>();


            for(int i=0;i<colCount;i++){
                TableInfo tableInfo=new TableInfo();
                tableInfo.setFieldName(metaData.getColumnName(i+1).trim());
                tableInfo.setFieldText(metaData.getColumnLabel(i+1));
                tableInfo.setFieldSize(metaData.getPrecision(i+1));
                tableInfo.setFieldDecimal(metaData.getScale(i+1));
                tableInfo.setFieldType(metaData.getColumnType(i+1));
                //     tableInfo.setCellStyle(getCellAttributes(workBook, c, tableInfo));

                hashMap.put(i, tableInfo);
            }

            //Row and Column Indexes
            int idx=0;
            int idy=0;

            HSSFRow row=sheet1.createRow(idx);
            TableInfo tableInfo=new TableInfo();

            Iterator<Integer> iterator=hashMap.keySet().iterator();

            while(iterator.hasNext()){
                Integer key=(Integer)iterator.next();

                tableInfo=hashMap.get(key);
                c=row.createCell(idy);
                c.setCellValue(tableInfo.getFieldText());
                c.setCellStyle(cs);
                if(tableInfo.getFieldSize() > tableInfo.getFieldText().trim().length()){
                    sheet1.setColumnWidth(idy, (tableInfo.getFieldSize()* 10));
                }
                else {
                    sheet1.setColumnWidth(idy, (tableInfo.getFieldText().trim().length() * 5));
                }
                idy++;
            }

            while (rs.next()) {

                idx++;
                row = sheet1.createRow(idx);
                //  System.out.println(idx);
                for (int i = 0; i < colCount; i++) {

                    c = row.createCell(i);
                    tableInfo = hashMap.get(i);

                    switch (tableInfo.getFieldType()) {
                        case 1:
                            c.setCellValue(rs.getString(i+1));
                            break;
                        case 2:
                            c.setCellValue(rs.getDouble(i+1));
                            break;
                        case 3:
                            c.setCellValue(rs.getDouble(i+1));
                            break;
                        default:
                            c.setCellValue(rs.getString(i+1));
                            break;
                    }
                    c.setCellStyle(tableInfo.getCellStyle());
                }

            }
            rs.close();
            stmt.close();
            conn.close();

            // String path="c:\\tmp\\test.xls";

            FileOutputStream fileOut = new FileOutputStream(file);

            workBook.write(fileOut);
            fileOut.close();


        } catch (SQLException | FileNotFoundException e) {
            System.out.println("Error in Method generateExcel(String file, String query) file: " + file + " query: "  + query);
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static Renderer<Quarantine> createNachrichtIDRenderer() {
        return LitRenderer.<Quarantine> of(
                   "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <span> ${item.NachrichtIDExtern} </span>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      ${item.NachrichtIDIntern}" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                                )
                .withProperty("NachrichtIDIntern", Quarantine::getNACHRICHTIDINTERN)
                .withProperty("NachrichtIDExtern", Quarantine::getID);
    }


    private static Renderer<Quarantine> createDateRenderer() {
        return LitRenderer.<Quarantine> of(
                        "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <span> ${item.EntranceDate} </span>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      ${item.CreationDate}" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                )
                .withProperty("EntranceDate", Quarantine::getENTRANCEDATE)
                .withProperty("CreationDate", Quarantine::getCREATIONDATE);
    }

    private static Renderer<Quarantine> createReceiverRenderer() {
        return LitRenderer.<Quarantine> of(
                        "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <span> ${item.ReceiverName} </span>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      ${item.ReceiverID}" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                )
                .withProperty("ReceiverName", Quarantine::getRECEIVERNAME)
                .withProperty("ReceiverID", Quarantine::getRECEIVERID);
    }

    private static Renderer<Quarantine> createSenderRenderer() {
        return LitRenderer.<Quarantine> of(
                        "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <span> ${item.SenderName} </span>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      ${item.SenderID}" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                )
                .withProperty("SenderName", Quarantine::getSENDERNAME)
                .withProperty("SenderID", Quarantine::getSENDERID);
    }

    private void refreshGrid(){

        Integer anz_MessageCheckFilenames = 0;

        for ( Quarantine item: lq){
            if (item.getEXCEPTIONCODE().contains("CHECK_FILENAMES")){
                anz_MessageCheckFilenames++;
            }
        }

        qgrid.getFooterRows().get(0).getCell(qgrid.getColumnByKey("ID")).setText(String.format("Gesamt: %s", lq.size() ) + "  Anzahl CheckFilenames: " + anz_MessageCheckFilenames);

//        qgrid.getDataProvider().refreshAll();
        Notification.show("Daten wurden aktualisiert",3000, Notification.Position.TOP_END);
    }

    private List<Quarantine> getQuarantaene(UI ui) {
        //String sql = "select substr(stacktrace,1,16) as Tag, EXCEPTIONCODE, count(*) as Anzahl, 4 as AnzahlInFH from QUARANTINE a where to_date(substr(stacktrace,1,10),'YYYY-MM_DD') > sysdate-30  group by substr(stacktrace,1,16), EXCEPTIONCODE order by 1 desc";
        //String sql = "select substr(stacktrace,1,10) as Tag, EXCEPTIONCODE, count(*) as Anzahl from EGVP.QUARANTINE@EGVP a where to_date(substr(stacktrace,1,10),'YYYY-MM_DD') > sysdate-30  group by substr(stacktrace,1,10), EXCEPTIONCODE order by 1 desc";

        /*String sql= "select egvp.id, egvp.entrancedate,egvp.creationdate, egvp.pobox, egvp.exceptioncode, egvp.receiverid, egvp.receivername, egvp.senderid, egvp.Sendername\n" +
                ", m.nachrichtidintern, m.art, m.fehlertag, m.verarbeitet,m.loeschtag \n" +
                "from EGVP.QUARANTINE@EGVP egvp\n" +
                "left outer join ekp.metadaten m\n" +
                "on egvp.id=m.NACHRICHTIDEXTERN\n" +
                "where (egvp.entrancedate is not null or egvp.creationdate is not null or to_date(substr(egvp.stacktrace,1,10),'YYYY-MM-DD') > sysdate -30)\n" +
                "and nvl(egvp.entrancedate,sysdate) >= sysdate -30\n" +
                "order by egvp.creationdate desc, egvp.entrancedate desc\n";*/

        String sql= "select * from ekp.v_egvp_quarantaene";

        if (!FehlertypCB.isEmpty()){
            sql = sql + " where EXCEPTIONCODE='" + FehlertypCB.getValue() + "'";
        }

        System.out.println("Abfrage EGVP-Quarantäne: " + sql);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;

        conf = comboBox.getValue();


        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(conf.getPassword());

        //ds.setUrl("jdbc:oracle:thin:@37.120.189.200:1521:xe");
        //ds.setUsername("SYSTEM");
        //ds.setPassword("Michael123");

        try {

            jdbcTemplate.setDataSource(ds);

            lq = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Quarantine.class));



            System.out.println("EGVP-Quarantäne eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            ui.access(() -> {
                Notification.show("Fehler: " + e.getMessage(),4000, Notification.Position.MIDDLE);
            });

            return null;
        }
        return lq;
    }
    public StreamResource getStreamResource(String filename, String content) {
        return new StreamResource(filename,
                () -> new ByteArrayInputStream(content.getBytes()));
    }
}