package com.example.application.views;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@PageTitle("FVM-Admin Tool")
@Route(value = "", layout= MainLayout.class)
@AnonymousAllowed
public class InfoView extends VerticalLayout {

   // @Value("${ldap.urls}")
    //private String ldapUrls;

    public InfoView(){



        add(new H2("Hallo und willkommen"));

        String yourContent_notloggedin ="Auf dieser Seite werden Tools und Hilfsmittel bereitgestellt, um FVM-Tätigkeiten zu vereinfachen.<br />" +
                "Ebenso wird den Justizen ermöglicht, Informationen direkt aus der <b>eKP</b> bzw. <b>EGVP-E</b> zu entnehmen.<br />" +
                "Die sich aktuell noch in Planung befindlichen Funktionen sind mit <i>geplant</i> gekennzeichnet.<br />" +
                "Ideen, Anregungen oder Verbesserungsvorschläge sind herzlich willkommen!&#128512;<br /><br />" +
                "Bitte als erstes einloggen!<br /><br />" +
                "Viele Grüße<br /><b>Euer Dataport FVM-Team</b>" ;

        String yourContent_loggedin ="Auf dieser Seite werden Tools und Hilfsmittel bereitgestellt, um FVM-Tätigkeiten zu vereinfachen.<br />" +
                "Ebenso wird den Justizen ermöglicht, Informationen direkt aus der <b>eKP</b> bzw. <b>EGVP-E</b> zu entnehmen.<br />" +
                "Die sich aktuell noch in Planung befindlichen Funktionen sind mit <i>geplant</i> gekennzeichnet.<br />" +
                "Ideen, Anregungen oder Verbesserungsvorschläge sind herzlich willkommen!&#128512;<br /><br />" +
                "Bitte im linken Auswahlmenü die gewünschte Funktionalität auswählen.<br /><br />" +
                "Viele Grüße<br /><b>Euer Dataport FVM-Team</b>" ;

        Html html_notLoggedin = new Html("<text>" + yourContent_notloggedin + "</text>");
        Html html_Loggedin = new Html("<text>" + yourContent_loggedin + "</text>");

        add(html_notLoggedin,html_Loggedin);


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserName = authentication.getName();

        if (currentUserName=="anonymousUser")
        {
            html_Loggedin.setVisible(false);
            html_notLoggedin.setVisible(true);

        }
        else
        {
            html_Loggedin.setVisible(true);
            html_notLoggedin.setVisible(false);


        }



    }


}
