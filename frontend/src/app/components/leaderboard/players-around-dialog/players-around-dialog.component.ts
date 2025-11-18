import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, output, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DialogModule } from 'primeng/dialog';
import { ButtonModule } from 'primeng/button';
import { TranslatePipe } from '../../../pipes/translate.pipe';
import { RankingService } from '../../../services/ranking.service';
import { Ranking } from '../../../models/ranking.model';
import { LEADERBOARD_CONSTANTS } from '../../../shared/constants/leaderboard.constants';

@Component({
  selector: 'app-players-around-dialog',
  standalone: true,
  imports: [CommonModule, DialogModule, ButtonModule, TranslatePipe],
  templateUrl: './players-around-dialog.component.html',
  styleUrls: ['./players-around-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlayersAroundDialogComponent {
  private readonly rankingService = inject(RankingService);
  private readonly destroyRef = inject(DestroyRef);

  readonly userId = input.required<number>();
  readonly visible = input.required<boolean>();
  readonly close = output<void>();
  readonly challenge = output<number>();

  readonly players = signal<Ranking[]>([]);
  readonly isLoading = signal<boolean>(false);

  constructor() {
    effect(() => {
      if (this.visible() && this.userId()) {
        this.loadPlayersAround();
      } else if (!this.visible()) {
        this.players.set([]);
        this.isLoading.set(false);
      }
    });
  }

  private loadPlayersAround(): void {
    this.isLoading.set(true);

    this.rankingService
      .getPlayersAround(this.userId(), LEADERBOARD_CONSTANTS.PLAYERS_AROUND_RANGE)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (players) => {
          this.players.set(players);
          this.isLoading.set(false);
        },
        error: () => {
          this.isLoading.set(false);
          this.players.set([]);
        },
      });
  }

  onClose(): void {
    this.close.emit();
  }

  onChallenge(userId: number): void {
    this.challenge.emit(userId);
  }
}

