import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class LoggerService {
  private readonly isDev = !environment.production;

  debug(...args: unknown[]): void {
    if (this.isDev) {
      console.log(...args);
    }
  }

  error(...args: unknown[]): void {
    console.error(...args);
  }

  warn(...args: unknown[]): void {
    if (this.isDev) {
      console.warn(...args);
    }
  }

  info(...args: unknown[]): void {
    if (this.isDev) {
      console.info(...args);
    }
  }
}
