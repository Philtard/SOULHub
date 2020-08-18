package io.horrorshow.soulhub.ui.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import io.horrorshow.soulhub.security.SecurityUtils;
import io.horrorshow.soulhub.ui.events.SOULPatchFullTextSearchEvent;
import io.horrorshow.soulhub.ui.events.SOULPatchesFilterEvent;
import io.horrorshow.soulhub.ui.filters.SOULPatchFilter;

public class SOULPatchesGridHeader extends Div {

    private static final long serialVersionUID = 8863450661909139044L;

    private final TextField namesFilter = new TextField("names filter");
    private final Checkbox showOnlyCurUser = new Checkbox("show only my soulpatches");

    private final TextField fullTextSearch = new TextField("search through file contents and descriptions");
    private final Button fullTextBtn = new Button("Find...");

    public SOULPatchesGridHeader() {
        setupNamesFilter();

        setupShowOnlyCurUserFilter();

        setupFullTextSearch();

        arrangeComponents();
    }

    private void setupFullTextSearch() {
        fullTextBtn.addClickListener(this::fullTextSearchBtnClicked);
    }

    private void fullTextSearchBtnClicked(ClickEvent<Button> event) {
        fireEvent(new SOULPatchFullTextSearchEvent(this, fullTextSearch.getValue()));
    }

    private void setupNamesFilter() {
        namesFilter.addValueChangeListener(e -> soulpatchFilterChanged());
        namesFilter.setPlaceholder("Filter SOULPatch names...");
        namesFilter.setClearButtonVisible(true);
        namesFilter.setValueChangeMode(ValueChangeMode.EAGER);
    }

    private void setupShowOnlyCurUserFilter() {
        showOnlyCurUser.setValue(false);
        var b = SecurityUtils.isUserLoggedIn();
        showOnlyCurUser.addValueChangeListener(e -> soulpatchFilterChanged());
        showOnlyCurUser.setReadOnly(!b);
        showOnlyCurUser.setVisible(b);
    }

    private void soulpatchFilterChanged() {
        SOULPatchFilter filter = SOULPatchFilter.getEmptyFilter();
        filter.setNamesFilter(namesFilter.getValue());
        filter.setOnlyCurUser(showOnlyCurUser.getValue());
        fireEvent(new SOULPatchesFilterEvent(this, filter));
    }

    private void arrangeComponents() {
        VerticalLayout filters =
                new VerticalLayout(namesFilter, showOnlyCurUser);
        VerticalLayout fullText =
                new VerticalLayout(fullTextSearch, fullTextBtn);
        HorizontalLayout layout = new HorizontalLayout();
        layout.setSpacing(false);
        layout.add(filters, fullText);
        add(layout);
    }

    public Registration addSOULPatchesFilterListener(
            ComponentEventListener<SOULPatchesFilterEvent> listener) {
        return addListener(SOULPatchesFilterEvent.class, listener);
    }

    public Registration addFullTextSearchListener(
            ComponentEventListener<SOULPatchFullTextSearchEvent> listener) {
        return addListener(SOULPatchFullTextSearchEvent.class, listener);
    }
}
