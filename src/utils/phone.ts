import { parsePhoneNumberFromString } from 'libphonenumber-js';
import { ParsedPhone } from '../types';

export function parsePhone(input: string): ParsedPhone | null {
  if (!input || !input.trim()) return null;
  const cleaned = input.trim();
  if (!/^[+0-9() .–-]{5,40}$/.test(cleaned)) {
    return null;
  }
  try {
    const phoneNumber = parsePhoneNumberFromString(cleaned, 'PL');
    if (phoneNumber && phoneNumber.isValid()) {
      return {
        e164: phoneNumber.format('E.164'),
        formatted: phoneNumber.formatInternational(),
        country: phoneNumber.country || 'PL'
      };
    }
  } catch {
    return null;
  }
  return null;
}

export function normalizeDialTarget(value: string): string | null {
  const normalized = value.replace(/[\s()\-.]/g, '');
  if (
    normalized.length >= 1 &&
    normalized.length <= 40 &&
    /^\+?[0-9*#]+$/.test(normalized) &&
    /[0-9]/.test(normalized)
  ) {
    return normalized;
  }
  return null;
}
