import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'ui-button',
  standalone: true,
  imports: [CommonModule],
  template: `<button
    [attr.type]="type"
    class="px-3 py-1.5 rounded font-medium transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
    [ngClass]="variant === 'primary' ? 'bg-slate-900 text-white hover:bg-slate-800' : 'bg-slate-200 text-slate-800 hover:bg-slate-300 dark:bg-slate-800 dark:text-slate-200 dark:hover:bg-slate-700'"
  >
    <ng-content />
  </button>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ButtonComponent {
  @Input() variant: 'primary' | 'secondary' = 'primary';
  @Input() type: 'button' | 'submit' = 'button';
}


