# CzyToTy? — język interfejsu 0.6

## Kierunek

Telefon i Wiadomości mają być wygodnymi, codziennymi aplikacjami z własną identyfikacją. Motyw zgodny z systemem lub wybrany ręcznie, akcent lawendowy/oceaniczny/miętowy, duże nagłówki, miękkie powierzchnie i proste przyciski. Ostrzeżenia mają być czytelne, ale nie dominować zwykłej rozmowy.

## Iteracja 0.6

Panel „Po Twojemu” udostępnia trwałe ustawienia wyglądu i podgląd. Dymki, listy i ekran połączenia korzystają z palety MaterialTheme. Kolory ważnych działań są dobierane z uwzględnieniem kontrastu. Większy tekst nie wyłącza skalowania czcionki Androida. Przełączniki mają całe wiersze dostępne do dotknięcia; filtry i motywy można przewijać poziomo na wąskim ekranie.

Rozmowa SMS chowa główne zakładki i nagłówek marki, zachowując własną strzałkę powrotu. Lista wyróżnia nieprzeczytane wątki i szkice, pokazuje przypięcia. Rozmowy przypięte są pierwsze; wewnątrz grup pozostaje kolejność od najnowszej. Szkice i przypięcia są metadanymi lokalnymi, bez zapisu do systemowej skrzynki.

Inspiracje GitHub sprawdzone 8 września 2026: [QUIK](https://github.com/quik-sms/quik), [Fossify Messages](https://github.com/FossifyOrg/Messages), [Fossify Phone](https://github.com/FossifyOrg/Phone), [Goodwy Dialer](https://github.com/Goodwy/Dialer). Własna implementacja w Compose, bez kopiowania kodu i grafiki tych projektów.

Aktualne punkty odniesienia: [personalizacja Google Messages](https://support.google.com/messages/answer/14790371?hl=en), [tła rozmów Apple Messages](https://support.apple.com/en-gb/guide/iphone/iph605fa06e4/ios). W tej iteracji ustawienia CzyToTy? dotyczą całej aplikacji; nie dodano zdjęć w tle ani indywidualnych motywów każdego wątku. Możliwości SMS/RCS/iMessage tych produktów nie są deklarowanymi funkcjami prototypu.

## Wdrożone elementy

- Telefon: Ostatnie / Kontakty / Klawiatura; okrągłe klawisze z literami, reakcją na naciśnięcie i haptyką; przytrzymanie 0 wpisuje +. Przycisk połączenia ma odrębny miętowy kolor.
- Wiadomości: wyszukiwarka, filtr według rzeczywistego stanu przeczytania, nazwy z dostępnych kontaktów, awatary wyliczane z etykiet, stały przycisk nowej wiadomości.
- Rozmowa SMS: dymki wyrównane do strony nadawcy, grupowanie dat, krótka godzina i faktyczny status, pole pisania na dole, możliwość zaznaczenia tekstu. Szkice są rozdzielane między wątki.
- Połączenie: tło w gradiencie, duży kontakt, osobne widoki przychodzący / aktywny / zawieszony, jednoznaczne przyciski odbierania i rozłączania. Stan oraz przyciski wynikają ze zdarzeń Telecom, nie z timera symulacji.
- Techniczne wyjaśnienia i konfiguracja są w informacjach / ustawieniach, a nie w głównej treści listy.

## Inspiracje sposobem obsługi

To wybrane wzorce, nie obiektywny ranking ani deklaracja skopiowania tych produktów.

- [Apple Phone — układ Unified](https://support.apple.com/en-gb/126076): ograniczenie przechodzenia między widokami przy najczęstszych czynnościach.
- [Samsung One UI — system projektowy](https://developer.samsung.com/one-ui/index.html): czytelna hierarchia i wygodne obszary interakcji.
- [Samsung One UI 7 — projekt interfejsu](https://design.samsung.com/global/contents/one-ui-7/): spójne powierzchnie i rozróżnialne, funkcjonalne ikony.

W aplikacji nie ma cudzych logo, skopiowanych zrzutów ekranu ani sztucznych rozmów dodanych dla wyglądu. Podglądy z danymi syntetycznymi powstają wyłącznie w źródłach testów (testy JVM/Robolectric w 0.6, wcześniej APK testowy) i są tak oznaczane w raporcie. Syntetyczne wiadomości nie są dołączane do zwykłego APK.
