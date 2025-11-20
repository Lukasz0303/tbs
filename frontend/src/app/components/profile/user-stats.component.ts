import { ChangeDetectionStrategy, Component, Input, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { User } from '../../models/user.model';
import { Ranking } from '../../models/ranking.model';
import { TranslateService } from '../../services/translate.service';

@Component({
  selector: 'app-user-stats',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-stats.component.html',
  styleUrls: ['./user-stats.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserStatsComponent {
  private readonly userSignal = signal<User | null>(null);
  private readonly rankingSignal = signal<Ranking | null>(null);

  @Input() set user(value: User) {
    this.userSignal.set(value);
  }
  get user(): User | null {
    return this.userSignal();
  }

  @Input() set ranking(value: Ranking | null) {
    this.rankingSignal.set(value);
  }
  get ranking(): Ranking | null {
    return this.rankingSignal();
  }

  readonly translate = inject(TranslateService);

  readonly hasRanking = computed(() => this.rankingSignal() !== null);

  readonly winRate = computed(() => {
    const user = this.userSignal();
    if (!user || !user.gamesPlayed || user.gamesPlayed === 0) {
      return 0;
    }
    return Math.round((user.gamesWon / user.gamesPlayed) * 100);
  });
}

