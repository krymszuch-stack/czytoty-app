import React, { useState, useEffect, useRef, useCallback } from 'react';
import {
  ShieldCheck,
  Shield,
  MicOff,
  Mic,
  Activity,
  Clipboard,
  Trash2,
  Radio,
  Sliders,
  Check,
  Volume2
} from 'lucide-react';
import {
  DTMF_TIMING,
  DTMF_FREQUENCIES,
  playSyntheticDtmfTone,
  detectIvrHelpline
} from '../services/dtmfStabilizer';

interface IvrStabilizerPanelProps {
  activeCallNumber: string;
  activeCallDisplayName: string;
  isMuted: boolean;
  onSetMuted: (muted: boolean) => void;
  onSendToneLog?: (digit: string) => void;
  enteredDigits: string;
  onDigitsChange: (digits: string) => void;
}

export function IvrStabilizerPanel({
  activeCallNumber,
  activeCallDisplayName,
  isMuted,
  onSetMuted,
  onSendToneLog,
  enteredDigits,
  onDigitsChange,
}: IvrStabilizerPanelProps) {
  // Wykrywanie infolinii/banku
  const ivrInfo = detectIvrHelpline(activeCallNumber, activeCallDisplayName);

  // Stan stabilizatora
  const [stabilizerEnabled, setStabilizerEnabled] = useState<boolean>(true);
  const [speedMode, setSpeedMode] = useState<'STANDARD' | 'FAST'>('STANDARD'); // 200ms vs 120ms
  const [showPasteModal, setShowPasteModal] = useState<boolean>(false);
  const [pasteInputValue, setPasteInputValue] = useState<string>('');

  // Stan transmisji i kolejki
  const [isTransmitting, setIsTransmitting] = useState<boolean>(false);
  const [currentTone, setCurrentTone] = useState<string | null>(null);
  const [queueLength, setQueueLength] = useState<number>(0);
  const [transmittedCount, setTransmittedCount] = useState<number>(0);
  const [totalInBatch, setTotalInBatch] = useState<number>(0);

  // Stan bramki Auto-Mute AEC
  const [isAutoMuted, setIsAutoMuted] = useState<boolean>(false);
  const [isTailBuffer, setIsTailBuffer] = useState<boolean>(false);

  // Referencje asynchroniczne
  const queueRef = useRef<string[]>([]);
  const isWorkerRunningRef = useRef<boolean>(false);
  const wasUserMutedBeforeTypingRef = useRef<boolean>(false);
  const isAutoMutedRef = useRef<boolean>(false);
  const abortTransmissionRef = useRef<boolean>(false);

  // Dostęp do najświeższego stanu wyciszenia bez desynchronizacji
  const isMutedRef = useRef<boolean>(isMuted);
  useEffect(() => {
    isMutedRef.current = isMuted;
  }, [isMuted]);

  const toneDuration = speedMode === 'STANDARD' ? DTMF_TIMING.STANDARD_TONE_MS : DTMF_TIMING.FAST_TONE_MS;
  const gapDuration = speedMode === 'STANDARD' ? DTMF_TIMING.STANDARD_GAP_MS : DTMF_TIMING.FAST_GAP_MS;

  // Główny procesor kolejki DTMF z zachowaniem ścisłego timingu ITU-T
  const processQueue = useCallback(async () => {
    if (isWorkerRunningRef.current) return;
    isWorkerRunningRef.current = true;
    abortTransmissionRef.current = false;
    setIsTransmitting(true);

    // Krok 1: W ułamku milisekundy przed pierwszym tonem, Bramka AEC odcina mikrofon
    if (!isAutoMutedRef.current) {
      wasUserMutedBeforeTypingRef.current = isMutedRef.current;
      isAutoMutedRef.current = true;
      setIsAutoMuted(true);
      onSetMuted(true);
    }

    while (queueRef.current.length > 0 && !abortTransmissionRef.current) {
      const digit = queueRef.current.shift()!;
      setQueueLength(queueRef.current.length);
      setCurrentTone(digit);

      // Wibracja haptics zsynchronizowana z początkiem tonu
      if (typeof navigator !== 'undefined' && 'vibrate' in navigator) {
        try {
          navigator.vibrate(35);
        } catch {
          // ignore
        }
      }

      onSendToneLog?.(digit);

      // Nadawanie podwójnego tonu sinusoidalnego Web Audio API
      await playSyntheticDtmfTone(digit, toneDuration);

      setTransmittedCount(prev => prev + 1);
      setCurrentTone(null);

      // Przerwa międzycyfrowa (Inter-digit silence)
      if (queueRef.current.length > 0 && !abortTransmissionRef.current) {
        await new Promise(res => setTimeout(res, gapDuration));
      }
    }

    // Krok 2: Po opróżnieniu kolejki – bufor bezpieczeństwa 350 ms
    if (isAutoMutedRef.current && !abortTransmissionRef.current) {
      setIsTailBuffer(true);
      await new Promise(res => setTimeout(res, DTMF_TIMING.AUTO_MUTE_TAIL_BUFFER_MS));
      setIsTailBuffer(false);

      // Jeśli użytkownik przed pisaniem nie miał wyciszonego mikrofonu, odblokowujemy mikrofon
      if (!wasUserMutedBeforeTypingRef.current) {
        onSetMuted(false);
      }
      isAutoMutedRef.current = false;
      setIsAutoMuted(false);
    }

    isWorkerRunningRef.current = false;
    setIsTransmitting(false);
    setCurrentTone(null);
    setQueueLength(0);
    setTransmittedCount(0);
    setTotalInBatch(0);
  }, [toneDuration, gapDuration, onSetMuted, onSendToneLog]);

  // Dodawanie cyfry / ciągu cyfr do kolejki
  const enqueueDigits = useCallback((digits: string) => {
    const valid = digits.split('').filter(d => DTMF_FREQUENCIES[d.toUpperCase()]);
    if (valid.length === 0) return;

    if (!stabilizerEnabled) {
      // Tryb bezpośredni bez stabilizatora
      valid.forEach(d => {
        playSyntheticDtmfTone(d, 120);
        onSendToneLog?.(d);
      });
      onDigitsChange((enteredDigits + valid.join('')).slice(-30));
      return;
    }

    queueRef.current.push(...valid);
    setQueueLength(queueRef.current.length);
    setTotalInBatch(prev => prev + valid.length);
    onDigitsChange((enteredDigits + valid.join('')).slice(-30));

    // Uruchom worker kolejki
    processQueue();
  }, [enteredDigits, onDigitsChange, onSendToneLog, stabilizerEnabled, processQueue]);

  // Przerwanie nadawania i wyczyszczenie
  const handleStopAndClear = () => {
    abortTransmissionRef.current = true;
    queueRef.current = [];
    setQueueLength(0);
    setIsTransmitting(false);
    setCurrentTone(null);
    setIsTailBuffer(false);

    if (isAutoMutedRef.current) {
      if (!wasUserMutedBeforeTypingRef.current) {
        onSetMuted(false);
      }
      isAutoMutedRef.current = false;
      setIsAutoMuted(false);
    }
    isWorkerRunningRef.current = false;
    onDigitsChange('');
  };

  const handleQuickPasteSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!pasteInputValue.trim()) return;
    const cleanDigits = pasteInputValue.replace(/[^0-9*#a-dA-D]/g, '');
    if (cleanDigits) {
      enqueueDigits(cleanDigits);
    }
    setPasteInputValue('');
    setShowPasteModal(false);
  };

  const handlePasteFromClipboard = async () => {
    try {
      if (typeof navigator !== 'undefined' && navigator.clipboard) {
        const text = await navigator.clipboard.readText();
        const clean = text.replace(/[^0-9*#a-dA-D]/g, '');
        if (clean) {
          enqueueDigits(clean);
          return;
        }
      }
    } catch {
      // Fallback do modala
    }
    setShowPasteModal(true);
  };

  const currentFreqs = currentTone ? DTMF_FREQUENCIES[currentTone.toUpperCase()] : null;

  return (
    <div className="w-full max-w-sm mx-auto space-y-3 animate-in fade-in duration-200">
      {/* Karta statusu Stabilizatora IVR */}
      <div
        className={`rounded-2xl border p-3 transition-all ${
          stabilizerEnabled
            ? isTransmitting
              ? 'bg-[#15162B] border-emerald-500/40 shadow-lg shadow-emerald-500/10'
              : 'bg-[#15162B]/90 border-[#2E2C4A]'
            : 'bg-[#121320]/60 border-white/5 opacity-70'
        }`}
      >
        {/* Górny pasek kontrolny stabilizatora */}
        <div className="flex items-center justify-between gap-2 pb-2 border-b border-white/5">
          <div className="flex items-center gap-2 min-w-0">
            <div
              className={`w-7 h-7 rounded-lg flex items-center justify-center transition-colors ${
                stabilizerEnabled
                  ? isTransmitting
                    ? 'bg-emerald-500/20 text-emerald-400'
                    : 'bg-[#7B4DFF]/20 text-[#A875FF]'
                  : 'bg-white/5 text-gray-400'
              }`}
            >
              <ShieldCheck className="w-4 h-4" />
            </div>
            <div className="truncate">
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-bold text-white tracking-tight">
                  Stabilizator IVR
                </span>
                <span className="text-[10px] font-mono px-1.5 py-0.2 rounded bg-purple-900/40 text-purple-300 border border-purple-500/20">
                  RFC 4733
                </span>
              </div>
              <p className="text-[10px] text-gray-400 truncate">
                {ivrInfo.isIvr ? ivrInfo.reason : 'Automatyczna bramka AEC + pacing tonów'}
              </p>
            </div>
          </div>

          {/* Toggle Włącz/Wyłącz */}
          <button
            onClick={() => setStabilizerEnabled(prev => !prev)}
            className={`px-2.5 py-1 rounded-lg text-[11px] font-medium transition-all ${
              stabilizerEnabled
                ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                : 'bg-white/10 text-gray-300 hover:bg-white/15'
            }`}
          >
            {stabilizerEnabled ? 'Włączony' : 'Wyłączony'}
          </button>
        </div>

        {/* Wskaźnik stanu Bramki AEC (Auto-Mute) */}
        {stabilizerEnabled && (
          <div className="pt-2 space-y-2">
            <div
              className={`p-2 rounded-xl text-xs flex items-center justify-between transition-all ${
                isAutoMuted
                  ? 'bg-emerald-950/40 border border-emerald-500/40 text-emerald-300'
                  : 'bg-[#0E0F1A] border border-white/5 text-gray-300'
              }`}
            >
              <div className="flex items-center gap-2">
                <div
                  className={`w-5 h-5 rounded-full flex items-center justify-center ${
                    isAutoMuted
                      ? 'bg-emerald-500 text-black animate-pulse'
                      : 'bg-white/10 text-gray-400'
                  }`}
                >
                  {isAutoMuted ? <MicOff className="w-3 h-3" /> : <Mic className="w-3 h-3" />}
                </div>
                <div>
                  <div className="font-semibold text-[11px] flex items-center gap-1.5">
                    {isAutoMuted ? (
                      <>
                        <span>Bramka AEC: Mikrofon odcięty</span>
                        {isTailBuffer && (
                          <span className="text-[9px] bg-amber-500/20 text-amber-300 px-1 py-0.2 rounded">
                            Bufor 350ms
                          </span>
                        )}
                      </>
                    ) : (
                      <span className="text-gray-300">Głośnomówiący chroniony (AEC gotowa)</span>
                    )}
                  </div>
                  <p className="text-[10px] text-gray-400">
                    {isAutoMuted
                      ? 'Zero echa i zniekształceń do centrali banku'
                      : 'Mikrofon wycisza się automatycznie w czasie nadawania'}
                  </p>
                </div>
              </div>

              {/* Tryb prędkości tonów */}
              <button
                onClick={() => setSpeedMode(prev => (prev === 'STANDARD' ? 'FAST' : 'STANDARD'))}
                title="Zmień profil timingów DTMF"
                className="text-[10px] font-mono px-2 py-0.5 rounded bg-white/5 text-gray-400 hover:text-white border border-white/5"
              >
                {speedMode === 'STANDARD' ? '200 ms' : '120 ms'}
              </button>
            </div>

            {/* Wizualizacja nadawania w toku */}
            {isTransmitting && (
              <div className="bg-[#0A0B14] rounded-xl p-2.5 border border-purple-500/30 flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <div className="flex items-center gap-0.5 h-4">
                    <span className="w-1 h-3 bg-[#8BE0BB] animate-bounce rounded-full" />
                    <span className="w-1 h-4 bg-[#7B4DFF] animate-bounce delay-75 rounded-full" />
                    <span className="w-1 h-2 bg-[#8BE0BB] animate-bounce delay-150 rounded-full" />
                  </div>
                  <div>
                    <span className="text-xs font-mono font-bold text-white">
                      Ton: {currentTone || '···'}
                    </span>
                    {currentFreqs && (
                      <span className="text-[10px] font-mono text-[#8BE0BB] ml-1.5">
                        {currentFreqs[0]}Hz + {currentFreqs[1]}Hz
                      </span>
                    )}
                  </div>
                </div>

                <div className="text-[11px] font-mono text-purple-300">
                  Wysłano: {transmittedCount}/{totalInBatch}
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Ekran wpisanych tonów z akcjami pomocniczymi */}
      <div className="bg-[#1A1B30]/90 backdrop-blur-md rounded-2xl p-3 border border-[#272540]">
        <div className="flex items-center justify-between mb-2 px-1">
          <span className="text-[11px] font-medium text-gray-400">
            Wprowadzony kod / tony:
          </span>
          <div className="flex items-center gap-1.5">
            <button
              onClick={handlePasteFromClipboard}
              className="flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-lg bg-white/5 hover:bg-white/10 text-[#A875FF] transition-colors"
              title="Wklej telekod lub PESEL"
            >
              <Clipboard className="w-3 h-3" />
              <span>Wklej kod</span>
            </button>
            {enteredDigits && (
              <button
                onClick={handleStopAndClear}
                className="flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-lg bg-red-500/10 hover:bg-red-500/20 text-red-300 transition-colors"
                title="Wyczyść cyfry i zatrzymaj nadawanie"
              >
                <Trash2 className="w-3 h-3" />
                <span>Wyczyść</span>
              </button>
            )}
          </div>
        </div>

        {/* Pole wyświetlacza wpisanych tonów */}
        <div className="h-10 px-3 rounded-xl bg-[#111222] border border-white/5 flex items-center justify-between font-mono text-lg tracking-widest text-[#F8FAFC]">
          <span className="truncate max-w-[240px]">
            {enteredDigits || <span className="text-gray-500 text-sm">Wpisz kod...</span>}
          </span>
          {isTransmitting && (
            <span className="flex h-2 w-2 relative">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-500"></span>
            </span>
          )}
        </div>

        {/* Klawiatura numeryczna DTMF */}
        <div className="grid grid-cols-3 gap-2 mt-3">
          {[
            { d: '1', sub: '' },
            { d: '2', sub: 'ABC' },
            { d: '3', sub: 'DEF' },
            { d: '4', sub: 'GHI' },
            { d: '5', sub: 'JKL' },
            { d: '6', sub: 'MNO' },
            { d: '7', sub: 'PQRS' },
            { d: '8', sub: 'TUV' },
            { d: '9', sub: 'WXYZ' },
            { d: '*', sub: '' },
            { d: '0', sub: '+' },
            { d: '#', sub: '' }
          ].map(({ d, sub }) => (
            <button
              key={d}
              onClick={() => enqueueDigits(d)}
              className="py-3 px-2 rounded-xl bg-[#22233C] hover:bg-[#2A2B48] active:bg-[#7B4DFF]/40 active:scale-95 transition-all flex flex-col items-center justify-center border border-white/5"
            >
              <span className="text-xl font-bold text-white leading-none">{d}</span>
              {sub && <span className="text-[9px] text-gray-400 mt-0.5 tracking-wider">{sub}</span>}
            </button>
          ))}
        </div>
      </div>

      {/* Modal wklejania kodu (PESEL / Telekod) */}
      {showPasteModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <form
            onSubmit={handleQuickPasteSubmit}
            className="bg-[#131424] border border-[#272540] rounded-2xl p-5 max-w-xs w-full space-y-3"
          >
            <div className="flex items-center gap-2">
              <Clipboard className="w-5 h-5 text-[#A875FF]" />
              <h3 className="text-base font-bold text-white">Wklej kod / PESEL</h3>
            </div>
            <p className="text-xs text-gray-400">
              Ciąg cyfr zostanie nadany z certyfikowanym timingiem 200 ms i wyciszeniem mikrofonu (AEC).
            </p>
            <input
              type="text"
              autoFocus
              value={pasteInputValue}
              onChange={e => setPasteInputValue(e.target.value)}
              placeholder="np. PESEL lub kod telebanku"
              className="w-full px-3 py-2 rounded-xl bg-[#1C1D36] border border-white/10 text-white font-mono text-center tracking-widest text-base focus:outline-none focus:border-[#7B4DFF]"
            />
            <div className="flex items-center gap-2 pt-1">
              <button
                type="button"
                onClick={() => setShowPasteModal(false)}
                className="flex-1 py-2 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-semibold text-gray-300"
              >
                Anuluj
              </button>
              <button
                type="submit"
                className="flex-1 py-2 rounded-xl bg-[#7B4DFF] hover:bg-[#6A3BEA] text-xs font-bold text-white"
              >
                Wyślij studyjnie
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
