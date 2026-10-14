import { CertSnapshot, DomainFinding, MessageAnalysis } from '../types';

export const CERT_MAX_AGE_MS = 5 * 60 * 1000;

export function normalizeDomain(raw: string): string | null {
  try {
    const cleaned = raw.trim().replace(/\.+$/, '').toLowerCase();
    if (cleaned.length <= 253 && cleaned.includes('.')) {
      const parts = cleaned.split('.');
      if (parts.every(p => p.length > 0 && p.length <= 63)) {
        return cleaned;
      }
    }
  } catch {
    return null;
  }
  return null;
}

export function matchDomain(host: string, domainSet: Set<string>): string | null {
  let candidate = normalizeDomain(host);
  if (!candidate) return null;

  while (candidate.includes('.')) {
    if (domainSet.has(candidate)) {
      return candidate;
    }
    const dotIndex = candidate.indexOf('.');
    if (dotIndex === -1) break;
    candidate = candidate.slice(dotIndex + 1);
  }
  return null;
}

export function extractHosts(text: string): string[] {
  const result = new Set<string>();

  // 1. Matches URLs (e.g. http:// or https://)
  const urlRegex = /(?:https?:\/\/)([^\s<>"{}]+)/gi;
  let match: RegExpExecArray | null;

  while ((match = urlRegex.exec(text)) !== null) {
    const full = match[0].replace(/[.,;:!)\]!?]+$/, '');
    try {
      const parsed = new URL(full.startsWith('http') ? full : `http://${full}`);
      const host = normalizeDomain(parsed.hostname);
      if (host) result.add(host);
    } catch {
      // Fallback extraction
      const hostCandidate = match[1].split('/')[0].split('?')[0].split('#')[0].replace(/.*@/, '');
      const host = normalizeDomain(hostCandidate);
      if (host) result.add(host);
    }
  }

  // 2. Matches plain domain names not in URLs
  const withoutUrls = text.replace(urlRegex, ' ');
  const domainRegex = /(?<![@\w.-])([a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,63}(?![@\w.-])/gi;

  while ((match = domainRegex.exec(withoutUrls)) !== null) {
    const host = normalizeDomain(match[0]);
    if (host) result.add(host);
  }

  return Array.from(result).slice(0, 100);
}

export function analyzeMessage(text: string, snapshot: CertSnapshot | null): MessageAnalysis {
  const lower = text.toLowerCase();
  const signals: string[] = [];

  const credentialKeywords = ['blik', 'kod autoryzacyjny', 'kod sms', 'hasło', 'haslo', 'pin do'];
  if (credentialKeywords.some(k => lower.includes(k))) {
    signals.push('Treść wspomina kod płatności lub dane dostępu. Nikomu ich nie przekazuj.');
  }

  const urgencyKeywords = [
    'dopła', 'doplac', 'doplat', 'zablokowane', 'zablokujemy',
    'natychmiast', 'pilne', 'ostatnia szansa'
  ];
  if (urgencyKeywords.some(k => lower.includes(k))) {
    signals.push('Występuje presja czasu lub wezwanie do dopłaty. To sygnał językowy, nie dowód oszustwa.');
  }

  const hosts = extractHosts(text);
  const domainSet = snapshot ? new Set(snapshot.domains) : null;

  const findings: DomainFinding[] = hosts.map(host => ({
    host,
    matchedDomain: domainSet ? matchDomain(host, domainSet) : null
  }));

  return {
    findings,
    localSignals: signals,
    snapshotTime: snapshot ? snapshot.fetchedAt : null
  };
}

export function isSnapshotFresh(snapshot: CertSnapshot | null, now: number = Date.now()): boolean {
  if (!snapshot) return false;
  return now >= snapshot.fetchedAt && now - snapshot.fetchedAt <= CERT_MAX_AGE_MS;
}
