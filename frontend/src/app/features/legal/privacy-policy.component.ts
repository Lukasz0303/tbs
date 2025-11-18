import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-privacy-policy',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="legal-page-container">
      <div class="legal-content">
        <div class="legal-header">
          <h1>Polityka prywatności</h1>
          <a routerLink="/" class="back-link">
            <span class="pi pi-arrow-left"></span>
            <span>Powrót</span>
          </a>
        </div>
        <div class="legal-body">
          <p>
            Niniejsza polityka określa zasady przetwarzania danych w aplikacji
            World at War: Turn‑Based Strategy. Korzystając z aplikacji, akceptujesz
            poniższe zasady.
          </p>

          <h2>Zakres przetwarzanych danych</h2>
          <ul>
            <li>Identyfikator użytkownika (gość lub konto zarejestrowane)</li>
            <li>Adres e‑mail dla kont zarejestrowanych</li>
            <li>Adres IP w celach bezpieczeństwa i ograniczenia nadużyć</li>
            <li>Dane rozgrywek i statystyki (ranking, wyniki, ruchy)</li>
          </ul>

          <h2>Cel przetwarzania</h2>
          <ul>
            <li>Umożliwienie rozgrywki oraz utrzymanie rankingów</li>
            <li>Zapewnienie bezpieczeństwa i stabilności działania</li>
            <li>Poprawa jakości usług oraz diagnostyka błędów</li>
          </ul>

          <h2>Podstawy prawne</h2>
          <p>
            Dane przetwarzamy na podstawie uzasadnionego interesu administratora
            (art. 6 ust. 1 lit. f RODO) oraz – w przypadku kont – na podstawie
            umowy o świadczenie usług (art. 6 ust. 1 lit. b RODO).
          </p>

          <h2>Okres przechowywania</h2>
          <p>
            Dane przechowujemy przez okres korzystania z aplikacji, a następnie
            przez czas niezbędny do realizacji celów statystycznych i
            bezpieczeństwa.
          </p>

          <h2>Odbiorcy danych</h2>
          <p>
            Dane mogą być przetwarzane przez dostawców infrastruktury niezbędnej do
            działania aplikacji (hosting, baza danych, analityka, dostawcy logowania).
          </p>

          <h2>Prawa użytkownika</h2>
          <ul>
            <li>Dostęp do danych, ich sprostowanie, usunięcie, ograniczenie</li>
            <li>Sprzeciw wobec przetwarzania</li>
            <li>Przenoszenie danych</li>
          </ul>

          <h2>Kontakt</h2>
          <p>W sprawach prywatności skontaktuj się z administratorem projektu.</p>
        </div>
      </div>
    </div>
  `,
  styleUrls: ['./legal.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivacyPolicyComponent {}


