package com.example.application.views;

import com.example.application.data.entity.FTPFile;
import com.example.application.data.service.ConfigurationService;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import javax.annotation.security.RolesAllowed;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@PageTitle("FileBrowser")
@Route(value = "filebrowser", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class FileBrowserView extends VerticalLayout {

    DateTimePicker startDateTimePicker;
    DateTimePicker endDateTimePicker;

    public FileBrowserView (ConfigurationService service) throws JSchException, SftpException {


        add(new H3("Logfile-Browser"));

        SftpClient cl = new SftpClient("37.120.189.200",9021,"michael");
//        cl.authPassword("7x24!admin4me");
        cl.authKey("C:\\tmp\\id_rsa","");

        //cl.listFiles("/tmp");

        List<FTPFile> files = cl.getFiles("/tmp");

        Grid<FTPFile> grid = new Grid<>(FTPFile.class, false);
        grid.addColumn(FTPFile::getName).setHeader("Name").setSortable(true);;
        grid.addColumn(FTPFile::getSize).setHeader("Größe").setSortable(true);;
        grid.addColumn(FTPFile::getErstellungszeit).setHeader("Erstellungszeit").setSortable(true);;

        Grid.Column<FTPFile> editColumn = grid.addComponentColumn(file -> {
            Button editButton = new Button("Download");
            editButton.addClickListener(e -> {

                Notification notification = Notification
                        .show("Download " + file.getName());


            });
            return editButton;
        }).setWidth("150px").setFlexGrow(0);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        //  GridListDataView<Metadaten> dataView =grid.setItems();

        grid.setHeight("800px");
        grid.getStyle().set("resize", "vertical");
        grid.getStyle().set("overflow", "auto");

        grid.setItems(files);

        Button button = new Button("Go");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);

        ComboBox<String> umgebungComboBox = new ComboBox<>("Umgebung");
        umgebungComboBox.setAllowCustomValue(true);
      //  add(umgebungComboBox);
        umgebungComboBox.setItems("Prod", "QS", "Test2", "Test3");
        //umgebungComboBox.setHelperText("Auswahl des Logverzeichniss");

        ComboBox<String> verzeichnisComboBox = new ComboBox<>("Verzeichnis");
        verzeichnisComboBox.setAllowCustomValue(true);
        //add(verzeichnisComboBox);
        verzeichnisComboBox.setItems("eKP-Logs", "eKP-Server Logs", "Soa-Logs", "Admin-Logs");
        //verzeichnisComboBox.setHelperText("Auswahl des Logverzeichniss");

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
        hl.add(umgebungComboBox,verzeichnisComboBox,startDateTimePicker, endDateTimePicker,button);
        hl.setAlignItems(Alignment.BASELINE);


        add(hl,grid);

    }

}
