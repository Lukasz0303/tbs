import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-terms-of-service',
  standalone: true,
  template: `
    <section class="fixed inset-0 z-40 flex items-center justify-center overflow-auto bg-black/60 backdrop-blur-sm p-4 sm:p-6 md:p-8">
      <div class="mx-auto max-w-3xl w-full rounded-2xl border border-white/10 bg-slate-900/80 text-slate-100 shadow-2xl">
        <div class="flex items-center justify-between gap-4 border-b border-white/10 px-6 py-4">
          <h1 class="text-xl sm:text-2xl font-semibold">Regulamin</h1>
          <button (click)="closeModal()" class="rounded-md border border-white/20 bg-white/10 px-3 py-1 text-sm text-slate-100 hover:bg-white/20">Zamknij</button>
        </div>
        <div class="px-6 py-6 space-y-4 leading-relaxed">
          <p class="text-slate-200">
            Regulamin określa zasady korzystania z aplikacji World at War:
            Turn‑Based Strategy. Korzystając z aplikacji, akceptujesz poniższe
            postanowienia.
          </p>

          <h2 class="mt-4 text-lg font-semibold text-white">Konto i dostęp</h2>
          <ul class="list-disc pl-6 space-y-1 text-slate-300">
            <li>Możesz grać jako gość lub założyć konto.</li>
            <li>Użytkownik odpowiada za bezpieczeństwo danych logowania.</li>
            <li>Administrator może ograniczyć dostęp w razie naruszeń.</li>
          </ul>

          <h2 class="mt-4 text-lg font-semibold text-white">Zasady gry i uczciwość</h2>
          <ul class="list-disc pl-6 space-y-1 text-slate-300">
            <li>Zakaz oszustw, wykorzystywania błędów i automatyzacji gry.</li>
            <li>Zakaz działań utrudniających innym korzystanie z aplikacji.</li>
          </ul>

          <h2 class="mt-4 text-lg font-semibold text-white">Dane i treści</h2>
          <p class="text-slate-300">
            Dane rozgrywek oraz statystyki są przetwarzane w celu zapewnienia
            funkcjonalności, rankingów i bezpieczeństwa.
          </p>

          <h2 class="mt-4 text-lg font-semibold text-white">Odpowiedzialność</h2>
          <p class="text-slate-300">
            Aplikacja udostępniana jest „tak jak jest”. Administrator nie ponosi
            odpowiedzialności za przerwy w działaniu, utratę danych lub szkody
            wynikłe z korzystania z aplikacji, w najszerszym zakresie dozwolonym
            przez prawo.
          </p>

          <h2 class="mt-4 text-lg font-semibold text-white">Zmiany regulaminu</h2>
          <p class="text-slate-300">
            Postanowienia mogą ulegać zmianom wraz z rozwojem aplikacji. O
            istotnych zmianach użytkownicy zostaną poinformowani w aplikacji.
          </p>

          <h2 class="mt-4 text-lg font-semibold text-white">Kontakt</h2>
          <p class="text-slate-300">W sprawach związanych z regulaminem skontaktuj się z administratorem projektu.</p>
        </div>
      </div>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TermsOfServiceComponent {
  private readonly router = inject(Router);

  closeModal(): void {
    this.router.navigateByUrl('/');
  }
}


