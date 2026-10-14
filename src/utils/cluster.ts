import { CallCluster, ClusterMatchResult, ClusterSettings, DeviceContact } from '../types';
import { parsePhone } from './phone';

export const DEFAULT_CLUSTER_SETTINGS: ClusterSettings = {
  prefixDigits: 6,           // First 6 national digits (e.g. 901 234)
  windowMinutes: 60,         // 60-minute correlation window
  minAttemptsToTrigger: 2,   // 2 calls from different suffixes trigger cluster
  autoBlockPool: true        // Auto-quarantine the entire pool
};

/**
 * Extracts a normalized prefix string and display mask.
 * e.g., +48901234567 -> prefix: "+48901234", mask: "+48 901 234 •••"
 */
export function extractClusterPrefix(
  phoneNumber: string,
  prefixDigits: number = 6
): { prefix: string; mask: string; nationalDigits: string } | null {
  const parsed = parsePhone(phoneNumber);
  const cleanNumber = parsed?.e164 || phoneNumber.replace(/[^\d+]/g, '');

  if (!cleanNumber) return null;

  // Handle Polish (+48) and general international numbers
  const hasPlus48 = cleanNumber.startsWith('+48');
  let national = cleanNumber;
  let countryCode = '+48';

  if (hasPlus48) {
    national = cleanNumber.slice(3);
  } else if (cleanNumber.startsWith('+')) {
    // Other country code
    countryCode = cleanNumber.slice(0, 3);
    national = cleanNumber.slice(3);
  } else if (cleanNumber.length === 9) {
    national = cleanNumber;
  }

  // Ensure we have enough digits to build prefix
  if (national.length < prefixDigits + 1) {
    return null;
  }

  const prefixSegment = national.slice(0, prefixDigits);
  const fullPrefix = `${countryCode}${prefixSegment}`;

  // Build human readable mask
  let maskFormatted = fullPrefix;
  if (countryCode === '+48' && national.length === 9) {
    // 901 234 xxx or 901 234 5xx
    const p1 = national.slice(0, 3);
    const p2 = national.slice(3, 6);
    const p3 = prefixDigits > 6 ? national.slice(6, prefixDigits) : '';
    const dotsCount = 9 - prefixDigits;
    const dots = '•'.repeat(dotsCount);

    maskFormatted = `+48 ${p1} ${p2} ${p3}${dots}`.trim();
  } else {
    maskFormatted = `${fullPrefix}••••`;
  }

  return {
    prefix: fullPrefix,
    mask: maskFormatted,
    nationalDigits: national
  };
}

/**
 * Evaluates whether an incoming call belongs to an existing cluster
 * or forms a new cluster based on recent call history within the time window.
 */
export function evaluateCallForCluster(
  incomingNumber: string,
  existingClusters: CallCluster[],
  recentCalls: { number: string; date: number }[],
  contacts: DeviceContact[],
  settings: ClusterSettings = DEFAULT_CLUSTER_SETTINGS,
  now: number = Date.now()
): {
  result: ClusterMatchResult;
  updatedCluster?: CallCluster;
  isNewCluster?: boolean;
} {
  const parsed = parsePhone(incomingNumber);
  const e164 = parsed?.e164 || incomingNumber;

  // RULE 1: Whitelist-First. Never cluster or block recognized contacts!
  const isContact = contacts.some(c => {
    const contactParsed = parsePhone(c.phone);
    return (contactParsed?.e164 || c.phone) === e164;
  });

  if (isContact) {
    return {
      result: {
        isMatch: false,
        cluster: null,
        shouldBlock: false,
        alertMessage: null
      }
    };
  }

  const prefixInfo = extractClusterPrefix(e164, settings.prefixDigits);
  if (!prefixInfo) {
    return {
      result: {
        isMatch: false,
        cluster: null,
        shouldBlock: false,
        alertMessage: null
      }
    };
  }

  // Check against existing detected clusters
  const existingCluster = existingClusters.find(c => c.prefix === prefixInfo.prefix);

  if (existingCluster) {
    if (existingCluster.status === 'ALLOWED_WHITELIST') {
      return {
        result: {
          isMatch: true,
          cluster: existingCluster,
          shouldBlock: false,
          alertMessage: 'Pula numerów oznaczona przez Ciebie jako zaufana.'
        }
      };
    }

    // Add number if new
    const updatedNumbers = Array.from(new Set([...existingCluster.numbers, e164]));
    const updatedCluster: CallCluster = {
      ...existingCluster,
      numbers: updatedNumbers,
      callCount: existingCluster.callCount + 1,
      lastSeen: now,
      riskScore: 'HIGH'
    };

    const shouldBlock =
      settings.autoBlockPool &&
      (existingCluster.status === 'ACTIVE_QUARANTINE' ||
        existingCluster.status === 'BLOCKED_PERMANENT');

    return {
      result: {
        isMatch: true,
        cluster: updatedCluster,
        shouldBlock,
        alertMessage: `Wykryto schemat rotacji centrali (${prefixInfo.mask}) — ${updatedNumbers.length} różne końcówki w puli.`
      },
      updatedCluster
    };
  }

  // Look for correlated calls in recent history within the time window
  const windowMs = settings.windowMinutes * 60 * 1000;
  const windowStart = now - windowMs;

  const correlatedCalls = recentCalls.filter(call => {
    if (call.date < windowStart) return false;
    const callPrefix = extractClusterPrefix(call.number, settings.prefixDigits);
    return callPrefix && callPrefix.prefix === prefixInfo.prefix;
  });

  const uniqueCorrelatedNumbers = Array.from(
    new Set([e164, ...correlatedCalls.map(c => c.number)])
  );

  // If we have >= minAttemptsToTrigger different numbers from same prefix in the window -> CREATE CLUSTER!
  if (uniqueCorrelatedNumbers.length >= settings.minAttemptsToTrigger) {
    const newCluster: CallCluster = {
      id: `cluster_${Date.now()}`,
      prefix: prefixInfo.prefix,
      mask: prefixInfo.mask,
      numbers: uniqueCorrelatedNumbers,
      callCount: correlatedCalls.length + 1,
      firstSeen: Math.min(...correlatedCalls.map(c => c.date), now),
      lastSeen: now,
      status: settings.autoBlockPool ? 'ACTIVE_QUARANTINE' : 'ACTIVE_QUARANTINE',
      riskScore: 'HIGH',
      reason: `Wykryto rotację ${uniqueCorrelatedNumbers.length} numerów z puli w oknie ${settings.windowMinutes} minut.`
    };

    return {
      result: {
        isMatch: true,
        cluster: newCluster,
        shouldBlock: settings.autoBlockPool,
        alertMessage: `Uwaga: Wykryto rotacyjny schemat połączeń (${prefixInfo.mask}) w oknie ${settings.windowMinutes} min!`
      },
      updatedCluster: newCluster,
      isNewCluster: true
    };
  }

  return {
    result: {
      isMatch: false,
      cluster: null,
      shouldBlock: false,
      alertMessage: null
    }
  };
}
