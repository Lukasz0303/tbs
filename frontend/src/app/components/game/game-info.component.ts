import { ChangeDetectionStrategy, Component, Input, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Game } from '../../models/game.model';
import { User } from '../../models/user.model';
import { TranslateService } from '../../services/translate.service';

@Component({
  selector: 'app-game-info',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-info.component.html',
  styleUrls: ['./game-info.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameInfoComponent {
  private readonly gameSignal = signal<Game | null>(null);
  private readonly currentUserSignal = signal<User | null>(null);

  @Input() set game(value: Game | null) {
    this.gameSignal.set(value);
  }
  get game(): Game | null {
    return this.gameSignal();
  }

  @Input() set currentUser(value: User | null) {
    this.currentUserSignal.set(value);
  }
  get currentUser(): User | null {
    return this.currentUserSignal();
  }

  readonly translate = inject(TranslateService);

  readonly opponentName = computed(() => {
    const game = this.gameSignal();
    const user = this.currentUserSignal();
    if (!game || !user) {
      return '-';
    }

    if (game.gameType === 'vs_bot') {
      const difficulty = game.botDifficulty || 'easy';
      return this.translate.translate(`game.bot.difficulty.${difficulty}`);
    }

    if (game.player1Id === user.userId) {
      return game.player2Id 
        ? this.translate.translate('game.info.opponent')
        : '-';
    }

    return this.translate.translate('game.info.opponent');
  });

  readonly statusLabel = computed(() => {
    const game = this.gameSignal();
    if (!game) {
      return '-';
    }
    return this.translate.translate(`game.status.${game.status}`);
  });

  readonly currentPlayerLabel = computed(() => {
    const game = this.gameSignal();
    if (!game || !game.currentPlayerSymbol) {
      return '-';
    }
    return game.currentPlayerSymbol.toUpperCase();
  });
}

