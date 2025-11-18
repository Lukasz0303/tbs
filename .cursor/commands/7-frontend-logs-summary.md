Przeanalizuj logi frontendu z wklejonego kontekstu (logi wklejone przez użytkownika) i wyciągnij z nich **najważniejsze informacje diagnostyczne**, koncentrując się na **ostatnim uruchomieniu** aplikacji.

### Zakres analizy
- Skup się przede wszystkim na:
  - Błędach kompilacji Angular (`NG…`, `TS…`, `plugin angular-compiler`, `Application bundle generation failed`)
  - Błędach TypeScript/HTML powiązanych z konkretnymi komponentami (np. `QueueStatusComponent`, `MatchmakingComponent`)
  - Ostrzeżeniach i błędach z `npm`/`node`, które mogą blokować uruchomienie frontendu
- Jeśli w logu są wpisy z wielu uruchomień:
  - Traktuj **najświeższy blok logów** jako główne źródło prawdy
  - Wspomnij o wcześniejszych problemach tylko wtedy, gdy pokazują **inny typ błędu** niż ostatni

### Co masz wyciągnąć z logów
1. **Status uruchomienia frontendu**
   - Czy build zakończył się sukcesem, czy porażką?
   - Jeśli porażką – wskaż **główną przyczynę** (np. błąd w konkretnym komponencie/serwisie)

2. **Krytyczne błędy (blokujące build lub start)**
   - Zidentyfikuj wszystkie błędy oznaczone jako:
     - `X [ERROR] NG…`
     - `TS….` z `plugin angular-compiler`
     - Błędy w szablonach komponentów (HTML) powiązane z TypeScript (np. "Property 'translateService' is private…")
   - Dla każdego błędu podaj:
     - **Krótki tytuł** (1 zdanie opisujące problem biznesowo/technicznie, np. "Szablon komponentu odwołuje się do prywatnego serwisu tłumaczeń")
     - **Ścieżkę do pliku** i **przybliżoną lokalizację** (komponent + plik `.ts`/`.html`)
     - **Istotę problemu** jednym–dwoma zdaniami (co dokładnie jest nie tak i dlaczego Angular się wywala)

3. **Istotne ostrzeżenia**
   - Wyłap ostrzeżenia, które:
     - Mogą wkrótce stać się błędami (np. deprecations, ostrzeżenia npm o przyszłych zmianach)
     - Mogą wpływać na stabilność środowiska (np. problemy z konfiguracją NPM/Node)
   - Pomiń drobne ostrzeżenia, które nie mają wpływu na działanie aplikacji, jeśli nie są kluczowe

4. **Propozycje naprawy**
   - Dla **każdego krytycznego błędu** zaproponuj 1–3 **konkretnych kroków naprawczych**, np.:
     - Zmiana modyfikatora (`private` → `public`), jeśli pole jest używane w template
     - Dostosowanie typów w strumieniach RxJS (doprecyzowanie typu zamiast `unknown`)
     - Poprawa importów (`rxjs` operators), jeśli log jasno pokazuje problem
   - Uszereguj sugestie według priorytetu:
     - Najpierw to, co **blokuje build**
     - Potem ostrzeżenia, które mogą być odłożone na później

### Format odpowiedzi
Przygotuj odpowiedź w następującej strukturze:

1. **📋 Podsumowanie**
   - 2–4 zdania opisujące ogólny stan: czy frontend się buduje, jakie są główne problemy, które komponenty są najbardziej problematyczne.

2. **❌ Krytyczne błędy**
   - Wypunktowana lista:
     - **[Krótki tytuł błędu]** – komponent/plik (np. `queue-status.component.html` / `QueueStatusComponent`)
       - Opis istoty problemu (1–2 zdania)

3. **⚠️ Istotne ostrzeżenia**
   - Wypunktowana lista tylko tych ostrzeżeń, które faktycznie mogą mieć wpływ na działanie lub przyszłe buildy.

4. **🛠 Plan naprawy**
   - Lista kroków w kolejności, w jakiej programista powinien je wykonać, np.:
     - **Krok 1**: Popraw widoczność `translateService` w `QueueStatusComponent` i `MatchmakingComponent`, tak aby szablon miał do niego dostęp.
     - **Krok 2**: Zaktualizuj użycie `rxjs`, aby nie odwoływać się do operatorów/funkcji, których moduł nie eksportuje.
     - **Krok 3**: Doprecyzuj typy w miejscach, gdzie kompilator raportuje `unknown`.

### Zasady prezentacji
- **Nie wklejaj pełnych bloków logów** – cytuj tylko najważniejsze linie w skróconej formie, jeśli są potrzebne do zrozumienia kontekstu.
- Skup się na tym, co **blokuje pracę** (build/start frontendu), a nie na każdym pojedynczym wpisie.
- Jeśli w logu **nie ma błędów blokujących**, powiedz to wprost i tylko krótko wymień ewentualne ostrzeżenia.

Po uruchomieniu tej komendy oczekuje się od AI **zwięzłego, praktycznego raportu**, który pomaga szybko zrozumieć, dlaczego frontend się nie buduje lub jakie problemy należy naprawić w pierwszej kolejności.


