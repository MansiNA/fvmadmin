package com.example.application.views;

import com.example.application.data.entity.QSql;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
import java.util.*;

@PageTitle("Table Export")
@Route(value = "table-view", layout= MainLayout.class)
public class TableView extends VerticalLayout {

    public static Connection conn;
    private ResultSet resultset;
    Grid<LinkedHashMap<String, Object>> grid2 = new Grid<>();
    public TableView() throws SQLException, FileNotFoundException {
        //add(new H1("Table View"));


        MenuBar menuBar = new MenuBar();
        Text selected = new Text("");
        //ComponentEventListener<ClickEvent<MenuItem>> listener = e -> selected.setText(e.getSource().getText());

        //Read File for SQLs
        File text = new File("C:\\tmp\\sql.txt");
        //Creating Scanner instance to read File in Java
        Scanner scnr = new Scanner(text);

        List<QSql> aList = new ArrayList<QSql>();


        ComponentEventListener<ClickEvent<MenuItem>> listener = e -> {
                 selected.setText(e.getSource().getText());

//                try {
//                    if(e.getSource().getText().contains("ERV-Mapping")) {
//                        show_grid("select * from books");
//                    }
//                    else {
//                        show_grid("select * from customer");
//                    }
//                } catch (SQLException ex) {
//                    throw new RuntimeException(ex);
//                }
//        };

            System.out.println("Ausgewählt: " + e.getSource().getText());

            for (QSql line : aList){
                if(e.getSource().getText().contains(line.Name))
                {
                    try {
                        System.out.println("jetzt Ausführen: " + line.SQL);
                        show_grid(line.SQL);
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                System.out.println("Name: " + line.Name );
              //  System.out.println("SQL: " + line.SQL );
            }



        };


            Div message = new Div(new Text("-->  "), selected);

        MenuItem move = menuBar.addItem("Auswahl Tabelle");
        SubMenu moveSubMenu = move.getSubMenu();



        //Reading each line of the file using Scanner class
        int lineNumber = 1;
        while(scnr.hasNextLine()){
            String line = scnr.nextLine();

            QSql s = new QSql();
            s.Name=line.split(";")[0];
            s.SQL=line.split(";")[1];
            //aList.add(Arrays.asList(line.split(";")));
            aList.add(s);


            //System.out.println("line " + lineNumber + " :" + line);
            System.out.println("Table: " + line.split(";")[0]);
            System.out.println("SQL: " + line.split(";")[1]);
            moveSubMenu.addItem(line.split(";")[0], listener);
            lineNumber++;
        }



        //moveSubMenu.addItem("Metadaten", listener);
        //moveSubMenu.addItem("ERV-Mapping", listener);

        HorizontalLayout horizontalLayout = new HorizontalLayout ();
        horizontalLayout.setWidth("100%");
        horizontalLayout.setAlignItems(Alignment.CENTER);
        horizontalLayout.add(menuBar);
        horizontalLayout.add(message);

        add(horizontalLayout);

        show_grid("select 'Choose Table first!' as Info from dual");

        add(grid2);
    }

    private void show_grid(String sql) throws SQLException
    {
        System.out.println(sql);
        // Create the grid and set its items
        //Grid<LinkedHashMap<String, Object>> grid2 = new Grid<>();
        grid2.removeAllColumns();

        //List<LinkedHashMap<String,Object>> rows = retrieveRows("select * from EKP.ELA_FAVORITEN where rownum<200");
        List<LinkedHashMap<String,Object>> rows = retrieveRows(sql);
        //List<LinkedHashMap<String,Object>> rows = retrieveRows("select * from EKP.AM_MAILBOX");

        grid2.setItems( rows); // rows is the result of retrieveRows

        // Add the columns based on the first row
        LinkedHashMap<String, Object> s = rows.get(0);
        for (Map.Entry<String, Object> entry : s.entrySet()) {
            grid2.addColumn(h -> h.get(entry.getKey().toString())).setHeader(entry.getKey()).setAutoWidth(true).setResizable(true).setSortable(true);
        }

        grid2.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
        grid2.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid2.addThemeVariants(GridVariant.LUMO_COMPACT);
        grid2.setAllRowsVisible(true);
        //grid2.setPageSize(16);
        //grid2.setPaginatorSize(5);
        // Add the grid to the page

        this.setPadding(false);
        this.setSpacing(false);
        this.setBoxSizing(BoxSizing.CONTENT_BOX);

        //add(grid2);

    }

    public List<LinkedHashMap<String,Object>> retrieveRows(String queryString) throws SQLException{

        List<LinkedHashMap<String, Object>> rows = new LinkedList<LinkedHashMap<String, Object>>();

        PreparedStatement s = null;
        ResultSet rs = null;
        try
        {
            String url="jdbc:oracle:thin:@37.120.189.200:1521:xe";
            String user="SYSTEM";
            String password="Michael123";

            Class.forName("oracle.jdbc.driver.OracleDriver");

            //   DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            Connection conn=DriverManager.getConnection(url, user, password);


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
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {

            try { rs.close(); } catch (Exception e) { /* Ignored */ }
            try { s.close(); } catch (Exception e) { /* Ignored */ }
            try { conn.close(); } catch (Exception e) { /* Ignored */ }


        }
       // conn.close();
        return rows;
    }

}
