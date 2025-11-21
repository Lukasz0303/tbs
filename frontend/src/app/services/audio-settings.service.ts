import { Injectable, inject } from '@angular/core';
import { PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject } from 'rxjs';

export interface AudioSettings {
  musicEnabled: boolean;
  clickSoundEnabled: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class AudioSettingsService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly storageKey = 'audio-settings';
  private readonly defaultSettings: AudioSettings = {
    musicEnabled: true,
    clickSoundEnabled: true,
  };
  private readonly settingsSubject = new BehaviorSubject<AudioSettings>(this.readSettings());

  readonly settings$ = this.settingsSubject.asObservable();

  getSettings(): AudioSettings {
    return this.settingsSubject.value;
  }

  setMusicEnabled(enabled: boolean): void {
    this.updateSettings({ musicEnabled: enabled });
  }

  setClickSoundEnabled(enabled: boolean): void {
    this.updateSettings({ clickSoundEnabled: enabled });
  }

  private updateSettings(partial: Partial<AudioSettings>): void {
    const nextSettings = { ...this.settingsSubject.value, ...partial };
    this.settingsSubject.next(nextSettings);
    this.persistSettings(nextSettings);
  }

  private readSettings(): AudioSettings {
    if (!isPlatformBrowser(this.platformId)) {
      return this.defaultSettings;
    }
    try {
      const stored = localStorage.getItem(this.storageKey);
      if (!stored) {
        return this.defaultSettings;
      }
      const parsed = JSON.parse(stored) as Partial<AudioSettings>;
      return {
        musicEnabled: parsed.musicEnabled ?? this.defaultSettings.musicEnabled,
        clickSoundEnabled: parsed.clickSoundEnabled ?? this.defaultSettings.clickSoundEnabled,
      };
    } catch {
      return this.defaultSettings;
    }
  }

  private persistSettings(settings: AudioSettings): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    try {
      localStorage.setItem(this.storageKey, JSON.stringify(settings));
    } catch {}
  }
}


