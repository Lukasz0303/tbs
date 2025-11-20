import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject, computed, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ButtonModule } from 'primeng/button';
import { Game } from '../../models/game.model';
import { TranslateService } from '../../services/translate.service';

@Component({
  selector: 'app-last-game-card',
  standalone: true,
  imports: [CommonModule, ButtonModule],
  templateUrl: './last-game-card.component.html',
  styleUrls: ['./last-game-card.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LastGameCardComponent {
  private readonly gameSignal = signal<Game | null>(null);

  @Input() set game(value: Game) {
    this.gameSignal.set(value);
  }
  get game(): Game | null {
    return this.gameSignal();
  }

  @Output() continueGame = new EventEmitter<number>();

  readonly translate = inject(TranslateService);

  readonly gameTypeLabel = computed(() => {
    const game = this.gameSignal();
    if (!game) {
      return '-';
    }
    return game.gameType === 'vs_bot'
      ? this.translate.translate('game.type.vsBot')
      : this.translate.translate('game.type.pvp');
  });

  readonly opponentLabel = computed(() => {
    const game = this.gameSignal();
    if (!game) {
      return '-';
    }
    if (game.gameType === 'vs_bot') {
      const difficulty = game.botDifficulty || 'easy';
      return this.translate.translate(`game.bot.difficulty.${difficulty}`);
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

  readonly lastMoveDate = computed(() => {
    const game = this.gameSignal();
    if (!game || !game.lastMoveAt) {
      return null;
    }
    return new Date(game.lastMoveAt);
  });

  onContinue(): void {
    const game = this.gameSignal();
    if (game) {
      this.continueGame.emit(game.gameId);
    }
  }
}

