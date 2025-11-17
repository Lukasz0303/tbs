import {
  ChangeDetectionStrategy,
  Component,
  EventEmitter,
  Input,
  Output,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';
import { TagModule } from 'primeng/tag';
import { Game } from '../../../models/game.model';
import { TranslatePipe } from '../../../pipes/translate.pipe';

@Component({
  selector: 'app-game-banner',
  standalone: true,
  imports: [
    CommonModule,
    CardModule,
    ButtonModule,
    TagModule,
    DatePipe,
    TranslatePipe,
  ],
  templateUrl: './game-banner.component.html',
  styleUrls: ['./game-banner.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameBannerComponent {
  @Input({ required: true }) game!: Game;
  @Output() continueGame = new EventEmitter<number>();

  onContinue(): void {
    this.continueGame.emit(this.game.gameId);
  }

  get gameTypeKey(): string {
    return this.game.gameType === 'vs_bot'
      ? 'home.banner.gameType.vsBot'
      : 'home.banner.gameType.pvp';
  }

  get difficultyKey(): string | null {
    if (!this.game.botDifficulty) {
      return null;
    }
    if (this.game.botDifficulty === 'easy') {
      return 'home.banner.difficulty.easy';
    }
    if (this.game.botDifficulty === 'medium') {
      return 'home.banner.difficulty.medium';
    }
    return 'home.banner.difficulty.hard';
  }

  get statusKey(): string {
    switch (this.game.status) {
      case 'waiting':
        return 'home.banner.status.waiting';
      case 'finished':
        return 'home.banner.status.finished';
      case 'abandoned':
        return 'home.banner.status.abandoned';
      case 'draw':
        return 'home.banner.status.draw';
      default:
        return 'home.banner.status.inProgress';
    }
  }

  get statusSeverity(): 'info' | 'success' | 'warning' | 'danger' | 'secondary' | 'contrast' {
    if (this.game.status === 'in_progress') {
      return 'info';
    }
    if (this.game.status === 'finished') {
      return 'success';
    }
    if (this.game.status === 'draw') {
      return 'warning';
    }
    return 'danger';
  }
}

