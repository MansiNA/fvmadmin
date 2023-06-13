package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.Metadaten;
import com.example.application.data.entity.fvm_monitoring;
import com.example.application.data.service.ConfigurationService;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSelectionModel;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.grid.contextmenu.GridMenuItem;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.richtexteditor.RichTextEditor;
import com.vaadin.flow.component.richtexteditor.RichTextEditorVariant;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.poi.ss.formula.functions.T;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.annotation.security.RolesAllowed;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;



import org.w3c.dom.*;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


@PageTitle("eKP-Cokpit | by DBUSS GmbH")
@Route(value = "cockpit", layout= MainLayout.class)
@RolesAllowed("ADMIN")
@CssImport(
        themeFor = "vaadin-grid",
        value = "./styles/styles.css"
)
public class CockpitView extends VerticalLayout {
    @Autowired
    JdbcTemplate jdbcTemplate;

    Grid<fvm_monitoring> grid = new Grid<>(fvm_monitoring.class, false);

    Dialog dialog_Beschreibung = new Dialog();

    RichTextEditor editor = new RichTextEditor();

    private ComboBox<Configuration> comboBox;
    static List<fvm_monitoring> param_Liste = new ArrayList<fvm_monitoring>();
    static List<fvm_monitoring> monitore;

    static VerticalLayout content = new VerticalLayout();;
    static Tab details;
    static Tab payment;

    public CockpitView(JdbcTemplate jdbcTemplate, ConfigurationService service) {
        this.jdbcTemplate = jdbcTemplate;

/*
        fvm_monitoring param = new fvm_monitoring();
        fvm_monitoring param1 = new fvm_monitoring();


        param.setID(1);
        param.setTitel("Anzahl Nachrichten im Fehlerhospital");
        param.setBeschreibung("<h1>Anzahl Nachrichten im Fehlerhospital</h1><p>WAbsolute Anzahl von Nachrichten im FH.</br>Diese werden Differenziert nach OrdG/FachG und Staatsanwaltschaften aufgeteilt.");
        param.setCheck_Intervall(5);
        param.setSQL("select count(*) from cat");
        param.setHandlungs_INFO("<p>Informationen an den jeweiligen Justiz-Bereich</p>");
        param.setWarning_Schwellwert(5);
        param.setError_Schwellwert(10);
        param.setAktueller_Wert(86);

        param_Liste.add(param);

        param1.setID(11);
        param1.setTitel("Mein letzter Check");
        param1.setBeschreibung("<h1>Huhu Cool, oder?</h2>");
        param1.setCheck_Intervall(30);
        param1.setSQL("select 700 from dual");
        param1.setHandlungs_INFO("<p>Hier eine genaue Beschreibung, was im Fehlerfall getan werden muss</p>");
        param1.setWarning_Schwellwert(30);
        param1.setError_Schwellwert(50);
        param1.setAktueller_Wert(2);


        param_Liste.add(param1);
*/

        Button refreshBtn = new Button("refresh");
        refreshBtn.getElement().setProperty("title","Zeigt die Metadaten zu dem gewünschten Attribut. Falls keine Einschränkung angegeben, werden nur die letzten 500 Einträge ausgegeben.");
        refreshBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        refreshBtn.addClickListener(clickEvent -> {

            param_Liste=getMonitoring();
            grid.setItems(param_Liste);

        });


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


        H1 h1 = new H1("ekP / EGVP-E Monitoring");
        add(h1);

        //editor.setVisible(false);

       // String htmlContent = "<h1>Beispielüberschrift</h1><p>Dies ist der Inhalt des Editors.</p>";
       // editor.asHtml().setValue(htmlContent);

        add(editor);
        editor.setVisible(false);



        Button closeButton = new Button("close", e -> dialog_Beschreibung.close());
        dialog_Beschreibung.getFooter().add(closeButton);



        /*grid.addColumn(fvm_monitoring::getID).setHeader("ID")
                .setAutoWidth(true).setResizable(true).setSortable(true);*/
        grid.addColumn(fvm_monitoring::getTitel).setHeader("Titel")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        grid.addColumn(fvm_monitoring::getCheck_Intervall).setHeader("Intervall")
                .setAutoWidth(true).setResizable(true).setSortable(true);
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

        grid.setItemDetailsRenderer(createPersonDetailsRenderer());


        grid.setSelectionMode(Grid.SelectionMode.SINGLE);




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

        comboBox = new ComboBox<>("Verbindung");
        List<Configuration> configList = service.findMessageConfigurations();
        comboBox.setItems(configList);
        comboBox.setItemLabelGenerator(Configuration::get_Message_Connection);
        comboBox.setValue(configList.get(1) );


        //   HorizontalLayout layout = new HorizontalLayout(comboBox,refreshBtn);
        HorizontalLayout layout = new HorizontalLayout(comboBox,refreshBtn);
        layout.setPadding(false);
        layout.setAlignItems(FlexComponent.Alignment.BASELINE);



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

        grid.setItems(param_Liste);
        grid.setHeight("800px");
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        add(layout,grid);
    }


    private static ComponentRenderer<PersonDetailsFormLayout, fvm_monitoring> createPersonDetailsRenderer() {
        return new ComponentRenderer<>(PersonDetailsFormLayout::new,
                PersonDetailsFormLayout::setPerson);
    }

    private static class PersonDetailsFormLayout extends FormLayout {
        private final TextField emailField = new TextField("Letzte Aktualisierung");
        private final TextField idField = new TextField("ID");

        public PersonDetailsFormLayout() {
            Stream.of(idField,emailField).forEach(field -> {
                field.setReadOnly(true);
                add(field);
            });

            setResponsiveSteps(new ResponsiveStep("0", 2));

        }

        public void setPerson(fvm_monitoring person) {

            if (person.getZeitpunkt() != null) {

                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
                String dateAsString = dateFormat.format(person.getZeitpunkt());

                emailField.setValue(dateAsString);
            }
            else {
                emailField.setValue("unbekannt...");
            }
            idField.setValue(person.getID().toString());

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

            addItem("Beschreibung", e -> e.getItem().ifPresent(person -> {
                 System.out.printf("Edit: %s%n", person.getID());
                dialog_Beschreibung.setHeaderTitle(person.getTitel());
              //  VerticalLayout dialogLayout = createDialogLayout(person.getBeschreibung());

                VerticalLayout dialogLayout =showDialog(person);

                dialog_Beschreibung.removeAll();
                dialog_Beschreibung.add(dialogLayout);
                dialog_Beschreibung.setModal(false);
                dialog_Beschreibung.setDraggable(true);
                dialog_Beschreibung.setResizable(true);
                dialog_Beschreibung.setWidth("800px");
                dialog_Beschreibung.setHeight("600px");
                dialog_Beschreibung.open();
            }));


            addItem("Historie", e -> e.getItem().ifPresent(monitor -> {
                dialog_Beschreibung.setHeaderTitle("Historie für " + monitor.getTitel() + " (" + monitor.getID() + ")");
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

            addItem("refresh", e -> e.getItem().ifPresent(person -> {
                System.out.printf("refresh: %s%n", person.getID());
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

    private VerticalLayout createDialogGraph(Integer id) {

        Chart chart = new Chart();
        chart.getConfiguration().getChart().setType(ChartType.SPLINE);

        com.vaadin.flow.component.charts.model.Configuration conf = chart.getConfiguration();

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
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "1200px").set("max-width", "100%");
        dialogLayout.getStyle().set("height", "800px").set("max-height", "100%");
        return dialogLayout;
    }



private static VerticalLayout showDialog(fvm_monitoring Inhalt){

        VerticalLayout dialogInhalt = new VerticalLayout();

    details = new Tab("Beschreibung");
    payment = new Tab("Handlungsanweisung");

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
            content.add(new Paragraph("This is the Shipping tab"));
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
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "1200px").set("max-width", "100%");
        dialogLayout.getStyle().set("height", "800px").set("max-height", "100%");


        return dialogLayout;
    }

    private List<fvm_monitoring> getMonitoring() {


        //String sql = "SELECT ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT, ERROR_SCHWELLWERT FROM EKP.FVM_MONITORING";

        String sql = "SELECT m.ID, SQL, TITEL,  BESCHREIBUNG, HANDLUNGS_INFO, CHECK_INTERVALL,  WARNING_SCHWELLWERT" +
                ", ERROR_SCHWELLWERT,mr.result as Aktueller_Wert, 100 / Error_schwellwert * case when mr.result>=Error_schwellwert then Error_Schwellwert else mr.result end  / 100 as Error_Prozent, Zeitpunkt FROM EKP.FVM_MONITORING m\n" +
                "left outer join EKP.FVM_MONITOR_RESULT mr\n" +
                "on m.id=mr.id\n" +
                "and mr.is_active='1'";


        System.out.println("Abfrage EKP.FVM_Monitoring (CockpitView.java): ");
        System.out.println(sql);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(conf.getPassword());

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


        String sql = "select ID,ZEITPUNKT,Result as Aktueller_Wert from EKP.FVM_MONITOR_RESULT where id= " + id + " order by Zeitpunkt desc";


        System.out.println("Abfrage EKP.FVM_Monitoring Historie (CockpitView.java): ");
        System.out.println(sql);

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(conf.getPassword());

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


}
