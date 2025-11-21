import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { Injectable, OnDestroy, inject } from '@angular/core';
import { PLATFORM_ID } from '@angular/core';
import { Subscription } from 'rxjs';
import { AudioSettingsService } from './audio-settings.service';

@Injectable({
  providedIn: 'root',
})
export class BackgroundMusicService implements OnDestroy {
  private readonly document = inject(DOCUMENT);
  private readonly platformId = inject(PLATFORM_ID);
  private readonly audioSettingsService = inject(AudioSettingsService);
  private readonly playlist = [
    'mixkit-yoga-music-05-385.mp3',
    'mixkit-zanarkand-forest-169.mp3',
    'mixkit-relaxation-meditation-365.mp3',
    'mixkit-yoga-song-444.mp3',
    'mixkit-vastness-184.mp3',
    'mixkit-relax-beat-292.mp3',
    'mixkit-relaxation-05-749.mp3',
  ];
  private currentTrackIndex = -1;
  private audio?: HTMLAudioElement;
  private isPlaying = false;
  private readonly handleTrackEnded = () => {
    if (!this.isPlaying) {
      return;
    }
    this.playNextTrack();
  };
  private settingsSubscription?: Subscription;

  constructor() {
    if (isPlatformBrowser(this.platformId)) {
      this.settingsSubscription = this.audioSettingsService.settings$.subscribe((settings) => {
        if (!settings.musicEnabled) {
          this.stop();
        }
      });
    }
  }

  play(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    if (!this.audioSettingsService.getSettings().musicEnabled) {
      return;
    }
    if (this.playlist.length === 0) {
      console.warn('BackgroundMusicService: Playlist is empty');
      return;
    }
    const audio = this.ensureAudio();
    if (!audio) {
      return;
    }
    if (this.isPlaying) {
      return;
    }
    this.advanceTrackIndex();
    const trackName = this.playlist[this.currentTrackIndex];
    console.log('BackgroundMusicService: Playing track', this.currentTrackIndex, trackName);
    this.loadCurrentTrack(audio);
    this.startPlayback(audio);
  }

  stop(): void {
    if (!this.audio) {
      return;
    }
    console.log('BackgroundMusicService: Stopping music');
    if (this.isPlaying) {
      this.audio.pause();
      this.audio.currentTime = 0;
      this.isPlaying = false;
    }
    this.audio.src = '';
    this.audio.load();
  }

  setVolume(volume: number): void {
    if (!this.audio) {
      return;
    }
    this.audio.volume = Math.max(0, Math.min(1, volume));
  }

  ngOnDestroy(): void {
    this.stop();
    this.settingsSubscription?.unsubscribe();
  }

  private ensureAudio(): HTMLAudioElement | undefined {
    if (this.audio) {
      return this.audio;
    }
    if (this.playlist.length === 0) {
      return undefined;
    }
    const audio = new Audio();
    audio.preload = 'auto';
    audio.volume = 0.2;
    audio.loop = false;
    audio.addEventListener('error', (e) => {
      console.error('BackgroundMusicService: Audio load error', e, audio.src);
    });
    audio.addEventListener('canplaythrough', () => {
      console.log('BackgroundMusicService: Music ready', audio.src);
    });
    audio.addEventListener('ended', this.handleTrackEnded);
    this.audio = audio;
    return this.audio;
  }

  private loadCurrentTrack(audio: HTMLAudioElement): void {
    const track = this.playlist[this.currentTrackIndex];
    if (!track) {
      return;
    }
    const url = `/assets/audio/${track}`;
    console.log('BackgroundMusicService: Loading track', this.currentTrackIndex, track, 'URL:', url);
    audio.removeEventListener('ended', this.handleTrackEnded);
    audio.src = '';
    audio.load();
    audio.src = url;
    audio.currentTime = 0;
    audio.addEventListener('ended', this.handleTrackEnded);
    audio.load();
  }

  private startPlayback(audio: HTMLAudioElement): void {
    void audio
      .play()
      .then(() => {
        this.isPlaying = true;
      })
      .catch((error) => {
        this.isPlaying = false;
        console.warn('BackgroundMusicService: Failed to play music', error);
      });
  }

  private playNextTrack(): void {
    if (!this.audio || this.playlist.length === 0) {
      return;
    }
    this.advanceTrackIndex();
    this.loadCurrentTrack(this.audio);
    this.startPlayback(this.audio);
  }

  private advanceTrackIndex(): void {
    if (this.playlist.length === 0) {
      this.currentTrackIndex = -1;
      return;
    }
    this.currentTrackIndex = (this.currentTrackIndex + 1) % this.playlist.length;
  }
}

