package com.example.application.views;

import com.example.application.data.service.CrmService;
import com.example.application.service.BackendService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import java.util.Timer;
import java.util.TimerTask;


@PageTitle("Dashboard | by DBUSS GmbH")
@Route(value = "dashboard", layout= MainLayout.class)
@CssImport(value="./styles/Gauge.css", themeFor = "vaadin-chart", include = "vaadin-chart-default-theme")
@AnonymousAllowed
public class DashboardView extends VerticalLayout{

    private BackendService bk_service;
    private CrmService service;
    private Span currentPrice = new Span();

    final  ListSeries mySeries;
    ListSeries series = new ListSeries("Speed", 139);

    Chart chart1 = new Chart();

    public DashboardView(CrmService service, BackendService bk_service){
        this.service = service;
        this.bk_service=bk_service;
        //add(new H1("FVM-Status Dashboard"));


        Paragraph paragraph= new Paragraph("Hier ist die Anzeige von aktuellen Metriken aus der DB geplant");
        paragraph.setMaxHeight("400px");


        add(paragraph);


        HorizontalLayout header = new HorizontalLayout();
        currentPrice.setText("Aktueller Wert: " + series.toString());


        header.add(currentPrice);

        add(header);




        //Iframe iframe = new Iframe();

        IFrame iframe = new IFrame();
        iframe.setSrc("https:\\www.dbuss.de");
        iframe.setWidthFull();
        iframe.setHeight("600px");

       // add(iframe);

        Anchor a = new Anchor("https:\\www.dbuss.de","DBUSS");
        //add(a);

  /*      Chart chart2 = new Chart();
        Configuration configuration2 = chart2.getConfiguration();

        configuration2.getChart().setType(ChartType.LINE);

        configuration2.getxAxis().setCategories("Jan", "Feb", "Mar", "Apr");

        DataSeries ds = new DataSeries();
        ds.setData(7.0, 6.9, 9.5, 14.5);

        DataLabels callout = new DataLabels(true);
        callout.setShape(Shape.CALLOUT);
        callout.setY(-12);
        ds.get(1).setDataLabels(callout);
        ds.get(2).setDataLabels(callout);
        configuration2.addSeries(ds);

        chart2.addClassName("first-chart");
        add(chart2);*/




       chart1 = build_chart(99);







   /*     runWhileAttached(chart1, () -> {
            Integer oldValue = series.getData()[0].intValue();
            Integer newValue = (int) (oldValue + (random.nextDouble() - 0.5) * 20.0);
            series.updatePoint(0, newValue);
            }
            , 5000
            , 12000
        );*/



        add(chart1);

        final TextField tf = new TextField("Enter a new value");
        add(tf);



        mySeries=series;


        Button update = new Button("Update", (e) -> {
            Integer newValue = new Integer(tf.getValue());
            mySeries.updatePoint(0, newValue);
        });
        add(update);








        final Chart chart = new Chart(ChartType.COLUMN);
        chart.setId("chart");

        final Configuration conf = chart.getConfiguration();

        conf.setTitle("Nachrichtendurchsatz");
        conf.setSubTitle("Quelle eKP / EGVP-W");
        conf.getLegend().setEnabled(false);

        XAxis x = new XAxis();
        x.setType(AxisType.CATEGORY);
        conf.addxAxis(x);

        YAxis y = new YAxis();
        y.setTitle("Anzahl Nachrichten");
        conf.addyAxis(y);

        PlotOptionsColumn column = new PlotOptionsColumn();
        column.setCursor(Cursor.POINTER);
        column.setDataLabels(new DataLabels(true));

        conf.setPlotOptions(column);

        DataSeries regionsSeries = new DataSeries();
        regionsSeries.setName("Gesamt");
        PlotOptionsColumn plotOptionsColumn = new PlotOptionsColumn();
        plotOptionsColumn.setColorByPoint(true);
        regionsSeries.setPlotOptions(plotOptionsColumn);

        DataSeriesItem regionItem = new DataSeriesItem(
                "Gesamt", 120);

        DataSeries countriesSeries = new DataSeries("Countries");
        countriesSeries.setId("Latin America and Carribean Countries");

        DataSeriesItem countryItem = new DataSeriesItem("Costa Rica", 64);
        DataSeries detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Costa Rica");
        String[] categories = new String[] { "Life Expectancy",
                "Well-being (0-10)", "Footprint (gha/capita)" };
        Number[] ys = new Number[] { 79.3, 7.3, 2.5 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Colombia", 59.8);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Colombia");
        ys = new Number[] { 73.7, 6.4, 1.8 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Belize", 59.3);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Belize");
        ys = new Number[] { 76.1, 6.5, 2.1 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("El Salvador", 58.9);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details El Salvador");
        ys = new Number[] { 72.2, 6.7, 2.0 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        regionsSeries.addItemWithDrilldown(regionItem, countriesSeries);

        regionItem = new DataSeriesItem("Western Nations", 50);

        countriesSeries = new DataSeries("Countries");
        countriesSeries.setId("Western Nations Countries");

        countryItem = new DataSeriesItem("New Zealand", 51.6);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details New Zealand");
        ys = new Number[] { 80.7, 7.2, 4.3 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Norway", 51.4);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Norway");
        ys = new Number[] { 81.1, 7.6, 4.8 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Switzerland", 50.3);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Switzerland");
        ys = new Number[] { 82.3, 7.5, 5.0 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("United Kingdom", 47.9);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details United Kingdom");
        ys = new Number[] { 80.2, 7.0, 4.7 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        regionsSeries.addItemWithDrilldown(regionItem, countriesSeries);

        regionItem = new DataSeriesItem("Middle East and North Africa", 53);

        countriesSeries = new DataSeries("Countries");
        countriesSeries.setId("Middle East and North Africa Countries");

        countryItem = new DataSeriesItem("Israel", 55.2);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Israel");
        ys = new Number[] { 81.6, 7.4, 4.0 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Algeria", 52.2);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Algeria");
        ys = new Number[] { 73.1, 5.2, 1.6 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Jordan", 51.7);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Jordan");
        ys = new Number[] { 73.4, 5.7, 2.1 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Palestine", 51.2);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Palestine");
        ys = new Number[] { 72.8, 4.8, 1.4 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        regionsSeries.addItemWithDrilldown(regionItem, countriesSeries);

        regionItem = new DataSeriesItem("Sub-Saharan Africa", 42);

        countriesSeries = new DataSeries("Countries");
        countriesSeries.setId("Sub-Saharan Africa Countries");

        countryItem = new DataSeriesItem("Nigeria", 51.6);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Nigeria");
        ys = new Number[] { 66.7, 4.6, 1.2 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Malawi", 42.5);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Malawi");
        ys = new Number[] { 54.2, 5.1, 0.8 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Ghana", 40.3);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Ghana");
        ys = new Number[] { 64.2, 4.6, 1.7 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Ethiopia", 39.2);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Ethiopia");
        ys = new Number[] { 59.3, 4.4, 1.1 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        regionsSeries.addItemWithDrilldown(regionItem, countriesSeries);

        regionItem = new DataSeriesItem("South Asia", 53);

        countriesSeries = new DataSeries("Countries");
        countriesSeries.setId("South Asia Countries");

        countryItem = new DataSeriesItem("Bangladesh", 56.3);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Bangladesh");
        ys = new Number[] { 68.9, 5.0, 0.7 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Pakistan", 54.1);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Pakistan");
        ys = new Number[] { 65.4, 5.3, 0.8 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("India", 50.9);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details India");
        ys = new Number[] { 65.4, 5.0, 0.9 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Sri Lanka", 51.2);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Sri Lanka");
        ys = new Number[] { 74.9, 4.2, 1.2 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        regionsSeries.addItemWithDrilldown(regionItem, countriesSeries);

        regionItem = new DataSeriesItem("East Asia", 55);

        countriesSeries = new DataSeries("Countries");
        countriesSeries.setId("East Asia Countries");

        countryItem = new DataSeriesItem("Vietnam", 60.4);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Vietnam");
        ys = new Number[] { 75.2, 5.8, 1.4 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Indonesia", 55.5);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Indonesia");
        ys = new Number[] { 69.4, 5.5, 1.1 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Thailand", 53.5);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Thailand");
        ys = new Number[] { 74.1, 6.2, 2.4 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        countryItem = new DataSeriesItem("Philippines", 52.4);
        detailsSeries = new DataSeries("Details");
        detailsSeries.setId("Details Philippines");
        ys = new Number[] { 68.7, 4.9, 1.0 };
        detailsSeries.setData(categories, ys);
        countriesSeries.addItemWithDrilldown(countryItem, detailsSeries);

        regionsSeries.addItemWithDrilldown(regionItem, countriesSeries);

        conf.addSeries(regionsSeries);

        add(chart);








    }

    private Chart build_chart(Integer wert) {

        Chart chart = new Chart();
        final Configuration configuration = chart.getConfiguration();
        configuration.getChart().setType(ChartType.GAUGE);
        configuration.setTitle("Durchsatz");
        configuration.getChart().setWidth(600);

        Pane pane = configuration.getPane();
        pane.setStartAngle(-150);
        pane.setEndAngle(150);

        YAxis yAxis = new YAxis();
        yAxis.setTitle("Nachrichten/h");
        yAxis.setMin(0);
        yAxis.setMax(300);
        yAxis.setTickLength(10);
        yAxis.setTickPixelInterval(30);
        yAxis.setTickPosition(TickPosition.INSIDE);
        yAxis.setMinorTickLength(10);
        yAxis.setMinorTickInterval("auto");
        yAxis.setMinorTickPosition(TickPosition.INSIDE);

        Labels labels = new Labels();
        labels.setStep(2);
        labels.setRotation("auto");
        yAxis.setLabels(labels);

        PlotBand[] bands = new PlotBand[3];
        bands[0] = new PlotBand();
        bands[0].setFrom(0);
        bands[0].setTo(120);
        bands[0].setClassName("band-0");
        bands[1] = new PlotBand();
        bands[1].setFrom(120);
        bands[1].setTo(160);
        bands[1].setClassName("band-1");
        bands[2] = new PlotBand();
        bands[2].setFrom(160);
        bands[2].setTo(200);
        bands[2].setClassName("band-2");
        yAxis.setPlotBands(bands);

        configuration.addyAxis(yAxis);

        series = new ListSeries("Speed", 139);

        PlotOptionsGauge plotOptionsGauge = new PlotOptionsGauge();
        SeriesTooltip tooltip = new SeriesTooltip();
        tooltip.setValueSuffix(" km/h");
        plotOptionsGauge.setTooltip(tooltip);
        series.setPlotOptions(plotOptionsGauge);

        configuration.addSeries(series);

        return chart;
    }

    Timer timer = new Timer();

    void refresh(Integer res){
       // System.out.println(("Refresh wurde aufgerufen: Übergebener Wert" + res));

        Configuration conf = chart1.getConfiguration();
        series = new ListSeries("Speed", res);
        conf.setSeries(series);
        chart1.drawChart();

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        UI ui = attachEvent.getUI();
        String session = VaadinSession.getCurrent().getSession().getId();
        UI cui = UI.getCurrent();

        System.out.println("In onAttache Methode, session=" + session);

       // ui.access(()->currentPrice.setText("huhu"));

        timer.schedule(new TimerTask() {
            @Override
            public void run() {


        bk_service.saveAsync("Huhu").addCallback(result -> {
            ui.access(() -> {

                Configuration configuration = chart1.getConfiguration();
                configuration.setTitle("Durchsatz: " + result);

                series = new ListSeries("Speed", result);
                configuration.setSeries(series);
               // configuration.addSeries(series);
            //    System.out.println("In run Methode " + result);

                currentPrice.setText("Wert:" + result);
                cui.access(() -> refresh(result));
             //   chart1.notify();



            });
        }, err -> {
            ui.access(() -> Notification.show("BOO"));
        });

            }

        }, 0, 50000);

        /*

        timer.schedule(new TimerTask() {
            //@Override
            public void run() {

                ui.access(()->{

                    final Integer z=readvalue();

                    Configuration configuration = chart1.getConfiguration();
                    configuration.setTitle("Durchsatz: " + z.toString());

                    series = new ListSeries("Speed", z);
                    configuration.setSeries(series);

                  try {
                      ui.access(()->chart1.notify());
                  }
                  catch(Exception ex)
                  {
                      System.out.println(ex.getMessage());
                  }
                    System.out.println("In run Methode z= " + z.toString() );


                });



            }
        }, 0, 5000);

        */

    //    series.addData(price)

        // Hook up to service for live updates
    /*    subscription =
                service
                        .getStockPrice(ticker)
                        .subscribe(
                                price -> {
                                    ui.access(
                                            () -> {
                                                currentPrice.setText("$" + price);
                                                series.addData(price);
                                            }
                                    );
                                }
                        );*/
    }

/*    private Integer readvalue() {

        Random random = new Random();
        int i = random.nextInt(300);
        return i;
    }*/

 /*   @Async
    public ListenableFuture<Integer> readvalue() {
        int i=0;
        try {
            Random random = new Random();
            i = random.nextInt(300);
            // pretend to save
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            return AsyncResult.forExecutionException(new RuntimeException("Error"));
        }

        return AsyncResult.forValue(i);
    }*/




    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // Cancel subscription when the view is detached
       // subscription.dispose();
        System.out.println("In onDetache Methode");
        timer.cancel();
        timer.purge();
        super.onDetach(detachEvent);
    }

  /*  public static void runWhileAttached(final Component component,
                                        final Runnable task, final int interval, final int initialPause) {
        // Until reliable push available in our demo servers
        UI.getCurrent().setPollInterval(interval);

        final Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(initialPause);
                    while (component.getUI() != null) {
                        Future<Void> future = component.getUI().access(task);
                        future.get();
                        Thread.sleep(interval);
                    }
                } catch (InterruptedException e) {
                } catch (ExecutionException e) {
                    Logger.getLogger(this.getClass().getName())
                            .log(Level.WARNING,
                                    "Stopping repeating command due to an exception",
                                    e);
                } catch (com.vaadin.ui.UIDetachedException e) {
                } catch (Exception e) {
                    Logger.getLogger(this.getClass().getName())
                            .log(Level.WARNING,
                                    "Unexpected exception while running scheduled update",
                                    e);
                }
                Logger.getLogger(this.getClass().getName()).log(Level.INFO,
                        "Thread stopped");
            }

            ;
        };
        thread.start();
    }*/


}
