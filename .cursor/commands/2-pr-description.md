Wygeneruj szczegółowy, profesjonalny opis Pull Requesta po angielsku na podstawie TYLKO faktycznych zmian w plikach na bieżącej gałęzi względem main. Jeśli nie ma commitów różniących się od main, analizuj niezacommitowane zmiany. 

WAŻNE INSTRUKCJE:
1. Analizuj TYLKO faktyczne różnice w plikach (diff), nie opisuj całej funkcjonalności projektu
2. Dla plików zmodyfikowanych (M) - sprawdź faktyczne zmiany używając git diff i opisz tylko to, co się zmieniło
3. Dla plików nowych (A) lub nieśledzonych - opisz krótko co zostało dodane, ale NIE opisuj całej implementacji klasy/serwisu
4. Skup się na zmianach, nie na istniejącej funkcjonalności
5. Jeśli plik ma 100+ linii i jest nowy, opisz go krótko (1-2 zdania), nie wymieniaj wszystkich metod

Format opisu powinien być zgodny z przykładem z pliku `.cursor/examples/pr-description-example.md` i zawierać następujące sekcje:

1. **Tytuł** - w formacie konwencjonalnego commita (feat:, fix:, refactor:, etc.) z krótkim opisem
2. **📋 Overview** - krótki opis głównego celu PR (2-3 zdania) - tylko o tym, co się zmieniło w tym PR
3. **✨ What's Changed** - główne sekcje zmian:
   - **New Features** - lista NOWYCH funkcjonalności dodanych w tym PR
   - **Core Components** - TYLKO nowe/zmodyfikowane komponenty:
     - **New Services** - tylko nowe serwisy lub znaczące zmiany w istniejących
     - **New Controllers** - tylko nowe kontrolery lub zmiany w endpointach
     - **New Exceptions** - tylko nowe wyjątki
     - **New DTOs/Models** - tylko nowe lub zmodyfikowane
   - **Improvements** - TYLKO poprawki i zmiany:
     - **Database & Performance** - tylko faktyczne optymalizacje i zmiany
     - **Error Handling** - tylko zmiany w obsłudze błędów
     - **Security & Validation** - tylko zmiany bezpieczeństwa
     - **Code Quality** - tylko refaktoring i poprawki
4. **🧪 Testing** - tylko nowe testy lub zmiany w testach
5. **🗄️ Database Changes** - tylko nowe migracje lub zmiany w repository
6. **📦 Files Changed** - statystyki zmian (liczba plików, insertions, deletions) z `git diff origin/main...HEAD --stat` (dla commitów) lub `git diff HEAD --stat` (dla niezacommitowanych zmian)
7. **🔍 Migration Notes** - informacje o migracjach lub ich braku
8. **🔄 Breaking Changes** - jeśli są breaking changes, w przeciwnym razie "None"
9. **✅ Checklist** - opcjonalna lista checkboxów dla reviewera

METODA ANALIZY:
1. **NAJPIERW** wykonaj `git fetch origin main` (lub `git fetch`) aby zsynchronizować lokalną referencję do main z serwerem
2. Sprawdź czy są commity różniące się od main:
   - Użyj `git diff origin/main...HEAD --name-status` aby zobaczyć listę zmienionych plików w commitach (używając origin/main zamiast lokalnego main)
   - Jeśli nie ma commitów różniących się, przejdź do analizy niezacommitowanych zmian
3. **Dla commitów różniących się od main:**
   - Dla każdego zmodyfikowanego pliku (M) użyj `git diff origin/main...HEAD -- <file>` aby zobaczyć faktyczne zmiany
   - Dla nowych plików (A) - przeczytaj tylko początek pliku aby zrozumieć jego cel, nie całą implementację
4. **Dla niezacommitowanych zmian:**
   - Użyj `git diff HEAD --name-status` aby zobaczyć listę niezacommitowanych plików
   - Użyj `git diff HEAD --stat` aby zobaczyć statystyki niezacommitowanych zmian
   - Dla każdego zmodyfikowanego pliku (M) użyj `git diff HEAD -- <file>` aby zobaczyć faktyczne zmiany
   - Dla nowych plików (nieśledzonych) - przeczytaj tylko początek pliku aby zrozumieć jego cel, nie całą implementację
5. Skup się na różnicach i zmianach, nie na całej zawartości

Opis powinien być:
- Krótki i zwięzły - skupiony na zmianach
- Z ikonami emoji dla lepszej czytelności
- Z wyróżnieniem TYLKO głównych zmian w tym PR
- Z jasnym opisem kontekstu - co się zmieniło, a nie co jest w projekcie

Po wpisaniu tej komendy, AI automatycznie przygotuje szczegółowy opis PR analizując faktyczne różnice w plikach.

WAŻNE: Opis PR powinien być zapisany do nowego pliku `PR_DESCRIPTION.md` w katalogu głównym projektu. NIE nadpisuj pliku `.cursor/commands/2-pr-description.md` - ten plik zawiera tylko instrukcje.

Po wygenerowaniu opisu AI zapyta, czy opis jest odpowiedni. Jeśli zaakceptujesz – AI przygotuje komendę do commita. Jeśli nie – będziesz mógł wybrać opcję dodatkowego skrócenia lub dostosowania opisu.
