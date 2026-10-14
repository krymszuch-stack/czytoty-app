/**
 * Silnik Wykrywacza Klonów (Anty-Homograf / Anty-Spoofing Sender ID)
 * Wykrywa próby podszywania się pod znane polskie marki:
 * - Podmiana liter na cyfry (InP0st, PKO_0)
 * - Podmiana wielkości liter (AlIegro: wielkie 'I' zamiast małego 'l')
 * - Cyrylica / Homoglify Unicode (np. 'а', 'е', 'о', 'р', 'с', 'у', 'х')
 * - Znaki interpunkcyjne na końcu (mBank., Allegro_)
 * - Odległość Levenshteina 1 lub 2 od chronionej marki
 */

export interface ProtectedBrand {
  name: string;
  category: 'bank' | 'courier' | 'gov' | 'telecom' | 'marketplace';
  aliases?: string[];
}

export const PROTECTED_POLISH_BRANDS: ProtectedBrand[] = [
  // Marketplace & E-commerce
  { name: 'Allegro', category: 'marketplace', aliases: ['allegro.pl'] },
  { name: 'OLX', category: 'marketplace', aliases: ['olx.pl'] },
  { name: 'Vinted', category: 'marketplace', aliases: ['vinted.pl'] },

  // Kurierzy
  { name: 'InPost', category: 'courier', aliases: ['Paczkomaty', 'InPost.pl'] },
  { name: 'DPD', category: 'courier', aliases: ['DPD Polska'] },
  { name: 'DHL', category: 'courier', aliases: ['DHL Express'] },
  { name: 'Poczta Polska', category: 'courier', aliases: ['Pocztex', 'Envelo'] },
  { name: 'GLS', category: 'courier' },
  { name: 'FedEx', category: 'courier' },

  // Banki
  { name: 'mBank', category: 'bank' },
  { name: 'PKO BP', category: 'bank', aliases: ['PKOBP', 'iKO', 'PKO'] },
  { name: 'Santander', category: 'bank', aliases: ['Santander Bank'] },
  { name: 'ING', category: 'bank', aliases: ['ING Bank', 'Moje ING'] },
  { name: 'Pekao', category: 'bank', aliases: ['Bank Pekao', 'PeoPay'] },
  { name: 'Alior Bank', category: 'bank', aliases: ['Alior'] },
  { name: 'Millennium', category: 'bank', aliases: ['Bank Millennium'] },
  { name: 'BNP Paribas', category: 'bank', aliases: ['BNP'] },
  { name: 'Credit Agricole', category: 'bank' },
  { name: 'BLIK', category: 'bank' },

  // GOV & Administracja
  { name: 'GOV.PL', category: 'gov', aliases: ['GOV', 'mObywatel', 'Profil Zaufany'] },
  { name: 'KAS', category: 'gov', aliases: ['Urząd Skarbowy', 'e-Urząd'] },
  { name: 'ZUS', category: 'gov' },

  // Telekomy & Energetyka
  { name: 'Orange', category: 'telecom' },
  { name: 'Play', category: 'telecom' },
  { name: 'Plus', category: 'telecom' },
  { name: 'T-Mobile', category: 'telecom' },
  { name: 'PGE', category: 'telecom', aliases: ['PGE Polska Grupa Energetyczna'] },
  { name: 'Tauron', category: 'telecom' },
  { name: 'Enea', category: 'telecom' }
];

// Mapa najczęstszych znaków cyrylicy/greki wyglądających identycznie jak litery łacińskie
export const CYRILLIC_CONFUSABLES: Record<string, string> = {
  '\u0430': 'a', // cyrylica 'а'
  '\u0410': 'A', // cyrylica 'А'
  '\u0435': 'e', // cyrylica 'е'
  '\u0415': 'E', // cyrylica 'Е'
  '\u043E': 'o', // cyrylica 'о'
  '\u041E': 'O', // cyrylica 'О'
  '\u0440': 'p', // cyrylica 'р'
  '\u0420': 'P', // cyrylica 'Р'
  '\u0441': 'c', // cyrylica 'с'
  '\u0421': 'C', // cyrylica 'С'
  '\u0443': 'y', // cyrylica 'у'
  '\u0445': 'x', // cyrylica 'х'
  '\u0425': 'X', // cyrylica 'Х'
  '\u0456': 'i', // ukraińskie 'і'
  '\u0406': 'I', // ukraińskie 'І'
  '\u0458': 'j', // cyrylica 'ј'
  '\u0408': 'J', // cyrylica 'Ј'
};

export interface HomographSegment {
  char: string;
  isSpoofed: boolean;
  expected?: string;
  reason?: string;
}

export interface HomographAnalysisResult {
  isSuspicious: boolean;
  originalSender: string;
  targetBrand: string | null;
  brandCategory?: string;
  reason: string | null;
  segments: HomographSegment[];
  confidence: 'HIGH' | 'MEDIUM' | 'LOW';
}

function levenshteinDistance(a: string, b: string): number {
  const matrix: number[][] = [];
  for (let i = 0; i <= b.length; i++) matrix[i] = [i];
  for (let j = 0; j <= a.length; j++) matrix[0][j] = j;

  for (let i = 1; i <= b.length; i++) {
    for (let j = 1; j <= a.length; j++) {
      if (b.charAt(i - 1) === a.charAt(j - 1)) {
        matrix[i][j] = matrix[i - 1][j - 1];
      } else {
        matrix[i][j] = Math.min(
          matrix[i - 1][j - 1] + 1, // zamiana
          matrix[i][j - 1] + 1,     // wstawienie
          matrix[i - 1][j] + 1      // usunięcie
        );
      }
    }
  }
  return matrix[b.length][a.length];
}

/**
 * Normalizuje tekst zamieniając cyrylicę na łacinę
 */
export function transliterateConfusables(text: string): { normalized: string; replacedIndices: number[] } {
  let normalized = '';
  const replacedIndices: number[] = [];

  for (let i = 0; i < text.length; i++) {
    const char = text[i];
    if (CYRILLIC_CONFUSABLES[char]) {
      normalized += CYRILLIC_CONFUSABLES[char];
      replacedIndices.push(i);
    } else {
      normalized += char;
    }
  }

  return { normalized, replacedIndices };
}

/**
 * Główna analiza nadawcy SMS pod kątem podszycia i klonowania
 */
export function analyzeSenderHomograph(sender: string): HomographAnalysisResult {
  const trimmed = sender.trim();
  if (!trimmed || trimmed.startsWith('+') || /^\d+$/.test(trimmed)) {
    // Standardowy numer telefonu (nie nazwa tekstowa nadawcy)
    return {
      isSuspicious: false,
      originalSender: sender,
      targetBrand: null,
      reason: null,
      segments: trimmed.split('').map(c => ({ char: c, isSpoofed: false })),
      confidence: 'LOW'
    };
  }

  // 1. Sprawdzenie obecności cyrylicy/homoglifów Unicode
  const { normalized, replacedIndices } = transliterateConfusables(trimmed);
  if (replacedIndices.length > 0) {
    const segments: HomographSegment[] = trimmed.split('').map((c, idx) => {
      const isReplaced = replacedIndices.includes(idx);
      return {
        char: c,
        isSpoofed: isReplaced,
        expected: isReplaced ? CYRILLIC_CONFUSABLES[c] : undefined,
        reason: isReplaced ? `Znak cyrylicy (${c}) udający literę łacińską (${CYRILLIC_CONFUSABLES[c]})` : undefined
      };
    });

    return {
      isSuspicious: true,
      originalSender: trimmed,
      targetBrand: normalized,
      reason: 'Wykryto ukryte znaki cyrylicy (homoglify Unicode) udające litery łacińskie!',
      segments,
      confidence: 'HIGH'
    };
  }

  // 2. Porównanie ze znanymi polskimi markami
  const lowerTrimmed = trimmed.toLowerCase();

  for (const brand of PROTECTED_POLISH_BRANDS) {
    const brandLower = brand.name.toLowerCase();

    // Dokładne dopasowanie wielkości liter i treści
    if (trimmed === brand.name) {
      return {
        isSuspicious: false,
        originalSender: trimmed,
        targetBrand: brand.name,
        brandCategory: brand.category,
        reason: null,
        segments: trimmed.split('').map(c => ({ char: c, isSpoofed: false })),
        confidence: 'LOW'
      };
    }

    // A. Przypadek InP0st (zero zamiast litery 'o')
    if (
      (brandLower === 'inpost' && /inp[0o]st/i.test(trimmed) && trimmed.includes('0')) ||
      (trimmed.includes('0') && trimmed.replace(/0/g, 'o').toLowerCase() === brandLower)
    ) {
      const segments: HomographSegment[] = trimmed.split('').map(c => ({
        char: c,
        isSpoofed: c === '0',
        expected: c === '0' ? 'o' : undefined,
        reason: c === '0' ? 'Użyto cyfry zero „0” zamiast litery „o”' : undefined
      }));

      return {
        isSuspicious: true,
        originalSender: trimmed,
        targetBrand: brand.name,
        brandCategory: brand.category,
        reason: `Podrobiony nadawca: użyto cyfry zero „0” w nazwie ${brand.name}`,
        segments,
        confidence: 'HIGH'
      };
    }

    // B. Przypadek AlIegro (wielkie 'I' zamiast małego 'l')
    if (brand.name === 'Allegro' && trimmed.toLowerCase() === 'allegro') {
      // Sprawdzamy czy litery w słowie nie mają 'I' (wielkiego i) zamiast 'l' (małego el)
      const segments: HomographSegment[] = trimmed.split('').map((c, idx) => {
        const expected = brand.name[idx];
        const isSuspiciousChar = c === 'I' && expected === 'l';
        return {
          char: c,
          isSpoofed: isSuspiciousChar,
          expected: isSuspiciousChar ? 'l' : undefined,
          reason: isSuspiciousChar ? 'Użyto wielkiej litery „I” zamiast małej „l”' : undefined
        };
      });

      if (segments.some(s => s.isSpoofed)) {
        return {
          isSuspicious: true,
          originalSender: trimmed,
          targetBrand: brand.name,
          brandCategory: brand.category,
          reason: `Wykryto podstawioną dużą literę „I” zamiast małej „l” w nazwie ${brand.name}`,
          segments,
          confidence: 'HIGH'
        };
      }
    }

    // C. Dziwne znaki na końcu lub w środku (np. 'mBank.', 'Allegro_')
    if (
      (trimmed.startsWith(brand.name) || lowerTrimmed.startsWith(brandLower)) &&
      /[._\-!]$/.test(trimmed) &&
      trimmed.length === brand.name.length + 1
    ) {
      const lastChar = trimmed[trimmed.length - 1];
      const segments: HomographSegment[] = trimmed.split('').map((c, idx) => ({
        char: c,
        isSpoofed: idx === trimmed.length - 1,
        expected: idx === trimmed.length - 1 ? '(brak)' : undefined,
        reason: idx === trimmed.length - 1 ? `Podejrzany znak interpunkcyjny „${lastChar}” na końcu nazwy` : undefined
      }));

      return {
        isSuspicious: true,
        originalSender: trimmed,
        targetBrand: brand.name,
        brandCategory: brand.category,
        reason: `Podejrzany dopisek znaku „${lastChar}” na końcu nazwy ${brand.name}`,
        segments,
        confidence: 'HIGH'
      };
    }

    // D. Odległość Levenshteina 1 dla nazw o długości >= 4 znaków
    if (brand.name.length >= 4) {
      const dist = levenshteinDistance(lowerTrimmed, brandLower);
      if (dist === 1) {
        // Znajdź różniący się znak
        const segments: HomographSegment[] = trimmed.split('').map((c, idx) => {
          const isDiff = idx < brand.name.length && c.toLowerCase() !== brand.name[idx]?.toLowerCase();
          return {
            char: c,
            isSpoofed: isDiff,
            expected: brand.name[idx],
            reason: isDiff ? `Znak różni się od oficjalnej nazwy ${brand.name}` : undefined
          };
        });

        return {
          isSuspicious: true,
          originalSender: trimmed,
          targetBrand: brand.name,
          brandCategory: brand.category,
          reason: `Nazwa różni się o 1 znak od chronionej marki ${brand.name} (możliwe podszycie)`,
          segments,
          confidence: 'MEDIUM'
        };
      }
    }
  }

  return {
    isSuspicious: false,
    originalSender: trimmed,
    targetBrand: null,
    reason: null,
    segments: trimmed.split('').map(c => ({ char: c, isSpoofed: false })),
    confidence: 'LOW'
  };
}
