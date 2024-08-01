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
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        hl.setAlignItems(FlexComponent.Alignment.BASELINE);
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

                                //System.out.println(clickedItem.getLastName());
                                Notification.show("Postfach " + clickedItem.getUSER_ID() + " wird eingeschaltet...");

                             //   clickedItem.setQUANTIFIER(1);
                                updateMessageBox(clickedItem,"1");
                                protokollService.logAction(clickedItem.getUSER_ID() + " wurde eingeschaltet.", "");
                                updateList();
                            }
                            else {
                                showShutdownReasonDialog(reason -> {
                                    Notification.show("Postfach " + clickedItem.getUSER_ID() + " wird ausgeschaltet...");
                                    updateMessageBox(clickedItem, "0");
                                    protokollService.logAction(clickedItem.getUSER_ID() + " wurde ausgeschaltet.", reason);
                                    updateList();
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
        affectedMailboxes = protokollService.findAllMailboxShutdowns();
        if (affectedMailboxes.isEmpty()) {
            onOffButton.setText("Alle ausschalten");
        } else {
            onOffButton.setText(affectedMailboxes.size() + " wieder einschalten");
        }

      //  grid.setItems(mailboxen);
        Span title = new Span("Übersicht der Postfächer");
        title.getStyle().set("font-weight", "bold");
        HorizontalLayout headerLayout = new HorizontalLayout(title, menuButton, onOffButton);
        headerLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
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

        for (Mailbox mailbox : mailboxen) {
            if (mailbox.getQUANTIFIER() == 1) { // only disable active mailboxes
                mailbox.setQUANTIFIER(0);
                updateMessageBox(mailbox, "0");
                protokollService.logAction(mailbox.getUSER_ID() + " wurde ausgeschaltet.", reason);
                protokollService.saveMailboxShutdownState(mailbox.getUSER_ID(), reason);
            }
        }
        affectedMailboxes = protokollService.findAllMailboxShutdowns();
        onOffButton.setText(affectedMailboxes.size() + " wieder einschalten");
        onOffButton.setEnabled(true);
        updateList();
    }

    private void reEnableMailboxes() {
        onOffButton.setEnabled(false);
        for (MailboxShutdown mailboxShutdown : affectedMailboxes) {
        //for (Mailbox mailbox : mailboxen) {
            Mailbox mailbox = mailboxen.stream()
                    .filter(mailboxReEnable -> mailboxReEnable.getUSER_ID().equals(mailboxShutdown.getMailboxId()))
                    .findFirst()
                    .orElse(null);
            if (mailbox != null && mailbox.getQUANTIFIER() == 0) { // re-enable only those disabled by "alle ausschalten"
                mailbox.setQUANTIFIER(1);
                updateMessageBox(mailbox, "1");
                protokollService.logAction(mailbox.getUSER_ID() + " wurde eingeschaltet.", "");
            }
        }
        protokollService.deleteShutdownTable();
        affectedMailboxes.clear();
        onOffButton.setText("Alle ausschalten");
        onOffButton.setEnabled(true);
        updateList();
    }
    private void allMailBoxTurnOnOffOld() {
        boolean shouldTurnOn = mailboxen.stream().anyMatch(mailbox -> mailbox.getQUANTIFIER() == 0);

        for (Mailbox mailbox : mailboxen) {
            if (shouldTurnOn) {
                if (mailbox.getQUANTIFIER() == 0) {
                    mailbox.setQUANTIFIER(1);
                    updateMessageBox(mailbox, "1");
                    protokollService.logAction(mailbox.getUSER_ID() + " wurde eingeschaltet.","");
                    Notification.show("Postfach " + mailbox.getUSER_ID() + " wird eingeschaltet...", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
            } else {
                if (mailbox.getQUANTIFIER() == 1) {
                    mailbox.setQUANTIFIER(0);
                    updateMessageBox(mailbox, "0");
                    protokollService.logAction(mailbox.getUSER_ID() + " wurde ausgeschaltet.","");
                    Notification.show("Postfach " + mailbox.getUSER_ID() + " wird ausgeschaltet...", 3000, Notification.Position.MIDDLE)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                }
            }
        }

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

    private void updateMessageBox(Mailbox mb, String i) {
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

        } catch (Exception e) {
        System.out.println("Exception: " + e.getMessage());
        }

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
