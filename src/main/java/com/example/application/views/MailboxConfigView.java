package com.example.application.views;

import com.example.application.data.entity.Person;
import com.example.application.data.service.DataService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.NativeButtonRenderer;
import com.vaadin.flow.data.renderer.TemplateRenderer;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.List;

@PageTitle("Mailbox Verwaltung")
@Route(value = "mailbox-config", layout= MainLayout.class)
public class MailboxConfigView  extends Div {

    Grid<Person> grid = new Grid<>(Person.class, false);

    public MailboxConfigView()  {

        add(new H1("Postfach Verwaltung eKP HH QS"));



       // grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addColumn(createEmployeeTemplateRenderer()).setHeader("Postfach")
                .setAutoWidth(true).setFlexGrow(0);
//        grid.addColumn(Person::getProfession).setHeader("Profession")
//                .setAutoWidth(true);
        grid.addColumn(createStatusComponentRenderer()).setHeader("Status")
                .setAutoWidth(true);

        grid.addColumn(
                new NativeButtonRenderer<>("Enable/Disable",
                        clickedItem -> {
                            //System.out.println(clickedItem.getLastName());
                            Notification.show("Postfach " + clickedItem.getLastName() + " wird geändert..." );

                            clickedItem.setIsActive(false);
                            clickedItem.setLastName("Huhu");

                            updateList();
                        })
        );

      //  updateList();
        List<Person> people = DataService.getPeople();
        grid.setItems(people);
        add(grid);

    }

    private void updateList() {

        List<Person> people = DataService.getPeople();
        people.get(1).setLastName("hhh");
        grid.setItems(people);

    }

    private static TemplateRenderer<Person> createEmployeeTemplateRenderer() {
        return TemplateRenderer.<Person>of(
                        "<vaadin-horizontal-layout style=\"align-items: center;\" theme=\"spacing\">"
                                + "<vaadin-avatar img=\"[[item.pictureUrl]]\" name=\"[[item.fullName]]\" alt=\"User avatar\"></vaadin-avatar>"
                                + "  <vaadin-vertical-layout style=\"line-height: var(--lumo-line-height-m);\">"
                                + "    <span> [[item.fullName]] </span>"
                                + "    <span style=\"font-size: var(--lumo-font-size-s); color: var(--lumo-secondary-text-color);\">"
                                + "      [[item.email]]" + "    </span>"
                                + "  </vaadin-vertical-layout>"
                                + "</vaadin-horizontal-layout>")
                .withProperty("pictureUrl", Person::getPictureUrl)
                .withProperty("fullName", Person::getFullName)
                .withProperty("email", Person::getEmail);
    }

    private static final SerializableBiConsumer<Span, Person> statusComponentUpdater = (span, person) -> {
        boolean isAvailable = "Online".equals(person.getStatus());
        String theme = String
                .format("badge %s", isAvailable ? "success" : "error");
        span.getElement().setAttribute("theme", theme);
        span.setText(person.getStatus());
    };

    private static ComponentRenderer<Span, Person> createStatusComponentRenderer() {
        return new ComponentRenderer<>(Span::new, statusComponentUpdater);
    }

}
