# Ochrona jako główny temat demo

## Kolejność pokazu: około 60–90 sekund

1. **09 — Kto dzwoni?** Syntetyczne połączenie otwiera rzeczywisty `CallScreen`. Aplikacja odczytuje pojedynczy zapis SQLite i pokazuje „Bank DEMO · moja etykieta”, źródło oraz brak potwierdzenia tożsamości. Etykieta pochodzi z urządzenia, nie z rejestru banków.
2. **07 — SMS „z banku”.** Fikcyjny SMS zawiera prośbę o hasło, informację o blokadzie konta i domenę `bank-demo.example`. Rzeczywisty `DomainChecker` wydobywa domenę, znajduje ją w testowym zbiorze i zwraca dwa sygnały językowe. `AnalysisCard` pokazuje wynik. W aplikacji taki zbiór jest pobierany z CERT; w tym obrazie podpis „DEMO” oznacza testowe dane.
3. **10 — Własna blokada.** Numer → normalizacja E.164 → zapis SQLite → decyzja `PhoneProtection.shouldReject` → odpowiedź `CallScreeningService` do Androida. Odrzucanie wymaga systemowej roli filtrowania i włączonej ochrony. Nie jest zależne od internetu.
4. **08 — Brak danych.** Nieznany numer i brak domeny na liście nie prowadzą do statusu „bezpieczny”. To istotny stan produktu, także przy nowym zagrożeniu, którego lista jeszcze nie zawiera.
5. Opcjonalnie **01 i 05** — wygoda komunikatora i personalizacja na zakończenie.

## Co jest wdrożone, a co pozostaje celem

| Pytanie | Co obecnie sprawdza kod | Czego to nie potwierdza |
|---|---|---|
| Czy rozpoznaję numer? | Kontakty Androida oraz własna etykieta/notatka | Faktycznej tożsamości osoby wykonującej połączenie |
| Czy numer ma poprawny format? | Normalizacja i plan numeracji libphonenumber | Aktywności, właściciela ani bezpieczeństwa |
| Czy numer ma być blokowany? | Własna blokada i włączone filtrowanie | Publicznej oceny fraud/spam |
| Czy link jest zgłoszony? | Dopasowanie domeny i subdomen do lokalnej kopii CERT | Bezpieczeństwa domen bez wpisu i stron po przekierowaniu |
| Czy treść jest podejrzana? | Proste reguły dotyczące kodów, danych dostępu i presji czasu | Rozstrzygnięcia o oszustwie ani analizy audio rozmowy |
| Czy to naprawdę bank? | Brak niezależnego mechanizmu potwierdzania | Tego wyniku nie pokazujemy jako działającej funkcji |

Jeśli docelowym wyróżnikiem ma być potwierdzenie banku, to jest jeszcze **luka funkcjonalna**, a nie tylko brak slajdu. Potrzebne są ustalone źródło danych, zasady jego aktualizacji i mechanizm weryfikacji. Nawet dopasowanie numeru do katalogu banku nie powinno automatycznie potwierdzać tożsamości rozmówcy.

## Pochodzenie danych

Wyłącznie testy JVM/Robolectric. Numery, bank i wiadomości są fikcyjne, domeny używają `.example`. Testy korzystają z wydzielonej bazy środowiska testowego i usuwają własny wpis po scenariuszu. Żadne połączenie ani SMS nie zostały wykonane, nie dodano fikcyjnych rekordów do zwykłego APK ani chmury.
