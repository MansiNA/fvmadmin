package com.example.application.views;

import com.example.application.data.entity.Ablaufdaten;
import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.Journal;
import com.example.application.data.entity.Metadaten;
import com.example.application.data.service.ConfigurationService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@PageTitle("Tabelle Metadaten")
@Route(value = "metadaten", layout= MainLayout.class)
@RolesAllowed({"ADMIN","USER"})
//@Route(value = "")
public class MetadatenView extends VerticalLayout {

    @Autowired
    JdbcTemplate jdbcTemplate;
    private ConfigurationService service;
    private ComboBox<Configuration> comboBox;

    ComboBox searchAttribut = new ComboBox<>("Such-Attribut");

    private String exportPath;

    Grid<Metadaten> grid = new Grid<>(Metadaten.class, false);
    Grid<Journal> gridEGVP = new Grid<>(Journal.class, false);
    //Grid<Ablaufdaten> gridAblaufdaten = new Grid<>(Ablaufdaten.class, false);
    Grid<Ablaufdaten> gridAblaufdaten = new Grid<>(Ablaufdaten.class, false);

    TextField filterText = new TextField();
    Integer ret = 0;
    Button searchBtn = new Button("Suche");
    Button searchOldMsgBtn = new Button("alle obsolete Nachrichten");

    Button smallButton = new Button("Export");
    String fileName;
    private Anchor anchor;
    DateTimePicker startDateTimePicker;
    DateTimePicker endDateTimePicker;

    static List<Metadaten> metadaten;
    List<Ablaufdaten> ablaufdaten;
    List<Journal> journal;
    GridListDataView<Metadaten> dataView=grid.setItems();
    TextField searchField = new TextField();

    ConfirmDialog dialog = new ConfirmDialog();

    public MetadatenView (@Value("${csv_exportPath}") String p_exportPath, ConfigurationService service){
    //public MetadatenView (ConfigurationService service){
        this.exportPath=p_exportPath;
        this.service = service;

        fileName=exportPath + "metadaten.xls";
     //   fileName= "/data/oracle/fvmadmin/csv_export/metadaten.xls";
        anchor = new Anchor(getStreamResource(fileName, "default content"), "click to download");

        add(new H3("Anzeige von Metadaten, sowie der jeweils zugehörigen Ablaufdaten und EGVP-E Journal Einträge"));
        anchor.setEnabled(false);


        searchAttribut.setItems("NachrichtIDIntern", "NachrichtIDExtern", "Sender","Senderaktenzeichen","SenderGovelloID","SenderpostfachName","Sendergeschäftszeichen","Empfänger","Empfaengeraktenzeichen","EmpfaengerGovelloID","Empfaengerpostfachname","Weiterleitungsgovelloid","Weiterleitungspostfachname","Betreff","Bemerkung","Fachverfahren","Fachbereich","Bearbeitername");
        searchAttribut.setWidth("200px");
        searchAttribut.setValue("NachrichtIDextern");
        searchAttribut.addValueChangeListener( e -> searchField.setPlaceholder(e.getValue().toString()));

        comboBox = new ComboBox<>("Verbindung");
        smallButton.setVisible(false);
        List<Configuration> configList = service.findMessageConfigurations();

        comboBox.setItems(configList);
        comboBox.setItemLabelGenerator(Configuration::get_Message_Connection);

      //  comboBox.setValue(service.findAllConfigurations().stream().findFirst().get());

        comboBox.setValue(configList.get(1) );

        addClassName("list-view");
      //  setSizeFull();

        startDateTimePicker = new DateTimePicker(
                "Start date and time");
        //startDateTimePicker.setValue(LocalDateTime.of(2020, 8, 25, 20, 0, 0));
        startDateTimePicker.setValue(LocalDateTime.now(ZoneId.systemDefault()).minusDays(1));

        endDateTimePicker = new DateTimePicker(
                "End date and time");
        //endDateTimePicker.setValue(LocalDateTime.of(2020, 9, 1, 20, 0, 0));
        endDateTimePicker.setValue(LocalDateTime.now(ZoneId.systemDefault()));
        startDateTimePicker.addValueChangeListener(
                e -> endDateTimePicker.setMin(e.getValue()));

        HorizontalLayout hl = new HorizontalLayout();
        hl.add(comboBox,searchAttribut,startDateTimePicker, endDateTimePicker);


        add(hl);

        gridAblaufdaten.addColumn(Ablaufdaten::getNAME_NLS).setHeader("NAME_NLS").setSortable(true).setResizable(true);
     //   gridAblaufdaten.addColumn(Ablaufdaten::getNAME).setHeader("NAME").setSortable(true).setResizable(true);
        gridAblaufdaten.addColumn(Ablaufdaten::getTYP).setHeader("TYP").setSortable(true).setResizable(true);
        Grid.Column<Ablaufdaten> date = gridAblaufdaten.addColumn(Ablaufdaten::getSTART_DATUM).setHeader("Start").setSortable(true).setResizable(true);
        gridAblaufdaten.addColumn(Ablaufdaten::getENDE_DATUM).setHeader("Ende").setSortable(true).setResizable(true);
        gridAblaufdaten.addColumn(Ablaufdaten::getTIMESTAMPVERSION).setHeader("Timestamp").setSortable(true).setResizable(true);
        gridAblaufdaten.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        gridAblaufdaten.addThemeVariants(GridVariant.LUMO_COMPACT);
        gridAblaufdaten.getStyle().set("resize", "vertical");
        gridAblaufdaten.getStyle().set("overflow", "auto");
        gridAblaufdaten.setHeight("200px");


        GridSortOrder<Ablaufdaten> order = new GridSortOrder<>(date, SortDirection.DESCENDING);

        gridAblaufdaten.sort(Arrays.asList(order));



        Grid.Column<Metadaten> nachrichtidexternColumn = grid.addColumn(Metadaten::getNACHRICHTIDEXTERN).setHeader("NachrichtID-Extern")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> nachrichtidinternColumn = grid.addColumn(Metadaten::getNACHRICHTIDINTERN).setHeader("NachrichtID-Intern")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> fehlertagColumn = grid.addColumn(Metadaten::getFEHLERTAG).setHeader("Fehlertag")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> verarbeitetColumn = grid.addColumn(Metadaten::getVERARBEITET).setHeader("Verarbeitet")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> loeschtagColumn = grid.addColumn(Metadaten::getLOESCHTAG).setHeader("Löschtag")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> SENDERAKTENZEICHENColumn = grid.addColumn(Metadaten::getSENDERAKTENZEICHEN).setHeader("SENDERAKTENZEICHEN")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> fachverfahrenColumn = grid.addColumn(Metadaten::getFACHVERFAHREN).setHeader("Fachverfahren")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> eingangsdatumServerColumn = grid.addColumn(Metadaten::getEINGANGSDATUMSERVER).setHeader("Eingangsdatum")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> statusColumn = grid.addColumn(Metadaten::getSTATUS).setHeader("Status")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> nachrichttypColumn = grid.addColumn(Metadaten::getNACHRICHTTYP).setHeader("Nachricht-Typ")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> bearbeiternameColumn = grid.addColumn(Metadaten::getBEARBEITERNAME).setHeader("Bearbeitername")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> papiervorgangColumn = grid.addColumn(Metadaten::getPAPIERVORGANG).setHeader("Papiervorgang")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> transportversionColumn = grid.addColumn(Metadaten::getTRANSPORTVERSION).setHeader("Transportversion")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> artColumn = grid.addColumn(Metadaten::getART).setHeader("Art")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        Grid.Column<Metadaten> senderColumn = grid.addColumn(Metadaten::getSENDER).setHeader("Sender")
                .setAutoWidth(true).setResizable(true).setSortable(true);

        SENDERAKTENZEICHENColumn.setVisible(false);
        nachrichtidexternColumn.setVisible(false);
        nachrichttypColumn.setVisible(false);
     //   nachrichtidinternColumn.setVisible(false);
        bearbeiternameColumn.setVisible(false);
        papiervorgangColumn.setVisible(false);
        fachverfahrenColumn.setVisible(false);
        transportversionColumn.setVisible(false);
        artColumn.setVisible(false);
        senderColumn.setVisible(false);


       // grid.setItemDetailsRenderer(createPersonDetailsRenderer());
       // grid.addItemDoubleClickListener(e->showAblaufdaten(e));
        grid.addItemClickListener(e->showAblaufdaten(e));


        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
      //  GridListDataView<Metadaten> dataView =grid.setItems();

        grid.setHeight("200px");
        grid.getStyle().set("resize", "vertical");
        grid.getStyle().set("overflow", "auto");


        Button menuButton = new Button("Show/Hide Columns");
        menuButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(
                menuButton);
        columnToggleContextMenu.addColumnToggleItem("Löschtag",
                loeschtagColumn);
        columnToggleContextMenu.addColumnToggleItem("Eingangsdatum-Server",
                eingangsdatumServerColumn);
        columnToggleContextMenu.addColumnToggleItem("Fachverfahren",
                fachverfahrenColumn);
        columnToggleContextMenu.addColumnToggleItem("Senderaktenzeichen",
                SENDERAKTENZEICHENColumn);
        columnToggleContextMenu.addColumnToggleItem("Verarbeitet",
                verarbeitetColumn);
        columnToggleContextMenu.addColumnToggleItem("Status",
                statusColumn);
        columnToggleContextMenu.addColumnToggleItem("NachrichtTyp",
                nachrichttypColumn);
        columnToggleContextMenu.addColumnToggleItem("NachrichtID-intern",
                nachrichtidinternColumn);
        columnToggleContextMenu.addColumnToggleItem("NachrichtID-extern",
                nachrichtidexternColumn);
        columnToggleContextMenu.addColumnToggleItem("Bearbeitername",
                bearbeiternameColumn);
        columnToggleContextMenu.addColumnToggleItem("Papiervorgang",
        papiervorgangColumn);
        columnToggleContextMenu.addColumnToggleItem("Transportversion",
                transportversionColumn);
        columnToggleContextMenu.addColumnToggleItem("Art",
                artColumn);
        columnToggleContextMenu.addColumnToggleItem("Sender",
                senderColumn);
        columnToggleContextMenu.addColumnToggleItem("Fehlertag",
                fehlertagColumn);



        gridEGVP.addColumn(Journal::getDDATE).setHeader("Datum")
                .setAutoWidth(true).setResizable(true).setSortable(true).setResizable(true);
        gridEGVP.addColumn(Journal::getA).setHeader("Aktion")
                .setAutoWidth(true).setResizable(true).setSortable(true).setResizable(true);
        gridEGVP.addColumn(Journal::getDauer).setHeader("Laufzeit")
                .setAutoWidth(true).setResizable(true).setSortable(true).setResizable(true);
        gridEGVP.getStyle().set("resize", "vertical");
        gridEGVP.getStyle().set("overflow", "auto");
        gridEGVP.setHeight("200px");


        searchField.setWidth("500px");
        searchField.setPlaceholder("Nachricht-ID");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setClearButtonVisible(true);

        /*searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> {
                                                    System.out.println("Suche nach: " + searchField.getValue());
                                                   // metadaten=getMailboxes(searchField.getValue());
                                                   // grid.setItems(metadaten);
                                                    try{
                                                        dataView.setFilter((item -> item.getNACHRICHTIDEXTERN().toLowerCase().contains(e.getValue().toLowerCase())));
                                                        //dataView.refreshAll();
                                                    }
                                                    catch (Exception exception) {
                                                        System.out.println("Keine Eintrag gefunden..." + exception.getMessage());
                                                    }

                                                });*/


        searchOldMsgBtn.getElement().setProperty("title","Zeigt alle Nachrichten, mit EingangsdatumServer älter 60 Tage");
        searchBtn.getElement().setProperty("title","Zeigt die Metadaten zu dem gewünschten Attribut. Falls keine Einschränkung angegeben, werden nur die letzten 500 Einträge ausgegeben.");

        searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        searchOldMsgBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout layout = new HorizontalLayout(searchField,searchBtn,searchOldMsgBtn,smallButton ,anchor);
        layout.setPadding(false);

        HorizontalLayout hl1 = new HorizontalLayout();
        hl1.add(layout);
        hl1.setAlignItems(FlexComponent.Alignment.BASELINE);


        add(hl1);



        //Export Button

     //   smallButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        smallButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        smallButton.addClickListener(clickEvent -> {
            Notification.show("Exportiere ausgewählte Metadaten");
            //System.out.println("aktuelle_SQL:" + aktuelle_SQL);
    //        try {
            //    generateExcel(exportPath + aktuelle_Tabelle + ".xls",aktuelle_SQL);

            //    File file= new File(exportPath + aktuelle_Tabelle +".xls");
            //    StreamResource streamResource = new StreamResource(file.getName(),()->getStream(file));

            //    anchor.setHref(streamResource);

            //    anchor.setEnabled(true);
                smallButton.setVisible(false);
            try {
              //  generateExcel("","");


                writeObjectsToXls(metadaten,fileName);
                //writeObjectsToCsv(metadaten,"c:\\tmp\\out.csv");

                File file= new File(fileName);
                StreamResource streamResource = new StreamResource(file.getName(),()->getStream(file));

                anchor.setHref(streamResource);
                //anchor = new Anchor(streamResource, String.format("%s (%d KB)", file.getName(), (int) file.length() / 1024));

                anchor.setEnabled(true);
                smallButton.setVisible(false);


            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            //    } catch (IOException e) {
        //        throw new RuntimeException(e);
        //    }
        });



        Span title = new Span("Metadaten");
        title.getStyle().set("font-weight", "bold");
        HorizontalLayout headerLayout = new HorizontalLayout(title, menuButton);
        headerLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        headerLayout.setFlexGrow(1, title);

        add(headerLayout,grid);

        Span title2 = new Span("Ablaufdaten");
        title2.getStyle().set("font-weight", "bold");

        Span title3 = new Span("EGVP-E Journal");
        title3.getStyle().set("font-weight", "bold");

        add(title2, gridAblaufdaten,title3, gridEGVP);
        searchBtn.addClickListener(clickEvent -> {

            UI ui = UI.getCurrent();
            dataView = grid.setItems();
            dataView.refreshAll();
            metadaten=null;

            gridAblaufdaten.setItems();
            gridEGVP.setItems();

            // Instruct client side to poll for changes and show spinner
            ui.setPollInterval(500);
            // Start background task
            CompletableFuture.runAsync(() -> {

                // Do some long running task
                try {
                    System.out.println("Hole Metadaten Infos");

                    metadaten=getMetadaten(ui);


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
                        System.out.println("Keine Metadaten Infos gefunden!");
                        dataView = grid.setItems();
                        dataView.refreshAll();
                        anchor.setEnabled(false);

                        return;
                    }
                    else{
                        //grid.setItems(metadaten);
                        dataView =grid.setItems(metadaten);
                        dataView.refreshAll();
                        smallButton.setVisible(true);
                        anchor.setEnabled(false);
                    }

                });
            });


        });
        searchOldMsgBtn.addClickListener(clickEvent -> {

            UI ui = UI.getCurrent();
            dataView = grid.setItems();
            dataView.refreshAll();
            metadaten=null;

            gridAblaufdaten.setItems();
            gridEGVP.setItems();

            // Instruct client side to poll for changes and show spinner
            ui.setPollInterval(500);
            // Start background task
            CompletableFuture.runAsync(() -> {

                // Do some long running task
                try {
                    System.out.println("Hole Mailbox Infos");

                    //metadaten=getMailboxes();
                    metadaten=getMailboxesoldMsg();


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
                        System.out.println("Keine Mailbox Infos gefunden!");
                        dataView = grid.setItems();
                        dataView.refreshAll();

                        return;
                    }
                    else{
                        //grid.setItems(metadaten);
                        dataView =grid.setItems(metadaten);
                        dataView.refreshAll();
                    }

                });
            });


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
    public StreamResource getStreamResource(String filename, String content) {
        return new StreamResource(filename,
                () -> new ByteArrayInputStream(content.getBytes()));
    }
    private static class ColumnToggleContextMenu extends ContextMenu {
        public ColumnToggleContextMenu(Component target) {
            super(target);
            setOpenOnClick(true);
        }

        void addColumnToggleItem(String label, Grid.Column<Metadaten> column) {
            MenuItem menuItem = this.addItem(label, e -> {
                column.setVisible(e.getSource().isChecked());
            });
            menuItem.setCheckable(true);
            menuItem.setChecked(column.isVisible());
        }
    }

   // private void showAblaufdaten(ItemDoubleClickEvent<Metadaten> e) {
   private void showAblaufdaten(ItemClickEvent<Metadaten> e) {

        System.out.println(("Aktualisiere Ablaufdaten Grid für NachrichtidIntern: " +  e.getItem().getNACHRICHTIDINTERN()));

        try{


        gridAblaufdaten.setItems();
        gridEGVP.setItems();
        gridAblaufdaten.setItems(getAblaufdaten(e.getItem().getNACHRICHTIDINTERN().toString()));
        gridEGVP.setItems((getJournal(e.getItem().getNACHRICHTIDEXTERN().toString())));
        }
        catch(Exception exeption)
        {
            System.out.println("ERROR: Konnte Ablaufdaten nicht ermitteln:" + exeption.getMessage());
        }

    }

    //  private static ComponentRenderer<PersonDetailsFormLayout, Metadaten> createPersonDetailsRenderer() {
  //      return new ComponentRenderer<>(PersonDetailsFormLayout::new,
  //              PersonDetailsFormLayout::setAblaufdaten);
  //  }

    private boolean matchesTerm(String value, String searchTerm) {
        return value.toLowerCase().contains(searchTerm.toLowerCase());
    }

    private void configureGrid() {
        grid.addClassName("metadaten-grid");
        grid.setSizeFull();
        grid.setColumns("NACHRICHTIDINTERN","NACHRICHTIDEXTERN", "STATUS", "FEHLERTAG", "VERARBEITET", "LOESCHTAG"); // primitive Variablen der Klasse können direkt angegeben werden.

        grid.getColumns().forEach(col -> col.setAutoWidth(true));


    }


    private List<Metadaten> getMailboxes(String searchTerm) {

        String sql = "select * from EKP.Metadaten where Nachrichtidextern ='" + searchTerm +"'";

        System.out.println("Filter EKP.Metadaten (MetadatenView.java) auf " + searchTerm );

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(conf.getPassword());

        try {

            jdbcTemplate.setDataSource(ds);

            metadaten = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Metadaten.class));



            System.out.println("Metadaten eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }

        return metadaten;
    }

    private List<Metadaten> getMailboxesoldMsg() {


        String sql = "select TIMESTAMPVERSION,\n" +
                "       SENDERROLLEN,\n" +
                "       ID,\n" +
                "       nvl(to_char(EINGANGSDATUMSERVER,'dd.MM.YYYY HH24:MI'),'unbekannt') as EINGANGSDATUMSERVER,\n" +
                "       NACHRICHTIDINTERN,\n" +
                "       NACHRICHTIDEXTERN,\n" +
                "       STATUS,\n" +
                "       NACHRICHTTYP,\n" +
                "       TRANSPORTART,\n" +
                "       TRANSPORTVERSION,\n" +
                "       ART,\n" +
                "       SENDER,\n" +
                "       SENDERAKTENZEICHEN,\n" +
                "       SENDERGOVELLOID,\n" +
                "       SENDERPOSTFACHNAME,\n" +
                "       SENDERGESCHAEFTSZEICHEN,\n" +
                "       EMPFAENGER,\n" +
                "       EMPFAENGERAKTENZEICHEN,\n" +
                "       EMPFAENGERGOVELLOID,\n" +
                "       EMPFAENGERPOSTFACHNAME,\n" +
                "       WEITERLEITUNGGOVELLOID,\n" +
                "       WEITERLEITUNGPOSTFACHNAME,\n" +
                "       BETREFF,\n" +
                "       BEMERKUNG,\n" +
                "       ERSTELLUNGSDATUM,\n" +
                "       ABHOLDATUM,\n" +
                "       VERFALLSDATUM,\n" +
                "       SIGNATURPRUEFUNGSDATUM,\n" +
                "       VALIDIERUNGSDATUM,\n" +
                "       SIGNATURSTATUS,\n" +
                "       FACHVERFAHREN,\n" +
                "       FACHBEREICH,\n" +
                "       SACHGEBIET,\n" +
                "       ABTEILUNGE1,\n" +
                "       ABTEILUNGE2,\n" +
                "       PRIO,\n" +
                "       XJUSTIZVERSION,\n" +
                "       MANUELLBEARBEITETFLAG,\n" +
                "       BEARBEITERNAME,\n" +
                "       BEARBEITERKENNUNG,\n" +
                "       FEHLERTAG,\n" +
                "       PAPIERVORGANG,\n" +
                "       VERARBEITET,\n" +
                "       LOESCHTAG\n" +
                "from EKP.Metadaten \n " +
        "where verarbeitet=1 and loeschtag=0\n" +
                "and eingangsdatumserver < sysdate -60\n" +
                "or (eingangsdatumserver is null and timestampversion < sysdate -60)\n";

        System.out.println("Abfrage EKP.Metadaten (MetadatenView.java) auf oldMsg: ");
        System.out.println(sql);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(conf.getPassword());

        try {

            jdbcTemplate.setDataSource(ds);

            metadaten = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Metadaten.class));



            System.out.println("Metadaten eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }

        return metadaten;
    }
    private List<Metadaten> getMetadaten(UI ui) {


        String sql = "select TIMESTAMPVERSION,\n" +
                "       SENDERROLLEN,\n" +
                "       ID,\n" +
                "       nvl(to_char(EINGANGSDATUMSERVER,'dd.MM.YYYY HH24:MI'),'unbekannt') as EINGANGSDATUMSERVER,\n" +
                "       NACHRICHTIDINTERN,\n" +
                "       NACHRICHTIDEXTERN,\n" +
                "       STATUS,\n" +
                "       NACHRICHTTYP,\n" +
                "       TRANSPORTART,\n" +
                "       TRANSPORTVERSION,\n" +
                "       ART,\n" +
                "       SENDER,\n" +
                "       SENDERAKTENZEICHEN,\n" +
                "       SENDERGOVELLOID,\n" +
                "       SENDERPOSTFACHNAME,\n" +
                "       SENDERGESCHAEFTSZEICHEN,\n" +
                "       EMPFAENGER,\n" +
                "       EMPFAENGERAKTENZEICHEN,\n" +
                "       EMPFAENGERGOVELLOID,\n" +
                "       EMPFAENGERPOSTFACHNAME,\n" +
                "       WEITERLEITUNGGOVELLOID,\n" +
                "       WEITERLEITUNGPOSTFACHNAME,\n" +
                "       BETREFF,\n" +
                "       BEMERKUNG,\n" +
                "       ERSTELLUNGSDATUM,\n" +
                "       ABHOLDATUM,\n" +
                "       VERFALLSDATUM,\n" +
                "       SIGNATURPRUEFUNGSDATUM,\n" +
                "       VALIDIERUNGSDATUM,\n" +
                "       SIGNATURSTATUS,\n" +
                "       FACHVERFAHREN,\n" +
                "       FACHBEREICH,\n" +
                "       SACHGEBIET,\n" +
                "       ABTEILUNGE1,\n" +
                "       ABTEILUNGE2,\n" +
                "       PRIO,\n" +
                "       XJUSTIZVERSION,\n" +
                "       MANUELLBEARBEITETFLAG,\n" +
                "       BEARBEITERNAME,\n" +
                "       BEARBEITERKENNUNG,\n" +
                "       FEHLERTAG,\n" +
                "       PAPIERVORGANG,\n" +
                "       VERARBEITET,\n" +
                "       LOESCHTAG\n" +
                "from EKP.Metadaten \n ";
                //"where nachrichtidextern is not null and (eingangsdatumserver is null or eingangsdatumserver > sysdate -1)";

        DateTimeFormatter formatters = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;
        String sd = null;
        try {
            fromDate = LocalDateTime.from(startDateTimePicker.getValue());
            toDate = LocalDateTime.from(endDateTimePicker.getValue());
            //sd = new SimpleDateFormat("dd.MM.yyyy hh24:mm:ss").format(startDateTimePicker.getValue());
        }
        catch (Exception ex)
        {
            System.out.println(ex.getMessage());
        }
      //  java.sql.Date sqlfromDate = java.sql.Date.valueOf(String.valueOf(fromDate));

      //  LocalDate toDate = LocalDate.from(endDateTimePicker.getValue());
      //  java.sql.Date sqltoDate = java.sql.Date.valueOf(toDate);

        if (searchField.getValue().isEmpty()) //Wenn nach nichts gesucht wird, nur auf Zeitraum einschränken:
        {
        //    sql = sql + "where lower(nachrichtidextern) like '%' and rownum < 500";
            sql = sql + "where eingangsdatumserver >= to_date('" + fromDate.format(formatters) + "','DD.MM.YYYY HH24:MI:SS')";
            sql = sql + " and eingangsdatumserver <= to_date('" + toDate.format(formatters) + "','DD.MM.YYYY HH24:MI:SS')";
         //   sql = sql + "where eingangsdatumserver >= to_date('" + sd + "','DD.MM.YYYY HH24:MI:SS')";
        }
        else if (!searchAttribut.getValue().toString().contains("NachrichtIDIntern"))
        {
            //sql = sql + "where lower(nachrichtidextern) like '%" + searchField.getValue().toLowerCase() + "%'" + "\nor nachrichtidintern=" + tryParseInt(searchField.getValue(),0);

            sql = sql + "where lower( " + searchAttribut.getValue().toString() + ") like '%" + searchField.getValue().toLowerCase() + "%'";

        }
        else //Nach NachrichtID-Intern suchen:
        {
            sql = sql + " where nachrichtidintern=" + tryParseInt(searchField.getValue(),0);
        }


//                + "or nachrichtidintern=" + Search;

        System.out.println("Abfrage EKP.Metadaten (MetadatenView.java): ");
        System.out.println("Von: " + startDateTimePicker.getValue());
        System.out.println("Bis: " + endDateTimePicker.getValue());


     //   Notification notification = Notification.show("Abfgae gestartet!: ");
     //   notification.setPosition(Notification.Position.MIDDLE);

        System.out.println(sql);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(conf.getPassword());

        try {

            jdbcTemplate.setDataSource(ds);

            metadaten = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Metadaten.class));



            System.out.println("Metadaten eingelesen");

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            ui.access(() -> {
                dialog.setText(new Html("<p>" + e.getMessage()+ "</p>"));
                dialog.setWidth("800px");
                dialog.setHeader("DB Abfrage fehlgeschlagen");
                dialog.setConfirmText("OK");
                dialog.open();
            });




        }

        return metadaten;
    }


    public int tryParseInt(String value, int defaultVal) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private List<Ablaufdaten> getAblaufdaten(String nachrichtidintern) {

        String sql = "select * from EKP.Ablaufdaten where Nachrichtidintern ='" + nachrichtidintern +"'";

        System.out.println("Filter EKP.Ablaufdaten (MetadatenView.java) auf " + nachrichtidintern );

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(conf.getPassword());

        try {

            jdbcTemplate.setDataSource(ds);

            ablaufdaten = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Ablaufdaten.class));



            System.out.println("Ablaufdaten eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }

        return ablaufdaten;
    }

    private List<Journal> getJournal(String nachrichtidextern) {

      //  String sql = "select * from EKP.Ablaufdaten where Nachrichtidintern ='" + nachrichtidintern +"'";


       /* String sql = "select ddate,a,ddate - lead(ddate) over (partition by j.pid order by ddate desc)  as dauer  \n" +
                "from egvp.journal@egvp j\n" +
                "inner join EGVP.PRotocol@EGVP p\n" +
                "on j.PID=p.PID\n" +
                "where p.customid='" + nachrichtidextern + "'\n" +
                "or p.MESSAGEID='" + nachrichtidextern + "'\n" +
                "order by 1 desc\n";*/
        String sql = "select ddate,Customid, MessageID, a, dauer from ekp.v_egvp_info where customid='" + nachrichtidextern + "'\n" +
                "or MESSAGEID='" + nachrichtidextern + "'\n" +
                "order by 1 desc\n";

        System.out.println("(MetadatenView.java) Hole Journal Einträge für  " + nachrichtidextern );

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(conf.getPassword());

        try {

            jdbcTemplate.setDataSource(ds);

            journal = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Journal.class));



            System.out.println("Journal eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }

        return journal;
    }

    private static class PersonDetailsFormLayout extends FormLayout {
        private final TextField emailField = new TextField("NachrichtidExtern");


        public PersonDetailsFormLayout() {
            Stream.of(emailField).forEach(field -> {
                field.setReadOnly(true);
                add(field);
            });

            setResponsiveSteps(new ResponsiveStep("0", 3));
            setColspan(emailField, 3);

        }

        public void setAblaufdaten(Configuration conf, Metadaten person) {
            emailField.setValue(person.getNACHRICHTIDEXTERN());
      //      getAblaufdaten(person.getNACHRICHTIDINTERN());

        }


    }

    private static void writeObjectsToCsv(List<Metadaten> objects, String filePath) throws IOException {
        FileWriter out = new FileWriter(filePath);
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL)) {
            for (Metadaten obj : objects) {
                printer.printRecord(obj.getNACHRICHTIDEXTERN(), obj.getNACHRICHTIDINTERN(), obj.getART());
            }
        }
    }


    public static void writeObjectsToXls(List<Metadaten> objects, String filePath) throws IOException {
        // Erstellen Sie ein Workbook-Objekt
        Workbook workbook = new HSSFWorkbook();
        // Erstellen Sie ein neues Arbeitsblatt im Workbook
        Sheet sheet = workbook.createSheet("Metadata");

        // Erstellen Sie die Header-Zeile im Arbeitsblatt
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("ID");
        headerRow.createCell(1).setCellValue("NACHRICHTIDINTERN");
        headerRow.createCell(2).setCellValue("NACHRICHTIDEXTERN");
        headerRow.createCell(3).setCellValue("STATUS");
        headerRow.createCell(4).setCellValue("NACHRICHTTYP");
        headerRow.createCell(5).setCellValue("TRANSPORTART");
        headerRow.createCell(6).setCellValue("TRANSPORTVERSION");
        headerRow.createCell(7).setCellValue("ART");
        headerRow.createCell(8).setCellValue("SENDER");
        headerRow.createCell(9).setCellValue("SENDERAKTENZEICHEN");
        headerRow.createCell(10).setCellValue("SENDERGOVELLOID");
        headerRow.createCell(11).setCellValue("SENDERPOSTFACHNAME");
        headerRow.createCell(12).setCellValue("SENDERGESCHAEFTSZEICHEN");
        headerRow.createCell(13).setCellValue("EMPFAENGER");
        headerRow.createCell(14).setCellValue("EMPFAENGERAKTENZEICHEN");
        headerRow.createCell(15).setCellValue("EMPFAENGERGOVELLOID");
        headerRow.createCell(16).setCellValue("EMPFAENGERPOSTFACHNAME");
        headerRow.createCell(17).setCellValue("WEITERLEITUNGGOVELLOID");
        headerRow.createCell(18).setCellValue("WEITERLEITUNGPOSTFACHNAME");
        headerRow.createCell(19).setCellValue("BETREFF");
        headerRow.createCell(20).setCellValue("BEMERKUNG");
        headerRow.createCell(21).setCellValue("ERSTELLUNGSDATUM");
        headerRow.createCell(22).setCellValue("ABHOLDATUM");
        headerRow.createCell(23).setCellValue("EINGANGSDATUMSERVER");
        headerRow.createCell(24).setCellValue("VERFALLSDATUM");
        headerRow.createCell(25).setCellValue("SIGNATURPRUEFUNGSDATUM");
        headerRow.createCell(26).setCellValue("VALIDIERUNGSDATUM");
        headerRow.createCell(27).setCellValue("SIGNATURSTATUS");
        headerRow.createCell(28).setCellValue("FACHVERFAHREN");
        headerRow.createCell(29).setCellValue("FACHBEREICH");
        headerRow.createCell(30).setCellValue("SACHGEBIET");
        headerRow.createCell(31).setCellValue("ABTEILUNGE1");
        headerRow.createCell(32).setCellValue("ABTEILUNGE2 ");
        headerRow.createCell(33).setCellValue("PRIO");
        headerRow.createCell(34).setCellValue("XJUSTIZVERSION");
        headerRow.createCell(35).setCellValue("MANUELLBEARBEITETFLAG");
        headerRow.createCell(36).setCellValue("BEARBEITERNAME");
        headerRow.createCell(37).setCellValue("BEARBEITERKENNUNG");
        headerRow.createCell(38).setCellValue("FEHLERTAG");
        headerRow.createCell(39).setCellValue("PAPIERVORGANG");
        headerRow.createCell(40).setCellValue("VERARBEITET");
        headerRow.createCell(41).setCellValue("LOESCHTAG");
        headerRow.createCell(42).setCellValue("SENDERROLLEN");
        headerRow.createCell(43).setCellValue("TIMESTAMPVERSION");



        // Fügen Sie die Daten in das Arbeitsblatt ein
        int rowNum = 1;
        for (Metadaten obj : objects) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(obj.getID());
            row.createCell(1).setCellValue(obj.getNACHRICHTIDINTERN());
            row.createCell(2).setCellValue(obj.getNACHRICHTIDEXTERN());
            row.createCell(3).setCellValue(obj.getSTATUS());
            row.createCell(4).setCellValue(obj.getNACHRICHTTYP());
            row.createCell(5).setCellValue(obj.getTRANSPORTVERSION());
            row.createCell(6).setCellValue(obj.getTRANSPORTART());
            row.createCell(7).setCellValue(obj.getART());
            row.createCell(8).setCellValue(obj.getSENDER());
            row.createCell(9).setCellValue(obj.getSENDERAKTENZEICHEN());
            row.createCell(10).setCellValue(obj.getSENDERGOVELLOID());
            row.createCell(11).setCellValue(obj.getSENDERPOSTFACHNAME());
            row.createCell(12).setCellValue(obj.getSENDERGESCHAEFTSZEICHEN());
            row.createCell(13).setCellValue(obj.getEMPFAENGER());
            row.createCell(14).setCellValue(obj.getEMPFAENGERAKTENZEICHEN());
            row.createCell(15).setCellValue(obj.getEMPFAENGERGOVELLOID());
            row.createCell(16).setCellValue(obj.getEMPFAENGERPOSTFACHNAME());
            row.createCell(17).setCellValue(obj.getWEITERLEITUNGGOVELLOID());
            row.createCell(18).setCellValue(obj.getWEITERLEITUNGPOSTFACHNAME());
            row.createCell(19).setCellValue(obj.getBETREFF());
            row.createCell(20).setCellValue(obj.getBEMERKUNG());
            row.createCell(21).setCellValue(obj.getERSTELLUNGSDATUM());
            row.createCell(22).setCellValue(obj.getABHOLDATUM());
            row.createCell(23).setCellValue(obj.getEINGANGSDATUMSERVER());
            row.createCell(24).setCellValue(obj.getVERFALLSDATUM());
            row.createCell(25).setCellValue(obj.getSIGNATURPRUEFUNGSDATUM());
            row.createCell(26).setCellValue(obj.getVALIDIERUNGSDATUM());
            row.createCell(27).setCellValue(obj.getSIGNATURSTATUS());
            row.createCell(28).setCellValue(obj.getFACHVERFAHREN());
            row.createCell(29).setCellValue(obj.getFACHBEREICH());
            row.createCell(30).setCellValue(obj.getSACHGEBIET());
            row.createCell(31).setCellValue(obj.getABTEILUNGE1());
            row.createCell(32).setCellValue(obj.getABTEILUNGE2());
            row.createCell(33).setCellValue(obj.getPRIO());
            row.createCell(34).setCellValue(obj.getXJUSTIZVERSION());
            row.createCell(35).setCellValue(obj.getMANUELLBEARBEITETFLAG());
            row.createCell(36).setCellValue(obj.getBEARBEITERNAME());
            row.createCell(37).setCellValue(obj.getBEARBEITERKENNUNG());
            row.createCell(38).setCellValue(obj.getFEHLERTAG());
            row.createCell(39).setCellValue(obj.getPAPIERVORGANG());
            row.createCell(40).setCellValue(obj.getVERARBEITET());
            row.createCell(41).setCellValue(obj.getLOESCHTAG());
            row.createCell(42).setCellValue(obj.getSENDERROLLEN());
            row.createCell(43).setCellValue(obj.getTIMESTAMPVERSION());

        }

        // Schreiben Sie das Workbook in eine Datei
        FileOutputStream fileOut = new FileOutputStream(filePath);
        workbook.write(fileOut);
        fileOut.close();
        workbook.close();
    }

}
