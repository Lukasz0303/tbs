package com.tbs.controller;

import com.tbs.dto.ranking.RankingAroundItem;
import com.tbs.dto.ranking.RankingAroundResponse;
import com.tbs.dto.ranking.RankingDetailResponse;
import com.tbs.dto.ranking.RankingItem;
import com.tbs.dto.ranking.RankingListResponse;
import com.tbs.exception.UserNotFoundException;
import com.tbs.service.RankingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingControllerTest {

    @Mock
    private RankingService rankingService;

    @InjectMocks
    private RankingController rankingController;

    private RankingListResponse rankingListResponse;
    private RankingDetailResponse rankingDetailResponse;
    private RankingAroundResponse rankingAroundResponse;

    @BeforeEach
    void setUp() {
        List<RankingItem> items = new ArrayList<>();
        items.add(new RankingItem(1L, 100L, "user1", 1000L, 10, 5, Instant.now()));
        items.add(new RankingItem(2L, 101L, "user2", 900L, 8, 4, Instant.now()));

        rankingListResponse = new RankingListResponse(
                items, 100L, 2, 50, 0, true, false
        );

        rankingDetailResponse = new RankingDetailResponse(
                10L, 1L, "testuser", 5000L, 50, 25, Instant.now()
        );

        List<RankingAroundItem> aroundItems = new ArrayList<>();
        aroundItems.add(new RankingAroundItem(8L, 200L, "user8", 800L, 8, 3));
        aroundItems.add(new RankingAroundItem(9L, 201L, "user9", 750L, 7, 2));
        aroundItems.add(new RankingAroundItem(10L, 1L, "testuser", 5000L, 50, 25));
        aroundItems.add(new RankingAroundItem(11L, 202L, "user11", 700L, 6, 1));
        aroundItems.add(new RankingAroundItem(12L, 203L, "user12", 650L, 5, 1));

        rankingAroundResponse = new RankingAroundResponse(aroundItems);
    }

    @Test
    void getRankings_shouldReturnRankingListWithStandardPagination() {
        Pageable pageable = PageRequest.of(0, 50, Sort.by("rankPosition").ascending());

        when(rankingService.getRankings(any(Pageable.class), any())).thenReturn(rankingListResponse);

        ResponseEntity<RankingListResponse> response = rankingController.getRankings(
                pageable, null, null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(2);
        assertThat(response.getBody().totalElements()).isEqualTo(100L);
        verify(rankingService, times(1)).getRankings(any(Pageable.class), any());
    }

    @Test
    void getRankings_shouldReturnRankingListWithStartRank() {
        Pageable pageable = PageRequest.of(0, 50);
        Integer startRank = 10;

        when(rankingService.getRankings(any(Pageable.class), eq(startRank))).thenReturn(rankingListResponse);

        ResponseEntity<RankingListResponse> response = rankingController.getRankings(
                pageable, startRank, null
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        verify(rankingService, times(1)).getRankings(any(Pageable.class), eq(startRank));
    }

    @Test
    void getRankings_shouldAdjustPageSizeWhenSizeProvided() {
        Pageable pageable = PageRequest.of(0, 50);
        Integer size = 25;

        when(rankingService.getRankings(any(Pageable.class), any())).thenReturn(rankingListResponse);

        ResponseEntity<RankingListResponse> response = rankingController.getRankings(
                pageable, null, size
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(rankingService, times(1)).getRankings(any(Pageable.class), any());
    }

    @Test
    void getRankings_shouldThrowExceptionWhenSizeExceedsMax() {
        Pageable pageable = PageRequest.of(0, 50);
        Integer size = 150;

        assertThatThrownBy(() -> rankingController.getRankings(pageable, null, size))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Size must not exceed 100");

        verify(rankingService, never()).getRankings(any(Pageable.class), any());
    }

    @Test
    void getUserRanking_shouldReturnRankingDetails() {
        Long userId = 1L;

        when(rankingService.getUserRanking(userId)).thenReturn(rankingDetailResponse);

        ResponseEntity<RankingDetailResponse> response = rankingController.getUserRanking(userId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().userId()).isEqualTo(1L);
        assertThat(response.getBody().username()).isEqualTo("testuser");
        assertThat(response.getBody().rankPosition()).isEqualTo(10L);
        verify(rankingService, times(1)).getUserRanking(userId);
    }

    @Test
    void getUserRanking_shouldThrowExceptionWhenUserNotFound() {
        Long userId = 999L;

        when(rankingService.getUserRanking(userId))
                .thenThrow(new UserNotFoundException("User not found with id: " + userId));

        assertThatThrownBy(() -> rankingController.getUserRanking(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: " + userId);

        verify(rankingService, times(1)).getUserRanking(userId);
    }

    @Test
    void getUserRanking_shouldThrowExceptionWhenUserIsGuest() {
        Long userId = 2L;

        when(rankingService.getUserRanking(userId))
                .thenThrow(new UserNotFoundException("Guest users are not included in rankings"));

        assertThatThrownBy(() -> rankingController.getUserRanking(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Guest users are not included in rankings");

        verify(rankingService, times(1)).getUserRanking(userId);
    }

    @Test
    void getRankingsAround_shouldReturnRankingsAroundUser() {
        Long userId = 1L;
        Integer range = 5;

        when(rankingService.getRankingsAround(userId, range)).thenReturn(rankingAroundResponse);

        ResponseEntity<RankingAroundResponse> response = rankingController.getRankingsAround(userId, range);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().items()).isNotEmpty();
        boolean userFound = response.getBody().items().stream()
                .anyMatch(item -> item.userId() == userId);
        assertThat(userFound).isTrue();
        verify(rankingService, times(1)).getRankingsAround(userId, range);
    }

    @Test
    void getRankingsAround_shouldUseDefaultRangeWhenNotProvided() {
        Long userId = 1L;

        when(rankingService.getRankingsAround(userId, 5)).thenReturn(rankingAroundResponse);

        ResponseEntity<RankingAroundResponse> response = rankingController.getRankingsAround(userId, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(rankingService, times(1)).getRankingsAround(userId, 5);
    }

    @Test
    void getRankingsAround_shouldThrowExceptionWhenUserNotFound() {
        Long userId = 999L;
        Integer range = 5;

        when(rankingService.getRankingsAround(userId, range))
                .thenThrow(new UserNotFoundException("User not found with id: " + userId));

        assertThatThrownBy(() -> rankingController.getRankingsAround(userId, range))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found with id: " + userId);

        verify(rankingService, times(1)).getRankingsAround(userId, range);
    }

    @Test
    void getRankingsAround_shouldThrowExceptionWhenUserIsGuest() {
        Long userId = 2L;
        Integer range = 5;

        when(rankingService.getRankingsAround(userId, range))
                .thenThrow(new UserNotFoundException("Guest users are not included in rankings"));

        assertThatThrownBy(() -> rankingController.getRankingsAround(userId, range))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("Guest users are not included in rankings");

        verify(rankingService, times(1)).getRankingsAround(userId, range);
    }
}
