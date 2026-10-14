# Uproszczony Interfejs Senior-Friendly, Numery Alarmowe, Baza Usług i Wizytówka Użytkownika

Pakiet usprawnień UX/UI dedykowany intuicyjnej obsłudze (w tym dla osób starszych), zintegrowanemu dialerowi 2w1 z natychmiastowym przyciskiem wiadomości, czerwonemu trybowi numerów alarmowych (112, 999, 998, 997), dynamicznej bazie firm i usług z trwałym zapisem oraz wizytówce profilowej z kontaktem ICE.

## User Review & Critical Decisions

> [!IMPORTANT]
> Kluczowe decyzje uzgodnione z użytkownikiem w Fazie 1:
> - **Szybki SMS pod kciukiem:** Duży, wyrazisty i jednoznaczny przycisk wiadomości w naturalnej strefie kciuka w dolnym pasku nawigacji („Napisz wiadomość” / instant switch do czatu), z natychmiastowym feedbackiem wizualnym.
> - **Numery alarmowe (112, 999, 998, 997):** Automatyczna zmiana koloru cyfr na alarmową czerwień, wykrzyknik `(!)` oraz duży, czytelny podpis służby ratunkowej (np. „Centrum Powiadamiania Ratunkowego”, „Pogotowie Ratunkowe”).
> - **Baza jednostek i firm (Smart Directory):** Podczas wpisywania cyfr pojawia się rozwijana lista podpowiedzi nad klawiaturą z ikonami branżowymi (np. 🥖 Piekarnia, 💊 Apteka, 🏥 Przychodnia, 📯 Poczta, 🏦 Bank) wraz z automatycznym trwałym importem pobranej wizytówki do lokalnej bazy danych.
> - **Karta użytkownika / Wizytówka:** Panel konta z wizytówką: imię, numer telefonu, awatar/zdjęcie profilowe, kontakt ratunkowy **ICE** (In Case of Emergency) oraz osobista notatka.

---

## 1. Overview & Core Concept

### A. Cel ergonomiczny i Senior-Friendly
Aplikacja ma być natychmiast zrozumiała nawet po przebudzeniu lub przy słabszym wzroku:
- **Zasada 1 dotknięcia (Instant Action):** Użytkownik widzi dokładnie dwa główne światy komunikacji: zielona słuchawka do dzwonienia oraz fioletowy, szeroki przycisk wiadomości pod kciukiem.
- **Większe pola dotykowe:** Wszystkie przyciski dialera i nawigacji spełniają standard $\ge 48\times 48\text{px}$ z wyraźnym kontrastem (WCAG AA).
- **Zero niepewności:** Brak ukrytych gestów czy niejasnych symboli; każdy stan ma jednoznaczny podpis tekstowy.

### B. Tryb numerów ratunkowych i służb szybkiego reagowania
Po wpisaniu numerów alarmowych w Polsce i UE:
- `112` — Numer Alarmowy (Centrum Powiadamiania Ratunkowego)
- `999` — Pogotowie Ratunkowe (Zespoły Ratownictwa Medycznego)
- `998` — Straż Pożarna (Państwowa Straż Pożarna)
- `997` — Policja
- `986` — Straż Miejska
- `991` / `992` — Pogotowie Energetyczne / Gazowe
Cyfry podświetlają się na pulsujący czerwony kolor z etykietą służby, informacją o priorytecie ratunkowym i dużym przyciskiem wezwania pomocy.

### C. Inteligentna baza firm i usług (Smart Business Directory)
- Wpisywanie numeru na bieżąco porównuje cyfry z wbudowaną bazą zweryfikowanych punktów usługowych (piekarnie, apteki, przychodnie, urzędy, banki).
- Każdy wpis zawiera dedykowaną ikonę (🥖, 💊, 🏥, 📯, 🏦), pełną nazwę, adres i godziny.
- Pierwsze wyszukanie zapisuje wizytówkę firmy na stałe do lokalnej bazy (`LocalStore`), dzięki czemu następnym razem działa w 100% offline z natychmiastowym czasem reakcji.

### D. Karta użytkownika i wizytówka z kontaktem ICE
- W zakładce Konto dodajemy sekcję „Moja Wizytówka”:
  - Imię i nazwisko / pseudonim
  - Numer telefonu
  - Awatar (wybór predefiniowanych ikon lub przesłanie grafiki)
  - Kontakt ratunkowy ICE (Imię i numer telefonu bliskiej osoby)
  - Prywatna/publiczna notatka (np. grupa krwi, alergie, informacje kontaktowe)
  - Podgląd wizytówki w formie estetycznej karty z możliwością szybkiego udostępnienia.

---

## 2. User Experience & Visual Design

### A. Dolny pasek nawigacji i strefa kciuka
- Dolny pasek otrzymuje powiększone ikony z czytelnym podziałem.
- Na ekranie telefonu (Klawiatura):
  - Pod klawiaturą numeryczną: duży przycisk **Zadzwoń** (miętowo-zielony) oraz bezpośredni, wyrazisty przycisk **Napisz SMS** (lawendowy fiolet), gwarantujący natychmiastowe przejście do edytora wiadomości dla wpisanego numeru.

### B. Dynamiczna lista podpowiedzi nad klawiaturą
- Gdy w polu wpisano $\ge 2$ cyfry, nad klawiaturą rozwija się kompaktowa, przewijana lista pasujących jednostek i firm.
- Kliknięcie podpowiedzi uzupełnia numer i wyświetla pełną kartę firmy z adresem, ikoną branżową i przyciskiem połączenia/SMS.

### C. Wizualny stan alarmowy (112, 999, etc.)
- Tło pola numeru zyskuje delikatną czerwoną poświatę (`bg-red-950/40 border-red-500/60`).
- Wyświetla się ikona wykrzyknika ratunkowego `AlertOctagon` oraz czytelny komunikat:
  *„Połączenie alarmowe z Centrum Powiadamiania Ratunkowego. Bezpłatne i priorytetowe.”*

---

## 3. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        CZYTOTY? SENIOR & DIRECTORY                     │
│                                                                        │
│  ┌──────────────────────┐  ┌────────────────────────────────────────┐  │
│  │   Dialer & Keypad    │  │        Emergency Resolver              │  │
│  │  - 1-Click SMS Thumb │  │  - Detects 112, 999, 998, 997, 986     │  │
│  │  - High-Contrast Pad │  │  - Glow Red Mode + Department Badge    │  │
│  └──────────┬───────────┘  └──────────────────┬─────────────────────┘  │
│             │                                 │                        │
│             ▼                                 ▼                        │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │             Smart Business & Places Directory Service            │  │
│  │  - Match prefix / phone in local verified directory              │  │
│  │  - Category Icons: Bakery, Pharmacy, Clinic, Post, Bank          │  │
│  │  - Persistent Cache & Import into LocalStore                     │  │
│  └──────────────────────────────────┬───────────────────────────────┘  │
│                                     │                                  │
│                                     ▼                                  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                  User Profile & ICE Business Card                │  │
│  │  - Name, Phone, Avatar, ICE Contact, Personal Notes              │  │
│  │  - Stored in LocalStore + optional Supabase sync                 │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

### Nowe modele danych (TypeScript)

```typescript
export interface EmergencyService {
  number: string;
  name: string;
  department: string;
  description: string;
  priority: 'CRITICAL';
}

export interface BusinessEntity {
  id: string;
  name: string;
  phone: string;
  category: 'bakery' | 'pharmacy' | 'clinic' | 'post' | 'bank' | 'service' | 'other';
  icon: string;         // 'bakery' | 'pill' | 'hospital' | 'mail' | 'building'
  address?: string;
  verified: boolean;
  importedAt?: number;
}

export interface UserBusinessCard {
  fullName: string;
  phone: string;
  avatarUrl?: string;
  avatarIcon?: string;
  iceContactName?: string;
  iceContactPhone?: string;
  note?: string;
  updatedAt: number;
}
```

---

## 4. Plan wdrożenia krok po kroku

1. **Baza numerów alarmowych i rozpoznawanie służb (`src/utils/emergency.ts`):**
   - Zdefiniowanie numerów `112`, `999`, `998`, `997`, `986`, `991`, `992`, `993`, `994`.
   - Funkcja sprawdzająca `getEmergencyInfo(number)`.
2. **Katalog firm i integracja z bazą lokalną (`src/services/directory.ts`):**
   - Zbiór popularnych wzorców firmowych i instytucji z polskiego rynku.
   - Moduł wyszukiwania w czasie rzeczywistym i trwałego zapisu do `LocalStore`.
3. **Karta użytkownika / Wizytówka ICE (`src/services/storage.ts`):**
   - Zapis i odczyt wizytówki `czytoty_user_card`.
4. **Rozszerzenie `AppContext.tsx`:**
   - Udostępnienie stanu wizytówki użytkownika, wyszukiwarki firm i rozpoznawania numerów alarmowych.
5. **Przebudowa UI Dialera (`src/tabs/DialerTab.tsx`):**
   - Czerwony tryb alarmowy przy wpisaniu `112`/`999`/`998`/`997` z dużym podpisem służby.
   - Rozwijana lista podpowiedzi firm nad klawiaturą z ikonami.
   - Duży, wygodny przycisk SMS w strefie kciuka obok zielonej słuchawki.
   - Przełącznik rozmiaru tekstu / trybu dużych przycisków.
6. **Rozbudowa panelu konta (`src/tabs/AccountTab.tsx`):**
   - Nowa sekcja: „Moja Wizytówka & Kontakt ICE” z formularzem edycji i podglądem wizytówki.
7. **Weryfikacja jakościowa:**
   - Kompilacja (`npm run build`), testy lintera (`npm run lint`), sprawdzenie działania na porcie 3000.
