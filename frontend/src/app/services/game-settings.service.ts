import { Injectable, inject } from '@angular/core';
import { PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BotDifficulty } from '../models/game.model';

export type GameMode = BotDifficulty | 'pvp';
export type BoardSize = 3 | 4 | 5;

export interface GameSettings {
  gameMode: GameMode;
  boardSize: BoardSize;
}

@Injectable({
  providedIn: 'root',
})
export class GameSettingsService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly storageKey = 'game-settings';
  private readonly defaultSettings: GameSettings = {
    gameMode: 'easy',
    boardSize: 3,
  };

  getSettings(): GameSettings {
    if (!isPlatformBrowser(this.platformId)) {
      return this.defaultSettings;
    }
    try {
      const stored = localStorage.getItem(this.storageKey);
      if (!stored) {
        return this.defaultSettings;
      }
      const parsed = JSON.parse(stored) as Partial<GameSettings>;
      return {
        gameMode: this.validateGameMode(parsed.gameMode) ?? this.defaultSettings.gameMode,
        boardSize: this.validateBoardSize(parsed.boardSize) ?? this.defaultSettings.boardSize,
      };
    } catch {
      return this.defaultSettings;
    }
  }

  saveSettings(settings: GameSettings): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    try {
      const validated: GameSettings = {
        gameMode: this.validateGameMode(settings.gameMode) ?? this.defaultSettings.gameMode,
        boardSize: this.validateBoardSize(settings.boardSize) ?? this.defaultSettings.boardSize,
      };
      localStorage.setItem(this.storageKey, JSON.stringify(validated));
    } catch {}
  }

  private validateGameMode(mode: unknown): GameMode | null {
    if (typeof mode !== 'string') {
      return null;
    }
    const validModes: GameMode[] = ['easy', 'medium', 'hard', 'pvp'];
    return validModes.includes(mode as GameMode) ? (mode as GameMode) : null;
  }

  private validateBoardSize(size: unknown): BoardSize | null {
    if (typeof size !== 'number') {
      return null;
    }
    const validSizes: BoardSize[] = [3, 4, 5];
    return validSizes.includes(size as BoardSize) ? (size as BoardSize) : null;
  }
}

