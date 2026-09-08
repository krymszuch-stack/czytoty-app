# CzyToTy? — prototyp 3w1 (Telefon + SMS + Ochrona)

Sprawdzanie wiadomości i linków na podstawie **rzeczywistej Listy Ostrzeżeń CERT Polska**, własny dialer i prototyp ekranu rozmów, podgląd i wysyłka SMS przez operatora, własne blokady numerów oraz prywatne notatki. Aplikacja nie przypisuje numerom zmyślonej reputacji ani nie udaje weryfikacji rozmówcy.

## Dostępne funkcje (3w1)

- **Telefon:** Ostatnie / Kontakty / Klawiatura, haptyka i przytrzymanie 0 dla +. Opcjonalna rola domyślnego Telefonu uruchamia własny ekran połączeń przez InCallService: odbieranie audio, odrzucanie, rozłączanie, mikrofon, urządzenia audio, DTMF, zawieszanie i łączenie rozmów, gdy operator udostępnia te funkcje. Połączenia z roli domyślnej są zlecane przez TelecomManager; bez tej roli numer otwiera się w obecnym telefonie systemowym. To integracja prototypowa wymagająca testów operatora przed codziennym użyciem.
- **SMS:** odczyt systemowej skrzynki i wątków po zgodzie, automatyczne odświeżanie, komponowanie i wysyłka przez domyślną kartę SIM. Wysyłka wymaga uprawnienia oraz zatwierdzenia odbiorcy i treści. Potwierdzenia każdej części są zapisywane lokalnie; zlecenie nie jest przedstawiane jako doręczenie. Przychodzące SMS-y są sprawdzane lokalnie względem zapisanej listy CERT i reguł tekstowych, z ostrzeżeniem po nadaniu zgód na odbiór i powiadomienia. Wiadomości zapisuje obecna domyślna aplikacja SMS; CzyToTy nie przejmuje tej roli.
- Wklejenie tekstu lub odebranie go przez systemowe **Udostępnij → CzyToTy? Prototyp**.
- Pobranie oficjalnej listy CERT, dopasowanie domen i ich subdomen, obsługa IDN, adresów z userinfo i końcowej interpunkcji. Sprawdzane strony nie są otwierane.
- Analiza tekstu i domen na telefonie. Treść wiadomości i sprawdzane adresy nie są wysyłane do Supabase ani usługi AI.
- Kopia listy w pamięci aplikacji, atomowa wymiana po poprawnym pobraniu, data aktualizacji, ostrzeżenie przy starej lub brakującej kopii. Odświeżanie co około 5 minut podczas korzystania z aplikacji, z przyciskiem ręcznego pobrania. Awaria źródła nie usuwa poprzednich danych.
- Normalizacja numeru do E.164 oraz sprawdzenie planu numeracji przez Google libphonenumber. **Poprawny format nie oznacza aktywnego numeru, zaufanego właściciela ani autentycznej tożsamości dzwoniącego.**
- Własne etykiety, notatki i lista blokad, zapisane w SQLite bez sztucznych danych startowych.
- Android CallScreeningService: odrzucanie wyłącznie numerów z własnej listy, po nadaniu systemowej roli i włączeniu filtrowania. Działa lokalnie, bez oczekiwania na sieć. Historia obejmuje tylko zdarzenia przekazane tej aplikacji przez Androida, maksymalnie 200 wpisów.
- Otwieranie systemowego telefonu i aplikacji SMS. Przekazanie podejrzanej wiadomości do CERT 8080 przygotowuje SMS; użytkownik sam zatwierdza wysłanie.
- Supabase: logowanie e-mail/hasłem, rejestracja z potwierdzeniem e-mail zależnym od ustawień Auth, prywatny zapis/odczyt/usunięcie notatki, kopiowanie notatki z konta na urządzenie oraz wylogowanie.

## Baza aplikacji

Utworzony osobny projekt: **czytoty-app**, Frankfurt, ref **ouszurtddchzztdcyrdx**.

[Panel projektu Supabase](https://supabase.com/dashboard/project/ouszurtddchzztdcyrdx)

- `data_sources`: publiczny katalog rzeczywiście używanych źródeł, tylko odczyt dla klientów.
- `private_notes`: klucz `(user_id, phone)`, etykieta do 80 znaków i notatka do 500 znaków. RLS ogranicza wszystkie operacje do właściciela. Anonimowy klient nie ma dostępu.
- Blokady, treści SMS i rejestr połączeń nie są synchronizowane. Przycisk **Zapisz w chmurze** wysyła wyłącznie wskazany numer, etykietę i notatkę.
- Pobieranie notatek pokazuje do 100 rekordów konta. Nie ma publicznych ocen społeczności ani publicznego katalogu prywatnych numerów.
- Koszt utworzenia projektu zwrócony przez integrację podczas konfiguracji: 0 USD/miesiąc. Nie jest to gwarancja ceny przy późniejszej zmianie planu lub wykorzystania.

Migracja znajduje się w `supabase/migrations/`. Nie uruchamiaj starego schematu z historycznych commitów.

## Uruchomienie i testowanie

Wymagane: JDK 21, Android SDK 36.1, dostęp do internetu na pierwszą aktualizację listy. APK wymaga Androida 8+. Konfiguracja roli filtrowania w tym prototypie wymaga Androida 10+ i telefonu udostępniającego tę rolę.

W nieśledzonym pliku `local.properties`:

```properties
sdk.dir=C:\\Users\\TWOJ_UZYTKOWNIK\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://ouszurtddchzztdcyrdx.supabase.co
SUPABASE_PUBLISHABLE_KEY=<publishable key z ustawień tego projektu>
```

Można też przekazać obie zmienne Supabase w środowisku kompilacji. Klucz publishable jest przeznaczony do aplikacji klienckiej; klucze service_role, secret i management nie mogą trafić do APK. Brak konfiguracji nie uruchamia żadnego fikcyjnego serwera — funkcje konta zgłoszą błąd, a lokalne nadal działają.

```powershell
.\scripts\build.ps1
# Z odblokowanym telefonem i aktywnym debugowaniem USB:
.\scripts\build.ps1 -DeviceTests
```

Skrypt uruchamia kompilację, testy jednostkowe i Android Lint. Czyści wyłącznie atrybut read-only plików wynikowych pod `build/`, ponieważ ten atrybut blokował Gradle na tym Windowsie. Plik końcowy: `artifacts/CzyToTy-prototype.apk`.

Identyfikator debug APK to `com.aistudio.ochronapolaczen.ktwzpd.prototype`. Dzięki temu prototyp może być zainstalowany obok wcześniejszej aplikacji. Testy urządzenia mogą odinstalować swój wariant aplikacji po zakończeniu; zainstaluj ponownie końcowy APK.

## Granice prototypu

- Nie ma uniwersalnej, podłączonej bazy reputacji wszystkich polskich numerów. Brak informacji nigdy nie jest zamieniany na ocenę „bezpieczny”.
- CERT jest źródłem zagrożeń domenowych, nie bazą telefonów. Aplikacja nie blokuje stron w innych aplikacjach, nie śledzi przekierowań i nie analizuje załączników.
- Reguły językowe są prostymi wskazówkami do ostrożności; nie są modelem AI ani rozstrzygnięciem o oszustwie.
- Android może nie przekazać aplikacji połączeń z numerów zapisanych w kontaktach lub z ograniczoną prezentacją numeru. Rejestr nie potwierdza odebrania ani czasu trwania rozmowy.
- Moduł SMS korzysta z rzeczywistej skrzynki Androida. Nie obsługuje MMS/RCS, usuwania ani oznaczania wiadomości jako przeczytane. Odczytuje początkowo 500 najnowszych SMS-ów, z możliwością rozszerzenia do 5000; w wątku pokazuje 500 najnowszych. Bezpośrednia wysyłka wymaga pełnego numeru i domyślnej karty SIM. Android może ograniczać dostęp do niektórych SMS-ów (np. kodów jednorazowych).
- Usunięto symulacje rozmów, fikcyjne oceny społeczności, niepotwierdzonych partnerów SAP, pozorną transkrypcję i obietnice analizy rozmów. Stary kod jest dostępny w historii Git.
- Rejestracja wymaga dostępnego mechanizmu wysyłania maili Supabase. Limit domyślnej usługi mailowej i brak własnego SMTP ograniczają otwarte testy rejestracji; nie obchodzimy weryfikacji adresu.
- Kopie zapasowe i transfer danych aplikacji przez Android są wyłączone. Usunięcie aplikacji usuwa dane lokalne; zapisane ręcznie notatki w Supabase pozostają na koncie.
- Historyczna prezentacja biznesowa w katalogu głównym nie opisuje gwarantowanych możliwości tego prototypu i nie jest dołączana do APK.

## Źródła

- [CERT Polska: Lista Ostrzeżeń i zasady integracji](https://cert.pl/lista-ostrzezen/)
- [Oficjalny feed aktywnych domen CERT v2](https://hole.cert.pl/domains/v2/domains.txt)
- [Google libphonenumber](https://github.com/google/libphonenumber)
- [Android: filtrowanie połączeń](https://developer.android.com/develop/connectivity/telecom/dialer-app/screen-calls)
- [Supabase: Row Level Security](https://supabase.com/docs/guides/database/postgres/row-level-security)

Wyniki wykonanych testów i zakres kontroli urządzenia: `artifacts/VERIFICATION.md`.


## Logo i ikony

Zasoby odtworzone wektorowo na podstawie dostarczonej planszy są w `artifacts/brand/`. Paczka `artifacts/CzyToTy-logo-ikony.zip` zawiera SVG i PNG: logo poziome, sam znak, cztery warianty ikony aplikacji oraz ikony telefonu, SMS, ochrony, wyszukiwania i konta. Android korzysta z ikon wektorowych i ikony adaptacyjnej z wariantem monochromatycznym. Instrukcja odtwarzania zasobów: `artifacts/brand/README.md`.


## Ekran połączenia w wersji 0.5

Konfiguracja: **Telefon → ⓘ → Ustaw jako domyślny Telefon**. Zmianę zatwierdza użytkownik w Androidzie. W tym samym oknie są uprawnienia połączeń, powiadomień i pełnoekranowego połączenia przy blokadzie, jeśli Android wymaga osobnej zgody.

Bez roli domyślnej wybieranie przekazuje numer do obecnej aplikacji telefonu. Z rolą wykorzystuje TelecomManager.placeCall; faktyczną telefonię i połączenia alarmowe obsługuje Android. Podczas rozmowy dostępne są akcje przekazane przez Telecom, a przycisk powrotu do rozmowy pojawia się w aplikacji. Nie jest to VoIP ani symulacja połączenia.

**Przed codziennym użyciem wymagane są testy operatora**: połączenie przychodzące przy odblokowanym i zablokowanym ekranie, odbieranie, rozłączanie, dźwięk, mikrofon, DTMF, Bluetooth i dual-SIM. Testy ekranu z danymi syntetycznymi nie potwierdzają tych funkcji. Brak kompletnej obsługi wideo/RTT; połączenia przychodzące w tym prototypie odbierane są jako audio.

Wiadomości nadal nie przejmują roli domyślnego SMS-a: pozostała pełna obsługa zapisu, MMS i odpowiednie testy. Szczegóły celu: `PRODUCT.md`. Wygląd i inspiracje: `DESIGN.md`.
