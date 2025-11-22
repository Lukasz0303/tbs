Przeprowadź szczegółowy code review zmian w kodzie frontendu na aktualnym branchu względem main. Jesteś wybitnym programistą Angular z wieloletnim doświadczeniem w TypeScript i starasz się jak najlepiej zaimplementować daną funkcjonalność.

Przeanalizuj zmiany zgodnie z najlepszymi praktykami Angular i TypeScript, skupiając się na:

1. **Jakość kodu i architektura**
   - Zgodność z zasadami clean code (early returns, guard clauses, unikanie else)
   - Prawidłowe użycie wzorców projektowych Angular (Components, Services, Directives, Pipes)
   - Struktura modułów i podział odpowiedzialności
   - Zgodność z SOLID principles
   - Feature-based architecture (features/components/services)

2. **Angular 17 best practices**
   - Standalone components zamiast NgModules gdzie możliwe
   - Konstruktorowa dependency injection zamiast property injection
   - OnPush change detection strategy dla lepszej wydajności
   - Reactive forms zamiast template-driven forms
   - Użycie RxJS operators (map, filter, switchMap, catchError) zamiast nested subscriptions
   - Proper unsubscription (takeUntilDestroyed, DestroyRef)
   - Lazy loading modułów i komponentów
   - TrackBy functions dla *ngFor
   - Async pipe zamiast manualnych subscriptions w template

3. **TypeScript 5 best practices**
   - Strict mode i type safety
   - Użycie type guards i type narrowing
   - Unikanie any, preferowanie unknown
   - Readonly dla immutable data
   - Proper use of interfaces vs types
   - Generic types gdzie odpowiednie
   - Optional chaining i nullish coalescing

4. **PrimeNG i UI/UX**
   - Spójność z motywem Verona (https://verona.primeng.org/)
   - Prawidłowe użycie komponentów PrimeNG
   - Responsywność i mobile-first approach
   - Accessibility (ARIA labels, keyboard navigation)
   - Angular Animations dla płynnych przejść
   - Konsystentne użycie stylów i komponentów

5. **SCSS i stylowanie**
   - Organizacja stylów (BEM, SMACSS lub podobne)
   - Unikanie głębokiego zagnieżdżenia (max 3-4 poziomy)
   - Użycie zmiennych SCSS dla kolorów, rozmiarów, breakpoints
   - Mobile-first responsive design
   - Unikanie !important
   - Proper use of mixins i functions

6. **Obsługa błędów i edge cases**
   - Sprawdzanie błędów na początku funkcji
   - Error handling w RxJS streams (catchError)
   - User-friendly komunikaty błędów
   - Loading states i error states w UI
   - Walidacja formularzy (reactive forms validators)
   - Handling null/undefined values

7. **Wydajność**
   - OnPush change detection strategy
   - Lazy loading i code splitting
   - Unikanie memory leaks (proper unsubscription)
   - Optymalizacja bundle size (tree shaking)
   - Virtual scrolling dla długich list
   - Debounce/throttle dla event handlers
   - Image optimization i lazy loading

8. **Bezpieczeństwo**
   - Sanityzacja danych użytkownika (DomSanitizer)
   - Ochrona przed XSS
   - Secure HTTP headers
   - Walidacja wszystkich danych wejściowych
   - Proper handling of sensitive data (tokens, credentials)

9. **Testy**
   - Pokrycie testami jednostkowymi (Jest + Angular Testing Library)
   - Testowanie komponentów, serwisów, pipes, directives
   - Testowanie edge cases i error scenarios
   - E2E testy (Cypress) dla krytycznych user journeys
   - Mockowanie zależności
   - Testowanie accessibility

10. **ESLint i Prettier**
    - Zgodność z regułami ESLint
    - Spójne formatowanie kodu
    - Brak warningów i błędów lintera

11. **Struktura projektu**
    - Prawidłowa organizacja plików zgodnie z feature-based architecture
    - Separacja concerns (components/services/features)
    - Reużywalne komponenty w ./components
    - Feature modules w ./features
    - Services w ./services

Przeanalizuj wszystkie zmiany w kodzie TypeScript, HTML i SCSS na aktualnym branchu i przygotuj szczegółowy raport z:
- Listą znalezionych problemów z priorytetami (krytyczny/wysoki/średni/niski)
- Sugestiami poprawek z przykładami kodu
- Dodatkowymi wskazówkami dotyczącymi najlepszych praktyk
- Pozytywnymi aspektami implementacji

Skoncentruj się na konstruktywnej krytyce, która pomoże ulepszyć kod zgodnie z najwyższymi standardami branżowymi.

---

### Format i miejsce raportu

**WAŻNE:** Wszystkie raporty code review muszą być zapisane w katalogu `reports/` w głównym katalogu projektu.

- Raport przygotuj w formacie identycznym jak w pliku `reports/frontend-pr-review-report.md`.
- Gotowy dokument zapisz **zawsze** w katalogu `reports/` (ścieżka: `reports/frontend-pr-review-report.md`).
- Nazwa pliku: `reports/frontend-pr-review-report.md` (lub z datą/git-hash jeśli potrzebne).

**Format raportu:**
1. **Nagłówek z kontekstem:**
   - Branch, commit, data, zakres zmian

2. **Checklista błędów na górze:**
   - Używaj ikon: `[❌🔴]` dla niewykonanych błędów krytycznych
   - `[❌🟠]` dla wysokich, `[❌🟡]` dla średnich, `[❌🔵]` dla niskich
   - `[✅🔴]` dla wykonanych (po naprawie zmień ❌ na ✅)
   - Legenda wyjaśniająca ikony

3. **Szczegółowy opis błędów:**
   - Każdy błąd ma nagłówek: `[❌🔴] #1. KRYTYCZNY - Tytuł`
   - Lokalizacja: ścieżka pliku i linie
   - Problem: opis problemu
   - Aktualny kod: blok kodu w formacie ```typescript lub ```scss
   - Rozwiązanie: bloki kodu w formacie ```typescript, ```html, ```scss

4. **Priorytety:**
   - 🔴 Krytyczny - napraw natychmiast (błędy runtime, security, data loss)
   - 🟠 Wysoki - wpływ na wydajność/stabilność (memory leaks, race conditions)
   - 🟡 Średni - kwestie UX/konfiguracji (responsywność, accessibility)
   - 🔵 Niski - refaktoryzacja, optymalizacja kodu

5. **Zasady:**
   - Tylko błędy do naprawy (bez pozytywnych aspektów)
   - Czytelny format tekstowy, bez nadmiernego formatowania Markdown
   - Bloki kodu z odpowiednimi językami (```typescript, ```html, ```scss)
   - Konkretne rozwiązania z przykładami kodu

6. **Podsumowanie priorytetów:**
   - Sekcja na końcu z priorytetami naprawy

