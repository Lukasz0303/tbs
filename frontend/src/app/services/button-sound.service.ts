import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Injectable, OnDestroy, inject } from '@angular/core';
import { PLATFORM_ID } from '@angular/core';
import { Subscription } from 'rxjs';
import { AudioSettingsService } from './audio-settings.service';

@Injectable({
  providedIn: 'root',
})
export class ButtonSoundService implements OnDestroy {
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly audioSettingsService = inject(AudioSettingsService);
  private readonly handleDocumentClick = (event: Event) => this.handleClick(event);
  private audioTemplate?: HTMLAudioElement;
  private listenerRegistered = false;
  private settingsSubscription?: Subscription;

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.register();
      this.settingsSubscription = this.audioSettingsService.settings$.subscribe((settings) => {
        if (settings.clickSoundEnabled) {
          this.register();
        } else {
          this.unregister();
        }
      });
    }
  }

  register(): void {
    if (this.listenerRegistered || !isPlatformBrowser(this.platformId)) {
      return;
    }
    this.document.addEventListener('click', this.handleDocumentClick, true);
    this.listenerRegistered = true;
  }

  unregister(): void {
    if (!this.listenerRegistered) {
      return;
    }
    this.document.removeEventListener('click', this.handleDocumentClick, true);
    this.listenerRegistered = false;
  }

  play(): void {
    if (!isPlatformBrowser(this.platformId) || !this.isClickSoundEnabled()) {
      return;
    }
    const source = this.ensureAudio();
    if (!source) {
      return;
    }
    const instance = source.cloneNode(true) as HTMLAudioElement;
    instance.volume = source.volume;
    void instance.play().catch((error) => {
      console.warn('ButtonSoundService: Failed to play sound', error);
    });
  }

  ngOnDestroy(): void {
    this.unregister();
    this.settingsSubscription?.unsubscribe();
  }

  private handleClick(event: Event): void {
    const target = event.target;
    if (!(target instanceof HTMLElement)) {
      return;
    }

    if (!this.isClickSoundEnabled()) {
      return;
    }

    const clickableElement = this.findClickableElement(target);
    if (!clickableElement) {
      return;
    }

    if (this.shouldSkipSound(clickableElement)) {
      return;
    }

    this.play();
  }

  private findClickableElement(element: HTMLElement): HTMLElement | null {
    let current: HTMLElement | null = element;

    while (current) {
      if (this.isClickableElement(current)) {
        return current;
      }
      current = current.parentElement;
    }

    return null;
  }

  private isClickableElement(element: HTMLElement): boolean {
    const tagName = element.tagName.toLowerCase();
    
    if (tagName === 'button' || tagName === 'a') {
      return true;
    }

    if (element.classList.contains('cell')) {
      return true;
    }

    if (element.getAttribute('role') === 'button') {
      return true;
    }

    if (element.hasAttribute('data-clickable')) {
      return true;
    }

    return false;
  }

  private shouldSkipSound(element: HTMLElement): boolean {
    if (element.dataset['sound'] === 'off') {
      return true;
    }

    if (element instanceof HTMLButtonElement && element.disabled) {
      return true;
    }

    if (element instanceof HTMLAnchorElement && element.hasAttribute('disabled')) {
      return true;
    }

    if (element.classList.contains('cell') && element.classList.contains('cell--disabled')) {
      return true;
    }

    return false;
  }

  private ensureAudio(): HTMLAudioElement | undefined {
    if (this.audioTemplate) {
      return this.audioTemplate;
    }
    const audio = new Audio('/assets/audio/button-click.wav');
    audio.preload = 'auto';
    audio.volume = 0.25;
    audio.addEventListener('error', (e) => {
      console.error('ButtonSoundService: Audio load error', e, audio.src);
    });
    audio.addEventListener('canplaythrough', () => {
      console.log('ButtonSoundService: Audio ready', audio.src);
    });
    this.audioTemplate = audio;
    return this.audioTemplate;
  }

  private isClickSoundEnabled(): boolean {
    return this.audioSettingsService.getSettings().clickSoundEnabled;
  }
}

