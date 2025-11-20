import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Game } from '../../models/game.model';
import { User } from '../../models/user.model';
import { TranslateService } from '../../services/translate.service';

@Component({
  selector: 'app-game-result-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-result-dialog.component.html',
  styleUrls: ['./game-result-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameResultDialogComponent {
  @Input() visible = false;
  @Input() game: Game | null = null;
  @Input() currentUser: User | null = null;
  @Input() pointsAtStake: number = 0;
  @Input() drawPoints: number = 0;
  @Output() close = new EventEmitter<void>();

  readonly translate = inject(TranslateService);

  isPlayerWinner(): boolean {
    if (!this.game || this.game.status !== 'finished') {
      return false;
    }

    if (this.game.gameType === 'vs_bot') {
      return Number(this.game.winnerId) === Number(this.game.player1Id);
    }

    if (this.game.gameType === 'pvp') {
      return this.currentUser !== null && this.game.winnerId !== null && Number(this.game.winnerId) === Number(this.currentUser.userId);
    }

    return false;
  }

  isPlayerLoser(): boolean {
    if (!this.game || this.game.status !== 'finished') {
      return false;
    }

    if (this.game.gameType === 'vs_bot') {
      return !this.isPlayerWinner() && this.game.winnerId !== null && Number(this.game.winnerId) !== Number(this.game.player1Id);
    }

    if (this.game.gameType === 'pvp') {
      return this.currentUser !== null && this.game.winnerId !== null && Number(this.game.winnerId) !== Number(this.currentUser.userId);
    }

    return false;
  }
}


