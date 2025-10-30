import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly darkClass = 'dark';

  enableDark(): void {
    document.documentElement.classList.add(this.darkClass);
  }

  disableDark(): void {
    document.documentElement.classList.remove(this.darkClass);
  }

  toggle(): void {
    document.documentElement.classList.toggle(this.darkClass);
  }
}


