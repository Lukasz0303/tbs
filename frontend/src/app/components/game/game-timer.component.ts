import { ChangeDetectionStrategy, Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlayerSymbol } from '../../models/game.model';
import { TranslateService } from '../../services/translate.service';

@Component({
  selector: 'app-game-timer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-timer.component.html',
  styleUrls: ['./game-timer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameTimerComponent {
  @Input() remainingSeconds: number = 10;
  @Input() currentPlayerSymbol: PlayerSymbol | null = null;

  readonly translate = inject(TranslateService);

  getTimerClass(): string {
    if (this.remainingSeconds <= 3) {
      return 'game-timer--danger';
    }
    if (this.remainingSeconds <= 5) {
      return 'game-timer--warning';
    }
    return 'game-timer--normal';
  }

  getTimerLabel(): string {
    return `${this.remainingSeconds}s`;
  }
}

