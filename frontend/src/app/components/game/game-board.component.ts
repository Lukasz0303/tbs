import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, EventEmitter, Input, Output, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { PlayerSymbol } from '../../models/game.model';
import { LoggerService } from '../../services/logger.service';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-game-board',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './game-board.component.html',
  styleUrls: ['./game-board.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameBoardComponent {
  @Input() boardSize: 3 | 4 | 5 = 3;
  @Input() disabled = false;
  @Input() gameStatus: 'waiting' | 'in_progress' | 'finished' | 'abandoned' | 'draw' = 'in_progress';
  @Input() gameType: 'vs_bot' | 'pvp' = 'vs_bot';
  @Input() boardState: (PlayerSymbol | null)[][] = [];
  @Input() currentPlayerSymbol: PlayerSymbol | null = null;
  @Input() winningCells: Array<{ row: number; col: number }> = [];

  @Output() move = new EventEmitter<{ row: number; col: number }>();

  private readonly destroyRef = inject(DestroyRef);
  private readonly logger = inject(LoggerService);
  private readonly clickSubject = new Subject<number>();

  constructor() {
    this.clickSubject
      .pipe(
        debounceTime(50),
        distinctUntilChanged(),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((index) => {
        this.onCellClickInternal(index);
      });
  }

  getCellsArray(): number[] {
    const size = this.boardSize || 3;
    return Array.from({ length: size * size }, (_, i) => i);
  }

  trackByIndex(index: number): number {
    return index;
  }

  getRowFromIndex(index: number): number {
    const size = this.boardSize || 3;
    if (size <= 0 || !Number.isInteger(size)) {
      this.logger.warn('Invalid boardSize in getRowFromIndex', { boardSize: this.boardSize, index });
      return 0;
    }
    return Math.floor(index / size);
  }

  getColFromIndex(index: number): number {
    const size = this.boardSize || 3;
    if (size <= 0 || !Number.isInteger(size)) {
      this.logger.warn('Invalid boardSize in getColFromIndex', { boardSize: this.boardSize, index });
      return 0;
    }
    return index % size;
  }

  onCellClick(index: number): void {
    if (!environment.production) {
      this.logger.debug('GAME_BOARD_CLICK', {
        index,
        boardSize: this.boardSize,
        gameType: this.gameType,
        gameStatus: this.gameStatus,
        disabledInput: this.disabled,
        cellDisabled: this.isCellDisabled(index),
        cellValue: this.getCellValue(index),
      });
    }
    this.clickSubject.next(index);
  }

  private onCellClickInternal(index: number): void {
    if (!environment.production) {
      this.logger.debug('GAME_BOARD_INTERNAL_CLICK', {
        index,
        cellDisabled: this.isCellDisabled(index),
        cellValue: this.getCellValue(index),
      });
    }
    if (this.isCellDisabled(index)) {
      return;
    }

    const row = this.getRowFromIndex(index);
    const col = this.getColFromIndex(index);
    const occupied = this.boardState[row]?.[col] != null;

    if (occupied) {
      return;
    }

    this.move.emit({ row, col });
  }

  isWinningCell(index: number): boolean {
    const row = this.getRowFromIndex(index);
    const col = this.getColFromIndex(index);
    return this.winningCells.some((c) => c.row === row && c.col === col);
  }

  getCellValue(index: number): PlayerSymbol | null {
    if (!this.boardState || !Array.isArray(this.boardState)) {
      this.logger.warn('Invalid boardState in getCellValue', { boardState: this.boardState });
      return null;
    }
    
    const row = this.getRowFromIndex(index);
    const col = this.getColFromIndex(index);
    
    if (row < 0 || col < 0 || row >= this.boardState.length) {
      return null;
    }
    
    const rowData = this.boardState[row];
    if (!Array.isArray(rowData) || col >= rowData.length) {
      return null;
    }
    
    return rowData[col] ?? null;
  }

  getCellAriaLabel(index: number): string {
    const value = this.getCellValue(index);
    const row = this.getRowFromIndex(index);
    const col = this.getColFromIndex(index);
    const disabled = this.isCellDisabled(index);
    const winning = this.isWinningCell(index);
    
    if (disabled) {
      return `Cell ${row + 1}, ${col + 1} - disabled`;
    }
    if (winning) {
      return `Cell ${row + 1}, ${col + 1} - ${value || 'empty'} - winning cell`;
    }
    return `Cell ${row + 1}, ${col + 1} - ${value || 'empty'}`;
  }
  
  isCellDisabled(index: number): boolean {
    const isWaitingVsBot = this.gameType === 'vs_bot' && this.gameStatus === 'waiting';
    if (isWaitingVsBot) {
      return false;
    }

    return this.disabled;
  }
}


