Przeprowadź pełną diagnozę backendu: upewnij się, że aplikacja się buduje, wymuś dobre praktyki Javy (Checkstyle + testy), przeanalizuj logi i spróbuj naprawić problem poprzez ponowny start usługi.

### Kroki które wykona komenda
1. Przejście do katalogu backendu.
2. Uruchomienie `./gradlew clean checkstyleMain checkstyleTest test`, aby zweryfikować styl i testy jednostkowe.
3. Zbudowanie artefaktu `./gradlew build` (potwierdza, że BE się buduje).
4. Restart backendu przez `./run-backend.sh restart`, co zazwyczaj rozwiązuje problemy po zmianach.
5. Analiza ostatnich logów (`tail`) oraz wyszukanie błędów (`grep`), aby potwierdzić czy problem ustąpił.

### Wykonanie
Uruchom w katalogu głównym:
```bash
cd /Users/lzi/Desktop/PROJEKTY/tbs-main/backend && \
./gradlew clean checkstyleMain checkstyleTest test && \
./gradlew build && \
./run-backend.sh restart && \
tail -n 200 application.log && \
grep -iE "error|exception" application.log || true
```

