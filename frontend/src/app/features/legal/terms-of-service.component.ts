import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-terms-of-service',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="legal-page-container">
      <div class="legal-content">
        <div class="legal-header">
          <h1>Regulamin</h1>
          <a routerLink="/" class="back-link">
            <span class="pi pi-arrow-left"></span>
            <span>Powrót</span>
          </a>
        </div>
        <div class="legal-body">
          <p>
            Regulamin określa zasady korzystania z aplikacji World at War:
            Turn‑Based Strategy. Korzystając z aplikacji, akceptujesz poniższe
            postanowienia.
          </p>

          <h2>Konto i dostęp</h2>
          <ul>
            <li>Możesz grać jako gość lub założyć konto.</li>
            <li>Użytkownik odpowiada za bezpieczeństwo danych logowania.</li>
            <li>Administrator może ograniczyć dostęp w razie naruszeń.</li>
          </ul>

          <h2>Zasady gry i uczciwość</h2>
          <ul>
            <li>Zakaz oszustw, wykorzystywania błędów i automatyzacji gry.</li>
            <li>Zakaz działań utrudniających innym korzystanie z aplikacji.</li>
          </ul>

          <h2>Dane i treści</h2>
          <p>
            Dane rozgrywek oraz statystyki są przetwarzane w celu zapewnienia
            funkcjonalności, rankingów i bezpieczeństwa.
          </p>

          <h2>Odpowiedzialność</h2>
          <p>
            Aplikacja udostępniana jest „tak jak jest". Administrator nie ponosi
            odpowiedzialności za przerwy w działaniu, utratę danych lub szkody
            wynikłe z korzystania z aplikacji, w najszerszym zakresie dozwolonym
            przez prawo.
          </p>

          <h2>Zmiany regulaminu</h2>
          <p>
            Postanowienia mogą ulegać zmianom wraz z rozwojem aplikacji. O
            istotnych zmianach użytkownicy zostaną poinformowani w aplikacji.
          </p>

          <h2>Kontakt</h2>
          <p>W sprawach związanych z regulaminem skontaktuj się z administratorem projektu.</p>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./legal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TermsOfServiceComponent {}


