package com.example.application.views;

import com.example.application.data.entity.FTPFile;
import com.example.application.data.entity.ServerConfiguration;
import com.example.application.data.service.ConfigurationService;
import com.example.application.data.service.ServerConfigurationService;
import com.example.application.utils.TaskStatus;
import com.example.application.utils.ThreadUtils;
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
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@PageTitle("FileBrowser")
@Route(value = "filebrowser", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class FileBrowserView extends VerticalLayout {

    private final ServerConfigurationService serverConfigurationService;

    Grid<FTPFile> grid;
    SftpClient cl;
    Long von;
    Long bis;
    private TextArea tailTextArea = new TextArea();
    ComboBox<String> pathCombobox = new ComboBox<>("Choose Path");
    ComboBox<ServerConfiguration> serverconfigComboBox = new ComboBox<>("Host");
    TaskStatus stat = new TaskStatus();
    Label Filelabel = new Label();

    UI ui=UI.getCurrent();
    private String selectedPath;
    private ServerConfiguration selectedServerConfig;

    public FileBrowserView (ConfigurationService service, ServerConfigurationService serverConfigurationService) throws JSchException, SftpException {
        String sshDownloadPath= "downloads/";
        this.serverConfigurationService = serverConfigurationService;

        try {
            List<ServerConfiguration> serverConfigList = serverConfigurationService.findAllConfigurations();
            if (serverConfigList != null && !serverConfigList.isEmpty()) {
                serverconfigComboBox.setItems(serverConfigList);
                serverconfigComboBox.setItemLabelGenerator(ServerConfiguration::getHostAlias);
                selectedServerConfig = serverConfigList.get(0);
                serverconfigComboBox.setValue(selectedServerConfig);
                populatePathCombobox(selectedServerConfig);
            }
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }

        serverconfigComboBox.addValueChangeListener(event -> {
            selectedServerConfig = event.getValue();
            if (selectedServerConfig != null) {
                populatePathCombobox(selectedServerConfig);
            }
        });

        pathCombobox.addValueChangeListener(event -> {
            selectedPath = event.getValue();
        });


        add(new H3("Logfile-Browser"));


        Button EndTaskBtn = new Button("Tail beenden");
        EndTaskBtn.addClickListener(e-> {
                    stat.setActive(false);
                    EndTaskBtn.setVisible(false);

//            ThreadUtils.dumpThreads();


                    ThreadGroup group = Thread.currentThread().getThreadGroup();
                    Thread[] threads = new Thread[group.activeCount()];
                    group.enumerate(threads);

                    Thread tailThread = new Thread();

                    for (Thread thread : threads) {
                        if (thread != null && thread.getName().equals("Tail-Thread")) {
                            tailThread = thread;
                            thread.interrupt();
                        }

                    }
            System.out.println("Tail Thread-Status: " + tailThread.getState());

        });





        tailTextArea.setMaxHeight("600px");
        tailTextArea.setWidthFull();
    //    tailTextArea.setReadOnly(true);
        tailTextArea.setValue("Noch keine Datei ausgewählt...");

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
                        System.out.println("download-Button gedrückt für: " + selectedPath + "/" + file.getName());
                        return new ByteArrayInputStream(getByteFile(selectedServerConfig.getHostName(), selectedPath ,file.getName(), sshDownloadPath));
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
            Button editButton = new Button("Tail -f");
            editButton.addClickListener(e -> {
                System.out.println("Tail-Button gedrückt für: " + selectedPath + "/" + file.getName());
                stat.setActive(false);
                EndTaskBtn.setVisible(true);

                //Welche Threads sind aktuell ongoing?
                ThreadUtils.dumpThreads();




                try {
                    tail(selectedPath+ "/" + file.getName());
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
                System.out.println("Show-Button gedrückt für: " + selectedPath + "/" + file.getName());
                try {
                    showFile(selectedPath + "/" + file.getName());
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
                refresh(selectedPath, selectedServerConfig.getHostName(), berlinDateTime_von, berlinDateTime_bis);
            } catch (JSchException ex) {
                throw new RuntimeException(ex);
            } catch (SftpException ex) {
                throw new RuntimeException(ex);
            }
        });

        HorizontalLayout hl1 = new HorizontalLayout();
        hl1.add(serverconfigComboBox, pathCombobox);
        hl1.setAlignItems(Alignment.BASELINE);

        HorizontalLayout hl2 = new HorizontalLayout();
        hl2.add(startDateTimePicker, endDateTimePicker,button);
        hl2.setAlignItems(Alignment.BASELINE);


        HorizontalLayout hl = new HorizontalLayout();

        Label label=new Label("File: ");
        hl.add(label,Filelabel,EndTaskBtn);

        EndTaskBtn.setVisible(false);

        add(hl1,hl2,grid,hl,tailTextArea);

        stat.setActive(false);

    }

    private void tail(String file) throws JSchException, IOException, SftpException {
        cl = new SftpClient(selectedServerConfig.getHostName(),Integer.parseInt(selectedServerConfig.getSshPort()),selectedServerConfig.getUserName());
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
        cl.authKey(selectedServerConfig.getSshKey(),"");
        cl.TailRemoteLogFile(tailTextArea, file, stat);
        Filelabel.setText(stat.getLogfile());
    }

    private void populatePathCombobox(ServerConfiguration config) {
        String pathList = config.getPathList();
        if (pathList != null && !pathList.isEmpty()) {
            List<String> paths = Arrays.asList(pathList.split(";"));
            pathCombobox.setItems(paths);
            System.out.println("selected path "+paths.get(0));
            selectedPath = paths.get(0);
            pathCombobox.setValue(selectedPath);
        } else {
            pathCombobox.clear();
        }
    }
    private void showFile(String file) throws JSchException, IOException, SftpException {

        cl = new SftpClient(selectedServerConfig.getHostName(),Integer.parseInt(selectedServerConfig.getSshPort()),selectedServerConfig.getUserName());
        stat.setActive(false);
        stat.setLogfile((file));

        //cl.authPassword("7x24!admin4me");
        cl.authKey(selectedServerConfig.getSshKey(),"");
        cl.ReadRemoteLogFile(ui, tailTextArea, file);
        Filelabel.setText(stat.getLogfile());
    }

    //private void refresh(String ftp_Path, String host, Long von, Long bis) throws JSchException, SftpException {
    private void refresh(String ftp_Path, String host, ZonedDateTime von, ZonedDateTime bis) throws JSchException, SftpException {

        System.out.println("ftp_Path=" + ftp_Path );
        System.out.println("sSHKeyfile=" + selectedServerConfig.getSshKey() );
        System.out.println("host=" + host );
        System.out.println("sSHPort=" + selectedServerConfig.getSshPort() );
        System.out.println("sSHUser=" + selectedServerConfig.getUserName() );

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
        cl = new SftpClient(host, Integer.parseInt(selectedServerConfig.getSshPort()),selectedServerConfig.getUserName());


        //cl.authPassword("7x24!admin4me");
        cl.authKey(selectedServerConfig.getSshKey(),"");

        //List<FTPFile> files = cl.getFiles(ftp_Path, von.toEpochSecond() * 1_000_000_000L,bis.toEpochSecond() * 1_000_000_000L);
        List<FTPFile> files = cl.getFiles(ftp_Path, von.toEpochSecond() ,bis.toEpochSecond());
        grid.setItems(files);

        cl.close();
    }
    private void getPlainFile(String sSHHost, String SourceFile, String TargetFile, String Downloadpath) throws JSchException, SftpException {

        cl = new SftpClient(sSHHost, Integer.parseInt(selectedServerConfig.getSshPort()),selectedServerConfig.getUserName());
        cl.authKey(selectedServerConfig.getSshKey(),"");
        cl.downloadFile(SourceFile, Downloadpath + TargetFile  );
        cl.close();
    }

    private byte[] getByteFile(String sSHHost, String SourceFile, String TargetFile, String Downloadpath) throws JSchException, SftpException, IOException {

        cl = new SftpClient(sSHHost, Integer.parseInt(selectedServerConfig.getSshPort()),selectedServerConfig.getUserName());
        cl.authKey(selectedServerConfig.getSshKey(),"");
   //     cl.downloadFile(SourceFile, Downloadpath + TargetFile  );
        System.out.println("vvvvvvvvvvv "+ SourceFile);
        var ret = cl.readFile(SourceFile + TargetFile);
        cl.close();
        System.out.println("In Methode getByteFile!");
        return ret;
    }

}
