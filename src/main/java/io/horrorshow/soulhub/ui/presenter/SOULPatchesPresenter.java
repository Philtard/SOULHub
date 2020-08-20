package io.horrorshow.soulhub.ui.presenter;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.spring.annotation.SpringComponent;
import io.horrorshow.soulhub.data.AppUser;
import io.horrorshow.soulhub.data.SOULPatch;
import io.horrorshow.soulhub.data.SOULPatchRating;
import io.horrorshow.soulhub.security.SecurityUtils;
import io.horrorshow.soulhub.service.SOULPatchService;
import io.horrorshow.soulhub.service.UserService;
import io.horrorshow.soulhub.ui.UIConst;
import io.horrorshow.soulhub.ui.components.SOULPatchReadOnly;
import io.horrorshow.soulhub.ui.components.SOULPatchesGridHeader;
import io.horrorshow.soulhub.ui.dataproviders.SOULPatchesGridDataProvider;
import io.horrorshow.soulhub.ui.events.*;
import io.horrorshow.soulhub.ui.filters.SOULPatchFilter;
import io.horrorshow.soulhub.ui.views.SOULPatchesView;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;

@SpringComponent
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Log4j2
public class SOULPatchesPresenter {

    private final SOULPatchesGridDataProvider dataProvider;
    private final UserService userService;
    private final SOULPatchService soulPatchService;
    private SOULPatchesView view;

    public SOULPatchesPresenter(@Autowired SOULPatchesGridDataProvider dataProvider,
                                @Autowired UserService userService,
                                @Autowired SOULPatchService soulPatchService) {
        this.dataProvider = dataProvider;
        this.userService = userService;
        this.soulPatchService = soulPatchService;

        dataProvider.setPageObserver(this::observePage);
    }

    private void observePage(Page<SOULPatch> soulPatchPage) {
        log.debug("page observer soulpatches: {}, pages: {}",
                soulPatchPage.getTotalElements(),
                soulPatchPage.getTotalPages());
    }

    public void init(SOULPatchesView view) {
        this.view = view;
        view.getGrid().setDataProvider(dataProvider);
        view.getGrid().addSPFileSelectListener(this::spFileSelected);
        view.getGrid().addSOULPatchRatingsListener(this::soulpatchRating);
        view.getGrid().asSingleSelect().addValueChangeListener(this::soulPatchesGridSelection);
        view.getHeader().addFullTextSearchListener(this::fullTextSearchEvent);
        view.getHeader().addValueChangeListener(this::soulpatchesHeaderChanged);


        view.getSpFileReadOnlyDialog()
                .getSpFileReadOnly()
                .addSPFileDownloadListener(this::spFileDownloaded);

        view.getSoulPatchReadOnlyDialog().addValueChangeListener(this::soulPatchReadOnlyChanged);
        view.getSoulPatchReadOnlyDialog()
                .getSoulPatchReadOnly()
                .setSOULPatchZipInputStreamProvider(soulPatchService::getZipSOULPatchStreamProvider);

        view.getSoulPatchReadOnlyDialog()
                .getSoulPatchReadOnly()
                .addSOULPatchDownloadListener(this::soulPatchDownloaded);
    }

    private void soulPatchReadOnlyChanged(
            AbstractField.ComponentValueChangeEvent
                    <SOULPatchReadOnly, SOULPatch> event) {
        view.getSoulPatchReadOnlyDialog()
                .getEditSOULPatchBtn()
                .setVisible(userService.isCurrentUserOwnerOf(event.getValue()));
    }

    private void soulPatchDownloaded(SOULPatchDownloadEvent event) {
        soulPatchService.incrementNoDownloadsAndSave(event.getSoulPatch());
        dataProvider.refreshItem(event.getSoulPatch());
    }

    private void spFileDownloaded(SPFileDownloadEvent event) {
        soulPatchService.incrementNoDownloadsAndSave(event.getSpFile().getSoulPatch());
        dataProvider.refreshItem(event.getSpFile().getSoulPatch());
    }

    private void soulpatchesHeaderChanged(
            AbstractField.ComponentValueChangeEvent<SOULPatchesGridHeader, SOULPatchFilter> event) {
        log.debug("soulpatch header value changed: {}", event.getValue());
        var filter = new SOULPatchService.SOULPatchesFetchFilter();
        filter.setNamesFilter(event.getValue().getNamesFilter());
        if (event.getValue().isOnlyCurUser()) {
            userService.getCurrentAppUser().ifPresent(appUser ->
                    filter.getUsersFilter().add(appUser));
        }
        dataProvider.setFilter(filter);
    }

    private void soulPatchesGridSelection(
            AbstractField.ComponentValueChangeEvent<Grid<SOULPatch>, SOULPatch> event) {
        view.getGrid().asSingleSelect().getOptionalValue()
                .ifPresentOrElse(
                        soulPatch -> view.getSoulPatchReadOnlyDialog().open(soulPatch),
                        () -> view.getSoulPatchReadOnlyDialog().close());
    }

    private void fullTextSearchEvent(SOULPatchFullTextSearchEvent event) {
        log.debug("full text search event: {}", event.getValue());
    }

    private void soulpatchRating(SOULPatchRatingEvent event) {
        log.debug("soulpatch rating {}", event);
        if (SecurityUtils.isUserLoggedIn() && userService.getCurrentAppUser().isPresent()) {
            AppUser currentUser = userService.getCurrentAppUser().get();
            event.getSoulPatch().getRatings().stream()
                    .filter(rating -> rating.getAppUser()
                            .equals(currentUser)).distinct().findAny()
                    .ifPresentOrElse(
                            rating -> {
                                // rating present
                                rating.setStars(event.getValue());
                                soulPatchService.save(event.getSoulPatch());
                                log.debug("rating by {} exists {}",
                                        currentUser.getEmail(),
                                        rating.toString());
                            },
                            () -> {
                                // no rating present
                                SOULPatchRating rating = new SOULPatchRating();
                                rating.setAppUser(currentUser);
                                rating.setSoulPatch(event.getSoulPatch());
                                rating.setStars(event.getValue());
                                event.getSoulPatch().getRatings().add(rating);
                                soulPatchService.save(event.getSoulPatch());
                                log.debug("no rating by {} existed, created {}",
                                        currentUser.getEmail(),
                                        rating.toString());
                            });
        }

        dataProvider.refreshItem(event.getSoulPatch());
    }

    private void spFileSelected(SPFileSelectEvent event) {
        log.debug("sp file selected {}", event);
        view.getSpFileReadOnlyDialog().open(event.getSpFile());
    }

    public void onNavigation(String parameter, Map<String, List<String>> parameterMap) {

        if (parameterMap.containsKey(UIConst.PARAM_SHOW_BY_CURRENT_USER)
                && parameterMap.get(UIConst.PARAM_SHOW_BY_CURRENT_USER).stream()
                .anyMatch(Boolean.TRUE.toString()::equalsIgnoreCase)) {

            view.getHeader().setValue(SOULPatchFilter.getOnlyCurrentUser());
        }
        log.debug("on navigation with parameter: {} and parameterMap {}",
                parameter, parameterMap.toString());
    }
}