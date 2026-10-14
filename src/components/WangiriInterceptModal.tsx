import React, { useState, useEffect } from 'react';
import {
  ShieldAlert,
  AlertTriangle,
  PhoneOff,
  PhoneCall,
  Lock,
  Unlock,
  Coins,
  Globe,
  Info,
  CheckCircle2,
  X
} from 'lucide-react';
import { WangiriRiskInfo } from '../types';

interface WangiriInterceptModalProps {
  isOpen: boolean;
  targetNumber: string;
  riskInfo: WangiriRiskInfo;
  onBlockAndDismiss: (number: string) => void;
  onConfirmCall: (number: string) => void;
  onCancel: () => void;
}

export function WangiriInterceptModal({
  isOpen,
  targetNumber,
  riskInfo,
  onBlockAndDismiss,
  onConfirmCall,
  onCancel
}: WangiriInterceptModalProps) {
  const [holdProgress, setHoldProgress] = useState(0);
  const [isHolding, setIsHolding] = useState(false);
  const [bypassUnlocked, setBypassUnlocked] = useState(false);

  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isHolding && !bypassUnlocked) {
      interval = setInterval(() => {
        setHoldProgress(prev => {
          if (prev >= 100) {
            setBypassUnlocked(true);
            setIsHolding(false);
            return 100;
          }
          return prev + 5;
        });
      }, 50);
    } else if (!isHolding && !bypassUnlocked) {
      setHoldProgress(0);
    }
    return () => clearInterval(interval);
  }, [isHolding, bypassUnlocked]);

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/85 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-md bg-gradient-to-b from-[#231218] via-[#16101B] to-[#0D0B14] rounded-3xl border-2 border-red-500/70 p-5 text-white shadow-2xl shadow-red-950/80 space-y-4">
        {/* Close icon */}
        <button
          onClick={onCancel}
          className="absolute right-4 top-4 p-1.5 rounded-full bg-white/5 hover:bg-white/10 text-gray-400 hover:text-white transition-colors"
          title="Zamknij i anuluj"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header with pulsating badge */}
        <div className="flex items-start gap-3 pt-1">
          <div className="p-3 rounded-2xl bg-red-600/25 border border-red-500/50 text-red-400 shadow-lg shadow-red-600/30 shrink-0">
            <ShieldAlert className="w-8 h-8 animate-pulse text-red-400" />
          </div>
          <div>
            <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-red-600/30 border border-red-500/50 text-red-300 text-[11px] font-extrabold uppercase tracking-wider">
              <AlertTriangle className="w-3.5 h-3.5" />
              Tarcza Wangiri · Przechwycenie
            </div>
            <h3 className="text-xl font-extrabold text-white mt-1 leading-tight">
              Wysokie ryzyko oszustwa taryfowego!
            </h3>
            <p className="text-xs text-red-200/80 mt-0.5">
              Próba oddzwonienia na niebezpieczny numer z jednym sygnałem
            </p>
          </div>
        </div>

        {/* Caller & Country Card */}
        <div className="p-3.5 rounded-2xl bg-black/50 border border-red-500/30 space-y-2">
          <div className="flex items-center justify-between">
            <span className="text-xs text-[#9499AD] font-semibold">Numer docelowy:</span>
            <span className="text-base font-mono font-bold text-white tracking-wide">
              {targetNumber}
            </span>
          </div>

          <div className="flex items-center justify-between pt-1.5 border-t border-white/10">
            <span className="text-xs text-[#9499AD] font-semibold flex items-center gap-1">
              <Globe className="w-3.5 h-3.5 text-blue-400" />
              Lokalizacja:
            </span>
            <span className="text-xs font-bold text-white flex items-center gap-1.5">
              <span className="text-base">{riskInfo.countryFlag}</span>
              <span>{riskInfo.countryName}</span>
            </span>
          </div>

          {/* Cost estimation banner */}
          <div className="flex items-center justify-between pt-1.5 border-t border-white/10">
            <span className="text-xs text-[#FFD089] font-semibold flex items-center gap-1">
              <Coins className="w-3.5 h-3.5" />
              Szacowany koszt:
            </span>
            <span className="text-sm font-extrabold text-[#FF8596] font-mono">
              ~{riskInfo.estimatedCostRange}
            </span>
          </div>
        </div>

        {/* Camouflage Alert if applicable */}
        {riskInfo.isCamouflage && riskInfo.camouflageTarget && (
          <div className="p-3 rounded-2xl bg-[#32131D] border border-red-500/40 text-xs text-red-200 leading-relaxed flex items-start gap-2.5">
            <span className="text-lg shrink-0">⚠️</span>
            <div>
              <strong className="text-white block font-bold">
                Kamuflaż optyczny: {riskInfo.camouflageTarget}
              </strong>
              Prefiks <span className="font-mono font-bold text-red-300">{riskInfo.prefix}</span> został
              dobrany celowo przez oszustów, by na ekranie telefonu przypominał numer stacjonarny ze stolicy!
            </div>
          </div>
        )}

        {/* Mechanism explanation */}
        <div className="p-3 rounded-2xl bg-[#171329] border border-[#272540] text-xs text-[#C5C8D8] leading-relaxed space-y-1">
          <p className="font-semibold text-purple-200 flex items-center gap-1">
            <Info className="w-3.5 h-3.5 text-[#A875FF]" />
            Jak działa ten mechanizm?
          </p>
          <p className="text-[11px] text-[#9499AD]">
            Automat celowo puszcza 1 sygnał (One-Ring) i rozłącza się. Kiedy oddzwaniasz, płacisz wysoką stawkę za każdą minutę połączenia, a oszuści sztucznie przedłużają rozmowę dźwiękami oczekiwania na linii.
          </p>
        </div>

        {/* Action Buttons */}
        <div className="space-y-2.5 pt-1">
          {/* Recommended Primary Button: Block and Protect */}
          <button
            onClick={() => onBlockAndDismiss(targetNumber)}
            className="w-full py-3.5 px-4 rounded-2xl bg-gradient-to-r from-red-600 via-rose-600 to-red-700 hover:from-red-500 hover:to-red-600 active:scale-[0.98] text-white font-extrabold text-sm flex items-center justify-center gap-2 shadow-xl shadow-red-700/40 transition-all border border-red-400/50"
          >
            <PhoneOff className="w-5 h-5" />
            <span>Zablokuj i chroń oszczędności (Zalecane)</span>
          </button>

          {/* Forced Call Bypass with Anti-Clickjacking / Senior Protection */}
          <div className="pt-1">
            {!bypassUnlocked ? (
              <button
                type="button"
                onMouseDown={() => setIsHolding(true)}
                onMouseUp={() => setIsHolding(false)}
                onMouseLeave={() => setIsHolding(false)}
                onTouchStart={() => setIsHolding(true)}
                onTouchEnd={() => setIsHolding(false)}
                className="w-full py-2.5 px-4 rounded-xl bg-[#1C1625] hover:bg-[#251D33] border border-[#3E2D54] text-xs text-purple-300 font-semibold flex items-center justify-center gap-2 relative overflow-hidden transition-all select-none"
              >
                {/* Hold progress bar */}
                <div
                  className="absolute left-0 top-0 bottom-0 bg-red-600/30 transition-all duration-75"
                  style={{ width: `${holdProgress}%` }}
                />
                <Lock className="w-3.5 h-3.5 z-10 text-purple-400" />
                <span className="z-10">
                  {isHolding
                    ? `Przytrzymaj jeszcze chwilę (${Math.round(holdProgress)}%)...`
                    : 'Przytrzymaj 2 sekundy, aby odblokować połączenie'}
                </span>
              </button>
            ) : (
              <div className="space-y-1.5 animate-in fade-in">
                <button
                  type="button"
                  onClick={() => onConfirmCall(targetNumber)}
                  className="w-full py-2.5 px-4 rounded-xl bg-amber-600/30 hover:bg-amber-600/50 border border-amber-500 text-amber-200 font-bold text-xs flex items-center justify-center gap-2 transition-all"
                >
                  <Unlock className="w-3.5 h-3.5 text-amber-400" />
                  <span>Połącz na własne ryzyko (Świadome koszty)</span>
                </button>
                <p className="text-[10px] text-center text-amber-300/70">
                  Zabezpieczenie przed przypadkowym tapnięciem odblokowane.
                </p>
              </div>
            )}
          </div>

          {/* Cancel button */}
          <button
            type="button"
            onClick={onCancel}
            className="w-full py-2 rounded-xl text-xs text-[#9499AD] hover:text-white transition-colors"
          >
            Anuluj i wróć do telefonu
          </button>
        </div>
      </div>
    </div>
  );
}
