package com.example.application.views;

import com.example.application.data.entity.Configuration;
import com.example.application.data.entity.Mailbox;
import com.example.application.data.entity.MailboxShutdown;
import com.example.application.data.service.ConfigurationService;
import com.example.application.data.service.ProtokollService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.LitRenderer;
import com.vaadin.flow.data.renderer.NativeButtonRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;

@PageTitle("Mailbox Verwaltung")
@Route(value = "mailbox-config", layout= MainLayout.class)
@RolesAllowed({"ADMIN","PF_ADMIN"})
public class MailboxConfigView  extends VerticalLayout {
    @Autowired
    JdbcTemplate jdbcTemplate;
    private ConfigurationService service;
    private ProtokollService protokollService;
    private ComboBox<Configuration> comboBox;
    Grid<Mailbox> grid = new Grid<>(Mailbox.class, false);
    Integer ret = 0;
   // Button button = new Button("load");
    Button refresh = new Button("refresh");
    Button onOffButton = new Button("");
    private List<MailboxShutdown> affectedMailboxes;
    private List<Mailbox> mailboxen;
    private String switchLable;

    public MailboxConfigView(ConfigurationService service, ProtokollService protokollService)  {

        this.service = service;
        this.protokollService = protokollService;

        refresh.setEnabled(false);
        onOffButton.setEnabled(false);

        comboBox = new ComboBox<>("Verbindung");
        List<Configuration> configList = service.findMessageConfigurations();
        if (configList != null && !configList.isEmpty()) {
            comboBox.setItems(configList);
            comboBox.setItemLabelGenerator(Configuration::getName);
        }
    //    comboBox.setValue(service.findAllConfigurations().stream().findFirst().get());
        comboBox.setPlaceholder("auswählen");

        comboBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                refresh.setEnabled(true);  // Enable the refresh button when an item is selected
                onOffButton.setEnabled(true);
            }
        });

        HorizontalLayout hl = new HorizontalLayout();
     //   hl.add(comboBox,button,refresh);
        hl.add(comboBox,refresh);
        hl.setAlignItems(Alignment.BASELINE);
         setSizeFull();
        add(hl);


       // grid.setSelectionMode(Grid.SelectionMode.MULTI);

        //grid.addColumn(createEmployeeTemplateRenderer()).setHeader("Name des Postfachs")
        //        .setAutoWidth(true).setResizable(true);

        grid.addColumn(Mailbox::getNAME).setHeader("Name")
                .setWidth("22em").setFlexGrow(0).setResizable(true).setSortable(true);

        Grid.Column<Mailbox> DaystoExpireColumn = grid.addColumn((Mailbox::getDAYSTOEXPIRE)).setHeader("Zert. Ablauf")
                .setWidth("8em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<Mailbox> RoleIDColumn = grid.addColumn((Mailbox::getROLEID)).setHeader("Role ID")
                .setWidth("12em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<Mailbox> EgvpPFColumn = grid.addColumn((Mailbox::getStatus)).setHeader("EGVP-E PF Status")
                .setWidth("10em").setFlexGrow(0).setResizable(true).setSortable(true);
        grid.addColumn((Mailbox::getIn_egvp_wartend)).setHeader("wartend in EGVP-E")
                .setWidth("10em").setFlexGrow(0).setResizable(true).setSortable(true);
            //    .setWidth("4em").setFlexGrow(0);
        Grid.Column<Mailbox> inVerarbeitungColumn = grid.addColumn((Mailbox::getAktuell_in_eKP_verarbeitet)).setHeader("in Verarbeitung")
                .setWidth("10em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<Mailbox> haengendColumn = grid.addColumn((Mailbox::getIn_ekp_haengend)).setHeader("hängend")
                .setWidth("8em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<Mailbox> FHColumn = grid.addColumn((Mailbox::getIn_ekp_fehlerhospital)).setHeader("im FH")
                .setWidth("6em").setFlexGrow(0).setResizable(true).setSortable(true);
        Grid.Column<Mailbox> KONVERTIERUNGSDIENSTEColumn = grid.addColumn(Mailbox::getKONVERTIERUNGSDIENSTE).setHeader("hat Konvertierungsdienst")
                .setWidth("12em").setFlexGrow(0).setResizable(true).setSortable(true);
//        grid.addColumn(createStatusComponentRenderer()).setHeader("Status")
//                .setAutoWidth(true).setResizable(true);


        grid.addComponentColumn(mb -> createStatusIcon(mb.getQUANTIFIER()))
                //.setTooltipGenerator(person -> person.getStatus())
                .setWidth("4em").setFlexGrow(0)
                .setHeader("Status");

        grid.addColumn(
                  new NativeButtonRenderer<>(
//                          "Switch",
                item -> {
                    // Determine the switch label based on the QUANTIFIER value
                    return item.getQUANTIFIER() == 0 ? "Einschalten" : "Ausschalten";
                },
                        clickedItem -> {

                            if (clickedItem.getQUANTIFIER()==0) {

                             //   clickedItem.setQUANTIFIER(1);
                                String result = updateMessageBox(clickedItem,"1");
                                if(result.equals("Ok")) {
                                    Notification.show("Postfach " + clickedItem.getUSER_ID() + " wird eingeschaltet...");
                                    protokollService.logAction(clickedItem.getUSER_ID() + " wurde eingeschaltet.", "");
                                    updateList();
                                } else {
                                    Notification.show(result).addThemeVariants(NotificationVariant.LUMO_ERROR);;
                                }

                            }
                            else {
                                showShutdownReasonDialog(reason -> {
                                    String result = updateMessageBox(clickedItem, "0");
                                    if(result.equals("Ok")) {
                                        Notification.show("Postfach " + clickedItem.getUSER_ID() + " wird ausgeschaltet...");
                                        protokollService.logAction(clickedItem.getUSER_ID() + " wurde ausgeschaltet.", reason);
                                        updateList();
                                    }  else {
                                        Notification.show(result).addThemeVariants(NotificationVariant.LUMO_ERROR);;
                                    }
                                });
                            }
                           // clickedItem.setIsActive(false);
                           // clickedItem.setLastName("Huhu");


                        })
        );




        grid.setItemDetailsRenderer(createPersonDetailsRenderer());
        grid.addThemeVariants(GridVariant.LUMO_COMPACT);

      //  inVerarbeitungColumn.setVisible(false);
        RoleIDColumn.setVisible(false);
        EgvpPFColumn.setVisible(false);
        DaystoExpireColumn.setVisible(false);

        haengendColumn.setVisible(false);
        FHColumn.setVisible(false);
        KONVERTIERUNGSDIENSTEColumn.setVisible(false);

        Button menuButton = new Button("Show/Hide Columns");
        menuButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        ColumnToggleContextMenu columnToggleContextMenu = new ColumnToggleContextMenu(menuButton);
        columnToggleContextMenu.addColumnToggleItem("Zert. Ablauf", DaystoExpireColumn);
        columnToggleContextMenu.addColumnToggleItem("Role-ID", RoleIDColumn);
        columnToggleContextMenu.addColumnToggleItem("EGVP PF Status", EgvpPFColumn);
        columnToggleContextMenu.addColumnToggleItem("in Verarbeitung", inVerarbeitungColumn);
        columnToggleContextMenu.addColumnToggleItem("hängende Nachrichten", haengendColumn);
        columnToggleContextMenu.addColumnToggleItem("im Fehlerhospital", FHColumn);
        columnToggleContextMenu.addColumnToggleItem("Konvertierungsdienste", KONVERTIERUNGSDIENSTEColumn);

      //  updateList();

        affectedMailboxes = new ArrayList<>();

      //  grid.setItems(mailboxen);
        Span title = new Span("Übersicht der Postfächer");
        title.getStyle().set("font-weight", "bold");
        HorizontalLayout headerLayout = new HorizontalLayout(title, menuButton, onOffButton);
        headerLayout.setAlignItems(Alignment.BASELINE);
        headerLayout.setFlexGrow(1, title);

        add(headerLayout,grid);

        onOffButton.addClickListener(e -> allMailBoxTurnOnOff());

        refresh.addClickListener(e -> updateList());

       // button.addClickListener(clickEvent -> {
        comboBox.addValueChangeListener(event->{

            UI ui = UI.getCurrent();
            grid.setItems();
            mailboxen=null;
            // Instruct client side to poll for changes and show spinner
            ui.setPollInterval(500);
            // Start background task
            CompletableFuture.runAsync(() -> {

                // Do some long running task
                try {
                    System.out.println("Hole Mailbox Infos");

                    mailboxen=getMailboxes();

                    //Thread.sleep(2000); //2 Sekunden warten
                    Thread.sleep(20); //2 Sekunden warten

                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (Exception e) {
                    // Need to use access() when running from background thread
                    ui.access(() -> {
                        Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
                    });
                  //  return; // Exit if an exception occurs
                }

                // Need to use access() when running from background thread
                ui.access(() -> {
                    // Stop polling and hide spinner
                    ui.setPollInterval(-1);

                    if (mailboxen == null || mailboxen.isEmpty()) {
                        onOffButton.setEnabled(false);
                        refresh.setEnabled(false);
                        if(mailboxen != null && mailboxen.isEmpty()) {
                            Notification.show("Keine Mailbox Infos gefunden!", 5000, Notification.Position.MIDDLE);
                        }
                        return;
                    } else {
                        affectedMailboxes = fetchTableData();
                        if (affectedMailboxes.isEmpty()) {
                            System.out.println("empty....................."+affectedMailboxes.size());
                            onOffButton.setText("Alle ausschalten");
                        } else {
                            System.out.println("affect....................."+affectedMailboxes.size());
                            onOffButton.setText(affectedMailboxes.size() + " wieder einschalten");
                        }
                        grid.setItems(mailboxen);
                    }

                });
            });


        });


    }

    private void allMailBoxTurnOnOff() {
        if (onOffButton.getText().startsWith("Alle ausschalten")) {
            showShutdownReasonDialog(this::disableAllMailboxes);
        } else {
            reEnableMailboxes();
        }
    }

    private void showShutdownReasonDialog(Consumer<String> onConfirm) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Bitte Shutdown Grund angeben");

        TextArea reasonTextArea = new TextArea("Grund");
        reasonTextArea.setWidthFull();

        Button confirmButton = new Button("Bestätigen", event -> {
            String reason = reasonTextArea.getValue();
            if (reason != null && !reason.isEmpty()) {
                onConfirm.accept(reason);
                dialog.close();
            } else {
                Notification.show("Bitte geben Sie einen Grund an", 3000, Notification.Position.MIDDLE);
            }
        });

        Button cancelButton = new Button("Abbrechen", event -> dialog.close());

        HorizontalLayout buttons = new HorizontalLayout(confirmButton, cancelButton);
        VerticalLayout dialogLayout = new VerticalLayout(reasonTextArea, buttons);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private void disableAllMailboxes(String reason) {
        onOffButton.setEnabled(false);
        String result = "";
        createVerbindungShutdownTable();
        for (Mailbox mailbox : mailboxen) {
            if (mailbox.getQUANTIFIER() == 1) {
                mailbox.setQUANTIFIER(0);
                result = updateMessageBox(mailbox, "0");
                if(result.equals("Ok")) {
                    protokollService.logAction(mailbox.getUSER_ID() + " wurde ausgeschaltet.", reason);
                    //    protokollService.saveMailboxShutdownState(mailbox.getUSER_ID(), reason);
                    insertMailboxShutdown(mailbox.getUSER_ID(), reason);
                }
            }
        }
        affectedMailboxes = fetchTableData();
        if(result.equals("Ok")) {
            onOffButton.setText(affectedMailboxes.size() + " wieder einschalten");
        } else if(!result.equals("")){
            Notification.show("Error while Alle ausschalten").addThemeVariants(NotificationVariant.LUMO_ERROR);;
        }
        onOffButton.setEnabled(true);
        updateList();
    }

    private void reEnableMailboxes() {
        String result = "";
        onOffButton.setEnabled(false);
        for (MailboxShutdown mailboxShutdown : affectedMailboxes) {
        //for (Mailbox mailbox : mailboxen) {
            Mailbox mailbox = mailboxen.stream()
                    .filter(mailboxReEnable -> mailboxReEnable.getUSER_ID().equals(mailboxShutdown.getMailboxId()))
                    .findFirst()
                    .orElse(null);
            if (mailbox != null && mailbox.getQUANTIFIER() == 0) { // re-enable only those disabled by "alle ausschalten"
                mailbox.setQUANTIFIER(1);
                result = updateMessageBox(mailbox, "1");
                if(result.equals("Ok")) {
                    protokollService.logAction(mailbox.getUSER_ID() + " wurde eingeschaltet.", "");
                }
            }
        }
      //  protokollService.deleteShutdownTable();
        deleteVerbindungShutdownTable();
        affectedMailboxes.clear();
        onOffButton.setText("Alle ausschalten");
        onOffButton.setEnabled(true);
        updateList();
    }

    private Icon createStatusIcon(Integer quantifier) {
        //boolean isAvailable = "Available".equals(status);

        Icon icon;
        if (quantifier == 1) {
            icon = VaadinIcon.CHECK.create();
            switchLable = "Ausschalten";
            icon.getElement().getThemeList().add("badge success");
        } else {
            icon = VaadinIcon.CLOSE_SMALL.create();
            switchLable = "Einschalten";
            icon.getElement().getThemeList().add("badge error");
        }
        icon.getStyle().set("padding", "var(--lumo-space-xs");
        return icon;
    }


    private static class ColumnToggleContextMenu extends ContextMenu {
        public ColumnToggleContextMenu(Component target) {
            super(target);
            setOpenOnClick(true);
        }

        void addColumnToggleItem(String label, Grid.Column<Mailbox> column) {
            MenuItem menuItem = this.addItem(label, e -> {
                column.setVisible(e.getSource().isChecked());
            });
            menuItem.setCheckable(true);
            menuItem.setChecked(column.isVisible());
        }
    }

    private String updateMessageBox(Mailbox mb, String i) {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();

        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));
        try {

            jdbcTemplate.setDataSource(ds);

            jdbcTemplate.execute("update EKP.MAILBOX_CONFIG set quantifier=" + i + " where user_id='" + mb.getUSER_ID() +"'");

            jdbcTemplate.execute("update EKP.MAILBOX_CONFIG_Folder set quantifier=" + i + " where user_id='" + mb.getUSER_ID() +"'");

            return "Ok";
        } catch (Exception e) {
        System.out.println("Exception: " + e.getMessage());
            return e.getMessage();
        }

    }

    private void createVerbindungShutdownTable() {

        String tableName = getTableName();
        try {
            connectWithDefaultDatabase();
            if (!tableExists(tableName)) {
                System.out.println("Creating table: " + tableName);

                String sql = "CREATE TABLE \"" + tableName + "\" (mailbox_id VARCHAR2(255) NOT NULL, shutdown_reason VARCHAR2(255) NOT NULL)";

                System.out.println("Executing SQL: " + sql);

                jdbcTemplate.execute(sql);

                System.out.println("Table created successfully.");
            } else {
                System.out.println("Table already exists: " + tableName);
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            connectionClose(jdbcTemplate);
        }
    }

    private boolean tableExists(String tableName) {
        try {
            String checkTableSql = "SELECT COUNT(*) FROM all_tables WHERE table_name = ?";
            int tableCount = jdbcTemplate.queryForObject(checkTableSql, Integer.class, tableName);

            return tableCount > 0;
        } catch (Exception e) {
            System.out.println("Exception while checking table existence: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    private void connectWithDefaultDatabase() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl("jdbc:oracle:thin:@37.120.189.200:1521:xe");
        ds.setUsername("EKP_MONITOR");
        ds.setPassword("ekp123");
        this.jdbcTemplate = new JdbcTemplate(ds);
    }

    public void connectionClose(JdbcTemplate jdbcTemplate) {
        Connection connection = null;
        DataSource dataSource = null;
        try {
            connection = jdbcTemplate.getDataSource().getConnection();
            dataSource = jdbcTemplate.getDataSource();
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

    private void insertMailboxShutdown(String mailboxId, String reason) {

        String tableName = getTableName();
        try {
            connectWithDefaultDatabase();

            String sql = "INSERT INTO \"" + tableName + "\" (mailbox_id, shutdown_reason) VALUES (?, ?)";
            jdbcTemplate.update(sql, mailboxId, reason);

            System.out.println("Data inserted successfully.");
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            connectionClose(jdbcTemplate);
        }
    }

    private List<MailboxShutdown> fetchTableData() {

        String tableName = getTableName();
        List<MailboxShutdown> results = new ArrayList<>();

        try {
            connectWithDefaultDatabase();

            if (tableExists(tableName)) {

                String sql = "SELECT * FROM \"" + tableName + "\"";

                System.out.println("Executing SQL: " + sql);

                results = jdbcTemplate.query(sql, (rs, rowNum) -> {
                    MailboxShutdown mailboxShutdown = new MailboxShutdown();
                    mailboxShutdown.setMailboxId(rs.getString("mailbox_id"));
                    mailboxShutdown.setShutdownReason(rs.getString("shutdown_reason"));
                    return mailboxShutdown;
                });

                System.out.println("Data fetched successfully.");
            } else {
                System.out.println("Table does not exist: " + tableName);
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace();
        } finally {
            connectionClose(jdbcTemplate);
        }

        return results;
    }

    private void deleteVerbindungShutdownTable() {

        String tableName = getTableName();

        try {
            connectWithDefaultDatabase();

            System.out.println("Deleting table: " + tableName);

            String sql = "DROP TABLE \"" + tableName + "\"";

            System.out.println("Executing SQL: " + sql);

            jdbcTemplate.execute(sql);

            System.out.println("Table deleted successfully.");
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            e.printStackTrace(); // Log the full stack trace for better debugging
        } finally {
            connectionClose(jdbcTemplate);
        }
    }

    private String getTableName() {
        Configuration conf = comboBox.getValue();
        return "FVMADMIN_MB_" + conf.getName().replace("-", "_").replace(" ", "").trim() + "_SHUTDOWN";
    }

    private List<Mailbox> getMailboxes() {

        //String sql = "select name,court_id,quantifier, user_id,typ,konvertierungsdienste from EKP.MAILBOX_CONFIG";

        //String sql = "select name,court_id,quantifier, user_id,typ,konvertierungsdienste from EKP.MAILBOX_CONFIG";

        String sql="select Name,user_id,court_id,typ,konvertierungsdienste,max_message_count,DAYSTOEXPIRE,ROLEID,STATUS,in_egvp_wartend,quantifier,aktuell_in_eKP_verarbeitet,in_ekp_haengend,in_ekp_warteschlange,in_ekp_fehlerhospital from EKP.v_Postfach_Incoming_Status";

        System.out.println("Abfrage EKP.Mailbox_Config (MailboxConfigView.java)");

        DriverManagerDataSource ds = new DriverManagerDataSource();
        Configuration conf;
        conf = comboBox.getValue();


        ds.setUrl(conf.getDb_Url());
        ds.setUsername(conf.getUserName());
        ds.setPassword(Configuration.decodePassword(conf.getPassword()));

        try {

            jdbcTemplate.setDataSource(ds);

            mailboxen = jdbcTemplate.query(
                    sql,
                    new BeanPropertyRowMapper(Mailbox.class));



            System.out.println("MAILBOX_CONFIG eingelesen");

        } catch (Exception e) {
         //   System.out.println("Exception: " + e.getMessage());
            throw new RuntimeException("Error querying the database: " + e.getMessage(), e);
           // Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }

        return mailboxen;
    }


    private void updateList() {

        List<Mailbox> people = getMailboxes();
      // people.get(1).setLastName("hhh");
        if(people != null) {
            grid.setItems(people);
        }

    }

    private static Renderer<Mailbox> createEmployeeTemplateRenderer() {
  /*      return LitRenderer.<Mailbox>of(
                        "<vaadin-horizontal-layout style=\"align-items: center;\" theme=\"spacing\">"
                                + "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <h4> ${item.Name} </h4>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      (${item.User_ID})" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                                + "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <span> ${item.Court_ID} </span>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      (${item.Typ})" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                                + "</vaadin-horizontal-layout>")
                .withProperty("Name", Mailbox::getNAME)
                .withProperty("User_ID", Mailbox::getUSER_ID)
                .withProperty("Court_ID", Mailbox::getCOURT_ID)
                .withProperty("Quantifier", Mailbox::getQUANTIFIER)
                .withProperty("Typ", Mailbox::getTYP)
                .withProperty("Konvertierungsdienste", Mailbox::getKONVERTIERUNGSDIENSTE)
                ;
*/
        return LitRenderer.<Mailbox>of(
                        "<vaadin-horizontal-layout style=\"align-items: center;\" theme=\"spacing\">"
                                + "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <h4> ${item.Name} </h4>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      (${item.User_ID})" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                                + "</vaadin-horizontal-layout>")
                .withProperty("Name", Mailbox::getNAME)
                .withProperty("User_ID", Mailbox::getUSER_ID)
                .withProperty("Court_ID", Mailbox::getCOURT_ID)
                .withProperty("Quantifier", Mailbox::getQUANTIFIER)
                .withProperty("Typ", Mailbox::getTYP)
                .withProperty("Konvertierungsdienste", Mailbox::getKONVERTIERUNGSDIENSTE)
                ;

    }

  /*  private static final SerializableBiConsumer<Span, Mailbox> statusComponentUpdater = (span, Mailbox) -> {

        if (Mailbox.getQUANTIFIER()==0){
            String theme = String.format("badge %s", "error");
            span.getElement().setAttribute("theme", theme);
            span.setText("offline");
            }
            else
            {
                String theme = String.format("badge %s", "success");
                span.getElement().setAttribute("theme", theme);
                span.setText("online");
            }



    };

    private static ComponentRenderer<Span, Mailbox> createStatusComponentRenderer() {
        return new ComponentRenderer<>(Span::new, statusComponentUpdater);
    }*/

    private static final SerializableBiConsumer<Button, Mailbox> statusComponentUpdater = (button, Mailbox) -> {

        if (Mailbox.getQUANTIFIER()==0){
            String theme = String.format("badge %s", "error");
            button.getElement().setAttribute("theme", theme);
            button.setText("offline");
        }
        else
        {
            String theme = String.format("badge %s", "success");
            button.getElement().setAttribute("theme", theme);
            button.setText("online");
        }



    };

    private static ComponentRenderer<Button, Mailbox> createStatusComponentRenderer() {
        return new ComponentRenderer<>(Button::new, statusComponentUpdater);
    }


    private static ComponentRenderer<PersonDetailsFormLayout, Mailbox> createPersonDetailsRenderer() {
        return new ComponentRenderer<>(PersonDetailsFormLayout::new,
                PersonDetailsFormLayout::setPerson);
    }
    private static class PersonDetailsFormLayout extends FormLayout {
        private final TextField safeIDField = new TextField("Safe-ID");
        private final TextField courtIDField = new TextField("Court-ID");
        private final TextField TypField = new TextField("Typ");
        private final TextField MaxMessageCountField = new TextField("Max_Message_Count");

        public PersonDetailsFormLayout() {
            Stream.of(safeIDField, courtIDField, TypField,MaxMessageCountField).forEach(field -> {
                field.setReadOnly(true);
                add(field);
            });

            setResponsiveSteps(new ResponsiveStep("0", 4));
            //setColspan(safeIDField, 2);
            //setColspan(courtIDField, 2);
            //setColspan(TypField, 2);
        }

        public void setPerson(Mailbox mailbox) {
            safeIDField.setValue(mailbox.getUSER_ID());
            courtIDField.setValue(mailbox.getCOURT_ID());
            TypField.setValue(mailbox.getTYP());
            if(mailbox.getMAX_MESSAGE_COUNT() != null) {
                MaxMessageCountField.setValue(mailbox.getMAX_MESSAGE_COUNT());
            }
        }
    }
}
