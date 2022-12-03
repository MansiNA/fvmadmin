package com.example.application.views;

import com.example.application.DownloadLinksArea;
import com.example.application.UploadArea;
import com.example.application.data.entity.TableInfo;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.io.*;
import java.sql.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;

@PageTitle("Table Export")
@Route(value = "table-view", layout= MainLayout.class)
@ConfigurationPropertiesScan
public class TableExportView extends VerticalLayout {

  //  @Value("${export.tables}")
  //  private String tables;

    //@Autowired
    //private Environment env;

    CheckboxGroup<String> checkboxGroup = new CheckboxGroup<>();



    //private String tables = env.getProperty("export.tables");

    public TableExportView() throws IOException {

        add(new H1("Table Export"));




//        Properties prop = new Properties();
//
//        try (OutputStream output = new FileOutputStream("./config.properties")) {
//
//            // set the properties value
//            prop.setProperty("export.tables", "TabA;TabB");
//
//            // save properties to project root folder
//            prop.store(output, null);
//
//            System.out.println(prop);
//
//        } catch (IOException io) {
//            io.printStackTrace();
//        }


  //      System.out.println(prop.getProperty("export.tables"));

        Properties properties = new Properties();
        BufferedInputStream stream = new BufferedInputStream(new FileInputStream("config.properties"));
        properties.load(stream);
        stream.close();
        String tables = properties.getProperty("export.tables");
//
//        System.out.println(tables);

//        List<String> tab = new ArrayList<String>();
//        tab.add(("Tab1"));
//        tab.add(("Tab2"));
//        tab.add(("Tab3"));



        if(tables==null || tables.isEmpty() ){
            add(new H2("Keine Tabellen freigegeben!"));
        }
        else {

            String[] tab = tables.split(";");

            checkboxGroup.setLabel("Auswahl der zu exportierenden Tabellen");
            checkboxGroup.setItems (tab);


            //checkboxGroup.select("Order ID", "Customer");
            checkboxGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);

            add(checkboxGroup);

       //Download-Area:
       File uploadFolder = getUploadFolder();
       UploadArea uploadArea = new UploadArea(uploadFolder);
       DownloadLinksArea linksArea = new DownloadLinksArea(uploadFolder);

        Button button = new Button("Start");
        Paragraph info = new Paragraph(infoText());
        button.addClickListener(clickEvent -> {
            info.setText(infoText());

            for (String table : checkboxGroup.getSelectedItems())
            {
                System.out.println("Start export von: " + table);
                try {
                    File folder = getUploadFolder();
                 //   String file = folder.getPath() + "\\" + table + ".xls";
                    String file = folder.getPath() + "/" + table + ".xls";

                    generateExcel(file, "select * from " + table);
                    linksArea.refreshFileLinks();
                } catch (IOException e) {
                    System.out.println("Error: " + e.toString());
                }
            }

        });

        HorizontalLayout horizontalLayout = new HorizontalLayout(button, info);
        horizontalLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        add(horizontalLayout);



            uploadArea.getUploadField().addSucceededListener(e -> {
                uploadArea.hideErrorField();
                linksArea.refreshFileLinks();
            });

           // add(uploadArea, linksArea);
            add(linksArea);



        }

       // generateExcel("c:\\tmp\\test.xls", "select  EMPNO, ENAME, JOB, MGR, HIREDATE, SAL, COMM, DEPTNO, WORK_CITY, WORK_COUNTRY from APEX_040000.WWV_DEMO_EMP");

    }

    private static File getUploadFolder() {
        File folder = new File("uploaded-files");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }


    private String infoText() {

        if (checkboxGroup.getSelectedItems().isEmpty() )
        {
            return "";
        }

        return String.format("Export der Tabellen: %s ",checkboxGroup.getSelectedItems() );
    }

    private static void generateExcel(String file, String query) throws IOException {
        try {
            String url="jdbc:oracle:thin:@37.120.189.200:1521:xe";
            String user="SYSTEM";
            String password="Michael123";

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
                System.out.println(idx);
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
