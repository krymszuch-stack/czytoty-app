import { EmergencyService } from '../types';

export const EMERGENCY_SERVICES: EmergencyService[] = [
  {
    number: '112',
    name: 'Europejski Numer Alarmowy',
    department: 'Centrum Powiadamiania Ratunkowego (CPR)',
    description:
      'Zintegrowane dysponowanie służbami: Pogotowie Ratunkowe, Państwowa Straż Pożarna, Policja, WOPR i GOPR. Działa bezpłatnie, priorytetowo oraz bez karty SIM.',
    priority: 'CRITICAL',
    freeAndDirect: true
  },
  {
    number: '999',
    name: 'Pogotowie Ratunkowe',
    department: 'Zespoły Ratownictwa Medycznego (ZRM)',
    description:
      'Dyspozytornia medyczna. Bezpośrednie wezwanie karetki pogotowia w stanach nagłego zagrożenia życia, utraty przytomności lub zawału serca.',
    priority: 'CRITICAL',
    freeAndDirect: true
  },
  {
    number: '998',
    name: 'Straż Pożarna',
    department: 'Państwowa i Ochotnicza Straż Pożarna (PSP / OSP)',
    description:
      'Pożary, wypadki komunikacyjne, katastrofy budowlane, usuwanie skutków nawałnic i zagrożenia chemiczne.',
    priority: 'CRITICAL',
    freeAndDirect: true
  },
  {
    number: '997',
    name: 'Policja',
    department: 'Wojewódzkie / Powiatowe Komendy Policji',
    description:
      'Zgłoszenia napadów, kradzieży, zakłóceń porządku, przestępstw i bezpośredniego zagrożenia bezpieczeństwa osobistego.',
    priority: 'CRITICAL',
    freeAndDirect: true
  },
  {
    number: '986',
    name: 'Straż Miejska',
    department: 'Straż Miejska / Gminna ds. Porządku Publicznego',
    description:
      'Interwencje porządkowe, nieprawidłowe parkowanie, zakłócanie ciszy nocnej, ochrona środowiska i zwierząt.',
    priority: 'CRITICAL',
    freeAndDirect: true
  },
  {
    number: '991',
    name: 'Pogotowie Energetyczne',
    department: 'Dyspozycja Mocy Operatora Systemu Dystrybucyjnego (OSD)',
    description:
      'Awarie sieci elektrycznej, zerwane linie napowietrzne, brak zasilania w dzielnicy, iskrzące transformatory.',
    priority: 'CRITICAL',
    freeAndDirect: true
  },
  {
    number: '992',
    name: 'Pogotowie Gazowe',
    department: 'Polska Spółka Gazownictwa (PSG) - Pogotowie Gazowe',
    description:
      'Wyczuwalny ulatniający się gaz, nieszczelności instalacji gazowych, uszkodzenia gazociągów.',
    priority: 'CRITICAL',
    freeAndDirect: true
  },
  {
    number: '993',
    name: 'Pogotowie Ciepłownicze',
    department: 'Miejskie Przedsiębiorstwo Energetyki Cieplnej (MPEC)',
    description:
      'Awarie sieci ciepłowniczej, pęknięcia rurociągów z gorącą wodą, brak ogrzewania miejskiego.',
    priority: 'CRITICAL',
    freeAndDirect: true
  },
  {
    number: '994',
    name: 'Pogotowie Wodno-Kanalizacyjne',
    department: 'Miejskie Przedsiębiorstwo Wodociągów i Kanalizacji (MPWiK)',
    description:
      'Poważne wycieki wody na ulicy, brak wody w sieci miejskiej, wybicie ścieków lub zatory kanalizacyjne.',
    priority: 'CRITICAL',
    freeAndDirect: true
  },
  {
    number: '984',
    name: 'Pogotowie Rzeczne i Wodne',
    department: 'Wodne Ochotnicze Pogotowie Ratunkowe (WOPR)',
    description:
      'Wypadki na rzekach, jeziorach i zalewach, tonięcia, załamanie lodu pod człowiekiem.',
    priority: 'CRITICAL',
    freeAndDirect: true
  },
  {
    number: '985',
    name: 'Ratownictwo Górskie i Morskie',
    department: 'GOPR / TOPR / Morska Służba Poszukiwania i Ratownictwa (SAR)',
    description:
      'Zaginięcia na szlakach górskich, wypadki taternickie, lawiny, akcje ratunkowe na Bałtyku.',
    priority: 'CRITICAL',
    freeAndDirect: true
  },
  {
    number: '116123',
    name: 'Kryzysowy Telefon Zaufania dla Dorosłych',
    department: 'Instytut Psychologii Zdrowia PTP',
    description:
      'Bezpłatne wsparcie psychologiczne dla osób dorosłych w kryzysie emocjonalnym, żałobie, samotności i depresji.',
    priority: 'CRITICAL',
    freeAndDirect: true
  },
  {
    number: '116111',
    name: 'Telefon Zaufania dla Dzieci i Młodzieży',
    department: 'Fundacja Dajemy Dzieciom Siłę',
    description:
      'Anonimowa pomoc dla dzieci i młodzieży w trudnych sytuacjach życiowych.',
    priority: 'CRITICAL',
    freeAndDirect: true
  }
];

export function normalizeEmergencyInput(raw: string): string {
  if (!raw) return '';
  return raw.replace(/[^0-9]/g, '');
}

export function getEmergencyInfo(rawInput: string): EmergencyService | null {
  const digits = normalizeEmergencyInput(rawInput);
  if (!digits) return null;

  // Direct match or trailing match (e.g. "+48 112" -> "48112" -> ends with "112")
  for (const service of EMERGENCY_SERVICES) {
    if (digits === service.number) {
      return service;
    }
  }

  // If input starts with 48 (Poland code)
  if (digits.startsWith('48')) {
    const stripped = digits.slice(2);
    for (const service of EMERGENCY_SERVICES) {
      if (stripped === service.number) {
        return service;
      }
    }
  }

  return null;
}

export function isEmergencyPrefix(rawInput: string): boolean {
  const digits = normalizeEmergencyInput(rawInput);
  if (!digits) return false;

  // Short 3-digit emergency codes in Poland: 112, 999, 998, 997, 986, 991, 992, 993, 994, 984, 985
  const emergencyNumbers = ['112', '999', '998', '997', '986', '991', '992', '993', '994', '984', '985', '116123', '116111'];
  return emergencyNumbers.includes(digits) || (digits.startsWith('48') && emergencyNumbers.includes(digits.slice(2)));
}
