import { isPlatformBrowser } from '@angular/common';
import { Injectable, inject } from '@angular/core';
import { PLATFORM_ID } from '@angular/core';
import { AudioSettingsService } from './audio-settings.service';

@Injectable({
  providedIn: 'root',
})
export class GameResultSoundService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly audioSettingsService = inject(AudioSettingsService);
  private victoryTemplate?: HTMLAudioElement;
  private defeatTemplate?: HTMLAudioElement;

  playVictory(): void {
    const settings = this.audioSettingsService.getSettings();
    if (!settings.clickSoundEnabled) {
      return;
    }
    this.play(this.ensureVictoryAudio());
  }

  playDefeat(): void {
    const settings = this.audioSettingsService.getSettings();
    if (!settings.clickSoundEnabled) {
      return;
    }
    this.play(this.ensureDefeatAudio());
  }

  private play(source?: HTMLAudioElement): void {
    if (!isPlatformBrowser(this.platformId) || !source) {
      return;
    }
    const instance = source.cloneNode(true) as HTMLAudioElement;
    instance.volume = source.volume;
    void instance.play().catch((error) => {
      console.warn('GameResultSoundService: Failed to play sound', error);
    });
  }

  private ensureVictoryAudio(): HTMLAudioElement | undefined {
    if (this.victoryTemplate) {
      return this.victoryTemplate;
    }
    const audio = new Audio('/assets/audio/victory-fanfare.wav');
    audio.preload = 'auto';
    audio.volume = 0.35;
    audio.addEventListener('error', (e) => {
      console.error('GameResultSoundService: Victory audio load error', e, audio.src);
    });
    this.victoryTemplate = audio;
    return this.victoryTemplate;
  }

  private ensureDefeatAudio(): HTMLAudioElement | undefined {
    if (this.defeatTemplate) {
      return this.defeatTemplate;
    }
    const audio = new Audio('/assets/audio/defeat-stinger.wav');
    audio.preload = 'auto';
    audio.volume = 0.3;
    audio.addEventListener('error', (e) => {
      console.error('GameResultSoundService: Defeat audio load error', e, audio.src);
    });
    this.defeatTemplate = audio;
    return this.defeatTemplate;
  }
}


