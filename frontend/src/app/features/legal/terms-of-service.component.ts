import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TranslateService } from '../../services/translate.service';

@Component({
  selector: 'app-terms-of-service',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="legal-page-container">
      <div class="legal-content">
        <div class="legal-header">
          <h1>{{ translate.translate('legal.terms.title') }}</h1>
          <button type="button" (click)="goBack()" class="back-link">
            <span class="pi pi-arrow-left"></span>
            <span>{{ translate.translate('legal.terms.back') }}</span>
          </button>
        </div>
        <div class="legal-body">
          <p>
            {{ translate.translate('legal.terms.intro') }}
          </p>

          <h2>{{ translate.translate('legal.terms.account.title') }}</h2>
          <ul>
            <li>{{ translate.translate('legal.terms.account.guest') }}</li>
            <li>{{ translate.translate('legal.terms.account.security') }}</li>
            <li>{{ translate.translate('legal.terms.account.restriction') }}</li>
          </ul>

          <h2>{{ translate.translate('legal.terms.fairness.title') }}</h2>
          <ul>
            <li>{{ translate.translate('legal.terms.fairness.cheating') }}</li>
            <li>{{ translate.translate('legal.terms.fairness.hindering') }}</li>
          </ul>

          <h2>{{ translate.translate('legal.terms.data.title') }}</h2>
          <p>
            {{ translate.translate('legal.terms.data.processing') }}
          </p>

          <h2>{{ translate.translate('legal.terms.responsibility.title') }}</h2>
          <p>
            {{ translate.translate('legal.terms.responsibility.disclaimer') }}
          </p>

          <h2>{{ translate.translate('legal.terms.changes.title') }}</h2>
          <p>
            {{ translate.translate('legal.terms.changes.notice') }}
          </p>

          <h2>{{ translate.translate('legal.terms.contact.title') }}</h2>
          <p>{{ translate.translate('legal.terms.contact.info') }}</p>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./legal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TermsOfServiceComponent {
  private readonly router = inject(Router);
  readonly translate = inject(TranslateService);

  goBack(): void {
    this.router.navigate(['/']);
  }
}


