# Plan implementacji widoku NotFoundComponent

> **Źródło**: `.ai/implementation-plans-ui/09_not-found-component.md`

## 1. Przegląd

NotFoundComponent to widok wyświetlający komunikat o błędzie 404, gdy użytkownik próbuje uzyskać dostęp do nieistniejącej strony w aplikacji. Komponent zapewnia przyjazny komunikat błędu oraz łatwą nawigację z powrotem do głównych sekcji aplikacji (strona główna, ranking, profil).

Komponent jest widokiem czysto prezentacyjnym, który nie wymaga integracji z API backendu. Jego głównym celem jest zapewnienie pozytywnego doświadczenia użytkownika w sytuacji, gdy żądana strona nie istnieje.

Główne funkcjonalności:
- Wyświetlanie komunikatu o błędzie 404
- Przyjazny komunikat dla użytkownika
- Nawigacja do głównych sekcji aplikacji (strona główna, ranking)
- Responsywny design dostosowany do różnych rozdzielczości ekranu
- Wsparcie dla wielu języków (i18n): angielski (podstawowy), polski (dodatkowy)
- Animacje fade-in dla płynnego wyświetlania
- Pełna dostępność (ARIA labels, keyboard navigation, screen reader support)

Komponent obsługuje przypadki brzegowe związane z nieprawidłowymi ścieżkami URL w aplikacji.

## 2. Routing widoku

**Ścieżka routingu**: `**` (catch-all route)

**Konfiguracja routingu**:
```typescript
{
  path: '',
  component: MainLayoutComponent,
  children: [
    { path: '', component: HomeComponent },
    { path: 'auth', component: AuthComponent },
    { path: 'game', component: GameDashboardComponent },
    { path: 'leaderboard', component: LeaderboardComponent },
    { path: '**', component: NotFoundComponent },
  ],
}
```

**Lokalizacja pliku routingu**: `frontend/src/app/app.routes.ts`

**Guardy**: Brak (widok publiczny, dostępny dla wszystkich)

**Uwagi**: 
- Komponent jest umieszczony jako ostatnia reguła routingu (`**`) w celu przechwycenia wszystkich niepasujących ścieżek
- Komponent jest renderowany wewnątrz `MainLayoutComponent`, więc będzie miał dostęp do nawigacji i layoutu głównego

## 3. Struktura komponentów

```
NotFoundComponent (główny komponent)
├── Kontener główny (.not-found-container)
├── Zawartość (.not-found-content)
│   ├── Ikona błędu (.not-found-icon)
│   ├── Nagłówek 404 (h1)
│   ├── Tytuł błędu (h2)
│   ├── Komunikat (p)
│   ├── Sekcja przycisków (.not-found-actions)
│   │   ├── ButtonModule (PrimeNG - przycisk "Powrót do strony głównej")
│   │   └── ButtonModule (PrimeNG - przycisk "Ranking")
│   └── Sekcja linków (.not-found-links)
│       ├── RouterLink (Strona główna)
│       ├── RouterLink (Ranking)
│       └── RouterLink (Profil) - opcjonalny
└── Angular Animations (fade-in, pulse)
```

**Hierarchia komponentów**:
- NotFoundComponent jest komponentem standalone
- Komponent używa PrimeNG ButtonModule do przycisków
- Komponent używa Angular Router do nawigacji
- Komponent używa Angular Animations do animacji
- Komponent jest renderowany wewnątrz MainLayoutComponent

## 4. Szczegóły komponentów

### NotFoundComponent

**Opis komponentu**: Główny komponent widoku 404, wyświetlający komunikat o błędzie i zapewniający nawigację do głównych sekcji aplikacji. Komponent jest czysto prezentacyjny i nie wymaga integracji z API backendu. Zarządza nawigacją do strony głównej, rankingu i innych sekcji aplikacji.

**Główne elementy HTML**:
- Kontener główny (`.not-found-container`) - flexbox container z centrowaniem
- Zawartość (`.not-found-content`) - główny kontener z zawartością, max-width 600px
- Ikona błędu (`.not-found-icon`) - ikona wykrzyknika (można użyć SVG lub ikony tekstowej)
- Nagłówek 404 (`<h1>404</h1>`) - duży numer błędu
- Tytuł błędu (`<h2>Strona nie została znaleziona</h2>`) - tytuł komunikatu
- Komunikat (`<p>Przepraszamy, strona której szukasz nie istnieje lub została przeniesiona.</p>`) - opis błędu
- Sekcja przycisków (`.not-found-actions`) - kontener z przyciskami akcji
  - PrimeNG Button (label="Powrót do strony głównej") - przycisk "Powrót do strony głównej"
  - PrimeNG Button (label="Ranking", severity="secondary") - przycisk "Ranking"
- Sekcja linków (`.not-found-links`) - kontener z linkami nawigacyjnymi
  - RouterLink do `/` - link do strony głównej
  - RouterLink do `/leaderboard` - link do rankingu
  - RouterLink do `/profile` - link do profilu (opcjonalny, jeśli istnieje)

**Obsługiwane zdarzenia**:
- `ngOnInit()` - inicjalizacja komponentu (opcjonalnie, do logowania lub analityki)
- `navigateToHome()` - obsługa kliknięcia przycisku "Powrót do strony głównej", nawigacja do `/`
- `navigateToLeaderboard()` - obsługa kliknięcia przycisku "Ranking", nawigacja do `/leaderboard`
- `navigateToProfile()` - obsługa kliknięcia linku "Profil", nawigacja do `/profile` (opcjonalne)

**Obsługiwana walidacja**:
- Brak walidacji formularzy (komponent nie zawiera formularzy)
- Walidacja routingu (sprawdzenie czy route istnieje przed nawigacją - automatycznie obsługiwane przez Angular Router)
- Sprawdzenie czy użytkownik jest zalogowany (opcjonalnie, dla linku do profilu)

**Typy**:
- Brak niestandardowych typów DTO (komponent nie komunikuje się z API)
- `Router` - Angular Router do nawigacji
- `Observable<void>` - dla operacji nawigacji (opcjonalnie)

**Propsy**: Brak (komponent główny, nie przyjmuje propsów)

## 5. Typy

Komponent nie wymaga żadnych niestandardowych typów DTO ani ViewModel, ponieważ jest komponentem czysto prezentacyjnym bez integracji z API backendu.

**Używane typy Angular**:
- `Router` - Angular Router service
- `void` - typ zwracany przez metody nawigacji

**Uwagi**: 
- Komponent nie potrzebuje typów do komunikacji z backendem
- Wszystkie dane wyświetlane w komponencie są statyczne (teksty, komunikaty)

## 6. Zarządzanie stanem

Komponent nie wymaga zarządzania stanem, ponieważ:
- Nie pobiera danych z API
- Nie przechowuje żadnych danych użytkownika
- Nie wymaga reakcji na zmiany stanu aplikacji
- Wszystkie dane są statyczne (teksty, komunikaty)

**Brak customowych hooków**: Komponent nie wymaga żadnych customowych hooków ani serwisów do zarządzania stanem.

**Ewentualne użycie serwisów**:
- `Router` - do nawigacji (wstrzykiwany przez dependency injection)
- `TranslateService` - do obsługi i18n (jeśli używany jest Angular i18n lub ngx-translate)

**Change Detection Strategy**: 
- Zalecane użycie `ChangeDetectionStrategy.OnPush` dla lepszej wydajności (komponent nie wymaga ciągłej aktualizacji)

## 7. Integracja API

**Brak integracji API**: Komponent nie wymaga integracji z żadnymi endpointami API backendu, ponieważ jest komponentem czysto prezentacyjnym.

**Uwagi**:
- Komponent nie wykonuje żadnych wywołań HTTP
- Komponent nie wymaga autoryzacji ani uwierzytelnienia
- Komponent nie pobiera żadnych danych z serwera
- Komponent nie wysyła żadnych danych do serwera

**Potencjalne przyszłe rozszerzenia** (poza zakresem MVP):
- Logowanie prób dostępu do nieistniejących stron (analytics)
- Sugerowanie podobnych stron na podstawie URL
- Wyświetlanie popularnych stron jako alternatyw

## 8. Interakcje użytkownika

### 8.1 Kliknięcie przycisku "Powrót do strony głównej"

**Akcja użytkownika**: Użytkownik klika przycisk "Powrót do strony głównej"

**Oczekiwany wynik**:
- Nawigacja do strony głównej (`/`)
- Płynne przejście z animacją fade-out/fade-in
- Załadowanie zawartości strony głównej

**Implementacja**:
```typescript
navigateToHome(): void {
  this.router.navigate(['/']);
}
```

### 8.2 Kliknięcie przycisku "Ranking"

**Akcja użytkownika**: Użytkownik klika przycisk "Ranking"

**Oczekiwany wynik**:
- Nawigacja do strony rankingu (`/leaderboard`)
- Płynne przejście z animacją fade-out/fade-in
- Załadowanie zawartości rankingu

**Implementacja**:
```typescript
navigateToLeaderboard(): void {
  this.router.navigate(['/leaderboard']);
}
```

### 8.3 Kliknięcie linku nawigacyjnego

**Akcja użytkownika**: Użytkownik klika link nawigacyjny (np. "Strona główna", "Ranking", "Profil")

**Oczekiwany wynik**:
- Nawigacja do odpowiedniej sekcji aplikacji
- Płynne przejście z animacją fade-out/fade-in
- Załadowanie zawartości wybranej sekcji

**Implementacja**:
- Użycie `routerLink` w template (automatyczna nawigacja przez Angular Router)

### 8.4 Nawigacja klawiaturą

**Akcja użytkownika**: Użytkownik nawiguje po komponencie używając klawiatury (Tab, Enter, Space)

**Oczekiwany wynik**:
- Fokus przechodzi między elementami interaktywnymi (przyciski, linki)
- Widoczne są wskaźniki fokusu (outline)
- Enter/Space aktywuje przycisk lub link
- Płynna nawigacja bez problemów z dostępnością

**Implementacja**:
- Użycie natywnych właściwości HTML dla dostępności
- Dodanie odpowiednich ARIA labels
- Stylowanie `:focus` dla wskaźników fokusu

## 9. Warunki i walidacja

Komponent nie wymaga walidacji formularzy ani danych wejściowych, ponieważ nie zawiera formularzy ani pól wejściowych.

### 9.1 Walidacja routingu

**Warunek**: Sprawdzenie czy route docelowy istnieje przed nawigacją

**Walidacja**: Automatycznie obsługiwana przez Angular Router

**Implementacja**: 
- Angular Router automatycznie sprawdza czy route istnieje
- Jeśli route nie istnieje, zostanie wyświetlony ponownie komponent NotFoundComponent (unikanie nieskończonej pętli)

**Obsługa błędów**:
- Jeśli route nie istnieje, użytkownik pozostaje na stronie 404
- Nie ma potrzeby wyświetlania dodatkowych komunikatów błędów

### 9.2 Sprawdzenie dostępności sekcji (opcjonalne)

**Warunek**: Sprawdzenie czy użytkownik ma dostęp do sekcji (np. profil wymaga logowania)

**Walidacja**: Opcjonalna walidacja przed nawigacją do sekcji wymagającej logowania

**Implementacja**:
- Sprawdzenie statusu użytkownika (gość/zarejestrowany) przed wyświetleniem linku do profilu
- Warunkowe wyświetlanie linków w zależności od statusu użytkownika

**Uwagi**: 
- W zakresie MVP można wyświetlać wszystkie linki, a obsługę przekierowania do logowania pozostawić dla komponentu docelowego
- Alternatywnie, można ukryć linki do sekcji wymagających logowania dla użytkowników gości

## 10. Obsługa błędów

Komponent nie wymaga obsługi błędów związanych z API, ponieważ nie komunikuje się z backendem.

### 10.1 Obsługa błędów routingu

**Scenariusz**: Użytkownik próbuje nawigować do nieistniejącej strony

**Obsługa**: 
- Angular Router automatycznie przekierowuje do komponentu NotFoundComponent
- Komponent wyświetla przyjazny komunikat błędu
- Komponent oferuje opcje nawigacji do głównych sekcji

**Implementacja**:
- Automatyczna obsługa przez Angular Router (catch-all route `**`)
- Komponent wyświetla statyczny komunikat błędu

### 10.2 Obsługa błędów nawigacji

**Scenariusz**: Błąd podczas nawigacji (np. problem z routerem)

**Obsługa**:
- Angular Router automatycznie obsługuje błędy nawigacji
- W przypadku błędu, użytkownik pozostaje na stronie 404
- Brak potrzeby wyświetlania dodatkowych komunikatów błędów

**Implementacja**:
- Opcjonalnie można dodać obsługę błędów w metodach nawigacji:
```typescript
navigateToHome(): void {
  this.router.navigate(['/']).catch(() => {
    console.error('Navigation error');
  });
}
```

### 10.3 Obsługa przypadków brzegowych

**Scenariusz**: Użytkownik wchodzi bezpośrednio na nieistniejącą stronę (np. przez URL)

**Obsługa**:
- Komponent wyświetla komunikat błędu
- Komponent oferuje opcje nawigacji do głównych sekcji
- Użytkownik może łatwo wrócić do aplikacji

**Scenariusz**: Użytkownik odświeża stronę na nieistniejącej ścieżce

**Obsługa**:
- Komponent wyświetla komunikat błędu
- Komponent oferuje opcje nawigacji do głównych sekcji
- Użytkownik może łatwo wrócić do aplikacji

## 11. Kroki implementacji

### Krok 1: Przygotowanie struktury komponentu

1. Utwórz plik `not-found.component.ts` w katalogu `frontend/src/app/features/not-found/`
2. Utwórz plik `not-found.component.html` w katalogu `frontend/src/app/features/not-found/`
3. Utwórz plik `not-found.component.scss` w katalogu `frontend/src/app/features/not-found/`
4. Utwórz plik `not-found.component.spec.ts` w katalogu `frontend/src/app/features/not-found/` (dla testów)

### Krok 2: Implementacja komponentu TypeScript

1. Zdefiniuj komponent jako standalone component
2. Zaimportuj wymagane moduły: `CommonModule`, `RouterModule`, `ButtonModule` (PrimeNG)
3. Wstrzyknij `Router` przez dependency injection
4. Zaimplementuj metody nawigacji: `navigateToHome()`, `navigateToLeaderboard()`
5. Ustaw `ChangeDetectionStrategy.OnPush` dla lepszej wydajności
6. Dodaj właściwości dla i18n (jeśli używany jest serwis tłumaczeń)

### Krok 3: Implementacja template HTML

1. Utwórz strukturę HTML z kontenerami i sekcjami
2. Dodaj ikonę błędu (SVG lub ikona tekstowa)
3. Dodaj nagłówek 404 i tytuł błędu
4. Dodaj komunikat błędu
5. Dodaj sekcję przycisków z PrimeNG Button
6. Dodaj sekcję linków z RouterLink
7. Dodaj ARIA labels dla dostępności
8. Dodaj atrybuty i18n dla tłumaczeń (jeśli używany jest Angular i18n)

### Krok 4: Implementacja stylów SCSS

1. Zdefiniuj style dla kontenera głównego (flexbox, centrowanie)
2. Zdefiniuj style dla zawartości (max-width, text-align)
3. Zdefiniuj style dla ikony błędu (rozmiar, kolor, animacja pulse)
4. Zdefiniuj style dla nagłówków i tekstu
5. Zdefiniuj style dla sekcji przycisków (flexbox, gap)
6. Zdefiniuj style dla sekcji linków (flexbox, gap, hover)
7. Dodaj style dla responsywności (media queries)
8. Dodaj style dla dark mode (jeśli używany)
9. Dodaj style dla animacji fade-in

### Krok 5: Implementacja animacji

1. Zdefiniuj animację fade-in dla całego komponentu
2. Zdefiniuj animację pulse dla ikony błędu
3. Dodaj smooth transitions dla przycisków i linków
4. Zintegruj animacje z Angular Animations (jeśli używane)

### Krok 6: Konfiguracja routingu

1. Sprawdź konfigurację routingu w `app.routes.ts`
2. Upewnij się, że NotFoundComponent jest zdefiniowany jako catch-all route (`**`)
3. Sprawdź, czy komponent jest renderowany wewnątrz MainLayoutComponent
4. Przetestuj routing dla różnych nieistniejących ścieżek

### Krok 7: Implementacja i18n (jeśli wymagane)

1. Dodaj klucze tłumaczeń dla wszystkich tekstów w komponencie
2. Zaimplementuj serwis tłumaczeń (jeśli używany jest ngx-translate lub Angular i18n)
3. Zaktualizuj template, aby używał kluczy tłumaczeń
4. Dodaj tłumaczenia dla języka angielskiego (podstawowy)
5. Dodaj tłumaczenia dla języka polskiego (dodatkowy)
6. Przetestuj przełączanie języków

### Krok 8: Implementacja dostępności

1. Dodaj ARIA labels dla wszystkich elementów interaktywnych
2. Dodaj role dla sekcji (np. `role="region"`, `role="navigation"`)
3. Dodaj aria-live dla komunikatu błędu
4. Zapewnij odpowiednie wskaźniki fokusu (outline)
5. Przetestuj nawigację klawiaturą
6. Przetestuj z screen readerem

### Krok 9: Testy jednostkowe

1. Utwórz testy dla komponentu (`not-found.component.spec.ts`)
2. Przetestuj wyświetlanie komponentu
3. Przetestuj nawigację do strony głównej
4. Przetestuj nawigację do rankingu
5. Przetestuj wyświetlanie linków nawigacyjnych
6. Przetestuj dostępność (ARIA labels, keyboard navigation)

### Krok 10: Testy E2E (Cypress)

1. Utwórz testy E2E dla widoku 404
2. Przetestuj scenariusz: Próba dostępu do nieistniejącej strony
3. Przetestuj scenariusz: Nawigacja z strony 404 do głównych sekcji
4. Przetestuj scenariusz: Odświeżenie strony na nieistniejącej ścieżce
5. Przetestuj responsywność na różnych rozdzielczościach ekranu

### Krok 11: Optymalizacja i refaktoryzacja

1. Sprawdź wydajność komponentu (ChangeDetectionStrategy.OnPush)
2. Zoptymalizuj animacje (użyj `will-change` jeśli potrzebne)
3. Sprawdź rozmiar bundle (tree-shaking)
4. Przetestuj na różnych przeglądarkach
5. Przetestuj responsywność na różnych urządzeniach

### Krok 12: Dokumentacja

1. Zaktualizuj dokumentację komponentu (komentarze w kodzie - tylko jeśli wymagane przez wyjątkowe przypadki)
2. Zaktualizuj README projektu (jeśli wymagane)
3. Zaktualizuj dokumentację routingu (jeśli wymagane)

## 12. Uwagi implementacyjne

### 12.1 PrimeNG Components

**Uwaga**: Projekt używa PrimeNG jako biblioteki UI. Należy użyć komponentów PrimeNG zgodnie z dokumentacją.

**Implementacja**:
- Użyj PrimeNG ButtonModule do przycisków
- Użyj PrimeNG MessageModule do komunikatów (opcjonalnie)
- Dostosuj style używając SCSS zgodnie z design systemem projektu

### 12.2 Angular Animations

**Uwaga**: Projekt używa Angular Animations dla animacji. Należy użyć `@angular/animations` zamiast CSS animations dla lepszej integracji z Angular.

**Implementacja**:
- Zdefiniuj animacje w komponencie używając `@Component({ animations: [...] })`
- Użyj `[@fadeIn]` i `[@pulse]` w template
- Zintegruj animacje z Angular Animations

### 12.3 i18n

**Uwaga**: Projekt wspiera i18n (angielski i polski), ale implementacja może różnić się w zależności od używanego rozwiązania (Angular i18n vs ngx-translate).

**Implementacja**:
- Sprawdź jakie rozwiązanie i18n jest używane w projekcie
- Dostosuj implementację do istniejącego rozwiązania
- Dodaj tłumaczenia dla wszystkich tekstów w komponencie

### 12.4 Dark Mode

**Uwaga**: Projekt wspiera dark mode. Należy zapewnić odpowiednie style dla dark mode.

**Implementacja**:
- Użyj SCSS z media queries lub PrimeNG themes dla dark mode
- Przetestuj wygląd komponentu w trybie jasnym i ciemnym
- Zapewnij odpowiedni kontrast kolorów w obu trybach

### 12.5 Responsywność

**Uwaga**: Komponent powinien być responsywny i działać dobrze na różnych rozdzielczościach ekranu.

**Implementacja**:
- Użyj SCSS z media queries dla responsywności
- Przetestuj komponent na różnych rozdzielczościach ekranu
- Zapewnij odpowiednie odstępy i rozmiary czcionek dla różnych rozdzielczościach

