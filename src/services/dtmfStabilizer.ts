import { VERIFIED_SEED_DIRECTORY } from './directory';

/**
 * Standard ITU-T Q.23 / RFC 4733 DTMF Dual-Tone Frequencies
 * Low Group: 697, 770, 852, 941 Hz
 * High Group: 1209, 1336, 1477, 1633 Hz
 */
export const DTMF_FREQUENCIES: Record<string, [number, number]> = {
  '1': [697, 1209],
  '2': [697, 1336],
  '3': [697, 1477],
  'A': [697, 1633],
  '4': [770, 1209],
  '5': [770, 1336],
  '6': [770, 1477],
  'B': [770, 1633],
  '7': [852, 1209],
  '8': [852, 1336],
  '9': [852, 1477],
  'C': [852, 1633],
  '*': [941, 1209],
  '0': [941, 1336],
  '#': [941, 1477],
  'D': [941, 1633],
};

export const DTMF_TIMING = {
  /** Certyfikowany czas trwania tonu: 200 ms (zakres ITU-T: 180-220 ms) */
  STANDARD_TONE_MS: 200,
  /** Certyfikowana przerwa między tonami: 140 ms (zakres ITU-T: 120-150 ms) */
  STANDARD_GAP_MS: 140,
  /** Tryb przyspieszony (dla zaawansowanych): 120 ms ton + 80 ms przerwa */
  FAST_TONE_MS: 120,
  FAST_GAP_MS: 80,
  /** Bufor bezpieczeństwa bramki Auto-Mute po ostatnim tonie: 350 ms */
  AUTO_MUTE_TAIL_BUFFER_MS: 350,
  /** Łagodny profil obwiedni (5 ms attack/release), aby uniknąć klików przesterowujących filtr Goertzela */
  RAMP_TIME_SEC: 0.005,
};

export interface DtmfStabilizerState {
  enabled: boolean;
  isTransmitting: boolean;
  currentDigit: string | null;
  queue: string[];
  processedCount: number;
  totalInBatch: number;
  autoMuteActive: boolean;
  autoMuteTailActive: boolean;
  detectedIvr: boolean;
  entityName: string | null;
  speedMode: 'STANDARD' | 'FAST';
}

class AudioContextManager {
  private ctx: AudioContext | null = null;

  getContext(): AudioContext | null {
    if (typeof window === 'undefined') return null;
    const AudioCtx = window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext;
    if (!AudioCtx) return null;

    if (!this.ctx || this.ctx.state === 'closed') {
      try {
        this.ctx = new AudioCtx();
      } catch {
        return null;
      }
    }
    if (this.ctx.state === 'suspended') {
      this.ctx.resume().catch(() => {});
    }
    return this.ctx;
  }
}

export const audioCtxManager = new AudioContextManager();

/**
 * Generuje sprzętowo/studyjnie parę czystych tonów sinusoidalnych (Dual-Tone)
 * z precyzyjną obwiednią 5ms attack/release (zero-crossing).
 */
export function playSyntheticDtmfTone(
  digit: string,
  durationMs: number = DTMF_TIMING.STANDARD_TONE_MS
): Promise<void> {
  return new Promise((resolve) => {
    const freqs = DTMF_FREQUENCIES[digit.toUpperCase()];
    if (!freqs) {
      resolve();
      return;
    }

    const ctx = audioCtxManager.getContext();
    if (!ctx) {
      setTimeout(resolve, durationMs);
      return;
    }

    try {
      const now = ctx.currentTime;
      const durationSec = durationMs / 1000;
      const rampSec = DTMF_TIMING.RAMP_TIME_SEC;

      // Master gain node z łagodną obwiednią (eliminacja klików i trzasków)
      const masterGain = ctx.createGain();
      masterGain.gain.setValueAtTime(0.0001, now);
      masterGain.gain.linearRampToValueAtTime(0.32, now + rampSec);
      masterGain.gain.setValueAtTime(0.32, now + durationSec - rampSec);
      masterGain.gain.linearRampToValueAtTime(0.0001, now + durationSec);
      masterGain.connect(ctx.destination);

      // Low frequency oscillator
      const oscLow = ctx.createOscillator();
      oscLow.type = 'sine';
      oscLow.frequency.setValueAtTime(freqs[0], now);
      const gainLow = ctx.createGain();
      gainLow.gain.setValueAtTime(0.5, now);
      oscLow.connect(gainLow);
      gainLow.connect(masterGain);

      // High frequency oscillator
      const oscHigh = ctx.createOscillator();
      oscHigh.type = 'sine';
      oscHigh.frequency.setValueAtTime(freqs[1], now);
      const gainHigh = ctx.createGain();
      gainHigh.gain.setValueAtTime(0.5, now);
      oscHigh.connect(gainHigh);
      gainHigh.connect(masterGain);

      oscLow.start(now);
      oscHigh.start(now);

      const stopTime = now + durationSec;
      oscLow.stop(stopTime);
      oscHigh.stop(stopTime);

      setTimeout(() => {
        try {
          oscLow.disconnect();
          oscHigh.disconnect();
          gainLow.disconnect();
          gainHigh.disconnect();
          masterGain.disconnect();
        } catch {
          // Ignore cleanup errors
        }
        resolve();
      }, durationMs);
    } catch {
      setTimeout(resolve, durationMs);
    }
  });
}

/**
 * Automatyczne wykrywanie infolinii, banków i systemów IVR
 */
export function detectIvrHelpline(phoneNumber: string, contactName?: string): {
  isIvr: boolean;
  reason: string;
  entityName: string | null;
  category?: string;
} {
  const cleanNum = phoneNumber.replace(/\D/g, '');
  const searchName = (contactName || '').toLowerCase();

  // 1. Sprawdzenie w zweryfikowanej bazie podmiotów
  const matchedBiz = VERIFIED_SEED_DIRECTORY.find((b) => {
    const bClean = b.phone.replace(/\D/g, '');
    return (
      (cleanNum && bClean && (bClean.endsWith(cleanNum) || cleanNum.endsWith(bClean))) ||
      b.name.toLowerCase().includes(searchName)
    );
  });

  if (matchedBiz) {
    const isInstitution = ['bank', 'clinic', 'pharmacy', 'post'].includes(matchedBiz.category);
    const hasHelplineWord =
      matchedBiz.name.toLowerCase().includes('infolinia') ||
      matchedBiz.address?.toLowerCase().includes('infolinia') ||
      matchedBiz.address?.toLowerCase().includes('obsługa');

    if (isInstitution || hasHelplineWord) {
      return {
        isIvr: true,
        reason:
          matchedBiz.category === 'bank'
            ? 'Infolinia bankowa (automatyczny moduł IVR/PIN/PESEL)'
            : 'Infolinia instytucji / IVR z menu tonowym',
        entityName: matchedBiz.name,
        category: matchedBiz.category,
      };
    }
  }

  // 2. Prefiksy darmowe i błękitne (800, 801)
  if (cleanNum.startsWith('800') || cleanNum.startsWith('48800')) {
    return {
      isIvr: true,
      reason: 'Bezpłatna infolinia 800 (automatyczny system IVR)',
      entityName: contactName || 'Infolinia 800',
      category: 'helpline',
    };
  }

  if (cleanNum.startsWith('801') || cleanNum.startsWith('48801')) {
    return {
      isIvr: true,
      reason: 'Ogólnokrajowa infolinia 801 (centrala IVR)',
      entityName: contactName || 'Infolinia 801',
      category: 'helpline',
    };
  }

  // 3. Skrócone numery infolinii (np. 19999, 19555, 19xxx)
  if (/^(48)?19\d{3}$/.test(cleanNum)) {
    return {
      isIvr: true,
      reason: 'Skrócony numer centrali bankowej/usługowej (19xxx)',
      entityName: contactName || `Centrala ${cleanNum}`,
      category: 'bank',
    };
  }

  // 4. Słowa kluczowe w nazwie kontaktu
  const keywords = ['bank', 'infolinia', 'santander', 'pko', 'mbank', 'ing', 'alior', 'pekao', 'millennium', 'pomoc', 'bok', 'call center', 'rezerwacja'];
  for (const kw of keywords) {
    if (searchName.includes(kw)) {
      return {
        isIvr: true,
        reason: 'Wykryto słowo kluczowe infolinii w nazwie',
        entityName: contactName || 'Infolinia / Bank',
        category: 'bank',
      };
    }
  }

  return {
    isIvr: false,
    reason: 'Standardowy numer telefonu',
    entityName: null,
  };
}
