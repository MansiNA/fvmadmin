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
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@PageTitle("FileBrowser")
@Route(value = "filebrowser", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class FileBrowserView extends VerticalLayout {

    DateTimePicker startDateTimePicker;
    DateTimePicker endDateTimePicker;

    Grid<FTPFile> grid;
    SftpClient cl;
    Long von;
    Long bis;

    public FileBrowserView (ConfigurationService service) throws JSchException, SftpException {


        add(new H3("Logfile-Browser"));



        //cl.listFiles("/tmp");



        grid = new Grid<>(FTPFile.class, false);
        grid.addColumn(FTPFile::getName).setHeader("Name").setSortable(true);;
        grid.addColumn(FTPFile::getSize).setHeader("Größe").setSortable(true);;
        grid.addColumn(FTPFile::getErstellungszeit).setHeader("Erstellungszeit").setSortable(true);;

        Grid.Column<FTPFile> editColumn = grid.addComponentColumn(file -> {
            Button editButton = new Button("Download");
            editButton.addClickListener(e -> {

                Notification notification = Notification
                        .show("Download " + file.getName());
                try {
                    getFile("/tmp/" + file.getName(),"c:\\tmp\\mq.txt");


                } catch (SftpException ex) {
                    throw new RuntimeException(ex);
                } catch (JSchException ex) {
                    throw new RuntimeException(ex);
                }

                Runtime rs = Runtime.getRuntime();
                try {
                    rs.exec("C:\\Program Files (x86)\\Notepad++\\notepad++.exe c:\\tmp\\mq.txt");

                }
                catch (IOException ex) {
                    System.out.println(ex);
                }


            });
            return editButton;
        }).setWidth("150px").setFlexGrow(0);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        //  GridListDataView<Metadaten> dataView =grid.setItems();

        grid.setHeight("800px");
        grid.getStyle().set("resize", "vertical");
        grid.getStyle().set("overflow", "auto");




        Button button = new Button("Refresh");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        button.addClickListener(e-> {
            try {
                refresh();
            } catch (JSchException ex) {
                throw new RuntimeException(ex);
            } catch (SftpException ex) {
                throw new RuntimeException(ex);
            }
        });

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

        von = startDateTimePicker.getValue().toEpochSecond(ZoneOffset.UTC);
        bis = endDateTimePicker.getValue().toEpochSecond(ZoneOffset.UTC);

        System.out.println("Von: " + von );
        System.out.println("Bis: " + bis );

        refresh();


        add(hl,grid);

    }

    private void refresh() throws JSchException, SftpException {

        von = startDateTimePicker.getValue().toEpochSecond(ZoneOffset.UTC);
        bis = endDateTimePicker.getValue().toEpochSecond(ZoneOffset.UTC);

        cl = new SftpClient("37.120.189.200",9021,"michael");
        //cl.authPassword("7x24!admin4me");
        cl.authKey("C:\\tmp\\id_rsa","");

        List<FTPFile> files = cl.getFiles("/tmp", von,bis);
        grid.setItems(files);

        cl.close();
    }
    private void getFile(String SourceFile, String TargetFile) throws JSchException, SftpException {
        SftpClient cl = new SftpClient("37.120.189.200",9021,"michael");
        cl.authKey("C:\\tmp\\id_rsa","");
        cl.downloadFile(SourceFile, TargetFile  );
        cl.close();
    }
}
