import { ChangeDetectionStrategy, Component, Input, inject, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Game } from '../../models/game.model';
import { TranslateService } from '../../services/translate.service';

@Component({
  selector: 'app-game-bot-info',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-bot-info.component.html',
  styleUrls: ['./game-bot-info.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameBotInfoComponent {
  private readonly gameSignal = signal<Game | null>(null);

  @Input() set game(value: Game | null) {
    this.gameSignal.set(value);
  }
  get game(): Game | null {
    return this.gameSignal();
  }

  readonly translate = inject(TranslateService);

  readonly botDifficultyLabel = computed(() => {
    const game = this.gameSignal();
    if (!game || game.gameType !== 'vs_bot' || !game.botDifficulty) {
      return '-';
    }
    return this.translate.translate(`game.bot.difficulty.${game.botDifficulty}`);
  });

  readonly statusLabel = computed(() => {
    const game = this.gameSignal();
    if (!game) {
      return '-';
    }
    return this.translate.translate(`game.status.${game.status}`);
  });

  readonly gameTypeLabel = computed(() => {
    const game = this.gameSignal();
    if (!game) {
      return '-';
    }
    return game.gameType === 'vs_bot'
      ? this.translate.translate('game.type.vsBot')
      : this.translate.translate('game.type.pvp');
  });

  readonly totalMoves = computed(() => {
    const game = this.gameSignal();
    return game?.totalMoves ?? 0;
  });

  readonly currentPlayerSymbol = computed(() => {
    const game = this.gameSignal();
    return game?.currentPlayerSymbol?.toUpperCase() ?? null;
  });

  readonly botSymbol = computed(() => {
    const game = this.gameSignal();
    if (!game || game.gameType !== 'vs_bot') {
      return null;
    }
    return 'O';
  });

  readonly botAvatar = computed(() => {
    const game = this.gameSignal();
    if (!game || game.gameType !== 'vs_bot' || !game.botDifficulty) {
      return null;
    }
    if (game.botDifficulty === 'easy') {
      return 'assets/BOT_1_3.png';
    }
    if (game.botDifficulty === 'medium') {
      return 'assets/BOT_2_3.png';
    }
    if (game.botDifficulty === 'hard') {
      return 'assets/BOT_3_3.png';
    }
    return null;
  });
}

