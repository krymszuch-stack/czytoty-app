import React, { useState } from 'react';
import {
  AlertTriangle,
  ShieldAlert,
  PhoneOff,
  Eye,
  EyeOff,
  MicOff,
  CheckCircle2,
  Lock,
  Unlock,
  Radio
} from 'lucide-react';
import { CorrelatedScamAlert } from '../utils/scamCorrelator';

interface ScamCorrelatorAlertProps {
  alert: CorrelatedScamAlert;
  onEndCall: () => void;
  onMuteMic: () => void;
  onDismiss: () => void;
}

export function ScamCorrelatorAlert({
  alert,
  onEndCall,
  onMuteMic,
  onDismiss
}: ScamCorrelatorAlertProps) {
  const [isCodeUnlocked, setIsCodeUnlocked] = useState(false);
  const [confirmedSlider, setConfirmedSlider] = useState(false);

  const handleSliderChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const val = Number(e.target.value);
    if (val >= 90) {
      setConfirmedSlider(true);
      setIsCodeUnlocked(true);
    } else {
      setConfirmedSlider(false);
      setIsCodeUnlocked(false);
    }
  };

  return (
    <div className="w-full max-w-sm mx-auto my-3 animate-in fade-in slide-in-from-top-4 duration-300">
      <div className="bg-gradient-to-b from-[#380D14] via-[#240A10] to-[#16070B] border-2 border-red-500/80 rounded-2xl p-4 shadow-2xl shadow-red-500/30 text-left space-y-3 relative overflow-hidden">
        {/* Górny pasek z migającą syreną */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-red-600/30 border border-red-500 flex items-center justify-center text-red-400 animate-pulse">
              <ShieldAlert className="w-5 h-5 text-red-400" />
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-black tracking-wider uppercase text-red-300">
                  Korelator Scamu
                </span>
                <span className="text-[9px] font-mono font-bold px-1.5 py-0.5 rounded bg-red-500/20 text-red-200 border border-red-500/40">
                  Fuzja 2w1
                </span>
              </div>
              <p className="text-[10px] text-red-400/90 font-medium">
                Telefon & SMS w jednej aplikacji
              </p>
            </div>
          </div>

          <span className="flex h-2.5 w-2.5 relative">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-red-400 opacity-75"></span>
            <span className="relative inline-flex rounded-full h-2.5 w-2.5 bg-red-500"></span>
          </span>
        </div>

        {/* Czerwone ostrzeżenie główne */}
        <div className="bg-red-500/20 border border-red-500/40 rounded-xl p-3 text-red-100 space-y-1.5">
          <p className="text-xs font-bold leading-snug">
            🚨 Ktoś na linii prosi Cię o ten kod?
          </p>
          <p className="text-[11px] text-red-200/90 leading-relaxed">
            Bank <strong className="text-white underline">NIGDY</strong> nie prosi o odczytanie kodu SMS, BLIK ani hasła podczas rozmowy! To typowy scenariusz kradzieży oszczędności.
          </p>
        </div>

        {/* Podgląd SMS-a z cenzurą */}
        <div className="bg-[#0C0507] rounded-xl p-3 border border-red-900/50 space-y-2">
          <div className="flex items-center justify-between text-[11px]">
            <span className="font-semibold text-gray-300 flex items-center gap-1">
              <span>SMS od:</span>
              <span className="text-red-300 font-bold bg-red-950/80 px-1.5 py-0.5 rounded border border-red-800/40">
                {alert.smsSender}
              </span>
            </span>
            <span className="text-[10px] text-red-400 font-mono">
              Właśnie teraz
            </span>
          </div>

          {/* Treść SMS: ukryta vs odsłonięta */}
          <div className="text-xs font-mono text-gray-200 bg-black/40 p-2.5 rounded-lg border border-white/5 leading-relaxed break-words">
            {isCodeUnlocked ? (
              <div>
                <span className="text-gray-300">{alert.smsOriginalBody}</span>
                <div className="mt-2 p-1.5 bg-red-900/40 border border-red-500/50 rounded text-red-300 text-[10px] font-sans font-bold flex items-center gap-1.5">
                  <AlertTriangle className="w-4 h-4 text-red-400 shrink-0" />
                  <span>NIE CZYTAJ TEGO KODU ROZMÓWCY! ROZŁĄCZ SIĘ!</span>
                </div>
              </div>
            ) : (
              <div>
                <span className="text-gray-400">
                  {alert.censoredBody.split('[ KOD UKRYTY: •••••• ]')[0]}
                </span>
                <span className="inline-flex items-center gap-1 px-2 py-0.5 mx-1 rounded bg-red-500/30 text-red-300 border border-red-500/60 font-bold">
                  <Lock className="w-3 h-3" />
                  [ KOD UKRYTY: •••••• ]
                </span>
                <span className="text-gray-400">
                  {alert.censoredBody.split('[ KOD UKRYTY: •••••• ]')[1] || ''}
                </span>
              </div>
            )}
          </div>

          {/* Suwak bezpieczeństwa odblokowujący kod */}
          {!isCodeUnlocked && (
            <div className="pt-1 space-y-1">
              <div className="flex items-center justify-between text-[10px] text-gray-400">
                <span>Przesuń suwak, aby odsłonić:</span>
                <span className="text-red-400 font-medium">Zabezpieczenie 2w1</span>
              </div>
              <input
                type="range"
                min="0"
                max="100"
                defaultValue="0"
                onChange={handleSliderChange}
                className="w-full accent-red-500 cursor-pointer h-2 bg-red-950/60 rounded-lg appearance-none"
              />
              <p className="text-[9px] text-center text-gray-400 italic">
                „Rozumiem, że nie podaję tego kodu rozmówcy na linii”
              </p>
            </div>
          )}
        </div>

        {/* Akcje ratunkowe */}
        <div className="space-y-2 pt-1">
          <button
            onClick={onEndCall}
            className="w-full py-3 px-4 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold text-xs flex items-center justify-center gap-2 shadow-lg shadow-red-600/30 active:scale-95 transition-all"
          >
            <PhoneOff className="w-4 h-4" />
            <span>Rozłącz oszusta (Chroń pieniądze)</span>
          </button>

          <div className="flex items-center gap-2">
            <button
              onClick={onMuteMic}
              className="flex-1 py-2 rounded-xl bg-white/10 hover:bg-white/15 text-gray-200 text-xs font-medium flex items-center justify-center gap-1.5 transition-colors"
            >
              <MicOff className="w-3.5 h-3.5" />
              <span>Wycisz mikrofon</span>
            </button>

            <button
              onClick={onDismiss}
              className="py-2 px-3 rounded-xl bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white text-xs transition-colors"
            >
              Ukryj ostrzeżenie
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
