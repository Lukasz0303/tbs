import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-auth',
  standalone: true,
  template: `<div class="prose dark:prose-invert"><h2>Auth</h2><p>Placeholder logowania.</p></div>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthComponent {}


