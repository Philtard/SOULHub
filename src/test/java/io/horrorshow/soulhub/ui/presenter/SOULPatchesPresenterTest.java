package io.horrorshow.soulhub.ui.presenter;

import io.horrorshow.soulhub.data.AppUser;
import io.horrorshow.soulhub.data.SOULPatch;
import io.horrorshow.soulhub.data.SPFile;
import io.horrorshow.soulhub.service.SOULPatchService;
import io.horrorshow.soulhub.service.UserService;
import io.horrorshow.soulhub.ui.dataproviders.SOULPatchesGridDataProvider;
import io.horrorshow.soulhub.ui.events.SOULPatchDownloadEvent;
import io.horrorshow.soulhub.ui.events.SOULPatchFullTextSearchEvent;
import io.horrorshow.soulhub.ui.events.SPFileDownloadEvent;
import io.horrorshow.soulhub.ui.filters.SOULPatchFilter;
import io.horrorshow.soulhub.ui.views.SOULPatchesView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

class SOULPatchesPresenterTest {
    @Mock
    SOULPatchesGridDataProvider dataProvider;
    @Mock
    UserService userService;
    @Mock
    SOULPatchService soulPatchService;

    SOULPatchesPresenter presenter;
    SOULPatchesView view;

    @BeforeEach
    void init() {
        MockitoAnnotations.initMocks(this);
        presenter = new SOULPatchesPresenter(dataProvider, userService, soulPatchService);
        view = new SOULPatchesView(presenter);
        presenter.init(view);
    }

    @Test
    void onNavigation_no_filter_without_parameter() {
        view.getHeader().addValueChangeListener(event -> fail("no event expected"));

        presenter.onNavigation(null, new HashMap<>());
    }

    @Test
    void onNavigation_filter_by_user() {
        view.getHeader().addValueChangeListener(event -> {
            var appUser = event.getValue().getAppUserFilter();
            assertThat(appUser.size() == 1);
            assertThat(
                    appUser.stream()
                            .map(AppUser::getUserName)
                            .allMatch(
                                    s -> s.equals("username returned by service")));
        });
        var map = Map.of("user", List.of("4711"));
        var userToReturn = new AppUser();
        userToReturn.setUserName("username from service");
        when(userService.findById(4711L)).thenReturn(Optional.of(userToReturn));

        presenter.onNavigation(null, map);
    }

    @Test
    void onNavigation_filter_by_multiple_users() {
        view.getHeader().addValueChangeListener(event -> {
            var appUser = event.getValue().getAppUserFilter();
            assertThat(appUser.size() == 3);
            var usernames =
                    appUser
                            .stream()
                            .map(AppUser::getUserName)
                            .collect(Collectors.toSet());
            assertThat(usernames.contains("aaa"));
            assertThat(usernames.contains("bbb"));
            assertThat(usernames.contains("ccc"));
        });
        var map = Map.of("user", List.of("1", "2", "3"));
        var aaa = new AppUser();
        aaa.setUserName("aaa");
        var bbb = new AppUser();
        bbb.setUserName("bbb");
        var ccc = new AppUser();
        ccc.setUserName("ccc");
        when(userService.findById(1L)).thenReturn(Optional.of(aaa));
        when(userService.findById(2L)).thenReturn(Optional.of(bbb));
        when(userService.findById(3L)).thenReturn(Optional.of(ccc));

        presenter.onNavigation(null, map);
    }

    @Test
    void onNavigation_no_filter_on_illegal_user_param() {
        view.getHeader().addValueChangeListener(
                event -> fail("no event expected"));
        var map = Map.of("user", List.of("invalid_id"));
        verify(userService, never()).findById(any());
        presenter.onNavigation(null, map);
    }

    @Test
    void on_full_text_search_event_create_search_filter_for_dataprovider() {
        String SEARCH_STRING = "find-me";
        var event = new SOULPatchFullTextSearchEvent(view, SEARCH_STRING);
        presenter.onFullTextSearch(event);

        var fetchFilterArgumentCaptor
                = ArgumentCaptor.forClass(SOULPatchService.SOULPatchesFetchFilter.class);

        verify(dataProvider).setFilter(fetchFilterArgumentCaptor.capture());

        var resultFetchFilter
                = fetchFilterArgumentCaptor.getValue();
        assertThat(resultFetchFilter.getFullTextSearch().isPresent());
        assertThat(resultFetchFilter.getFullTextSearch().get().equals(SEARCH_STRING));
    }

    @Test
    void on_grid_header_change_set_filters() {
        var filter = new SOULPatchFilter();
        filter.setNamesFilter("namesfilter");
        var u1 = new AppUser();
        var u2 = new AppUser();
        u1.setUserName("user 1");
        u2.setUserName("user 2");
        filter.setOnlyCurUser(true);
        filter.setAppUserFilter(Set.of(u1, u2));

        var cu = new AppUser();
        cu.setUserName("cur user");
        when(userService.getCurrentAppUser()).thenReturn(Optional.of(cu));

        var captor
                = ArgumentCaptor.forClass(SOULPatchService.SOULPatchesFetchFilter.class);

        presenter.onSOULPatchesHeaderValueChanged(filter);

        verify(dataProvider).setFilter(captor.capture());

        var res = captor.getValue();
        assertThat(res.getNamesFilter().isPresent());
        assertThat(res.getNamesFilter().get().equals("namesfilter"));
        assertThat(res.getUsersFilter().containsAll(Set.of(cu, u1, u2)));
    }

    @Test
    void increment_download_counter_on_soulpatch_download() {
        var soulPatch = new SOULPatch();
        soulPatch.setId(4711L);
        var event = new SOULPatchDownloadEvent(view.getSoulPatchReadOnlyDialog(), soulPatch);

        presenter.onSOULPatchDownload(event);

        verify(soulPatchService).incrementNoDownloadsAndSave(soulPatch);
        verify(dataProvider).refreshItem(soulPatch);
    }

    @Test
    void increment_download_counter_on_spfile_download() {
        var spFile = new SPFile();
        var soulPatch = new SOULPatch();
        soulPatch.setId(4711L);
        spFile.setSoulPatch(soulPatch);
        var event = new SPFileDownloadEvent(view.getSpFileReadOnlyDialog(), spFile);

        presenter.onSPFileDownload(event);

        verify(soulPatchService).incrementNoDownloadsAndSave(soulPatch);
        verify(dataProvider).refreshItem(soulPatch);
    }
}