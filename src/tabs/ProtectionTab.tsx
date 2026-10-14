import React, { useState } from 'react';
import {
  Shield,
  ShieldAlert,
  ShieldCheck,
  RefreshCw,
  Trash2,
  Info,
  PhoneCall,
  CheckCircle2,
  XCircle
} from 'lucide-react';
import { useApp } from '../context/AppContext';

export function ProtectionTab() {
  const {
    protectionEnabled,
    setProtectionEnabled,
    notes,
    screeningEvents,
    clearScreeningEvents,
    simulateIncomingCall
  } = useApp();

  const [testNumber, setTestNumber] = useState('');

  const blockedCount = notes.filter(n => n.blocked).length;

  const handleSimulateCall = (e: React.FormEvent) => {
    e.preventDefault();
    if (!testNumber.trim()) return;
    simulateIncomingCall(testNumber.trim());
  };

  const formatTimestamp = (time: number) => {
    const d = new Date(time);
    return `${d.toLocaleDateString('pl-PL')} ${d.toLocaleTimeString('pl-PL', {
      hour: '2-digit',
      minute: '2-digit'
    })}`;
  };

  return (
    <div className="flex flex-col h-full space-y-5 overflow-y-auto pr-1 pb-20">
      <div>
        <h2 className="text-3xl font-extrabold text-white tracking-tight">
          Ochrona połączeń
        </h2>
        <p className="text-sm text-[#9499AD] mt-1">
          Lokalne filtrowanie połączeń przychodzących na podstawie Twojej własnej listy blokad.
        </p>
      </div>

      {/* Status Card */}
      <div className="p-4 rounded-2xl bg-[#131424] border border-[#272540] space-y-3 shadow-md">
        <div className="flex items-center gap-3">
          {protectionEnabled ? (
            <ShieldCheck className="w-6 h-6 text-[#22C55E]" />
          ) : (
            <ShieldAlert className="w-6 h-6 text-[#FFD089]" />
          )}
          <div>
            <h3 className="text-sm font-bold text-white">
              {protectionEnabled
                ? 'Blokowanie Twojej listy jest włączone'
                : 'Blokowanie jest nieaktywne'}
            </h3>
            <p className="text-xs text-[#9499AD] mt-0.5 leading-relaxed">
              {protectionEnabled
                ? 'Aplikacja ma rolę filtrowania połączeń. Blokuje wyłącznie numery, które samodzielnie dodasz do listy.'
                : 'Aby blokować zapisane numery, włącz filtrowanie połączeń.'}
            </p>
          </div>
        </div>

        {/* Protection Switch */}
        <div className="flex items-center justify-between pt-2 border-t border-[#272540]">
          <span className="text-sm font-semibold text-white">
            Blokuj numery z mojej listy
          </span>
          <input
            type="checkbox"
            checked={protectionEnabled}
            onChange={e => setProtectionEnabled(e.target.checked)}
            className="w-5 h-5 accent-[#7B4DFF] rounded"
          />
        </div>

        <p className="text-xs text-[#9499AD]">
          Zapisane blokady: <span className="font-bold text-white">{blockedCount}</span>
        </p>
      </div>

      {/* Simulator Test Panel */}
      <div className="p-4 rounded-2xl bg-[#1A1B30]/70 border border-[#272540] space-y-3">
        <h3 className="text-xs font-bold uppercase tracking-wider text-purple-300">
          Przetestuj filtrowanie połączeń
        </h3>
        <p className="text-xs text-[#9499AD]">
          Wpisz dowolny numer (lub z Twojej listy blokad), aby przetestować działanie filtru.
        </p>
        <form onSubmit={handleSimulateCall} className="flex gap-2">
          <input
            type="text"
            value={testNumber}
            onChange={e => setTestNumber(e.target.value)}
            placeholder="+48 799 599 888"
            className="flex-1 py-2 px-3 rounded-xl bg-[#131424] border border-[#272540] text-sm text-white focus:outline-none focus:border-[#7B4DFF]"
          />
          <button
            type="submit"
            disabled={!testNumber.trim()}
            className="py-2 px-3.5 rounded-xl bg-[#7B4DFF] hover:bg-[#6a3ee0] disabled:opacity-40 text-white text-xs font-semibold flex items-center gap-1.5 transition-all shadow-md"
          >
            <PhoneCall className="w-3.5 h-3.5" />
            <span>Sprawdź</span>
          </button>
        </form>
      </div>

      {/* Screening Events Registry */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-base font-bold text-white">
            Rejestr filtrowania · {screeningEvents.length}
          </h3>
          {screeningEvents.length > 0 && (
            <button
              onClick={clearScreeningEvents}
              className="text-xs text-[#EF5350] hover:underline flex items-center gap-1"
            >
              <Trash2 className="w-3 h-3" />
              <span>Wyczyść rejestr</span>
            </button>
          )}
        </div>

        <p className="text-xs text-[#6C6F85]">
          Zdarzenia odebrane przez tę aplikację, maksymalnie 200. To nie jest pełna historia rozmów. Android może pomijać numery z kontaktów i połączenia o ograniczonej prezentacji numeru.
        </p>

        {screeningEvents.length === 0 ? (
          <p className="text-xs text-[#9499AD] py-6 text-center">
            Nie zarejestrowano jeszcze żadnych połączeń.
          </p>
        ) : (
          <div className="space-y-2">
            {screeningEvents.slice(0, 50).map(event => (
              <div
                key={event.id}
                className={`p-3 rounded-xl border flex items-center justify-between ${
                  event.blocked
                    ? 'bg-[#281419]/70 border-[#EF5350]/40'
                    : 'bg-[#131424] border-[#272540]'
                }`}
              >
                <div className="flex items-center gap-3">
                  {event.blocked ? (
                    <XCircle className="w-5 h-5 text-[#EF5350] flex-shrink-0" />
                  ) : (
                    <CheckCircle2 className="w-5 h-5 text-[#22C55E] flex-shrink-0" />
                  )}
                  <div>
                    <p className="text-sm font-bold font-mono text-white">
                      {event.phone}
                    </p>
                    <p
                      className={`text-xs ${
                        event.blocked ? 'text-[#FF8596]' : 'text-[#9499AD]'
                      }`}
                    >
                      {event.blocked
                        ? 'Odrzucono według Twojej listy'
                        : 'Dopuszczono przez filtr'}
                    </p>
                  </div>
                </div>

                <span className="text-[11px] text-[#6C6F85]">
                  {formatTimestamp(event.time)}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* InfoCard: Tożsamość rozmówcy */}
      <div className="p-4 rounded-2xl bg-[#131424] border border-[#272540] space-y-1.5">
        <div className="flex items-center gap-2 text-white font-bold text-xs">
          <Info className="w-4 h-4 text-[#A875FF]" />
          <span>Tożsamość rozmówcy</span>
        </div>
        <p className="text-xs text-[#9499AD] leading-relaxed">
          Numer wyświetlany podczas połączenia może być sfałszowany. Prototyp nie nasłuchuje rozmów, nie rozpoznaje głosu i nie potwierdza, że dzwoni bank lub inna instytucja.
        </p>
      </div>
    </div>
  );
}
