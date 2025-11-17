import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, EventEmitter, Input, Output, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime, distinctUntilChanged } from 'rxjs';
import { PlayerSymbol } from '../../models/game.model';

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
  private readonly clickSubject = new Subject<number>();

  constructor() {
    this.clickSubject
      .pipe(
        debounceTime(300),
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
    return Math.floor(index / this.boardSize);
  }

  getColFromIndex(index: number): number {
    return index % this.boardSize;
  }

  onCellClick(index: number): void {
    this.clickSubject.next(index);
  }

  private onCellClickInternal(index: number): void {
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
    const row = this.getRowFromIndex(index);
    const col = this.getColFromIndex(index);
    return this.boardState[row]?.[col] ?? null;
  }

  isCellDisabled(index: number): boolean {
    const isWaitingVsBot = this.gameType === 'vs_bot' && this.gameStatus === 'waiting';
    if (isWaitingVsBot) {
      return false;
    }

    if (this.disabled) {
      return true;
    }

    return this.gameStatus !== 'in_progress';
  }
}


