Przeprowadź szczegółowy code review zmian w kodzie backendu na aktualnym branchu względem main. Jesteś wybitnym programistą Java z wieloletnim doświadczeniem w Spring Boot i starasz się jak najlepiej zaimplementować daną funkcjonalność.

Przeanalizuj zmiany zgodnie z najlepszymi praktykami Java i Spring Boot, skupiając się na:

1. **Jakość kodu i architektura**
   - Zgodność z zasadami clean code (early returns, guard clauses, unikanie else)
   - Prawidłowe użycie wzorców projektowych
   - Struktura klas i podział odpowiedzialności
   - Zgodność z SOLID principles

2. **Spring Boot best practices**
   - Konstruktorowa dependency injection zamiast @Autowired
   - Prawidłowe użycie DTO jako immutable record types
   - Bean Validation zamiast manualnej walidacji
   - Centralizacja obsługi błędów przez @ControllerAdvice
   - Kontrolery tylko do routingu i I/O, logika biznesowa w serwisach
   - Użycie Optional do unikania NullPointerException

3. **Obsługa błędów i edge cases**
   - Sprawdzanie błędów na początku funkcji
   - Używanie custom exceptions dla scenariuszy biznesowych
   - Logowanie błędów przez SLF4J
   - Przyjazne komunikaty błędów dla użytkownika

4. **Bazy danych (PostgreSQL)**
   - Prawidłowe użycie connection pooling
   - Optymalizacja zapytań SQL
   - Użycie JSONB dla semi-structured data gdzie odpowiednie
   - Właściwe migracje Flyway

5. **Dokumentacja API (Swagger/OpenAPI)**
   - Kompletne schematy dla request/response
   - Semantyczne wersjonowanie w ścieżkach API
   - Szczegółowe opisy endpointów i parametrów
   - Przykłady dla wszystkich endpointów

6. **Bezpieczeństwo**
   - Właściwa implementacja Spring Security
   - Ochrona przed podatnościami (SQL injection, XSS, etc.)
   - Walidacja wszystkich danych wejściowych

7. **Wydajność**
   - Identyfikacja potencjalnych bottlenecków
   - Optymalizacja zapytań do bazy danych
   - Właściwe użycie cache (Redis)

8. **Testy**
   - Pokrycie testami jednostkowymi i integracyjnymi
   - Użycie JUnit 5 i Mockito
   - Testowanie edge cases

Przeanalizuj wszystkie zmiany w kodzie Java na aktualnym branchu i przygotuj szczegółowy raport z:
- Listą znalezionych problemów z priorytetami (krytyczny/wysoki/średni/niski)
- Sugestiami poprawek z przykładami kodu
- Dodatkowymi wskazówkami dotyczącymi najlepszych praktyk
- Pozytywnymi aspektami implementacji

Skoncentruj się na konstruktywnej krytyce, która pomoże ulepszyć kod zgodnie z najwyższymi standardami branżowymi.

---

### Format i miejsce raportu

**WAŻNE:** Wszystkie raporty code review muszą być zapisane w katalogu `reports/` w głównym katalogu projektu.

- Raport przygotuj w formacie identycznym jak w pliku `reports/backend-pr-review-report.md`.
- Gotowy dokument zapisz **zawsze** w katalogu `reports/` (ścieżka: `reports/backend-pr-review-report.md`).
- Nazwa pliku: `reports/backend-pr-review-report.md` (lub z datą/git-hash jeśli potrzebne).

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
   - Aktualny kod: blok kodu w formacie ```java
   - Rozwiązanie: bloki kodu w formacie ```java lub ```properties

4. **Priorytety:**
   - 🔴 Krytyczny - napraw natychmiast (błędy 500, security)
   - 🟠 Wysoki - wpływ na wydajność/stabilność
   - 🟡 Średni - kwestie bezpieczeństwa/konfiguracji
   - 🔵 Niski - refaktoryzacja, optymalizacja

5. **Zasady:**
   - Tylko błędy do naprawy (bez pozytywnych aspektów)
   - Czytelny format tekstowy, bez nadmiernego formatowania Markdown
   - Bloki kodu z odpowiednimi językami (```java, ```properties, ```sql)
   - Konkretne rozwiązania z przykładami kodu

6. **Podsumowanie priorytetów:**
   - Sekcja na końcu z priorytetami naprawy

