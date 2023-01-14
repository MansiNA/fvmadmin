package com.example.application.views;

import com.example.application.data.service.ConfigurationService;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@PageTitle("FileBrowser")
@Route(value = "filebrowser", layout= MainLayout.class)
public class FileBrowserView extends VerticalLayout {

    public FileBrowserView (ConfigurationService service) throws JSchException, SftpException {


        add(new H3("File-Browser"));

        SftpClient cl = new SftpClient("37.120.189.200",9021,"michael");
//        cl.authPassword("7x24!admin4me");
        cl.authKey("C:\\tmp\\id_rsa","");

        cl.listFiles("/tmp");
    }

}
