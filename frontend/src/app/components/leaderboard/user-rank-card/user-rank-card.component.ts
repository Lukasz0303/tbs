import { ChangeDetectionStrategy, ChangeDetectorRef, Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TranslatePipe } from '../../../pipes/translate.pipe';
import { RankingService } from '../../../services/ranking.service';
import { LoggerService } from '../../../services/logger.service';
import { Ranking } from '../../../models/ranking.model';
import { LEADERBOARD_CONSTANTS } from '../../../shared/constants/leaderboard.constants';

@Component({
  selector: 'app-user-rank-card',
  standalone: true,
  imports: [CommonModule, CardModule, ButtonModule, TranslatePipe],
  templateUrl: './user-rank-card.component.html',
  styleUrls: ['./user-rank-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserRankCardComponent {
  private readonly rankingService = inject(RankingService);
  private readonly logger = inject(LoggerService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly ranking = input.required<Ranking>();
  readonly challenge = output<number>();

  readonly showPlayersAround = signal<boolean>(false);
  readonly players = signal<Ranking[]>([]);
  readonly isLoading = signal<boolean>(false);

  onShowPlayersAround(): void {
    const currentValue = this.showPlayersAround();
    this.showPlayersAround.set(!currentValue);
    this.cdr.markForCheck();

    if (!currentValue && this.players().length === 0) {
      this.loadPlayersAround();
    }
  }

  private loadPlayersAround(): void {
    this.isLoading.set(true);
    this.cdr.markForCheck();

    this.rankingService
      .getPlayersAround(this.ranking().userId, LEADERBOARD_CONSTANTS.PLAYERS_AROUND_RANGE)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (players) => {
          this.players.set(players);
          this.isLoading.set(false);
          this.cdr.markForCheck();
        },
        error: (error) => {
          this.logger.error('Error loading players around:', error);
          this.isLoading.set(false);
          this.players.set([]);
          this.cdr.markForCheck();
        },
      });
  }

  onChallenge(userId: number): void {
    this.challenge.emit(userId);
  }
}

