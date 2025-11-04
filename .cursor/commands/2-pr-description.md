Wygeneruj szczegółowy, profesjonalny opis Pull Requesta po angielsku na podstawie wszystkich zmian na bieżącej gałęzi względem main. Opis powinien być gotowy do wklejenia do formularza opisu PR na GitHub.

Format opisu powinien być zgodny z przykładem z pliku `.cursor/examples/pr-description-example.md` i zawierać następujące sekcje:

1. **Tytuł** - w formacie konwencjonalnego commita (feat:, fix:, refactor:, etc.) z krótkim opisem
2. **📋 Overview** - krótki opis głównego celu PR (2-3 zdania)
3. **✨ What's Changed** - główne sekcje zmian:
   - **New Features** - lista nowych funkcjonalności z krótkim opisem
   - **Core Components** - podział na:
     - **New Services** - lista nowych serwisów z opisem
     - **New Controllers** - lista nowych kontrolerów z endpointami
     - **New Exceptions** - lista nowych wyjątków
     - **New DTOs/Models** - jeśli są nowe
   - **Improvements** - podział na kategorie:
     - **Database & Performance** - optymalizacje, zmiany w bazie
     - **Error Handling** - poprawki w obsłudze błędów
     - **Security & Validation** - poprawki bezpieczeństwa
     - **Code Quality** - refaktoring, poprawki jakości kodu
4. **🧪 Testing** - informacje o testach (dodane testy, skrypty testowe)
5. **🗄️ Database Changes** - zmiany w bazie danych (migracje, zmiany w repository)
6. **📦 Files Changed** - statystyki zmian (liczba plików, insertions, deletions)
7. **🔍 Migration Notes** - informacje o migracjach lub ich braku
8. **🔄 Breaking Changes** - jeśli są breaking changes, w przeciwnym razie "None"
9. **✅ Checklist** - opcjonalna lista checkboxów dla reviewera

Opis powinien być:
- Krótki i zwięzły
- Z ikonami emoji dla lepszej czytelności
- Z wyróżnieniem głównych zmian, nowych funkcji, poprawków i breaking changes
- Z jasnym opisem kontekstu i celu PR

Po wpisaniu tej komendy, AI automatycznie przygotuje szczegółowy opis PR — nie musisz samodzielnie uruchamiać żadnych dodatkowych poleceń w terminalu.

Po wygenerowaniu opisu AI zapyta, czy opis jest odpowiedni. Jeśli zaakceptujesz – AI przygotuje komendę do commita. Jeśli nie – będziesz mógł wybrać opcję dodatkowego skrócenia lub dostosowania opisu.