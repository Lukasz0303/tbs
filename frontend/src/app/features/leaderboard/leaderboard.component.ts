import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter, switchMap, take } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { TranslatePipe } from '../../pipes/translate.pipe';
import { TranslateService } from '../../services/translate.service';
import { RankingService } from '../../services/ranking.service';
import { MatchmakingService } from '../../services/matchmaking.service';
import { AuthService } from '../../services/auth.service';
import { LoggerService } from '../../services/logger.service';
import { Ranking } from '../../models/ranking.model';
import { TableLazyLoadEvent } from 'primeng/table';
import { UserRankCardComponent } from '../../components/leaderboard/user-rank-card/user-rank-card.component';
import { LEADERBOARD_CONSTANTS } from '../../shared/constants/leaderboard.constants';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [
    CommonModule,
    TableModule,
    ButtonModule,
    ToastModule,
    TranslatePipe,
    UserRankCardComponent,
  ],
  providers: [MessageService],
  templateUrl: './leaderboard.component.html',
  styleUrls: ['./leaderboard.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaderboardComponent implements OnInit {
  private readonly rankingService = inject(RankingService);
  private readonly matchmakingService = inject(MatchmakingService);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);
  private readonly translateService = inject(TranslateService);
  private readonly logger = inject(LoggerService);
  private readonly destroyRef = inject(DestroyRef);

  readonly rankings = signal<Ranking[]>([]);
  readonly userRanking = signal<Ranking | null>(null);
  readonly totalRecords = signal<number>(0);
  readonly isLoading = signal<boolean>(true);
  readonly isChallenging = signal<boolean>(false);
  readonly currentUserId = signal<number | null>(null);
  readonly pageSize = LEADERBOARD_CONSTANTS.DEFAULT_PAGE_SIZE;

  ngOnInit(): void {
    this.loadUserRanking();
    this.loadRanking(0, this.pageSize);
  }

  private loadUserRanking(): void {
    this.authService
      .getCurrentUser()
      .pipe(
        take(1),
        filter((user) => user !== null && !user.isGuest),
        switchMap((user) => this.rankingService.getUserRanking(user!.userId)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (ranking) => {
          this.userRanking.set(ranking);
          this.currentUserId.set(ranking.userId);
        },
        error: () => {
          this.userRanking.set(null);
        },
      });
  }

  private loadRanking(page: number, size: number): void {
    this.isLoading.set(true);

    this.rankingService
      .getRanking(page, size)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          this.rankings.set(response.content);
          this.totalRecords.set(response.totalElements);
          this.isLoading.set(false);
        },
        error: (error) => {
          this.isLoading.set(false);
          this.handleError(error);
        },
      });
  }

  onLazyLoad(event: TableLazyLoadEvent): void {
    if (event.first === undefined || event.rows === undefined || event.rows === null) {
      return;
    }
    const page = event.first / event.rows;
    this.loadRanking(page, event.rows);
  }

  onChallengePlayer(userId: number): void {
    if (this.isChallenging()) {
      return;
    }

    this.isChallenging.set(true);

    this.matchmakingService
      .challengePlayer(userId, LEADERBOARD_CONSTANTS.DEFAULT_BOARD_SIZE)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (game) => {
          this.isChallenging.set(false);
          this.messageService.add({
            severity: 'success',
            summary: this.translate('leaderboard.challenge.success.title'),
            detail: this.translate('leaderboard.challenge.success.detail'),
          });
          this.router.navigate(['/game', game.gameId]).catch(() => {});
        },
        error: (error) => {
          this.isChallenging.set(false);
          this.handleChallengeError(error);
        },
      });
  }


  isCurrentUser(userId: number): boolean {
    return this.currentUserId() === userId;
  }

  trackByUserId(index: number, ranking: Ranking): number {
    return ranking.userId;
  }

  private handleError(error: unknown): void {
    this.logger.error('Error loading leaderboard:', error);

    if (error instanceof HttpErrorResponse) {
      this.messageService.add({
        severity: 'error',
        summary: this.translate('leaderboard.error.title'),
        detail: error.error?.message || this.translate('leaderboard.error.load'),
      });
    } else {
      this.messageService.add({
        severity: 'error',
        summary: this.translate('leaderboard.error.title'),
        detail: this.translate('leaderboard.error.load'),
      });
    }
  }

  private handleChallengeError(error: unknown): void {
    this.logger.error('Error challenging player:', error);

    if (!(error instanceof HttpErrorResponse)) {
      this.messageService.add({
        severity: 'error',
        summary: this.translate('leaderboard.challenge.error.title'),
        detail: this.translate('leaderboard.challenge.error.generic'),
      });
      return;
    }

    let message = this.translate('leaderboard.challenge.error.generic');

    if (error.status === 404) {
      message = this.translate('leaderboard.challenge.error.notFound');
    } else if (error.status === 409) {
      message = this.translate('leaderboard.challenge.error.unavailable');
    }

    this.messageService.add({
      severity: 'error',
      summary: this.translate('leaderboard.challenge.error.title'),
      detail: message,
    });
  }

  private translate(key: string): string {
    return this.translateService.translate(key);
  }
}
