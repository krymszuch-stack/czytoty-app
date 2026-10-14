import { WangiriRiskInfo, WangiriRiskLevel, WangiriSettings } from '../types';
import { parsePhone } from './phone';

export interface WangiriDestination {
  prefix: string;
  country: string;
  flag: string;
  avgCostPln: number;
  costRange: string;
  camouflageTarget?: string;
  notes: string;
}

export const WANGIRI_HIGH_RISK_DESTINATIONS: WangiriDestination[] = [
  {
    prefix: '+247',
    country: 'Wyspa Wniebowstąpienia',
    flag: '🇦🇨',
    avgCostPln: 32.5,
    costRange: '25 – 42 zł/min',
    notes: 'Kultowy kierunek ataków Wangiri. Małe terytorium o wysokich stawkach rozliczeniowych.'
  },
  {
    prefix: '+236',
    country: 'Republika Środkowoafrykańska',
    flag: '🇨🇫',
    avgCostPln: 24.0,
    costRange: '18 – 35 zł/min',
    notes: 'Masowe kampanie botów generujące 1-sekundowe pingi w nocy i nad ranem.'
  },
  {
    prefix: '+235',
    country: 'Czad',
    flag: '🇹🇩',
    avgCostPln: 22.0,
    costRange: '16 – 30 zł/min',
    notes: 'Wysokie koszty tranzytu telekomunikacyjnego.'
  },
  {
    prefix: '+223',
    country: 'Mali',
    flag: '🇲🇱',
    avgCostPln: 21.0,
    costRange: '15 – 28 zł/min',
    camouflageTarget: 'Warszawa (kierunkowy 22 / 022)',
    notes: 'Celowy kamuflaż optyczny pod stacjonarne numery ze strefy warszawskiej (+22).'
  },
  {
    prefix: '+221',
    country: 'Senegal',
    flag: '🇸🇳',
    avgCostPln: 19.5,
    costRange: '14 – 26 zł/min',
    camouflageTarget: 'Warszawa (kierunkowy 22 / 022)',
    notes: 'Kamuflaż optyczny: numer zaczyna się od +22, ofiara myśli że dzwoni urząd ze stolicy.'
  },
  {
    prefix: '+222',
    country: 'Mauretania',
    flag: '🇲🇷',
    avgCostPln: 20.0,
    costRange: '15 – 27 zł/min',
    camouflageTarget: 'Warszawa (kierunkowy 22 / 022)',
    notes: 'Kamuflaż pod Warszawę.'
  },
  {
    prefix: '+224',
    country: 'Gwinea',
    flag: '🇬🇳',
    avgCostPln: 22.5,
    costRange: '16 – 30 zł/min',
    camouflageTarget: 'Warszawa (kierunkowy 22 / 022)',
    notes: 'Kamuflaż pod Warszawę.'
  },
  {
    prefix: '+225',
    country: 'Wybrzeże Kości Słoniowej',
    flag: '🇨🇮',
    avgCostPln: 19.0,
    costRange: '14 – 26 zł/min',
    camouflageTarget: 'Warszawa (kierunkowy 22 / 022)',
    notes: 'Kamuflaż pod Warszawę.'
  },
  {
    prefix: '+226',
    country: 'Burkina Faso',
    flag: '🇧🇫',
    avgCostPln: 18.5,
    costRange: '14 – 25 zł/min',
    camouflageTarget: 'Warszawa (kierunkowy 22 / 022)',
    notes: 'Kamuflaż pod Warszawę.'
  },
  {
    prefix: '+243',
    country: 'Demokratyczna Republika Konga',
    flag: '🇨🇩',
    avgCostPln: 21.5,
    costRange: '16 – 29 zł/min',
    notes: 'Częsty wektor ping-calli w Polsce.'
  },
  {
    prefix: '+216',
    country: 'Tunezja',
    flag: '🇹🇳',
    avgCostPln: 16.5,
    costRange: '12 – 24 zł/min',
    notes: 'Kierunek pozaunijny o podwyższonej stawce połączeń głosowych.'
  },
  {
    prefix: '+255',
    country: 'Tanzania',
    flag: '🇹🇿',
    avgCostPln: 18.0,
    costRange: '13 – 26 zł/min',
    notes: 'Numery satelitarne i komórkowe poza regulacją UE.'
  },
  {
    prefix: '+387',
    country: 'Bośnia i Hercegowina',
    flag: '🇧🇦',
    avgCostPln: 12.0,
    costRange: '9 – 18 zł/min',
    notes: 'Kraj europejski poza UE/EOG — brak unijnych limitów roamingowych (Eurotaryfy).'
  },
  {
    prefix: '+675',
    country: 'Papua Nowa Gwinea',
    flag: '🇵🇬',
    avgCostPln: 29.0,
    costRange: '22 – 38 zł/min',
    notes: 'Wyspiarski prefiks pacyficzny o ekstremalnie wysokich stawkach terminacji.'
  },
  {
    prefix: '+685',
    country: 'Samoa',
    flag: '🇼🇸',
    avgCostPln: 27.5,
    costRange: '20 – 36 zł/min',
    notes: 'Wysokie koszty łączy pacyficznych.'
  },
  {
    prefix: '+509',
    country: 'Haiti',
    flag: '🇭🇹',
    avgCostPln: 23.0,
    costRange: '17 – 32 zł/min',
    notes: 'Rejon Karaibów o podwyższonej opłacie.'
  },
  {
    prefix: '+290',
    country: 'Wyspa Świętej Heleny',
    flag: '🇸🇭',
    avgCostPln: 28.0,
    costRange: '22 – 36 zł/min',
    notes: 'Terytorium zamorskie Wielkiej Brytanii na Atlantyku.'
  },
  {
    prefix: '+881',
    country: 'Globalstar / Iridium (Sieć Satelitarna)',
    flag: '🛰️',
    avgCostPln: 45.0,
    costRange: '35 – 65 zł/min',
    notes: 'Pasma satelitarne. Koszt minuty może przekroczyć 50 zł!'
  },
  {
    prefix: '+882',
    country: 'Międzynarodowe Sieci Specjalne (Inmarsat/Thuraya)',
    flag: '🛰️',
    avgCostPln: 48.0,
    costRange: '38 – 70 zł/min',
    notes: 'Numery satelitarne o zmiennej taryfikacji specjalnej.'
  }
];

export const DEFAULT_WANGIRI_SETTINGS: WangiriSettings = {
  shieldEnabled: true,
  autoBlockHighRiskPing: true,
  interceptOutgoingCall: true,
  alertOnCamouflage: true,
  interceptedAttemptsCount: 0
};

/**
 * Normalizes phone number into clean international format
 */
export function cleanInternational(phone: string): string {
  const p = parsePhone(phone);
  if (p?.e164) return p.e164;
  const raw = phone.replace(/[^\d+]/g, '');
  if (raw.startsWith('00')) return '+' + raw.slice(2);
  return raw.startsWith('+') ? raw : '+' + raw;
}

/**
 * Checks if number matches Polish Premium Rate numbers (PRN 70x, 80x)
 */
export function checkPolishPremiumRate(cleanNumber: string): {
  isPremium: boolean;
  prefix: string;
  costPln: number;
  costRange: string;
} | null {
  let national = cleanNumber.replace(/[^\d]/g, '');
  if (national.startsWith('48') && national.length === 11) {
    national = national.slice(2);
  }

  if (national.length >= 3) {
    const p3 = national.slice(0, 3);
    if (['700', '701', '703', '704', '707', '708'].includes(p3)) {
      // In Poland PRN 70x cost depends on 4th or 5th digit (e.g. 708 4xx vs 708 8xx)
      const sub = national.length >= 4 ? parseInt(national[3], 10) : 5;
      const cost = Math.max(2.5, Math.min(25, sub * 2.8));
      return {
        isPremium: true,
        prefix: `+48 ${p3} •••`,
        costPln: cost,
        costRange: `${(cost * 0.8).toFixed(1)} – ${(cost * 1.3).toFixed(1)} zł/min (lub za poł.)`
      };
    }
  }
  return null;
}

/**
 * Evaluates whether a phone number matches Wangiri attack indicators.
 */
export function evaluateWangiriRisk(
  phoneNumber: string,
  callDurationSec: number = 0,
  ringDurationSec: number = 2,
  isMissedOrRejected: boolean = true
): WangiriRiskInfo {
  const clean = cleanInternational(phoneNumber);

  // 1. Check Polish Premium Rate
  const prn = checkPolishPremiumRate(clean);
  if (prn) {
    return {
      isWangiri: true,
      riskLevel: 'CRITICAL',
      countryName: 'Polska (Usługa Premium Rate 70x)',
      countryFlag: '🇵🇱',
      prefix: prn.prefix,
      matchedPrefix: prn.prefix,
      estimatedCostPlnMin: prn.costPln,
      estimatedCostRange: prn.costRange,
      isOneRing: isMissedOrRejected && callDurationSec === 0,
      durationSec: callDurationSec,
      ringDurationSec,
      isCamouflage: false,
      camouflageTarget: null,
      explanation: 'Numer o podwyższonej opłacie (UKE Premium Rate). Oddzwonienie może skutkować naliczeniem wysokiej opłaty za połączenie lub minutę.',
      recommendation: 'Nie oddzwaniaj! To krajowy numer specjalny generujący wysokie opłaty na rachunku.'
    };
  }

  // 2. Check High-Risk International Destinations
  for (const dest of WANGIRI_HIGH_RISK_DESTINATIONS) {
    if (clean.startsWith(dest.prefix)) {
      const isOneRing = isMissedOrRejected && callDurationSec === 0 && ringDurationSec <= 4;
      const isCamouflage = Boolean(dest.camouflageTarget);

      return {
        isWangiri: true,
        riskLevel: isOneRing ? 'CRITICAL' : 'HIGH',
        countryName: dest.country,
        countryFlag: dest.flag,
        prefix: dest.prefix,
        matchedPrefix: dest.prefix,
        estimatedCostPlnMin: dest.avgCostPln,
        estimatedCostRange: dest.costRange,
        isOneRing,
        durationSec: callDurationSec,
        ringDurationSec,
        isCamouflage,
        camouflageTarget: dest.camouflageTarget || null,
        explanation: isOneRing
          ? `Wykryto klasyczny atak "One-Ring & Drop" (połączenie trwało zaledwie ${ringDurationSec}s). Automat rozłączył się celowo, by skłonić Cię do kosztownego oddzwonienia.`
          : `Połączenie z egzotycznej destynacji (${dest.country}), poza strefą taniego roamingu UE. Oddzwonienie niesie wysoki koszt minutowy.`,
        recommendation: `Zablokuj ten numer i NIE ODDZWANIAJ! Szacowany koszt minuty połączenia to ${dest.costRange}.`
      };
    }
  }

  // 3. Check General Foreign Non-EEA Prefixes
  // If number starts with foreign code other than standard European Union (+49, +44, +33, etc.)
  if (clean.startsWith('+') && !clean.startsWith('+48')) {
    const isOneRing = isMissedOrRejected && callDurationSec === 0 && ringDurationSec <= 3.5;
    if (isOneRing) {
      return {
        isWangiri: true,
        riskLevel: 'SUSPECTED',
        countryName: 'Kierunek międzynarodowy',
        countryFlag: '🌍',
        prefix: clean.slice(0, 4),
        matchedPrefix: clean.slice(0, 4),
        estimatedCostPlnMin: 12.0,
        estimatedCostRange: '8 – 20 zł/min',
        isOneRing: true,
        durationSec: callDurationSec,
        ringDurationSec,
        isCamouflage: clean.startsWith('+22'),
        camouflageTarget: clean.startsWith('+22') ? 'Warszawa (kierunkowy 22)' : null,
        explanation: 'Nieodebrane bardzo krótkie połączenie zagraniczne (1 sygnał). Może to być próba naciągnięcia na kosztowne oddzwonienie.',
        recommendation: 'Zachowaj ostrożność. Upewnij się, czy znasz ten kontakt, zanim oddzwonisz.'
      };
    }
  }

  // Safe
  return {
    isWangiri: false,
    riskLevel: 'SAFE',
    countryName: 'Polska / Kontakt standardowy',
    countryFlag: '🇵🇱',
    prefix: '+48',
    matchedPrefix: '+48',
    estimatedCostPlnMin: 0,
    estimatedCostRange: '0 zł (w pakiecie no-limit)',
    isOneRing: false,
    durationSec: callDurationSec,
    ringDurationSec,
    isCamouflage: false,
    camouflageTarget: null,
    explanation: 'Standardowe połączenie krajowe lub bezpieczne.',
    recommendation: 'Bezpieczne połączenie.'
  };
}

/**
 * Specifically intercepts an outgoing dial attempt to determine if safety dialog is required.
 */
export function checkOutgoingWangiriRisk(phoneNumber: string): WangiriRiskInfo | null {
  const evalResult = evaluateWangiriRisk(phoneNumber, 0, 0, false);
  if (evalResult.isWangiri && (evalResult.riskLevel === 'CRITICAL' || evalResult.riskLevel === 'HIGH' || evalResult.riskLevel === 'SUSPECTED')) {
    return evalResult;
  }
  return null;
}
