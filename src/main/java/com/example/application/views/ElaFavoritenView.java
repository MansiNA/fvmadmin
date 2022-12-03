package com.example.application.views;

import com.example.application.DownloadLinksArea;
import com.example.application.UploadArea;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.io.File;


@PageTitle("Upload ELA-Favoriten | by DBUSS GmbH")
@Route(value = "ela-upload", layout= MainLayout.class)
public class ElaFavoritenView extends VerticalLayout {

    public ElaFavoritenView(){


        File uploadFolder = getUploadFolder();
        UploadArea uploadArea = new UploadArea(uploadFolder);
        DownloadLinksArea linksArea = new DownloadLinksArea(uploadFolder);

        uploadArea.getUploadField().addSucceededListener(e -> {
            uploadArea.hideErrorField();
            linksArea.refreshFileLinks();
        });

        add(uploadArea);






//        Grid<Film> grid = new Grid<>(Film.class);
//        add(new H1("Upload der ELA-Favoriten Excel-Liste"));
//
//
//        List<Film> customerList = new ArrayList<>();
//        try{
//            //step1 load the driver class
//            Class.forName("oracle.jdbc.driver.OracleDriver");
//
//            //step2 create  the connection object
//            Connection con= DriverManager.getConnection("jdbc:oracle:thin:@37.120.189.200:1521:xe","SYSTEM","Michael123");
//
//            //step3 create the statement object
//            Statement stmt=con.createStatement();
//
//            //step4 execute query
//            ResultSet rs=stmt.executeQuery("select Film_ID, Film_Name from FILM");
//            while(rs.next()) {
//                Film customer = new Film();
//                customer.setFilm_ID(Integer.getInteger(rs.getString(1)));
//                customer.setFilm_Name(rs.getString(2));
//
//                customerList.add(customer);
//            }
//
//            //step5 close the connection object
//            con.close();
//
//        }catch(Exception e){ System.out.println(e);}
//
//
//        //GRID
//      //  grid.setColumns("Film_ID", "Film_Name");
//        grid.setItems(customerList);
//        add(grid);


    }


    private static File getUploadFolder() {
        File folder = new File("uploaded-files");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }
}
