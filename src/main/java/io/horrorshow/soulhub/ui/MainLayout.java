package io.horrorshow.soulhub.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HighlightConditions;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import io.horrorshow.soulhub.security.SecurityUtils;
import io.horrorshow.soulhub.service.UserService;
import io.horrorshow.soulhub.ui.components.AppUserNavBarComponent;
import io.horrorshow.soulhub.ui.components.LoginNavBarComponent;
import io.horrorshow.soulhub.ui.views.AboutView;
import io.horrorshow.soulhub.ui.views.AdminView;
import io.horrorshow.soulhub.ui.views.SOULPatchesView;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.googleanalytics.tracking.EnableGoogleAnalytics;

@PWA(name = UIConst.TITLE,
        shortName = UIConst.TITLE,
        description = UIConst.DESCRIPTION,
        enableInstallPrompt = false)
@JsModule("@vaadin/vaadin-lumo-styles/presets/compact.js")
@CssImport("./styles/shared-styles.css")
@CssImport(value = "./styles/vaadin-text-field-styles.css",
        themeFor = "vaadin-text-field")
@Theme(value = Lumo.class, variant = Lumo.DARK)
@EnableGoogleAnalytics(value = "UA-177188327-1")
public class MainLayout extends AppLayout {

    private static final long serialVersionUID = -4791247729805265577L;

    private final UserService userService;

    public MainLayout(@Autowired UserService userService) {
        this.userService = userService;

        addToNavbar(createNavbar());
        addToDrawer(createDrawer());
    }

    private Component createNavbar() {
        Image logo = new Image("img/Logo.svg", "SOULHub logo");
        logo.addClickListener(event -> UI.getCurrent().navigate(SOULPatchesView.class));

        logo.addClassName("logo");

        DrawerToggle drawerToggle = new DrawerToggle();

        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("header");
        header.setWidth("100%");
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);


        header.add(drawerToggle, logo);
        Div navbarCenterSpace = new Div();
        header.add(navbarCenterSpace);
        header.expand(navbarCenterSpace);

        HorizontalLayout appUserNavBar;
        if (SecurityUtils.isUserLoggedIn() && userService.getCurrentAppUser().isPresent()) {
            appUserNavBar = new AppUserNavBarComponent(userService.getCurrentAppUser().get());
        } else {
            appUserNavBar = new LoginNavBarComponent();
        }
        header.add(appUserNavBar);

        return header;
    }

    private Component createDrawer() {
        VerticalLayout drawerLayout = new VerticalLayout();

        RouterLink soulPatchesLink =
                new RouterLink(UIConst.LINK_TEXT_SOULPATCHES, SOULPatchesView.class);
        soulPatchesLink.addComponentAsFirst(VaadinIcon.CLUSTER.create());
        soulPatchesLink.setHighlightCondition(HighlightConditions.sameLocation());
        drawerLayout.add(soulPatchesLink);

        RouterLink aboutLink =
                new RouterLink(UIConst.LINK_TEXT_ABOUT, AboutView.class);
        aboutLink.setHighlightCondition(HighlightConditions.sameLocation());
        drawerLayout.add(aboutLink);

        if (SecurityUtils.isAccessGranted(AdminView.class)) {
            RouterLink adminLink =
                    new RouterLink(UIConst.LINK_TEXT_ADMIN, AdminView.class);
            adminLink.setHighlightCondition(HighlightConditions.sameLocation());
            drawerLayout.add(adminLink);
        }

        return drawerLayout;
    }
}
