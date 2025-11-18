import { PlayerSymbol } from '../../models/game.model';

export function normalizeBoardState(boardState: unknown): (PlayerSymbol | null)[][] {
  if (!Array.isArray(boardState)) {
    return [];
  }

  if (boardState.length === 0) {
    return [];
  }

  return boardState.map((row: unknown) => {
    if (!Array.isArray(row)) {
      return [];
    }
    return row.map((cell: unknown) => {
      if (cell === null || cell === undefined || cell === '') {
        return null;
      }
      if (cell === 'x' || cell === 'o') {
        return cell as PlayerSymbol;
      }
      return null;
    });
  });
}

