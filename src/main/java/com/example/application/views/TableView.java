package com.example.application.views;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@PageTitle("Table Export")
@Route(value = "table-export", layout= MainLayout.class)
public class TableView extends VerticalLayout {

    public static Connection conn;
    private ResultSet resultset;

    public TableView() throws SQLException {
        add(new H1("Table View"));


       // Create the grid and set its items
        Grid<LinkedHashMap<String, Object>> grid2 = new Grid<>();


        List<LinkedHashMap<String,Object>> rows = retrieveRows("select * from EKP.ELA_FAVORITEN where rownum<500");
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
        add(grid2);



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
            rs.close();


        }
       // conn.close();
        return rows;
    }

}
