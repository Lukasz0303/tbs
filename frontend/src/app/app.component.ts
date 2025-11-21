import { Component, OnDestroy, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ButtonSoundService } from './services/button-sound.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnDestroy {
  private readonly buttonSoundService = inject(ButtonSoundService);
  title = 'frontend';

  constructor() {
    this.buttonSoundService.register();
  }

  ngOnDestroy(): void {
    this.buttonSoundService.unregister();
  }
}
