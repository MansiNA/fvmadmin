package com.example.application.views;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.router.Route;

@Route(value = "tables", layout= MainLayout.class)
public class MenuBarBasic extends HorizontalLayout {

    public MenuBarBasic(){
        History history = UI.getCurrent().getPage().getHistory();
    MenuBar menuBar = new MenuBar();
    Text selected = new Text("");
    ComponentEventListener<ClickEvent<MenuItem>> listener = e -> selected
            .setText(e.getSource().getText());
    Div message = new Div(new Text("Clicked item: "), selected);


    MenuItem move = menuBar.addItem("Tab Viewer");
    SubMenu moveSubMenu = move.getSubMenu();
moveSubMenu.addItem("Metadaten", listener);
moveSubMenu.addItem("ERV-Mapping", listener);

this.add(menuBar);
//this.add(new RouterLink("zurück", MainLayout.class ));
//        this.add(new RouterLink("zurück",);




}

}