import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-privacy-policy',
  standalone: true,
  template: `
    <section class="fixed inset-0 z-40 flex items-center justify-center overflow-auto bg-black/60 backdrop-blur-sm p-4 sm:p-6 md:p-8">
      <div class="mx-auto max-w-3xl w-full rounded-2xl border border-white/10 bg-slate-900/80 text-slate-100 shadow-2xl">
        <div class="flex items-center justify-between gap-4 border-b border-white/10 px-6 py-4">
          <h1 class="text-xl sm:text-2xl font-semibold">Polityka prywatności</h1>
          <button (click)="closeModal()" class="rounded-md border border-white/20 bg-white/10 px-3 py-1 text-sm text-slate-100 hover:bg-white/20">Zamknij</button>
        </div>
        <div class="px-6 py-6 space-y-4 leading-relaxed">
          <p class="text-slate-200">
            Niniejsza polityka określa zasady przetwarzania danych w aplikacji
            World at War: Turn‑Based Strategy. Korzystając z aplikacji, akceptujesz
            poniższe zasady.
          </p>

          <h2 class="mt-4 text-lg font-semibold text-white">Zakres przetwarzanych danych</h2>
          <ul class="list-disc pl-6 space-y-1 text-slate-300">
            <li>Identyfikator użytkownika (gość lub konto zarejestrowane)</li>
            <li>Adres e‑mail dla kont zarejestrowanych</li>
            <li>Adres IP w celach bezpieczeństwa i ograniczenia nadużyć</li>
            <li>Dane rozgrywek i statystyki (ranking, wyniki, ruchy)</li>
          </ul>

          <h2 class="mt-4 text-lg font-semibold text-white">Cel przetwarzania</h2>
          <ul class="list-disc pl-6 space-y-1 text-slate-300">
            <li>Umożliwienie rozgrywki oraz utrzymanie rankingów</li>
            <li>Zapewnienie bezpieczeństwa i stabilności działania</li>
            <li>Poprawa jakości usług oraz diagnostyka błędów</li>
          </ul>

          <h2 class="mt-4 text-lg font-semibold text-white">Podstawy prawne</h2>
          <p class="text-slate-300">
            Dane przetwarzamy na podstawie uzasadnionego interesu administratora
            (art. 6 ust. 1 lit. f RODO) oraz – w przypadku kont – na podstawie
            umowy o świadczenie usług (art. 6 ust. 1 lit. b RODO).
          </p>

          <h2 class="mt-4 text-lg font-semibold text-white">Okres przechowywania</h2>
          <p class="text-slate-300">
            Dane przechowujemy przez okres korzystania z aplikacji, a następnie
            przez czas niezbędny do realizacji celów statystycznych i
            bezpieczeństwa.
          </p>

          <h2 class="mt-4 text-lg font-semibold text-white">Odbiorcy danych</h2>
          <p class="text-slate-300">
            Dane mogą być przetwarzane przez dostawców infrastruktury niezbędnej do
            działania aplikacji (hosting, baza danych, analityka, dostawcy logowania).
          </p>

          <h2 class="mt-4 text-lg font-semibold text-white">Prawa użytkownika</h2>
          <ul class="list-disc pl-6 space-y-1 text-slate-300">
            <li>Dostęp do danych, ich sprostowanie, usunięcie, ograniczenie</li>
            <li>Sprzeciw wobec przetwarzania</li>
            <li>Przenoszenie danych</li>
          </ul>

          <h2 class="mt-4 text-lg font-semibold text-white">Kontakt</h2>
          <p class="text-slate-300">W sprawach prywatności skontaktuj się z administratorem projektu.</p>
        </div>
      </div>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PrivacyPolicyComponent {
  private readonly router = inject(Router);

  closeModal(): void {
    this.router.navigateByUrl('/');
  }
}


