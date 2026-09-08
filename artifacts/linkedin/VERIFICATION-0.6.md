# CzyToTy? 0.6.0 — weryfikacja 8 września 2026

## Wynik końcowy

`./scripts/build.ps1` — **BUILD SUCCESSFUL**, 1 min 14 s, po końcowej poprawce kontrastu i odtworzeniu ujęcia panelu Telefon.

| Kontrola | Wynik |
|---|---|
| APK debug | PASS, `artifacts/CzyToTy-prototype.apk` |
| Testy JVM/Robolectric | 31 testów, 0 porażek, 0 błędów |
| Android Lint | 0 błędów, 40 ostrzeżeń |
| Ręczna kontrola obrazów | 6 PNG obejrzanych; po poprawce ponownie sprawdzone zmienione widoki |
| Supabase | Aktywny projekt, kontrola RLS/grantów/ograniczeń i anonimowego klienta; szczegóły w `DATABASE-AUDIT-0.6.md` |
| CERT v2 | HTTP 200, 138 270 wpisów podczas kontroli |
| Urządzenie Android / operator | Nie uruchamiano — brak podłączonego telefonu |

SHA-256 APK:

`AD8360FD8C41ED92EE97AE9AA02A6030EAAD6B151ECFACFAB9684F862BD43E5C`

## Zakres testów

- 23 dotychczasowe testy: domeny CERT, cache i analiza, numery, dial target, lokalne notatki/blokady, ograniczenie historii filtrowania, potwierdzenia części SMS, odrzucenie duplikatów i błędów wysyłki.
- 3 nowe testy metadanych rozmów: szkice i przypięcia po ponownym otwarciu SQLite, niezależność wątków, aktualizacja/usunięcie wpisu, polskie znaki, limity, błędny klucz, integralność; wyszukiwanie we wcześniejszej treści z najnowszym podglądem i kolejnością przypięć.
- 5 testów UI/demo: listy w dwóch motywach, kierunek/status dymków, działająca klawiatura i przytrzymanie 0, zmiana motywu, rzeczywisty edytor SMS z zachowaniem szkicu po powrocie do skrzynki i odczytem zapisu SQLite.

Testy renderują sześć obrazów 822 × 1782 w `artifacts/linkedin/demo`. Syntetyczne dane istnieją tylko w źródłach testowych. Nie wykonano połączenia ani wysłania SMS-a. Dla ekranów 01–03 użyto demonstracyjnej kompozycji prawdziwych komponentów; 04 i 06 renderują panele aplikacji, 05 rzeczywiste kontrolki personalizacji.

Robolectric `captureToImage`/PixelCopy przekraczał limit czasu w tym Windowsie. Końcowe obrazy powstają przez `decorView.draw(Canvas)` na natywnym Bitmapie Androida. To sprawdzone wizualnie rendery widoków, nie zdjęcia urządzenia ani obrazy wygenerowane przez AI.

## Ostrzeżenia i ograniczenia

Lint: 2 ostrzeżenia wersji AGP, 8 wersji zależności Gradle, 2 rozmiaru ikon, 6 dostępnych nowszych wersji, 1 zbędnego warunku SDK, 12 nieużywanych zasobów, 8 propozycji KTX i 1 propozycja przeniesienia wersji do TOML. Nie wykonywano szerokiej aktualizacji zależności. Dodano wyłącznie zależność istniejącej wersji Compose UI Test do testów JVM i zaktualizowano lockfile.

Nie wykonano testów na fizycznym urządzeniu: modemu, dźwięku, Bluetooth, dual-SIM, zachowania przy zablokowanym ekranie, rzeczywistej klawiatury ekranowej i pełnego przebiegu uprawnień. Poprawiony test instrumentacyjny uwzględnia nowy powrót ze szkicu do skrzynki, ale nie został uruchomiony na telefonie w tej iteracji.

Nie ponawiano logowania/CRUD dwóch kont Supabase. Kontrola chmury nie zmieniała schematu ani danych. Szkice, przypięcia, treści SMS i historia połączeń nie są synchronizowane. Brak MMS/RCS i roli domyślnej aplikacji SMS; nie jest to jeszcze pełny zamiennik systemowych komunikatorów.

Przebieg historycznych testów na telefonie w wersji 0.5 pozostaje w `VERIFICATION.md`; nie należy traktować go jako testu APK 0.6.
