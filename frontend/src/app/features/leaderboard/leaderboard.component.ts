import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  template: `<div class="prose dark:prose-invert"><h2>Ranking</h2><p>Lista rankingowa (placeholder).</p></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LeaderboardComponent {}


