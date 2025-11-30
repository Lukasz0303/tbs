import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { TranslateService } from '../../services/translate.service';

@Component({
  selector: 'app-privacy-policy',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="legal-page-container">
      <div class="legal-content">
        <div class="legal-header">
          <h1>{{ translate.translate('legal.privacy.title') }}</h1>
          <button type="button" (click)="goBack()" class="back-link">
            <span class="pi pi-arrow-left"></span>
            <span>{{ translate.translate('legal.privacy.back') }}</span>
          </button>
        </div>
        <div class="legal-body">
          <p>
            {{ translate.translate('legal.privacy.intro') }}
          </p>

          <h2>{{ translate.translate('legal.privacy.scope.title') }}</h2>
          <ul>
            <li>{{ translate.translate('legal.privacy.scope.userId') }}</li>
            <li>{{ translate.translate('legal.privacy.scope.email') }}</li>
            <li>{{ translate.translate('legal.privacy.scope.ip') }}</li>
            <li>{{ translate.translate('legal.privacy.scope.gameData') }}</li>
          </ul>

          <h2>{{ translate.translate('legal.privacy.purpose.title') }}</h2>
          <ul>
            <li>{{ translate.translate('legal.privacy.purpose.gameplay') }}</li>
            <li>{{ translate.translate('legal.privacy.purpose.security') }}</li>
            <li>{{ translate.translate('legal.privacy.purpose.quality') }}</li>
          </ul>

          <h2>{{ translate.translate('legal.privacy.legal.title') }}</h2>
          <p>
            {{ translate.translate('legal.privacy.legal.basis') }}
          </p>

          <h2>{{ translate.translate('legal.privacy.retention.title') }}</h2>
          <p>
            {{ translate.translate('legal.privacy.retention.duration') }}
          </p>

          <h2>{{ translate.translate('legal.privacy.recipients.title') }}</h2>
          <p>
            {{ translate.translate('legal.privacy.recipients.info') }}
          </p>

          <h2>{{ translate.translate('legal.privacy.rights.title') }}</h2>
          <ul>
            <li>{{ translate.translate('legal.privacy.rights.access') }}</li>
            <li>{{ translate.translate('legal.privacy.rights.objection') }}</li>
            <li>{{ translate.translate('legal.privacy.rights.portability') }}</li>
          </ul>

          <h2>{{ translate.translate('legal.privacy.contact.title') }}</h2>
          <p>{{ translate.translate('legal.privacy.contact.info') }}</p>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./legal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivacyPolicyComponent {
  private readonly router = inject(Router);
  readonly translate = inject(TranslateService);

  goBack(): void {
    this.router.navigate(['/']);
  }
}


