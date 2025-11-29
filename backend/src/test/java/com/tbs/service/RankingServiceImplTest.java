package com.tbs.service;

import com.tbs.dto.ranking.RankingAroundResponse;
import com.tbs.dto.ranking.RankingDetailResponse;
import com.tbs.dto.ranking.RankingListResponse;
import com.tbs.model.User;
import com.tbs.repository.RankingRepository;
import com.tbs.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingServiceImplTest {

    @Mock
    private RankingRepository rankingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RankingServiceImpl rankingService;

    private User registeredUser;
    private User guestUser;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 50);
        
        registeredUser = new User();
        registeredUser.setId(1L);
        registeredUser.setUsername("testuser");
        registeredUser.setIsGuest(false);
        
        guestUser = new User();
        guestUser.setId(2L);
        guestUser.setIsGuest(true);
    }

    @Test
    void getRankings_shouldReturnRankingListWhenUsingStandardPagination() {
        List<Object[]> mockResults = createMockRankingResults(5);
        
        when(rankingRepository.countAllExcludingBot()).thenReturn(100L);
        when(rankingRepository.findRankingsRaw(anyInt(), anyInt())).thenReturn(mockResults);

        RankingListResponse response = rankingService.getRankings(pageable, null);

        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(5);
        assertThat(response.totalElements()).isEqualTo(100L);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.first()).isTrue();
        verify(rankingRepository, times(1)).findRankingsRaw(0, 50);
        verify(rankingRepository, times(1)).countAllExcludingBot();
    }

    @Test
    void getRankings_shouldReturnRankingListWhenUsingStartRank() {
        List<Object[]> mockResults = createMockRankingResults(10);
        Integer startRank = 10;
        
        when(rankingRepository.countAllExcludingBot()).thenReturn(100L);
        when(rankingRepository.findRankingsFromPositionRaw(startRank, 50)).thenReturn(mockResults);

        RankingListResponse response = rankingService.getRankings(pageable, startRank);

        assertThat(response).isNotNull();
        assertThat(response.content()).hasSize(10);
        verify(rankingRepository, times(1)).findRankingsFromPositionRaw(startRank, 50);
        verify(rankingRepository, never()).findRankingsRaw(anyInt(), anyInt());
    }

    @Test
    void getUserRanking_shouldReturnRankingDetailsForRegisteredUser() {
        Long userId = 1L;
        List<Object[]> mockResults = createMockRankingDetailResult(userId);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));
        when(rankingRepository.findByUserIdRaw(userId)).thenReturn(mockResults);

        RankingDetailResponse response = rankingService.getUserRanking(userId);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.username()).isEqualTo("testuser");
        verify(userRepository, times(1)).findById(userId);
        verify(rankingRepository, times(1)).findByUserIdRaw(userId);
    }

    @Test
    void getUserRanking_shouldThrowExceptionForGuestUser() {
        Long userId = 2L;
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(guestUser));

        assertThatThrownBy(() -> rankingService.getUserRanking(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Guest users are not included in rankings");
        
        verify(userRepository, times(1)).findById(userId);
        verify(rankingRepository, never()).findByUserIdRaw(anyLong());
    }

    @Test
    void getUserRanking_shouldThrowExceptionWhenUserNotFound() {
        Long userId = 999L;
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rankingService.getUserRanking(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not found with id: " + userId);
        
        verify(userRepository, times(1)).findById(userId);
        verify(rankingRepository, never()).findByUserIdRaw(anyLong());
    }

    @Test
    void getUserRanking_shouldThrowExceptionWhenRankingNotFound() {
        Long userId = 1L;
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));
        when(rankingRepository.findByUserIdRaw(userId)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> rankingService.getUserRanking(userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ranking not found for user with id: " + userId);
        
        verify(rankingRepository, times(1)).findByUserIdRaw(userId);
    }

    @Test
    void getRankingsAround_shouldReturnRankingsAroundUser() {
        Long userId = 1L;
        Integer range = 5;
        List<Object[]> mockResults = createMockRankingsAroundResults(userId, range);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));
        when(rankingRepository.findByUserIdRaw(userId)).thenReturn(createMockRankingDetailResult(userId));
        when(rankingRepository.findRankingsAroundUserRaw(userId, range)).thenReturn(mockResults);

        RankingAroundResponse response = rankingService.getRankingsAround(userId, range);

        assertThat(response).isNotNull();
        assertThat(response.items()).isNotEmpty();
        verify(userRepository, times(1)).findById(userId);
        verify(rankingRepository, times(1)).findByUserIdRaw(userId);
        verify(rankingRepository, times(1)).findRankingsAroundUserRaw(userId, range);
    }

    @Test
    void getRankingsAround_shouldThrowExceptionForGuestUser() {
        Long userId = 2L;
        Integer range = 5;
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(guestUser));

        assertThatThrownBy(() -> rankingService.getRankingsAround(userId, range))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Guest users are not included in rankings");
        
        verify(userRepository, times(1)).findById(userId);
        verify(rankingRepository, never()).findRankingsAroundUserRaw(anyLong(), anyInt());
    }

    @Test
    void getRankingsAround_shouldThrowExceptionWhenUserNotFound() {
        Long userId = 999L;
        Integer range = 5;
        
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rankingService.getRankingsAround(userId, range))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User not found with id: " + userId);
        
        verify(userRepository, times(1)).findById(userId);
        verify(rankingRepository, never()).findRankingsAroundUserRaw(anyLong(), anyInt());
    }

    @Test
    void getRankingsAround_shouldReturnEmptyListWhenNoPlayersAround() {
        Long userId = 1L;
        Integer range = 5;
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));
        when(rankingRepository.findByUserIdRaw(userId)).thenReturn(createMockRankingDetailResult(userId));
        when(rankingRepository.findRankingsAroundUserRaw(userId, range)).thenReturn(new ArrayList<>());

        RankingAroundResponse response = rankingService.getRankingsAround(userId, range);
        
        assertThat(response).isNotNull();
        assertThat(response.items()).isEmpty();
        
        verify(rankingRepository, times(1)).findByUserIdRaw(userId);
        verify(rankingRepository, times(1)).findRankingsAroundUserRaw(userId, range);
    }

    @Test
    void getRankingsAround_shouldThrowExceptionWhenUserNotInRanking() {
        Long userId = 1L;
        Integer range = 5;
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(registeredUser));
        when(rankingRepository.findByUserIdRaw(userId)).thenReturn(new ArrayList<>());

        assertThatThrownBy(() -> rankingService.getRankingsAround(userId, range))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User is not in ranking");
        
        verify(rankingRepository, times(1)).findByUserIdRaw(userId);
        verify(rankingRepository, never()).findRankingsAroundUserRaw(anyLong(), anyInt());
    }

    private List<Object[]> createMockRankingResults(int count) {
        List<Object[]> results = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            Object[] row = new Object[]{
                    (long) i,
                    (long) (100 + i),
                    "user" + i,
                    1000L + i * 100,
                    10 + i,
                    5 + i,
                    Timestamp.from(Instant.now())
            };
            results.add(row);
        }
        return results;
    }

    private List<Object[]> createMockRankingDetailResult(Long userId) {
        Object[] row = new Object[]{
                10L,
                userId,
                "testuser",
                5000L,
                50,
                25,
                Timestamp.from(Instant.now())
        };
        List<Object[]> results = new ArrayList<>();
        results.add(row);
        return results;
    }

    private List<Object[]> createMockRankingsAroundResults(Long userId, Integer range) {
        List<Object[]> results = new ArrayList<>();
        int userPosition = 10;
        
        for (int i = -range; i <= range; i++) {
            long position = userPosition + i;
            long currentUserId = (position == userPosition) ? userId : (100 + position);
            Object[] row = new Object[]{
                    position,
                    currentUserId,
                    "user" + currentUserId,
                    1000L + position * 100,
                    10 + (int) position,
                    5 + (int) position
            };
            results.add(row);
        }
        return results;
    }
}
