package com.example.application.views;

import com.example.application.data.entity.QSql;
import com.example.application.data.entity.TableInfo;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.BoxSizing;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;

import java.io.*;
import java.sql.*;
import java.util.*;

@PageTitle("Table Export")
@Route(value = "table-view", layout= MainLayout.class)
public class TableView extends VerticalLayout {

    public static Connection conn;
    private ResultSet resultset;
    private Button smallButton = new Button("Export");
    private String aktuelle_SQL="";
    private String aktuelle_Tabelle="";
    private Anchor anchor = new Anchor(getStreamResource(aktuelle_Tabelle + ".xls", "default content"), "click to download");

    Grid<LinkedHashMap<String, Object>> grid2 = new Grid<>();

    private static String url;
    private static String user;
    private static String password;

    public TableView() throws SQLException, IOException {
        //add(new H1("Table View"));

        anchor.getElement().setAttribute("download",true);
        anchor.setEnabled(false);
        smallButton.setVisible(false);

        MenuBar menuBar = new MenuBar();

        Properties properties = new Properties();
        BufferedInputStream stream = new BufferedInputStream(new FileInputStream("config.properties"));
        properties.load(stream);
        stream.close();
        url = properties.getProperty("tableview.url");
        user = properties.getProperty("tableview.user");
        password = properties.getProperty("tableview.password");



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
                        aktuelle_SQL=line.SQL;
                        aktuelle_Tabelle=line.Name;
                        show_grid(line.SQL);
                        anchor.setEnabled(false);
                        smallButton.setVisible(true);
                      //  TextField filenameTextField = new TextField("input file name here");
                      //  filenameTextField.setValue("default.txt");
                        //Anchor anchor = new Anchor(getStreamResource("default.txt", "default content"), "click me to download");

                  //      byte[] bytes = Files.readAllBytes(Paths.get("c:\\tmp\\" + aktuelle_Tabelle + ".xls"));

                    //    StreamResource resource = new StreamResource(aktuelle_Tabelle + ".xls",
//                                () -> new ByteArrayInputStream(bytes));
                        //    () -> new ByteArrayInputStream("Hello world".getBytes(StandardCharsets.UTF_8)));



                       // anchor.setHref(getStreamResource(aktuelle_Tabelle + ".xls", " contains some text"));
                 //       anchor.setHref(resource);


                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                System.out.println("Name: " + line.Name );
              //  System.out.println("SQL: " + line.SQL );
            }



        };

        //Div message = new Div(new Text("-->  "), selected);
        H3 message = new H3(selected);

        MenuItem move = menuBar.addItem("Auswahl");
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


        //Export Button

        smallButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        smallButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        smallButton.addClickListener(clickEvent -> {
            Notification.show("Exportiere " + aktuelle_Tabelle);
            //System.out.println("aktuelle_SQL:" + aktuelle_SQL);
            try {
                generateExcel("c:\\tmp\\" + aktuelle_Tabelle + ".xls",aktuelle_SQL);

                File file= new File("c:\\tmp\\" + aktuelle_Tabelle +".xls");
                StreamResource streamResource = new StreamResource(file.getName(),()->getStream(file));

                anchor.setHref(streamResource);
                //anchor = new Anchor(streamResource, String.format("%s (%d KB)", file.getName(), (int) file.length() / 1024));

                anchor.setEnabled(true);
                smallButton.setVisible(false);
          //      download("c:\\tmp\\" + aktuelle_Tabelle + ".xls");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });




        HorizontalLayout TableChooser = new HorizontalLayout ();
        //TableChooser.setAlignItems(Alignment.CENTER);
        TableChooser.setAlignItems(Alignment.BASELINE );
        TableChooser.add(menuBar);
        TableChooser.add(message);

        HorizontalLayout horizontalLayout = new HorizontalLayout ();
        horizontalLayout.setWidth("100%");
       // horizontalLayout.setAlignItems(Alignment.CENTER);
        horizontalLayout.setAlignItems(Alignment.BASELINE);
        horizontalLayout.setSpacing(true);
        horizontalLayout.setPadding(true);
       // horizontalLayout.setMargin(true);

        horizontalLayout.add(TableChooser);
        horizontalLayout.add(smallButton);
        horizontalLayout.add(anchor);





        horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);


        horizontalLayout.setFlexGrow(1,TableChooser);
        add(horizontalLayout);

        show_grid("select 'Choose Table first!' as Info from dual");

        add(grid2);


    }

    private InputStream getStream(File file) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return stream;
    }

    public StreamResource getStreamResource(String filename, String content) {
        return new StreamResource(filename,
                () -> new ByteArrayInputStream(content.getBytes()));
    }

    public static void fileOutputStreamByteSingle(String file, String data) throws IOException {
        byte[] bytes = data.getBytes();
        try (OutputStream out = new FileOutputStream(file)) {
            out.write(bytes);
        }
    }


    private void show_grid(String sql) throws SQLException, IOException {
        System.out.println(sql);
        // Create the grid and set its items
        //Grid<LinkedHashMap<String, Object>> grid2 = new Grid<>();
        grid2.removeAllColumns();

        //List<LinkedHashMap<String,Object>> rows = retrieveRows("select * from EKP.ELA_FAVORITEN where rownum<200");
        List<LinkedHashMap<String,Object>> rows = retrieveRows(sql);

        if(!rows.isEmpty()){
            grid2.setItems( rows); // rows is the result of retrieveRows

            // Add the columns based on the first row
            LinkedHashMap<String, Object> s = rows.get(0);
            for (Map.Entry<String, Object> entry : s.entrySet()) {
                grid2.addColumn(h -> h.get(entry.getKey().toString())).setHeader(entry.getKey()).setAutoWidth(true).setResizable(true).setSortable(true);
            }

            grid2.addThemeVariants(GridVariant.LUMO_WRAP_CELL_CONTENT);
            grid2.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
            grid2.addThemeVariants(GridVariant.LUMO_COMPACT);
            //   grid2.setAllRowsVisible(true);
            grid2.setPageSize(50);
            grid2.setHeight("800px");
            //grid2.setPaginatorSize(5);
            // Add the grid to the page

            this.setPadding(false);
            this.setSpacing(false);
            this.setBoxSizing(BoxSizing.CONTENT_BOX);

        }
        else {
            //Text txt = new Text("Es konnten keine Daten  abgerufen werden!");
            //add(txt);
        }

    }

    public List<LinkedHashMap<String,Object>> retrieveRows(String queryString) throws SQLException, IOException {




        List<LinkedHashMap<String, Object>> rows = new LinkedList<LinkedHashMap<String, Object>>();

        PreparedStatement s = null;
        ResultSet rs = null;
        try
        {
        //    String url="jdbc:oracle:thin:@37.120.189.200:1521:xe";
        //    String user="SYSTEM";
        //    String password="Michael123";

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
           // e.printStackTrace();
            add(new Text(e.getMessage()));

            return Collections.emptyList();
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

    private static void generateExcel(String file, String query) throws IOException {
        try {
            //String url="jdbc:oracle:thin:@37.120.189.200:1521:xe";
            //String user="SYSTEM";
            //String password="Michael123";

            Class.forName("oracle.jdbc.driver.OracleDriver");

            //   DriverManager.registerDriver(new oracle.jdbc.driver.OracleDriver());
            Connection conn=DriverManager.getConnection(url, user, password);


            PreparedStatement stmt=null;
            //Workbook
            HSSFWorkbook workBook=new HSSFWorkbook();
            HSSFSheet sheet1=null;

            //Cell
            Cell c=null;

            CellStyle cs=workBook.createCellStyle();
            HSSFFont f =workBook.createFont();
            f.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
            f.setFontHeightInPoints((short) 12);
            cs.setFont(f);


            sheet1=workBook.createSheet("Sheet1");


            // String query="select  EMPNO, ENAME, JOB, MGR, HIREDATE, SAL, COMM, DEPTNO, WORK_CITY, WORK_COUNTRY from APEX_040000.WWV_DEMO_EMP";
            stmt=conn.prepareStatement(query);
            ResultSet rs=stmt.executeQuery();

            ResultSetMetaData metaData=rs.getMetaData();
            int colCount=metaData.getColumnCount();

            LinkedHashMap<Integer, TableInfo> hashMap=new LinkedHashMap<Integer, TableInfo>();


            for(int i=0;i<colCount;i++){
                TableInfo tableInfo=new TableInfo();
                tableInfo.setFieldName(metaData.getColumnName(i+1).trim());
                tableInfo.setFieldText(metaData.getColumnLabel(i+1));
                tableInfo.setFieldSize(metaData.getPrecision(i+1));
                tableInfo.setFieldDecimal(metaData.getScale(i+1));
                tableInfo.setFieldType(metaData.getColumnType(i+1));
                //     tableInfo.setCellStyle(getCellAttributes(workBook, c, tableInfo));

                hashMap.put(i, tableInfo);
            }

            //Row and Column Indexes
            int idx=0;
            int idy=0;

            HSSFRow row=sheet1.createRow(idx);
            TableInfo tableInfo=new TableInfo();

            Iterator<Integer> iterator=hashMap.keySet().iterator();

            while(iterator.hasNext()){
                Integer key=(Integer)iterator.next();

                tableInfo=hashMap.get(key);
                c=row.createCell(idy);
                c.setCellValue(tableInfo.getFieldText());
                c.setCellStyle(cs);
                if(tableInfo.getFieldSize() > tableInfo.getFieldText().trim().length()){
                    sheet1.setColumnWidth(idy, (tableInfo.getFieldSize()* 10));
                }
                else {
                    sheet1.setColumnWidth(idy, (tableInfo.getFieldText().trim().length() * 5));
                }
                idy++;
            }

            while (rs.next()) {

                idx++;
                row = sheet1.createRow(idx);
              //  System.out.println(idx);
                for (int i = 0; i < colCount; i++) {

                    c = row.createCell(i);
                    tableInfo = hashMap.get(i);

                    switch (tableInfo.getFieldType()) {
                        case 1:
                            c.setCellValue(rs.getString(i+1));
                            break;
                        case 2:
                            c.setCellValue(rs.getDouble(i+1));
                            break;
                        case 3:
                            c.setCellValue(rs.getDouble(i+1));
                            break;
                        default:
                            c.setCellValue(rs.getString(i+1));
                            break;
                    }
                    c.setCellStyle(tableInfo.getCellStyle());
                }

            }
            rs.close();
            stmt.close();
            conn.close();

            // String path="c:\\tmp\\test.xls";

            FileOutputStream fileOut = new FileOutputStream(file);

            workBook.write(fileOut);
            fileOut.close();





        } catch (SQLException | FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
