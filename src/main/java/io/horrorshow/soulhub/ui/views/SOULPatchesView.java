package io.horrorshow.soulhub.ui.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.*;
import io.horrorshow.soulhub.ui.MainLayout;
import io.horrorshow.soulhub.ui.UIConst;
import io.horrorshow.soulhub.ui.components.SOULPatchReadOnlyDialog;
import io.horrorshow.soulhub.ui.components.SOULPatchesGrid;
import io.horrorshow.soulhub.ui.components.SOULPatchesGridHeader;
import io.horrorshow.soulhub.ui.components.SPFileReadOnlyDialog;
import io.horrorshow.soulhub.ui.presenter.SOULPatchesPresenter;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = UIConst.ROUTE_SOULPATCHES, layout = MainLayout.class)
@RouteAlias(value = UIConst.ROUTE_EMPTY, layout = MainLayout.class)
@PageTitle(UIConst.TITLE_SOULPATCHES)
public class SOULPatchesView extends Div
        implements HasUrlParameter<String> {

    private static final long serialVersionUID = 6587633236690463135L;

    private final SOULPatchesGrid grid = new SOULPatchesGrid();
    private final SOULPatchesGridHeader filter = new SOULPatchesGridHeader();
    private final SPFileReadOnlyDialog spFileReadOnlyDialog = new SPFileReadOnlyDialog();
    private final SOULPatchReadOnlyDialog soulPatchReadOnlyDialog = new SOULPatchReadOnlyDialog();

    private final SOULPatchesPresenter soulPatchesPresenter;

    public SOULPatchesView(@Autowired SOULPatchesPresenter soulPatchesPresenter) {
        this.soulPatchesPresenter = soulPatchesPresenter;
        soulPatchesPresenter.init(this);

        layoutComponents();
    }

    public SOULPatchesGrid getGrid() {
        return this.grid;
    }

    public SOULPatchesGridHeader getHeader() {
        return this.filter;
    }

    public SPFileReadOnlyDialog getSpFileReadOnlyDialog() {
        return spFileReadOnlyDialog;
    }

    public SOULPatchReadOnlyDialog getSoulPatchReadOnlyDialog() {
        return soulPatchReadOnlyDialog;
    }

    private void layoutComponents() {
        grid.setHeightByRows(true);
        filter.setWidthFull();

        VerticalLayout layout = new VerticalLayout();
        layout.add(filter);
        layout.add(grid);
        add(layout);
    }

    @Override
    public void setParameter(BeforeEvent event, @OptionalParameter String parameter) {
        var parameterMap = event.getLocation().getQueryParameters().getParameters();
        soulPatchesPresenter.onNavigation(parameter, parameterMap);
    }
}
