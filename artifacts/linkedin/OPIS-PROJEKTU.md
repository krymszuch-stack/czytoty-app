# CzyToTy? — telefon, wiadomości i lokalne sprawdzanie treści

**Prototyp Android 0.6.0 · Kotlin · Jetpack Compose · SQLite · Supabase**

CzyToTy? łączy klawiaturę telefonu, podgląd i wysyłkę SMS przez operatora oraz sprawdzanie domen na podstawie Listy Ostrzeżeń CERT Polska. Projekt rozwija wygodne, codzienne narzędzia komunikacji z czytelnym wyjaśnieniem, co aplikacja rzeczywiście potrafi sprawdzić.

## Interfejs tej iteracji

- Motyw systemowy, jasny i ciemny; akcent lawendowy, oceaniczny lub miętowy; kolory tapety na Androidzie 12+.
- Ustawienia większego tekstu wiadomości, zwartej listy, zaokrągleń dymków i wibracji klawiatury. Zmiany są widoczne od razu i zapisywane lokalnie.
- Przypinanie rozmów, filtr przypiętych i nieprzeczytanych, wizualne oznaczenie szkiców.
- Wyszukiwanie kontaktu, numeru lub treści we wszystkich załadowanych wiadomościach, z aktualnym podglądem rozmowy.
- Trwałe szkice oddzielone między wątkami. Zmiana odbiorcy z panelu Telefon wymaga rozstrzygnięcia, jeśli zastąpiłaby istniejący szkic nowej wiadomości.
- Więcej miejsca na rozmowę: na jej ekranie chowają się główne zakładki i pasek marki. Powrót prowadzi do skrzynki.
- Licznik części SMS, przejście z rozmowy do klawiatury telefonu, wyszukiwanie ostatnich połączeń i filtr nieodebranych.

## Dane i architektura

| Obszar | Miejsce zapisu | Zakres |
|---|---|---|
| Wiadomości, kontakty, historia telefonu | Dostawcy danych Androida | Dostęp po zgodzie użytkownika; aplikacja ogranicza liczbę wczytanych rekordów |
| Własne numery i zdarzenia filtrowania | SQLite `czytoty_prototype.db` | Prywatne notatki, blokady, do 200 zdarzeń |
| Potwierdzenia transmisji SMS | SQLite `sms-transmission.db` | Potwierdzenia części wysyłki, bez deklarowania doręczenia |
| Szkice i przypięcia | SQLite `sms-conversations.db` | Nowa, oddzielna baza metadanych rozmów; brak synchronizacji |
| Wygląd | Prywatne SharedPreferences | Motyw, akcent, tekst, zwartość, dymki, haptyka |
| Domeny ostrzegawcze | Lokalny cache oficjalnego feedu CERT | Analiza bez otwierania wskazanych stron |
| Opcjonalne notatki konta | Supabase / PostgreSQL | Właściciel rekordu kontrolowany przez RLS; zapis uruchamiany przez użytkownika |

## Granice

To nie jest baza tożsamości ani reputacji wszystkich numerów. CERT dotyczy domen, a reguły tekstowe są wskazówkami ostrożności. Wiadomości i sprawdzane adresy nie są wysyłane do usługi AI ani do Supabase.

Moduł SMS nie jest jeszcze domyślną aplikacją SMS, nie obsługuje MMS/RCS, kasowania ani oznaczania wiadomości jako przeczytane. Filtr pokazuje stan zapisany w systemowej skrzynce. Szkice są lokalne i nie synchronizują się z innymi komunikatorami. Usunięcie aplikacji usuwa dane lokalne.

Integracje Telecom i SMS wymagają dalszych rzeczywistych prób operatora, dźwięku, Bluetooth, blokady ekranu i dual-SIM. Ujęcia demo nie są dowodem takich testów.

## Inspiracje

Wybrane rozwiązania, sprawdzone 8 września 2026; nie jest to ranking wszystkich komunikatorów:

- [QUIK](https://github.com/quik-sms/quik): personalizacja i skupienie na codziennym korzystaniu z SMS.
- [Fossify Messages](https://github.com/FossifyOrg/Messages): wyszukiwanie wiadomości i ustawienia wyglądu.
- [Fossify Phone](https://github.com/FossifyOrg/Phone): ustawienia telefonu i dostęp do funkcji komunikacji.
- [Goodwy Dialer](https://github.com/Goodwy/Dialer): punkt odniesienia dla czytelnego panelu telefonu.

Zmiany w CzyToTy? napisano w istniejącym projekcie Compose. Nie importowano kodu, grafik ani znaków tych aplikacji. Google Messages i Apple Messages są odniesieniem dla oczekiwanej wygody; ten prototyp nie deklaruje ich pełnego zestawu funkcji.
