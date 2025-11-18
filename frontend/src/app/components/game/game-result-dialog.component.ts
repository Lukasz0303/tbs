import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Game } from '../../models/game.model';
import { User } from '../../models/user.model';
import { LoggerService } from '../../services/logger.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-game-result-dialog',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-result-dialog.component.html',
  styleUrls: ['./game-result-dialog.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameResultDialogComponent implements OnChanges {
  @Input() visible = false;
  @Input() game: Game | null = null;
  @Input() currentUser: User | null = null;
  @Output() close = new EventEmitter<void>();

  private readonly logger = inject(LoggerService);

  ngOnChanges(changes: SimpleChanges): void {
    if (this.game && this.currentUser && this.game.status === 'finished') {
      if (!environment.production) {
        this.logger.debug('GAME_RESULT_DIALOG_STATE', {
          gameId: this.game.gameId,
          gameType: this.game.gameType,
          status: this.game.status,
          winnerId: this.game.winnerId,
          player1Id: this.game.player1Id,
          player2Id: this.game.player2Id,
          currentUserId: this.currentUser.userId,
          isPlayerWinner: this.isPlayerWinner(),
          isPlayerLoser: this.isPlayerLoser(),
        });
      }
    }
  }

  isPlayerWinner(): boolean {
    if (!this.game || this.game.status !== 'finished') {
      return false;
    }

    if (this.game.gameType === 'vs_bot') {
      return this.game.winnerId === this.game.player1Id;
    }

    if (this.game.gameType === 'pvp') {
      if (!this.currentUser || this.game.winnerId === null) {
        return false;
      }
      return Number(this.game.winnerId) === Number(this.currentUser.userId);
    }

    return false;
  }

  isPlayerLoser(): boolean {
    if (!this.game || this.game.status !== 'finished') {
      return false;
    }

    if (this.game.gameType === 'vs_bot') {
      return !this.isPlayerWinner() && this.game.winnerId !== this.game.player1Id;
    }

    if (this.game.gameType === 'pvp') {
      if (!this.currentUser || this.game.winnerId === null) {
        return false;
      }
      return Number(this.game.winnerId) !== Number(this.currentUser.userId);
    }

    return false;
  }
}


