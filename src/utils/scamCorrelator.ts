import { CallItem, DeviceContact, DeviceSms } from '../types';

export interface CorrelatedScamAlert {
  id: string;
  activeCallNumber: string;
  activeCallDisplayName: string;
  isKnownContact: boolean;
  smsId: number;
  smsSender: string;
  smsTime: number;
  smsOriginalBody: string;
  censoredBody: string;
  extractedCode: string | null;
  matchedKeywords: string[];
  riskScore: 'CRITICAL' | 'HIGH';
  explanation: string;
}

const CRITICAL_SCAM_KEYWORDS = [
  'kod',
  'autoryzacj',
  'przelew',
  'blik',
  'nowe urządzenie',
  'nowego urządzenia',
  'limit',
  'zatwierdź',
  'potwierdź',
  'hasło',
  '3d-secure',
  '3ds',
  'transakcj',
  'logowanie',
  'bezpieczny rachunek',
  'anulowanie kredytu',
  'zablokowani',
  'weryfikacj'
];

/**
 * Wykrywa i ekstrahuje kod autoryzacyjny z treści SMS (BLIK, 3DS, SMS-bankowy)
 */
export function extractAuthorizationCode(body: string): { code: string | null; censoredBody: string } {
  // Szukamy kodów 4-8 cyfrowych, w tym z myślnikiem np. 123-456 lub "kod: 748291"
  const patterns = [
    /(?:kod(?:em)?|hasło|autoryzacji|blik|kod to)[:\s]+([0-9]{4,8}|[0-9]{3}[-\s][0-9]{3})/i,
    /\b([0-9]{3}[-][0-9]{3})\b/,
    /\b([0-9]{6})\b/, // standardowy 6-cyfrowy BLIK / mBank / PKO
    /\b([0-9]{4,8})\b/
  ];

  for (const regex of patterns) {
    const match = regex.exec(body);
    if (match && match[1]) {
      const code = match[1];
      const censoredBody = body.replace(code, '[ KOD UKRYTY: •••••• ]');
      return { code, censoredBody };
    }
  }

  return { code: null, censoredBody: body };
}

/**
 * Główna funkcja korelatora scamu: łączy trwające połączenie ze świeżo otrzymanym SMS-em
 */
export function evaluateScamCorrelation(
  activeCall: CallItem | undefined,
  contacts: DeviceContact[],
  recentSmsList: DeviceSms[],
  now: number = Date.now()
): CorrelatedScamAlert | null {
  if (!activeCall || activeCall.state !== 'ACTIVE') return null;

  // Sprawdzamy czy numer rozmówcy to zaufany, stały kontakt
  const cleanCallNum = activeCall.number.replace(/\D/g, '');
  const matchedContact = contacts.find(c => {
    const cleanC = c.phone.replace(/\D/g, '');
    return cleanC && cleanCallNum && (cleanC.endsWith(cleanCallNum) || cleanCallNum.endsWith(cleanC));
  });

  const isKnownContact = Boolean(matchedContact);
  const displayName = matchedContact?.name || activeCall.number;

  // Szukamy SMS-ów odebranych w czasie trwania połączenia (lub w oknie ostatnich 3 minut)
  const callStartTime = activeCall.connectedAt > 0 ? activeCall.connectedAt : now - 180000;
  // Margines 30 sekund przed połączeniem (częsty scenariusz: oszust wysyła SMS chwilę przed odebraniem)
  const windowStart = callStartTime - 30000;

  const relevantSms = recentSmsList.filter(sms => {
    return sms.incoming && sms.time >= windowStart;
  });

  if (relevantSms.length === 0) return null;

  // Analizujemy najświeższy pasujący SMS
  for (const sms of relevantSms) {
    const lowerBody = sms.body.toLowerCase();
    const matchedKeywords = CRITICAL_SCAM_KEYWORDS.filter(kw => lowerBody.includes(kw));

    // Jeśli zawiera słowa kluczowe banku/autoryzacji LUB link URL w trakcie rozmowy
    const hasSensitiveWords = matchedKeywords.length >= 1;
    const hasUrl = /https?:\/\/[^\s]+/i.test(sms.body);

    if (hasSensitiveWords || hasUrl) {
      const { code, censoredBody } = extractAuthorizationCode(sms.body);

      const explanation = !isKnownContact
        ? 'Rozmawiasz z numerem spoza kontaktów, a w tej samej chwili przyszedł SMS autoryzacyjny! Prawdziwy pracownik banku NIGDY nie prosi o odczytanie kodu z SMS podczas rozmowy.'
        : 'W trakcie połączenia przyszedł SMS z kodem autoryzacyjnym. Upewnij się, że nie czytasz go nikomu na głos.';

      return {
        id: `scam_alert_${sms.id}_${activeCall.id}`,
        activeCallNumber: activeCall.number,
        activeCallDisplayName: displayName,
        isKnownContact,
        smsId: sms.id,
        smsSender: sms.address,
        smsTime: sms.time,
        smsOriginalBody: sms.body,
        censoredBody,
        extractedCode: code,
        matchedKeywords,
        riskScore: !isKnownContact ? 'CRITICAL' : 'HIGH',
        explanation
      };
    }
  }

  return null;
}

/**
 * Generator symulowanego SMS-a bankowego do testów i demonstracji w czasie rzeczywistym
 */
export function generateBankScamSms(bankName: string = 'mBank'): DeviceSms {
  const codes = ['781-942', '593-018', '412-880', '935-127'];
  const randomCode = codes[Math.floor(Math.random() * codes.length)];
  const amount = (Math.floor(Math.random() * 40) + 10) * 100; // np. 2400 PLN

  return {
    id: Date.now(),
    thread: 9999,
    address: bankName,
    body: `${bankName}: Twój jednorazowy kod autoryzacji do przelewu ekspresowego na kwotę ${amount.toLocaleString('pl-PL')},00 PLN to: ${randomCode}. NIGDY nie podawaj tego kodu nikomu na infolinii ani przez telefon!`,
    time: Date.now(),
    type: 1, // Inbox
    read: false,
    incoming: true
  };
}
