package com.example.application.views;

import com.example.application.data.entity.AnwParameter;
import com.example.application.data.entity.Metadaten;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.annotation.security.RolesAllowed;
import java.util.ArrayList;
import java.util.List;

@PageTitle("eKP-Cokpit | by DBUSS GmbH")
@Route(value = "cockpit", layout= MainLayout.class)
@RolesAllowed("ADMIN")
public class CockpitView extends VerticalLayout {
    @Autowired
    JdbcTemplate jdbcTemplate;

    Grid<AnwParameter> grid = new Grid<>(AnwParameter.class, false);


    public CockpitView(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;

        AnwParameter param = new AnwParameter();
        AnwParameter param1 = new AnwParameter();
        List<AnwParameter> param_Liste = new ArrayList<AnwParameter>();

        param.setParameter_Name("Cursor");
        param.setParameter_Wert(123);
        param.setParameter_MaxAlowed(200);

        param_Liste.add(param);

        param1.setParameter_Name("CountMessages");
        param1.setParameter_Wert(34);
        param1.setParameter_MaxAlowed(10);

        param_Liste.add(param1);

        H1 h1 = new H1("eKP Cockpit");
        add(h1);


        grid.addColumn(AnwParameter::getParameter_Name).setHeader("Parameter-Name")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        grid.addColumn(AnwParameter::getParameter_Wert).setHeader("Parameter-Wert")
                .setAutoWidth(true).setResizable(true).setSortable(true);
        grid.addColumn(AnwParameter::getParameter_MaxAlowed).setHeader("Parameter-Maximal erlaubt")
                .setAutoWidth(true).setResizable(true).setSortable(true);

        grid.setItems(param_Liste);

        add(grid);
    }
}
