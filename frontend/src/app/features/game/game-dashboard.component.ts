import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-game-dashboard',
  standalone: true,
  template: `<div class="prose dark:prose-invert"><h2>Gra</h2><p>Dashboard gry (placeholder).</p></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GameDashboardComponent {}


