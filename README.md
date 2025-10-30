# 🌍 World at War: Turn-Based Strategy

## 🎯 Cel projektu
Aplikacja **World at War** ma dostarczyć użytkownikom angażującej rozrywki w trybie **jednoosobowym** oraz **wieloosobowym**.  
Celem MVP jest stworzenie prostej, ale satysfakcjonującej gry turowej, w której gracze mogą rywalizować między sobą lub z botem w klasyczną grę **kółko i krzyżyk (Tic-Tac-Toe)**.

---

## 🧩 Najmniejszy Zestaw Funkcjonalności (MVP)

1. **Rozgrywka na planszy gry**  
   - Podstawowa gra w kółko i krzyżyk (Tic-Tac-Toe).  

2. **Warianty planszy**  
   - Rozmiary: `3x3`, `4x4`, `5x5`.

3. **Tryb gościa**  
   - Możliwość natychmiastowego dołączenia do gry bez rejestracji.

4. **Rejestracja i logowanie**  
   - Tworzenie konta (nazwa użytkownika, e-mail, hasło).  
   - Logowanie się do istniejącego konta.

5. **Zapisywanie stanu gry**  
   - Automatyczny zapis postępu w grach jednoosobowych, umożliwiający ich kontynuację po ponownym uruchomieniu aplikacji.  
   - W rozgrywkach wieloosobowych gracz, który opuści grę, **przegrywa po 20 sekundach** nieaktywności.  
   - Identyfikacja gracza odbywa się:
     - po **adresie e-mail** (dla zarejestrowanych użytkowników),
     - po **adresie IP** (dla gości).

6. **System punktacji po wygranej partii**
   - 🧠 Z botem (łatwy poziom) → **+100 pkt**
   - ⚔️ Z botem (średni poziom) → **+500 pkt**
   - 👑 Z botem (trudny poziom) → **+1000 pkt**
   - 🧍‍♂️ Z innym graczem (PvP) → **+1000 pkt**

7. **Ranking graczy**adowa
   - Globalna tabela z wynikami, pozwalająca na porównanie osiągnięć.

8. **Pojedynki z innymi graczami**
   - Możliwość znalezienia przeciwnika online i dołączenia do rozgrywki w czasie rzeczywistym.

9. **Profil gracza**
   - Nazwa użytkownika  
   - Aktualne miejsce w rankingu  
   - Liczba punktów i rozegranych gier  
   - Estetyczne wyróżnienie pozycji gracza

10. **Funkcjonalności PvP**
    - Możliwość **poddania pojedynku**.  
    - Podgląd **czasu pozostałego na ruch przeciwnika**.  
    - Informacja o **liczbie tur** i **aktualnej turze**.

---

## 🚫 Poza Zakresem MVP
- Rozszerzenie gry o bardziej zaawansowane mechaniki strategiczne (inne niż kółko i krzyżyk).  

---

## ✅ Kryteria Sukcesu

### 🧾 Scenariusz I – Gracz vs Gracz (tryb gościa)
1. Gracz loguje się jako **gość**.  
2. Dołącza do rozgrywki **gracz vs gracz** (jeśli potrzeba – oczekuje na przeciwnika).  
3. Rozgrywa partię w kółko i krzyżyk.  
4. Po zakończeniu meczu otrzymuje **punkty** i trafia do **rankingu**.  

---

### 🧾 Scenariusz II – Rejestracja nowego użytkownika
1. Gracz wybiera opcję **utworzenia konta**.  
2. Wprowadza: **nazwę użytkownika**, **adres e-mail**, **hasło**.  
3. Loguje się jako **nowo utworzony użytkownik**.  

---

### 🧾 Scenariusz III – Gracz vs Bot
1. Gracz loguje się jako **gość**.  
2. Wybiera tryb **gracz vs bot** oraz poziom trudności (`łatwy`, `średni`, `trudny`).  
3. Rozgrywa partię w kółko i krzyżyk.  
4. Po zakończeniu gry otrzymuje punkty zgodnie z poziomem trudności i trafia do **rankingu**.  

---

### 🧾 Scenariusz IV – Rozgrywka z poziomu rankingu
1. Gracz loguje się jako **gość**.  
2. Przegląda **ranking graczy** i wybiera przeciwnika dostępnego online.  
3. Rozgrywa partię z wybranym graczem.  
4. Po zakończeniu meczu otrzymuje punkty, a jego **pozycja w rankingu** zostaje zaktualizowana.  

---

## 📊 Metryki Sukcesu

- ✅ Jeśli scenariusze **I–IV** zostaną w pełni zrealizowane – **zakres funkcjonalny MVP** został osiągnięty.  
- 🌐 Jeśli gra zostanie udostępniona publicznie pod **adresem URL** – **druga metryka sukcesu** została osiągnięta.  
- 🧪 Jeśli scenariusze **I–IV** zostaną przetestowane (częściowo lub całościowo) przy pomocy **testów e2e** – **czwarta metryka sukcesu** została osiągnięta.  

---

## 🧱 Dalszy Rozwój (Po MVP)
- Dodanie bardziej zaawansowanych typów gier strategicznych.  
- System znajomych i zaproszeń.  
- Chat podczas rozgrywki PvP.  
- Personalizacja profilu gracza.  
- Udoskonalenie SI bota.  

---

## ⚙️ Technologie (propozycje)
- **Frontend:** Angular + PrimeNG
- **Backend:** JAVA  
- **Baza danych:** PostgreSQL / Supabase  
- **Autoryzacja:** JWT / OAuth2  
