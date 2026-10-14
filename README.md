# CzyToTy? — aplikacja webowa React (Telefon + SMS + Ochrona)

Aplikacja przepisana z prototypu Androida na nowoczesną aplikację internetową w **React + TypeScript + Tailwind CSS** z serwerem **Express**. Zawiera wszystkie funkcje i logikę biznesową pierwowzoru: codzienne połączenia, wiadomości SMS, ochronę przed sygnałami oszustw na podstawie **rzeczywistej Listy Ostrzeżeń CERT Polska**, własne blokady numerów oraz integrację z prywatnymi notatkami w chmurze Supabase.

## Główne moduły

1. **Telefon (Dialer):**
   - Zakładki: **Ostatnie**, **Kontakty** i **Klawiatura**.
   - Okrągłe klawisze z literami, reakcją na naciśnięcie i wibracjami (Web Vibration API).
   - Formatowanie numeru w locie według standardu międzynarodowego (Google `libphonenumber-js`).
   - Wskaźnik obecności numeru na Twojej liście blokad.
   - Szybkie przejście do SMS i notatki/blokady numeru.
   - Ekran aktywnego połączenia: widok przychodzący/aktywny, DTMF, sterowanie mikrofonem i dźwiękiem, zawieszanie rozmowy, licznik czasu trwania.

2. **Wiadomości (SMS):**
   - Skrzynka odbiorcza i podział na wątki, wyszukiwarka rozmów i treści, filtry (Wszystkie, Nieprzeczytane, Przypięte).
   - Przypinanie rozmów na górze listy.
   - Dymki wiadomości przychodzących i wychodzących, grupowanie według daty, darmowy podgląd statusów.
   - Wykrywanie domen z listy CERT bezpośrednio w treści dymka (czerwone ostrzeżenie) oraz przycisk tarczy do natychmiastowej analizy.
   - Szkice wiadomości powiązane z wątkami, licznik znaków i części SMS (160 znaków na część).
   - Potwierdzenia transmisji operatora.

3. **Sprawdź (Skaner wiadomości i linków):**
   - Odczyt oficjalnego feedu **Listy Ostrzeżeń CERT Polska** (hole.cert.pl) z automatycznym serwerowym proxy i buforowaniem pamięciowym (TTL 5 min).
   - Ekstrakcja hostów z adresów URL i domen w tekście, normalizacja IDN.
   - Wykrywanie sygnałów językowych (presja czasu, kody BLIK, wezwania do dopłaty).
   - Przycisk szybkiego przekazania podejrzanego SMS-a do CERT na numer **8080**.

4. **Twoje numery & Blokady:**
   - Sprawdzanie poprawności numeru i planu numeracji.
   - Własne etykiety, prywatne notatki i przełącznik blokady.
   - Zapis lokalny (localStorage) oraz opcjonalny zapis w chmurze Supabase.
   - Bezpośrednie przejście do telefonu lub SMS.

5. **Ochrona połączeń:**
   - Włącznik filtrowania połączeń na podstawie własnej listy blokad.
   - Rejestr filtrowania połączeń (do 200 zdarzeń).
   - Interaktywny symulator filtrowania połączeń przychodzących.

6. **Po Twojemu (Personalizacja):**
   - Motyw: Systemowy, Jasny, Ciemny.
   - Akcent: Lawenda, Ocean, Mięta.
   - Opcje: Większy tekst wiadomości, zwarta lista rozmów, zaokrąglone dymki, wibracje klawiatury.
   - Dynamiczny podgląd dymka wiadomości.

7. **Prywatne notatki w chmurze (Supabase):**
   - Opcjonalne logowanie i rejestracja e-mail.
   - Zapisywanie, pobieranie i usuwanie prywatnych notatek powiązanych z kontem.

## Uruchomienie lokalne

```bash
# Instalacja zależności
npm install

# Uruchomienie serwera deweloperskiego (port 3000)
npm run dev

# Kompilacja produkcyjna
npm run build
```
