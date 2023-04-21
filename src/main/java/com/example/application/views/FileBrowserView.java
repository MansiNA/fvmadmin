package com.example.application.views;

import com.example.application.data.entity.FTPFile;
import com.example.application.data.entity.Metadaten;
import com.example.application.data.service.ConfigurationService;
import com.example.application.utils.TaskStatus;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.SortOrderProvider;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.StreamResourceWriter;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.security.RolesAllowed;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

@PageTitle("FileBrowser")
@Route(value = "filebrowser", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class FileBrowserView extends VerticalLayout {


    Grid<FTPFile> grid;
    SftpClient cl;
    String sSHKeyfile;
    String sSHUser;
    Integer sSHPort;
    Long von;
    Long bis;
    private TextArea tailTextArea = new TextArea();
    ComboBox<String> umgebungComboBox = new ComboBox<>("Umgebung");
    TaskStatus stat = new TaskStatus();
    Label Filelabel = new Label();

    UI ui=UI.getCurrent();

    public FileBrowserView (@Value("${SSHHost_List}") String SSHHost_List ,@Value("${SSHPort}") Integer SSHPort, @Value("${SSHUser}") String SSHUser,@Value("${SSHKeyfile}") String SSHKeyfile,  @Value("${FTPPath_List}") String FTPPath_List, @Value("${SSHDownloadPath}") String sshDownloadPath, ConfigurationService service) throws JSchException, SftpException {

        String ftp_path_1;
        String sSHHost;
        String downloadPath;

        downloadPath=sshDownloadPath;
        ftp_path_1=FTPPath_List;
        sSHKeyfile=SSHKeyfile;
        sSHHost=SSHHost_List;
        sSHUser=SSHUser;
        sSHPort=SSHPort;


        add(new H3("Logfile-Browser"));

        Button TaskBtn = new Button("Task beenden");
        TaskBtn.addClickListener(e->stat.setActive(false));

        Button ClearBtn = new Button("Clear");
        ClearBtn.addClickListener(e->{
            System.out.println("Clear-Button gedrückt!");
            System.out.println("Länge des TextAreas vorher: " + tailTextArea.getValue().length() );
            tailTextArea.setValue("");
            System.out.println("Länge des TextAreas nachher: " + tailTextArea.getValue().length() );
        });

        tailTextArea.setMaxHeight("600px");
        tailTextArea.setWidthFull();
        tailTextArea.setValue("Noch keine Datei ausgewählt...");

        ComboBox<String> verzeichnisComboBox = new ComboBox<>("Verzeichnis");

        umgebungComboBox.setAllowCustomValue(true);
        String[] hosts = sSHHost.split(";");
        umgebungComboBox.setItems(hosts);

        verzeichnisComboBox.setAllowCustomValue(true);
        String[] dirs = ftp_path_1.split(";");
        verzeichnisComboBox.setItems(dirs);
        verzeichnisComboBox.setWidth("600px");
        // verzeichnisComboBox.setHelperText("Auswahl des Logverzeichniss");

        DateTimePicker startDateTimePicker;
        DateTimePicker endDateTimePicker;

        startDateTimePicker = new DateTimePicker(
                "Start date and time");
        //startDateTimePicker.setValue(LocalDateTime.of(2020, 8, 25, 20, 0, 0));
        startDateTimePicker.setValue(LocalDateTime.now(ZoneId.systemDefault()).minusDays(1));

        endDateTimePicker = new DateTimePicker(
                "End date and time");
        //endDateTimePicker.setValue(LocalDateTime.of(2020, 9, 1, 20, 0, 0));
        endDateTimePicker.setValue(LocalDateTime.now(ZoneId.systemDefault()).plusHours(1));
        startDateTimePicker.addValueChangeListener(
                e -> endDateTimePicker.setMin(e.getValue()));


        //cl.listFiles("/tmp");

        grid = new Grid<>(FTPFile.class, false);
        grid.addColumn(FTPFile::getName).setHeader("Name").setSortable(true).setWidth("300px").setFlexGrow(0).setResizable(true);
        grid.addColumn(FTPFile::getSize).setHeader("Größe").setSortable(true).setWidth("80px").setFlexGrow(0).setResizable(true);
        Grid.Column<FTPFile> erstellungszeitColumn = grid.addColumn(FTPFile::getErstellungszeit).setHeader("Letzte Bearbeitung").setWidth("150px").setFlexGrow(0).setSortable(true).setResizable(true);

        /*Grid.Column<FTPFile> editColumn = grid.addComponentColumn(file -> {
            Button downloadButton = new Button("Copy");
            downloadButton.addClickListener(e -> {

                Notification notification = Notification
                        .show("Download " + file.getName());
                try {
                    //getFile("/tmp/" + file.getName(),"c:\\tmp\\mq.txt");
                    //refresh(verzeichnisComboBox.getValue(), umgebungComboBox.getValue(), startDateTimePicker.getValue().toEpochSecond(ZoneOffset.UTC), endDateTimePicker.getValue().toEpochSecond(ZoneOffset.UTC));
                    getPlainFile(umgebungComboBox.getValue(), verzeichnisComboBox.getValue() + "/" + file.getName(),file.getName(),downloadPath);


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
            return downloadButton;
        }).setWidth("150px").setFlexGrow(0);
*/

        grid.addComponentColumn(file -> {
            Button button = new Button("Download");
            Anchor anchor = new Anchor(new StreamResource(file.getName(), new InputStreamFactory() {
                @Override
                public InputStream createInputStream(){
                    try {
                        return new ByteArrayInputStream(getByteFile(umgebungComboBox.getValue(), verzeichnisComboBox.getValue() + file.getName(),file.getName(),downloadPath));
                    } catch (JSchException e) {
                        throw new RuntimeException(e);
                    } catch (SftpException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

            } ),"");

           anchor.getElement().setAttribute("download",true);
           anchor.getElement().appendChild(button.getElement());
           return anchor;
        }).setFlexGrow(0).setResizable(true);

        grid.addComponentColumn(file -> {
            Button editButton = new Button("Tail");
            editButton.addClickListener(e -> {
                System.out.println("Tail-Button gedrückt für: " + verzeichnisComboBox.getValue() + "/" + file.getName());
                stat.setActive(false);

                //Welche Threads sind aktuell ongoing?



                try {
                    tail(verzeichnisComboBox.getValue() + "/" + file.getName());
                } catch (JSchException ex) {
                    throw new RuntimeException(ex);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (SftpException ex) {
                    throw new RuntimeException(ex);
                }
            });
            return editButton;
        }).setWidth("100px").setFlexGrow(0).setResizable(true);

        grid.addComponentColumn(file -> {
            Button editButton = new Button("Show");
            editButton.addClickListener(e -> {
                System.out.println("Show-Button gedrückt für: " + verzeichnisComboBox.getValue() + "/" + file.getName());
                try {
                    showFile(verzeichnisComboBox.getValue() + "/" + file.getName());
                } catch (JSchException ex) {
                    throw new RuntimeException(ex);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (SftpException ex) {
                    throw new RuntimeException(ex);
                }
            });
            return editButton;
        }).setWidth("100px").setFlexGrow(0).setResizable(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setHeight("600px");
        grid.setWidthFull();
        grid.getStyle().set("resize", "vertical");
        grid.getStyle().set("overflow", "auto");

        GridSortOrder<FTPFile> order = new GridSortOrder<>(erstellungszeitColumn, SortDirection.DESCENDING);

        grid.sort(Arrays.asList(order));

        Button button = new Button("Refresh");
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        button.addClickListener(e-> {
            try {

                ZoneId berlinZone = ZoneId.of("Europe/Berlin");

                ZonedDateTime selectedDateTime_von = startDateTimePicker.getValue().atZone(ZoneId.systemDefault());
                ZonedDateTime berlinDateTime_von = selectedDateTime_von.withZoneSameInstant(ZoneId.of("Europe/Berlin"));

                ZonedDateTime selectedDateTime_bis = endDateTimePicker.getValue().atZone(ZoneId.systemDefault());
                ZonedDateTime berlinDateTime_bis = selectedDateTime_bis.withZoneSameInstant(ZoneId.of("Europe/Berlin"));

                //refresh(verzeichnisComboBox.getValue(), umgebungComboBox.getValue(), startDateTimePicker.getValue().toEpochSecond(ZoneOffset.UTC), endDateTimePicker.getValue().toEpochSecond(ZoneOffset.UTC));
                refresh(verzeichnisComboBox.getValue(), umgebungComboBox.getValue(), berlinDateTime_von, berlinDateTime_bis);
            } catch (JSchException ex) {
                throw new RuntimeException(ex);
            } catch (SftpException ex) {
                throw new RuntimeException(ex);
            }
        });

        HorizontalLayout hl1 = new HorizontalLayout();
        hl1.add(umgebungComboBox,verzeichnisComboBox);
        hl1.setAlignItems(Alignment.BASELINE);

        HorizontalLayout hl2 = new HorizontalLayout();
        hl2.add(startDateTimePicker, endDateTimePicker,button);
        hl2.setAlignItems(Alignment.BASELINE);


        HorizontalLayout hl = new HorizontalLayout();

        Label label=new Label("File: ");
        hl.add(label,Filelabel,TaskBtn,ClearBtn);

        add(hl1,hl2,grid,hl,tailTextArea);

        stat.setActive(false);

    }

    private void tail(String file) throws JSchException, IOException, SftpException {
        cl = new SftpClient(umgebungComboBox.getValue(),sSHPort,sSHUser);
        stat.setActive(true);
        stat.setLogfile((file));

        //System.out.println("Name des aktuellen Threads: " + Thread.currentThread().getName());


        Map<Thread, StackTraceElement[]> threads = Thread.getAllStackTraces();

        for (Thread t : threads.keySet()) {
            if (t.getName().startsWith("MQ-Tail")){
                System.out.println("Es läuft bereits ein Tail-Thread: " + t.getName());
                t.interrupt();
                return;

            }

        }


        //cl.authPassword("7x24!admin4me");
        cl.authKey(sSHKeyfile,"");
        cl.TailRemoteLogFile(tailTextArea, file, stat);
        Filelabel.setText(stat.getLogfile());
    }

    private void showFile(String file) throws JSchException, IOException, SftpException {
        cl = new SftpClient(umgebungComboBox.getValue(),sSHPort,sSHUser);
        stat.setActive(false);
        stat.setLogfile((file));

        //cl.authPassword("7x24!admin4me");
        cl.authKey(sSHKeyfile,"");
        cl.ReadRemoteLogFile(ui, tailTextArea, file);
        Filelabel.setText(stat.getLogfile());
    }

    //private void refresh(String ftp_Path, String host, Long von, Long bis) throws JSchException, SftpException {
    private void refresh(String ftp_Path, String host, ZonedDateTime von, ZonedDateTime bis) throws JSchException, SftpException {


        System.out.println("ftp_Path=" + ftp_Path );
        System.out.println("sSHKeyfile=" + sSHKeyfile );
        System.out.println("host=" + host );
        System.out.println("sSHPort=" + sSHPort );
        System.out.println("sSHUser=" + sSHUser );

        System.out.println("Von: " + von.toString());
        System.out.println("Bis: " + bis.toString());

    /*    Date date_von = new Date(von);
        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        String formattedDate_von = formatter.format(date_von);
        System.out.println(formattedDate_von);

        Date date_bis = new Date(bis);
        String formattedDate_bis = formatter.format(date_bis);
        System.out.println(formattedDate_bis);*/

      //  cl = new SftpClient("37.120.189.200",9021,"michael");
        cl = new SftpClient(host,sSHPort,sSHUser);


        //cl.authPassword("7x24!admin4me");
        cl.authKey(sSHKeyfile,"");

        //List<FTPFile> files = cl.getFiles(ftp_Path, von.toEpochSecond() * 1_000_000_000L,bis.toEpochSecond() * 1_000_000_000L);
        List<FTPFile> files = cl.getFiles(ftp_Path, von.toEpochSecond() ,bis.toEpochSecond());
        grid.setItems(files);

        cl.close();
    }
    private void getPlainFile(String sSHHost, String SourceFile, String TargetFile, String Downloadpath) throws JSchException, SftpException {
        cl = new SftpClient(sSHHost,sSHPort,sSHUser);
        cl.authKey(sSHKeyfile,"");
        cl.downloadFile(SourceFile, Downloadpath + TargetFile  );
        cl.close();
    }

    private byte[] getByteFile(String sSHHost, String SourceFile, String TargetFile, String Downloadpath) throws JSchException, SftpException, IOException {
        cl = new SftpClient(sSHHost,sSHPort,sSHUser);
        cl.authKey(sSHKeyfile,"");
        //cl.downloadFile(SourceFile, Downloadpath + TargetFile  );
        var ret = cl.readFile(SourceFile);
        cl.close();
        System.out.println("In Methode getByteFile!");
        return ret;
    }

}
