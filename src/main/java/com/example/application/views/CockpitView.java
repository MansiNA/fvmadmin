package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.fvm_monitoring;
import com.example.application.data.service.ConfigurationService;
import com.example.application.utils.myCallback;
import com.example.application.views.list.MonitoringForm;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.richtexteditor.RichTextEditorVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.wontlost.ckeditor.Config;
import com.wontlost.ckeditor.Constants;
import jakarta.annotation.security.RolesAllowed;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.wontlost.ckeditor.VaadinCKEditor;
import com.wontlost.ckeditor.VaadinCKEditorBuilder;

@PageTitle("eKP-Cokpit | by DBUSS GmbH")
@Route(value = "cockpit", layout= MainLayout.class)
@RolesAllowed("ADMIN")
@CssImport(
        themeFor = "vaadin-grid",
        value = "./styles/styles.css"
)
public class CockpitView extends VerticalLayout{
    @Autowired
    JdbcTemplate jdbcTemplate;

    myCallback callback=new myCallback() {
        @Override
        public void delete(fvm_monitoring mon) {
            System.out.println("Delete in CockpitView aufgerufen für " + mon.getTitel());
        }

        @Override
        public void save(fvm_monitoring mon) {
            System.out.println("Save in CockpitView aufgerufen für " + mon.getTitel());



            /*String sql = "update FVM_MONITORING \n" +
                    "set SQL='" + mon.getSQL() + "',\n" +
                    "TITEL='" + mon.getTitel() + "',\n" +
                    "Beschreibung='" + mon.getBeschreibung() + "',\n" +
                    "Handlungs_Info='" + mon.getHandlungs_INFO() + "',\n" +
                    "Check_Intervall=" + mon.getCheck_Intervall() + ",\n" +
                    "WARNING_SCHWELLWERT=" + mon.getWarning_Schwellwert() + ",\n" +
                    "ERROR_SCHWELLWERT=" + mon.getError_Schwellwert() + ",\n" +
                    "IS_ACTIVE='" + mon.getIS_ACTIVE() + "'\n" +
                    "SQL_Detail='"+ mon.getSQL_Detail() + "'\n" +
                    "where id=" + mon.getID();
*/

            String sql = "update FVM_MONITORING set " +
                    " SQL=?, " +
                    " TITEL=?," +
                    " Beschreibung=?," +
                    " Handlungs_Info=?," +
                    " Check_Intervall=?, " +
                    " WARNING_SCHWELLWERT=?, " +
                    " ERROR_SCHWELLWERT=?, " +
                    " IS_ACTIVE=?, " +
                    " SQL_Detail=? " +
                    "where id= ?";


            System.out.println("Update FVM_Monitoring (CockpitView.java): ");
            System.out.println(sql);

            DriverManagerDataSource ds = new DriverManagerDataSource();
            com.example.application.data.entity.Configuration conf;
            conf = comboBox.getValue();

            ds.setUrl(conf.getDb_Url());
            ds.setUsername(conf.getUserName());
            ds.setPassword(Configuration.decodePassword(conf.getPassword()));

            try {
                jdbcTemplate.setDataSource(ds);

                jdbcTemplate.update(sql , mon.getSQL()
                        , mon.getTitel()
                        , mon.getBeschreibung()
                        , mon.getHandlungs_INFO()
                        , mon.getCheck_Intervall()
                        , mon.getWarning_Schwellwert()
                        , mon.getError_Schwellwert()
                        , mon.getIS_ACTIVE()
                        , mon.getSQL_Detail()
                        , mon.getID()
                );

                //     jdbcTemplate.update(sql);
                System.out.println("Update durchgeführt");

            } catch (Exception e) {
                System.out.println("Exception: " + e.getMessage());
            }
            finally {
                form.setVisible(false);
            }

        }

        @Override
        public void cancel() {
            form.setVisible(false);
        }
    };
    Grid<fvm_monitoring> grid = new Grid<>(fvm_monitoring.class, false);

    Grid<LinkedHashMap<String, Object>> grid_metadata = new Grid<>();

    Dialog dialog_Beschreibung = new Dialog();
    Dialog dialog_Editor = new Dialog();

    // RichTextEditor editor = new RichTextEditor();

    private ComboBox<Configuration> comboBox;
    static List<fvm_monitoring> param_Liste = new ArrayList<fvm_monitoring>();
    static List<fvm_monitoring> monitore;

    static VerticalLayout content = new VerticalLayout();;
    static Tab details;
    static Tab payment;
    //static Tab data;
    //static TextField titel;

    static fvm_monitoring akt_mon;
    private ScheduledExecutorService executor;

    Checkbox autorefresh = new Checkbox();

    private Label lastRefreshLabel;
    private Label countdownLabel;

    private UI ui ;
    Instant startTime;
    ConfigurationService service;
    MonitoringForm form;


    public CockpitView(JdbcTemplate jdbcTemplate, ConfigurationService service) {
        this.jdbcTemplate = jdbcTemplate;
        this.service=service;

        addClassName("cockpit-view");
        setSizeFull();

        configureGrid();

        countdownLabel = new Label();
        lastRefreshLabel=new Label();

        ui= UI.getCurrent();

        Button bt = new Button("Test");

        bt.addClickListener(e -> {
            System.out.println("Test-Button gedrückt");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://jsonplaceholder.typicode.com/albums")).build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    //.thenAccept(System.out::println)
                    .thenApply(CockpitView::parse)
                    .join();
        });


        Button xmlBt = new Button("XML");

        xmlBt.addClickListener(e -> {
            System.out.println("XML-Button gedrückt");
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://jsonplaceholder.typicode.com/albums")).build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    //.thenAccept(System.out::println)
                    //.thenApply(CockpitView::parse)
                    .thenApply(CockpitView::xmlpare)
                    .join();
        });



        //  add(xmlBt);


        H2 h2 = new H2("ekP / EGVP-E Monitoring");
        add(h2);

        //editor.setVisible(false);

        // String htmlContent = "<h1>Beispielüberschrift</h1><p>Dies ist der Inhalt des Editors.</p>";
        // editor.asHtml().setValue(htmlContent);

        //add(editor);
        //editor.setVisible(false);



        Button closeButton = new Button("close", e -> dialog_Beschreibung.close());
        Button cancelEditButton = new Button("cancel", e -> dialog_Editor.close());
        Button saveEditButton = new Button("save", e -> {

            saveMonitor();
            dialog_Editor.close();
        });
        dialog_Beschreibung.getFooter().add(closeButton);

        dialog_Editor.getFooter().add(cancelEditButton,saveEditButton);


        //   HorizontalLayout layout = new HorizontalLayout(comboBox,refreshBtn);





        form = new MonitoringForm(callback);
        form.setVisible(false);



        add(getToolbar(),grid,form );


    }

    private void saveMonitor() {
        System.out.println("Titel:" + akt_mon.getTitel());

        System.out.println("Save gedrückt");
    }

    private Component getToolbar() {

        Button refreshBtn = new Button("refresh");
        refreshBtn.getElement().setProperty("title","Daten neu einlesen");
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        refreshBtn.addClickListener(clickEvent -> {

            param_Liste=getMonitoring();
            grid.setItems(param_Liste);
            updateLastRefreshLabel();

        });

        comboBox = new ComboBox<>("Verbindung");
        try {
            List<Configuration> configList = service.findMessageConfigurations();
            comboBox.setItems(configList);
            comboBox.setValue(configList.get(1));

            comboBox.setItemLabelGenerator(Configuration::getName);

        } catch (Exception e) {
            // Display the error message to the user
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }

        autorefresh.setLabel("Autorefresh");

        autorefresh.addClickListener(e->{

            if (autorefresh.getValue()){
                System.out.println("Autorefresh wird eingeschaltet.");
                startCountdown(Duration.ofSeconds(60));
                countdownLabel.setVisible(true);
            }
            else{
                System.out.println("Autorefresh wird ausgeschaltet.");
                stopCountdown();
                countdownLabel.setVisible(false);
            }

        });


        updateLastRefreshLabel();

        HorizontalLayout layout = new HorizontalLayout(comboBox,refreshBtn,lastRefreshLabel, autorefresh, countdownLabel);
        layout.setPadding(false);
        layout.setAlignItems(Alignment.BASELINE);

        return layout;

    }

    private void updateLastRefreshLabel() {
        LocalTime currentTime = LocalTime.now();
        String formattedTime = currentTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

        lastRefreshLabel.setText("letzte Aktualisierung: " + formattedTime);
    }

    private void configureGrid() {
        /*grid.addColumn(fvm_monitoring::getID).setHeader("ID")
                .setAutoWidth(true).setResizable(true).setSortable(true);*/
        grid.addColumn(fvm_monitoring::getTitel).setHeader("Titel")
                .setAutoWidth(true).setResizable(true).setSortable(true);
      /*  grid.addColumn(fvm_monitoring::getCheck_Intervall).setHeader("Intervall")
                .setAutoWidth(true).setResizable(true).setSortable(true); */
        grid.addColumn(fvm_monitoring::getWarning_Schwellwert).setHeader("Warning Schwellwert")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        grid.addColumn(fvm_monitoring::getError_Schwellwert ).setHeader("Error Schwellwert")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        grid.addColumn(fvm_monitoring::getAktueller_Wert).setHeader("Aktuell")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        //  grid.addColumn(fvm_monitoring::getBeschreibung).setHeader("Beschreibung")
        //          .setAutoWidth(true).setResizable(true).setSortable(true);
        //  grid.addColumn(fvm_monitoring::getHandlungs_INFO).setHeader("Handlungsinfo")
        //          .setAutoWidth(true).setResizable(true).setSortable(true);

        // Spalte für den Fortschritt mit ProgressBarRenderer
        grid.addColumn(new ComponentRenderer<>(item -> {
            ProgressBar progressBar = new ProgressBar();

            progressBar.setValue(item.getError_Prozent()); // Wert zwischen 0 und 1
            //progressBar.setValue(0.8); // Wert zwischen 0 und 1

            Double p = item.getError_Prozent();
            Double rounded;
            p = p*100;
            rounded = (double) Math.round(p);
            Text t = new Text(rounded.toString() + "%");
            HorizontalLayout hl = new HorizontalLayout();
            hl.add(t,progressBar);

            return hl;
        })).setHeader("Auslastung").setWidth("150px").setResizable(true);

        grid.addColumn(fvm_monitoring::getIS_ACTIVE).setHeader("Aktiv")
                .setAutoWidth(true).setResizable(true).setSortable(true);


        grid.setItemDetailsRenderer(createPersonDetailsRenderer());
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        // grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        // grid.setDetailsVisibleOnClick(false);

        grid.addItemClickListener(event -> {

            //  System.out.println("ClickEvent:" + event.getItem().getTitel());

            if (((ClickEvent) event).getClickCount() == 2){
                System.out.println("Double ClickEvent:" + event.getItem().getTitel());
                grid.setDetailsVisible(event.getItem(), !grid.isDetailsVisible(event.getItem()));
            }

            //   ClickEvent<fvm_monitoring> clickEvent = (ClickEvent<fvm_monitoring>) event;
            //   if (clickEvent.getClickCount() == 2) {
            //       grid.setDetailsVisible(event.getItem(), !grid.isDetailsVisible(event.getItem()));
            //   }
        });





        grid.setItems(param_Liste);
        grid.setHeight("800px");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid.setThemeName("dense");

        MonitorContextMenu contextMenu = new MonitorContextMenu(grid);

        grid.setClassNameGenerator(person -> {

            if (person.getAktueller_Wert() != null && person.getWarning_Schwellwert() != null && person.getError_Schwellwert() != null ) {

                if (person.getAktueller_Wert() >= person.getWarning_Schwellwert() && person.getAktueller_Wert() < person.getError_Schwellwert())
                    return "warning";
                if (person.getAktueller_Wert() >= person.getError_Schwellwert())
                    return "error";
            }
            return null;
        });



/*
        grid.addComponentColumn(file -> {
            MenuBar menuBar = new MenuBar();
            menuBar.addThemeVariants(MenuBarVariant.LUMO_TERTIARY);
            MenuItem menuItem = menuBar.addItem("•••");
            menuItem.getElement().setAttribute("aria-label", "More options");
            SubMenu subMenu = menuItem.getSubMenu();
            subMenu.addItem("Beschreibung", event -> {



                fvm_monitoring currentItem = new fvm_monitoring();
                try {
                    currentItem = grid.getSelectionModel().getFirstSelectedItem().get();
                }
                catch ( Exception e)
                {};

                if(currentItem != null){

                System.out.println("Beschreibung ausgewählt für ID: " + currentItem.getID() );
                dialog_Beschreibung.setHeaderTitle("Beschreibung des Monitors >" + currentItem.getTitel() +"<");

                VerticalLayout dialogLayout = createDialogLayout(currentItem.getBeschreibung());
                dialog_Beschreibung.removeAll();
                dialog_Beschreibung.add(dialogLayout);
                dialog_Beschreibung.setModal(false);
                dialog_Beschreibung.setDraggable(true);
                dialog_Beschreibung.setResizable(true);
                dialog_Beschreibung.open();
                }
            });
            subMenu.addItem("Handlungsanweisung", event -> {
                System.out.println("Handlungsanweisung ausgewählt...");
            });
            return menuBar;
        }).setWidth("120px").setFlexGrow(0);*/


    }

    private void stopCountdown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    private Duration calculateRemainingTime(Duration duration, Instant startTime) {
        Instant now = Instant.now();
        Instant endTime = startTime.plus(duration);
        return Duration.between(now, endTime);
    }

    private void updateCountdownLabel(Duration remainingTime) {
        long seconds = remainingTime.getSeconds();
        String formattedTime = String.format("%02d", (seconds % 60));

        if (remainingTime.isNegative()){
            startTime = Instant.now();
            param_Liste=getMonitoring();
            grid.setItems(param_Liste);
            updateLastRefreshLabel();
            return;
        }

        countdownLabel.setText("in " + formattedTime + " Sekunden");
    }

    private void startCountdown(Duration duration) {
        executor = Executors.newSingleThreadScheduledExecutor();

        startTime = Instant.now();

        executor.scheduleAtFixedRate(() -> {
            ui.access(() -> {
                Duration remainingTime = calculateRemainingTime(duration, startTime);
                updateCountdownLabel(remainingTime);
            });
        }, 0, 1, java.util.concurrent.TimeUnit.SECONDS);
    }


    private static ComponentRenderer<PersonDetailsFormLayout, fvm_monitoring> createPersonDetailsRenderer() {
        return new ComponentRenderer<>(PersonDetailsFormLayout::new,
                PersonDetailsFormLayout::setPerson);
    }



    private static class PersonDetailsFormLayout extends FormLayout {
        private final TextField lastRefreshField = new TextField("Letzte Aktualisierung");
        private final TextField refreshIntervallField = new TextField("Refresh-Intervall");
        private final TextField idField = new TextField("ID");

        public PersonDetailsFormLayout() {
            Stream.of(idField,refreshIntervallField,lastRefreshField).forEach(field -> {
                field.setReadOnly(true);
                add(field);
            });

            setResponsiveSteps(new ResponsiveStep("0", 3));

        }

        public void setPerson(fvm_monitoring person) {

            if (person.getZeitpunkt() != null) {

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                String dateAsString = dateFormat.format(person.getZeitpunkt());

                lastRefreshField.setValue(dateAsString);
            }
            else {
                lastRefreshField.setValue("unbekannt...");
            }
            idField.setValue(person.getID().toString());
            refreshIntervallField.setValue(person.getCheck_Intervall().toString());
        }
    }

    private static String xmlpare(String xml) {


        String input = "<data><object><property name=\"body\"><object><property name=\"item\"><object><property name=\"name\"><value type=\"string\">AdminServer</value></property><property name=\"state\"><value type=\"string\">RUNNING</value></property><property name=\"health\"><value type=\"string\">HEALTH_OK</value></property><property name=\"clusterName\"><value/></property><property name=\"currentMachine\"><value type=\"string\"></value></property><property name=\"weblogicVersion\"><value type=\"string\">WebLogic Server 12.2.1.4.0 Thu Sep 12 04:04:29 GMT 2019 1974621</value></property><property name=\"openSocketsCurrentCount\"><value type=\"number\">1</value></property><property name=\"heapSizeMax\"><value type=\"number\">518979584</value></property><property name=\"oSVersion\"><value type=\"string\">5.4.0-42-generic</value></property><property name=\"oSName\"><value type=\"string\">Linux</value></property><property name=\"javaVersion\"><value type=\"string\">1.8.0_202</value></property><property name=\"heapSizeCurrent\"><value type=\"number\">284954624</value></property><property name=\"heapFreeCurrent\"><value type=\"number\">76093760</value></property></object></property></object></property><property name=\"messages\"><array></array></property></object></data>";

        // Extract property name-value pairs
        Pattern pattern = Pattern.compile("<property name=\"(.*?)\"><value(.*?)>(.*?)</value></property>");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String name = matcher.group(1);
            String value = matcher.group(3);

            System.out.println("Name: " + name);
            System.out.println("Value: " + value);
            System.out.println();
        }


        return null;

    }

    public static String parse(String responseBody){

        JSONArray albums = new JSONArray(responseBody);
        for ( int i = 0; i < albums.length();i++){
            JSONObject album = albums.getJSONObject(i);
            int id = album.getInt("id");
            int userId = album.getInt("userId");
            String title = album.getString("title");
            System.out.println(id + "  " + title + " "  + userId);

        }


        return null;
    }

    private class MonitorContextMenu extends GridContextMenu<fvm_monitoring> {
        public MonitorContextMenu(Grid<fvm_monitoring> target) {
            super(target);

            addItem("Beschreibung", e -> e.getItem().ifPresent(a -> {
                System.out.printf("Edit: %s%n", a.getID());
                dialog_Beschreibung.setHeaderTitle(a.getTitel());
                //  VerticalLayout dialogLayout = createDialogLayout(person.getBeschreibung());

                VerticalLayout dialogLayout =showDialog(a);

                dialog_Beschreibung.removeAll();
                dialog_Beschreibung.add(dialogLayout);
                dialog_Beschreibung.setModal(false);
                dialog_Beschreibung.setDraggable(true);
                dialog_Beschreibung.setResizable(true);
                dialog_Beschreibung.setWidth("800px");
                dialog_Beschreibung.setHeight("600px");
                dialog_Beschreibung.open();
            }));


            addItem("Show Data", e -> e.getItem().ifPresent(a -> {
                //  System.out.printf("Daten anzeigen für: %s%n", a.getID());

                dialog_Beschreibung.setHeaderTitle("Detailabfrage für " + a.getTitel() + " (ID: " + a.getID() + ")");
                VerticalLayout dialogLayout = null;
                try {
                    dialogLayout = createDialogData(a.getSQL_Detail());
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                dialog_Beschreibung.removeAll();
                dialog_Beschreibung.add(dialogLayout);
                dialog_Beschreibung.setModal(false);
                dialog_Beschreibung.setDraggable(true);
                dialog_Beschreibung.setResizable(true);
                dialog_Beschreibung.open();

            }));




            addItem("Historie", e -> e.getItem().ifPresent(monitor -> {
                dialog_Beschreibung.setHeaderTitle("Historie für " + monitor.getTitel() + " (ID: " + monitor.getID() + ")");
                VerticalLayout dialogLayout = createDialogGraph(monitor.getID());

                dialog_Beschreibung.removeAll();
                dialog_Beschreibung.add(dialogLayout);
                dialog_Beschreibung.setModal(false);
                dialog_Beschreibung.setDraggable(true);
                dialog_Beschreibung.setResizable(true);
                dialog_Beschreibung.open();


                //System.out.printf("Delete: %s%n", person.getID());
            }));

            add(new Hr());

            addItem("edit", e -> e.getItem().ifPresent(monitor -> {
                System.out.printf("Edit: %s%n", monitor.getID());

                //   form.setVisible(true);
                //  form.setContact(monitor);

                showEditDialog(monitor);

            }));

            addItem("refresh", e -> e.getItem().ifPresent(person -> {
                System.out.printf("refresh im ContextMenü aufgerufen: %s%n", person.getID());
                refreshMonitor(person.getID());



            }));

         /*   GridMenuItem<fvm_monitoring> emailItem = addItem("Email",
                    e -> e.getItem().ifPresent(person -> {
                        // System.out.printf("Email: %s%n",
                        // person.getFullName());
                    }));
            GridMenuItem<fvm_monitoring> phoneItem = addItem("Call",
                    e -> e.getItem().ifPresent(person -> {
                        // System.out.printf("Phone: %s%n",
                        // person.getFullName());
                    }));*/

            setDynamicContentHandler(person -> {
                // Do not show context menu when header is clicked
                if (person == null)
                    return false;
                // emailItem.setText(String.format("Email: %s", person.getTitel()));
                return true;
            });
        }


    }

    private void refreshMonitor(Integer id) {

        String sql="begin parallel_proc(''," + id + "); end;";
        System.out.println(sql);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));

        try {

            jdbcTemplate.setDataSource(ds);

            jdbcTemplate.execute(sql);

            System.out.println("Refresh ausgeführt");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }

        return ;

    }

    private void fill_grid_metadata(String sql) throws SQLException, IOException {
        System.out.println(sql);
        // Create the grid and set its items
        //Grid<LinkedHashMap<String, Object>> grid2 = new Grid<>();
        grid_metadata.removeAllColumns();

        //List<LinkedHashMap<String,Object>> rows = retrieveRows("select * from EKP.ELA_FAVORITEN where rownum<200");
        List<LinkedHashMap<String,Object>> rows = retrieveRows(sql);

        if(!rows.isEmpty()){
            grid_metadata.setItems( rows); // rows is the result of retrieveRows

            // Add the columns based on the first row
            LinkedHashMap<String, Object> s = rows.get(0);
            for (Map.Entry<String, Object> entry : s.entrySet()) {
                grid_metadata.addColumn(h -> h.get(entry.getKey().toString())).setHeader(entry.getKey()).setAutoWidth(true).setResizable(true).setSortable(true);
            }

            grid_metadata.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
            grid_metadata.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
            grid_metadata.addThemeVariants(GridVariant.LUMO_COMPACT);
            //   grid2.setAllRowsVisible(true);
            grid_metadata.setPageSize(50);
            grid_metadata.setHeight("800px");

            grid_metadata.getStyle().set("resize", "vertical");
            grid_metadata.getStyle().set("overflow", "auto");

            //grid2.setPaginatorSize(5);
            // Add the grid to the page

            this.setPadding(false);
            this.setSpacing(false);
            this.setBoxSizing(BoxSizing.CONTENT_BOX);

        }
        else {
            //Text txt = new Text("Es konnten keine Daten  abgerufen werden!");
            //add(txt);
        }

    }

    public List<LinkedHashMap<String,Object>> retrieveRows(String queryString) throws SQLException, IOException {


        List<LinkedHashMap<String, Object>> rows = new LinkedList<LinkedHashMap<String, Object>>();

        PreparedStatement s = null;
        ResultSet rs = null;
        try
        {
            //    String url="jdbc:oracle:thin:@37.120.189.200:1521:xe";
            //    String user="SYSTEM";
            //    String password="Michael123";

            DriverManagerDataSource ds = new DriverManagerDataSource();
            Configuration conf;
            conf = comboBox.getValue();

            Class.forName("oracle.jdbc.driver.OracleDriver");

            String password = Configuration.decodePassword(conf.getPassword());
            //    Connection conn=DriverManager.getConnection(url, user, password);
            Connection conn= DriverManager.getConnection(conf.getDb_Url(), conf.getUserName(), password);


            s = conn.prepareStatement(queryString);

            int timeout = s.getQueryTimeout();
            if(timeout != 0)
                s.setQueryTimeout(0);

            rs = s.executeQuery();


            List<String> columns = new LinkedList<>();
            ResultSetMetaData resultSetMetaData = rs.getMetaData();
            int colCount = resultSetMetaData.getColumnCount();
            for(int i= 1 ; i < colCount+1 ; i++) {
                columns.add(resultSetMetaData.getColumnLabel(i));
            }

            while (rs.next()) {
                LinkedHashMap<String, Object> row  = new LinkedHashMap<String, Object>();
                for(String col : columns) {
                    int colIndex = columns.indexOf(col)+1;
                    String object = rs.getObject(colIndex)== null ? "" : String.valueOf(rs.getObject(colIndex));
                    row.put(col, object);
                }

                rows.add(row);
            }
        } catch (SQLException | IllegalArgumentException  | SecurityException e) {
            // e.printStackTrace();
            // add(new Text(e.getMessage()));

            Notification notification = Notification.show(e.getMessage());
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

            return Collections.emptyList();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {

            try { rs.close(); } catch (Exception e) { /* Ignored */ }
            try { s.close(); } catch (Exception e) { /* Ignored */ }

        }
        // conn.close();
        return rows;
    }

    private VerticalLayout createDialogData(String sql) throws SQLException, IOException {

        //fill_grid_metadata("select nachrichtidintern,nachrichtidextern,status,art,eingangsdatumserver,fehlertag,verarbeitet,loeschtag,senderpostfachname,empfaengeraktenzeichen from ekp.metadaten\n where rownum < 5");
        fill_grid_metadata(sql);

        VerticalLayout dialogLayout = new VerticalLayout(grid_metadata);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "1200px").set("max-width", "100%");
        dialogLayout.getStyle().set("height", "800px").set("max-height", "100%");
        return dialogLayout;
    }
    private VerticalLayout createDialogGraph(Integer id) {

        Chart chart = new Chart();
        chart.getConfiguration().getChart().setType(ChartType.SPLINE);

        com.vaadin.flow.component.charts.model.Configuration conf = chart.getConfiguration();
        // Configuration conf = chart.getConfiguration();

        conf.getxAxis().setType(AxisType.DATETIME);

        DataSeries series = new DataSeries("Zeit");
      /*  series.add(new DataSeriesItem(new Date(2023, 5, 1,10,30), 200));
        series.add(new DataSeriesItem(new Date(2023, 5, 1,11,35), 210));
        series.add(new DataSeriesItem(new Date(2023, 5, 1,12,40), 280));
        series.add(new DataSeriesItem(new Date(2023, 5, 1,14,45), 290));
        series.add(new DataSeriesItem(new Date(2023, 5, 1,15,50), 100));

        series.add(new DataSeriesItem(new Date(2023, 5, 2,10,30), 120));
        series.add(new DataSeriesItem(new Date(2023, 5, 2,11,35), 150));
        series.add(new DataSeriesItem(new Date(2023, 5, 2,12,40), 180));
        series.add(new DataSeriesItem(new Date(2023, 5, 2,15,45), 290));
        series.add(new DataSeriesItem(new Date(2023, 5, 2,16,50), 310));

        series.add(new DataSeriesItem(new Date(2023, 5, 3,10,30), 120));
        series.add(new DataSeriesItem(new Date(2023, 5, 3,11,35), 150));
        series.add(new DataSeriesItem(new Date(2023, 5, 3,12,40), 280));
        series.add(new DataSeriesItem(new Date(2023, 5, 3,16,45), 290));
        series.add(new DataSeriesItem(new Date(2023, 5, 3,18,50), 500));
        series.add(new DataSeriesItem(new Date(2023, 5, 3,20,50), 600));*/

        List<fvm_monitoring> ll =  getHistMonitoring(id);

        ll.forEach(eintrag -> {
            System.out.println(eintrag.getZeitpunkt() + ": " + eintrag.getAktueller_Wert());
            series.add(new DataSeriesItem(eintrag.getZeitpunkt(),eintrag.getAktueller_Wert()));
        });

        conf.addSeries(series);

        YAxis yaxis = new YAxis();
        yaxis.setTitle("Anzahl");
        conf.addyAxis(yaxis);

        VerticalLayout dialogLayout = new VerticalLayout(chart);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "1200px").set("max-width", "100%");
        dialogLayout.getStyle().set("height", "800px").set("max-height", "100%");
        return dialogLayout;
    }



    private static VerticalLayout showDialog(fvm_monitoring Inhalt){

        VerticalLayout dialogInhalt = new VerticalLayout();

        details = new Tab("Beschreibung");
        payment = new Tab("Handlungsanweisung");
        //data = new Tab("Daten");

        Tabs tabs = new Tabs();

        tabs.addSelectedChangeListener(
                event -> setContent(event.getSelectedTab(),Inhalt));


        tabs.add(details, payment);


        content.setSpacing(false);
        content.add(tabs);

        setContent(tabs.getSelectedTab(),Inhalt);

        dialogInhalt = new VerticalLayout();
        dialogInhalt.add(tabs,content);

        return dialogInhalt;

    }

    private VerticalLayout showEditDialog(fvm_monitoring monitor){
        VerticalLayout dialogLayout = new VerticalLayout();
        Dialog dialog = new Dialog();
        dialog.add(getTabsheet(monitor));
        dialog.setDraggable(true);
        dialog.setResizable(true);
        dialog.setWidth("1000px");
        dialog.setHeight("400px");
        //  Button addButton = new Button("add");
        Button cancelButton = new Button("Cancel");
        Button saveButton = new Button("Save");
        // Add buttons to the footer
        dialog.getFooter().add(saveButton, cancelButton);

        cancelButton.addClickListener(cancelEvent -> {
            dialog.close(); // Close the confirmation dialog
        });

        saveButton.addClickListener(saveEvent -> {
            System.out.println("saved data....");
            saveEditedMonitor(monitor);
            param_Liste=getMonitoring();
            grid.setItems(param_Liste);
            dialog.close(); // Close the confirmation dialog
        });

        dialog.open();

        return dialogLayout;

    }
    private TabSheet getTabsheet(fvm_monitoring monitor) {

        TabSheet tabSheet = new TabSheet();

        tabSheet.add("General", getGeneral(monitor));
        tabSheet.add("SQL-Abfrage", getSqlAbfrage(monitor));
        tabSheet.add("Beschreibung", getBeschreibung(monitor));
        tabSheet.add("Handlungsinformationen", getHandlungsinformationen(monitor));

        tabSheet.setSizeFull();
        tabSheet.setHeightFull();

        return tabSheet;
    }

    private void saveEditedMonitor(fvm_monitoring monitor) {
        callback.save(monitor);
    }

    private Component getHandlungsinformationen(fvm_monitoring monitor) {
        VerticalLayout content = new VerticalLayout();
        VaadinCKEditor editor;
        Button saveBtn = new Button("save");
        Button editBtn = new Button("edit");
        saveBtn.setVisible(false);
        editBtn.setVisible(true);

        Config config = new Config();
        config.setBalloonToolBar(Constants.Toolbar.values());
        config.setImage(new String[][]{},
                "", new String[]{"full", "alignLeft", "alignCenter", "alignRight"},
                new String[]{"imageTextAlternative", "|",
                        "imageStyle:alignLeft",
                        "imageStyle:full",
                        "imageStyle:alignCenter",
                        "imageStyle:alignRight"}, new String[]{});

        editor = new VaadinCKEditorBuilder().with(builder -> {

            builder.editorType = Constants.EditorType.CLASSIC;
            builder.width = "95%";
            builder.hideToolbar=false;
            builder.config = config;
        }).createVaadinCKEditor();

        //    editor.setReadOnly(true);
        editor.getStyle().setMargin ("-5px");
        editor.setValue(monitor.getHandlungs_INFO());
        editor.addValueChangeListener(event -> monitor.setHandlungs_INFO(event.getValue()));
//        saveBtn.addClickListener((event -> {
//            editBtn.setVisible(true);
//            saveBtn.setVisible(false);
//            //editor.setReadOnly(true);
//            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());
//
//        }));
//
//        editBtn.addClickListener(e->{
//            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());
//            editBtn.setVisible(false);
//            saveBtn.setVisible(true);
//            //editor.setReadOnly(false);
//        });

//        content.add(editor,editBtn,saveBtn);
        content.add(editor);
        return content;
    }

    private Component getBeschreibung(fvm_monitoring monitor) {
        VerticalLayout content = new VerticalLayout();
        VaadinCKEditor editor;
        Button saveBtn = new Button("save");
        Button editBtn = new Button("edit");
        saveBtn.setVisible(false);
        editBtn.setVisible(true);

        Config config = new Config();
        config.setBalloonToolBar(Constants.Toolbar.values());
        config.setImage(new String[][]{},
                "", new String[]{"full", "alignLeft", "alignCenter", "alignRight"},
                new String[]{"imageTextAlternative", "|",
                        "imageStyle:alignLeft",
                        "imageStyle:full",
                        "imageStyle:alignCenter",
                        "imageStyle:alignRight"}, new String[]{});

        editor = new VaadinCKEditorBuilder().with(builder -> {

            builder.editorType = Constants.EditorType.CLASSIC;
            builder.width = "95%";
            builder.hideToolbar=false;
            builder.config = config;
        }).createVaadinCKEditor();

        // editor.setReadOnly(true);
        editor.getStyle().setMargin ("-5px");
        editor.setValue(monitor.getBeschreibung());
        editor.addValueChangeListener(event -> monitor.setBeschreibung(event.getValue()));
//        saveBtn.addClickListener((event -> {
//            editBtn.setVisible(true);
//            saveBtn.setVisible(false);
//            //editor.setReadOnly(true);
//            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());
//
//        }));
//
//        editBtn.addClickListener(e->{
//            editor.setReadOnlyWithToolbarAction(!editor.isReadOnly());
//            editBtn.setVisible(false);
//            saveBtn.setVisible(true);
//            //editor.setReadOnly(false);
//        });

//        content.add(editor,editBtn,saveBtn);
        content.add(editor);
        return content;
    }

    private Component getSqlAbfrage(fvm_monitoring monitor) {
        VerticalLayout content = new VerticalLayout();
        TextField abfrage = new TextField("SQL-Abfrage");
        abfrage.setValue(monitor.getSQL());
        abfrage.setWidthFull();

        TextField detailabfrage = new TextField("SQL-Detail Abfrage");
        detailabfrage.setValue(monitor.getSQL_Detail());
        detailabfrage.setWidthFull();

        abfrage.addValueChangeListener(event -> monitor.setSQL(event.getValue()));
        detailabfrage.addValueChangeListener(event -> monitor.setSQL_Detail(event.getValue()));
        content.add(abfrage, detailabfrage);
        return content;
    }

    private Component getGeneral(fvm_monitoring monitor) {
        VerticalLayout content = new VerticalLayout();

        TextField titel = new TextField("Titel");
        titel.setValue(monitor.getTitel());
        titel.setWidthFull();

        IntegerField intervall = new IntegerField("Check-Intervall");
        IntegerField infoSchwellwert = new IntegerField("Warning Schwellwert");
        IntegerField errorSchwellwert = new IntegerField("Error Schwellwert");
        Checkbox checkbox = new Checkbox("aktiv");

        intervall.setValue(monitor.getCheck_Intervall());
        infoSchwellwert.setValue(monitor.getWarning_Schwellwert());
        errorSchwellwert.setValue(monitor.getError_Schwellwert());
        if(monitor.getIS_ACTIVE().equals("1")){
            checkbox.setValue(true);
        }

        // Add value change listeners to trigger binder updates
        intervall.addValueChangeListener(event -> monitor.setCheck_Intervall(event.getValue()));
        infoSchwellwert.addValueChangeListener(event -> monitor.setWarning_Schwellwert(event.getValue()));
        errorSchwellwert.addValueChangeListener(event -> monitor.setError_Schwellwert(event.getValue()));
        checkbox.addValueChangeListener(event -> monitor.setIS_ACTIVE(event.getValue() ? "1" : "0"));

        HorizontalLayout hr = new HorizontalLayout(intervall,infoSchwellwert,errorSchwellwert, checkbox);
        hr.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        content.add(titel, hr);
        return content;
    }

    private static VerticalLayout showEditDialogOld(fvm_monitoring Inhalt){

        VerticalLayout dialogInhalt = new VerticalLayout();
        TextField titel = new TextField("Titel");
        titel.setValue(Inhalt.getTitel());
        titel.setWidthFull();

        NumberField intervall = new NumberField("Intervall");
        NumberField infoSchwellwert = new NumberField("Warnung-Schwellert");
        NumberField errorSchwellwert = new NumberField("Fehler-Schwellwert");
        Checkbox checkbox = new Checkbox("aktiv");

        HorizontalLayout hr = new HorizontalLayout(intervall,infoSchwellwert,errorSchwellwert, checkbox);
        hr.setDefaultVerticalComponentAlignment(Alignment.BASELINE);

        TextArea sql_text = new TextArea ("SQL-Abfrage");
        sql_text.setWidthFull();

        Label descriptionLb = new Label("Beschreibung");
        RichTextEditor rte_Beschreibung = new RichTextEditor();
        RichTextEditor rte_Handlungsanweisung = new RichTextEditor();


        dialogInhalt = new VerticalLayout();
        dialogInhalt.add(titel, hr, sql_text, descriptionLb, rte_Beschreibung, rte_Handlungsanweisung, content);

        return dialogInhalt;

    }


    private static void setContent(Tab tab,fvm_monitoring inhalt ) {

        if(content != null ) {
            content.removeAll();
        }

        if (tab == null) {
            return;
        }
        if (tab.equals(details)) {

            RichTextEditor rte = new RichTextEditor();
            //rte.setMaxHeight("400px");
            //rte.setMinHeight("200px");
            rte.setWidthFull();
            rte.setHeightFull();
            rte.setReadOnly(true);
            rte.addThemeVariants(RichTextEditorVariant.LUMO_NO_BORDER);
            rte.asHtml().setValue(inhalt.getBeschreibung());

            //content.add(inhalt.getBeschreibung());
            content.add(rte);
        } else if (tab.equals(payment)) {
            RichTextEditor rte = new RichTextEditor();
            //rte.setMaxHeight("400px");
            //rte.setMinHeight("200px");
            rte.setWidthFull();
            rte.setHeightFull();
            rte.setReadOnly(true);
            rte.addThemeVariants(RichTextEditorVariant.LUMO_NO_BORDER);
            rte.asHtml().setValue(inhalt.getHandlungs_INFO());

            //content.add(inhalt.getBeschreibung());
            content.add(rte);

            //content.add(inhalt.getHandlungs_INFO());

        } else {
            content.add(new Paragraph("ToDo"));
        }
    }


    private static VerticalLayout createDialogLayout(String Beschreibung) {

        RichTextEditor rte = new RichTextEditor();
        //rte.setMaxHeight("400px");
        //rte.setMinHeight("200px");
        rte.setWidthFull();
        rte.setHeightFull();
        rte.setReadOnly(true);
        rte.addThemeVariants(RichTextEditorVariant.LUMO_NO_BORDER);
        rte.asHtml().setValue(Beschreibung);


        VerticalLayout dialogLayout = new VerticalLayout(rte);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "1200px").set("max-width", "100%");
        dialogLayout.getStyle().set("height", "800px").set("max-height", "100%");


        return dialogLayout;
    }

    private List<fvm_monitoring> getMonitoring() {


        //String sql = "SELECT ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT, ERROR_SCHWELLWERT FROM EKP.FVM_MONITORING";

        String sql = "SELECT m.ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT" +
                ", ERROR_SCHWELLWERT,mr.result as Aktueller_Wert, 100 / Error_schwellwert * case when mr.result>=Error_schwellwert then Error_Schwellwert else mr.result end  / 100 as Error_Prozent" +
                ", Zeitpunkt, m.is_active, nvl(m.sql_detail,'select ''Detail-SQL nicht definiert'' from dual') as sql_detail FROM FVM_MONITORING m\n" +
                "left outer join FVM_MONITOR_RESULT mr\n" +
                "on m.id=mr.id\n" +
                "and mr.is_active='1'";


        System.out.println("Abfrage EKP.FVM_Monitoring (CockpitView.java): ");
        System.out.println(sql);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));

        try {

            jdbcTemplate.setDataSource(ds);

            monitore = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(fvm_monitoring.class));



            System.out.println("FVM_Monitoring eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }

        return monitore;
    }


    private List<fvm_monitoring> getHistMonitoring(Integer id) {


        String sql = "select ID,ZEITPUNKT,Result as Aktueller_Wert from FVM_MONITOR_RESULT where id= " + id + " order by Zeitpunkt desc";


        System.out.println("Abfrage EKP.FVM_Monitoring Historie (CockpitView.java): ");
        System.out.println(sql);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));

        try {

            jdbcTemplate.setDataSource(ds);

            monitore = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(fvm_monitoring.class));



            System.out.println("FVM_Monitoring eingelesen");

        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }

        return monitore;
    }


    @Override
    protected void onDetach(DetachEvent event) {
        super.onDetach(event);
        // Stoppe den Timer, wenn das UI geschlossen wird
        stopCountdown();
    }



}
