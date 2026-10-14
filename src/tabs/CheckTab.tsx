import React, { useState, useEffect } from 'react';
import {
  Search,
  RefreshCw,
  AlertTriangle,
  ShieldCheck,
  Send,
  ExternalLink,
  Info
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { isSnapshotFresh } from '../utils/cert';

export function CheckTab() {
  const {
    certSnapshot,
    refreshingCert,
    refreshCert,
    analysis,
    analyze,
    clearAnalysis,
    messageToCheck,
    setMessageToCheck
  } = useApp();

  const [inputText, setInputText] = useState(messageToCheck || '');

  useEffect(() => {
    if (messageToCheck) {
      setInputText(messageToCheck);
      analyze(messageToCheck);
    }
  }, [messageToCheck, analyze]);

  const handleInputChange = (text: string) => {
    const truncated = text.slice(0, 20000);
    setInputText(truncated);
    setMessageToCheck(truncated);
    if (!truncated.trim()) {
      clearAnalysis();
    }
  };

  const handleRunAnalysis = () => {
    analyze(inputText);
  };

  const fresh = isSnapshotFresh(certSnapshot);
  const matches = analysis?.findings.filter(f => f.matchedDomain !== null) || [];

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
          Wiadomość lub link
        </h2>
        <p className="text-sm text-[#9499AD] mt-1">
          Wklej treść SMS-a albo udostępnij ją do CzyToTy? z komunikatora. Analiza odbywa się na tym urządzeniu.
        </p>
      </div>

      {/* SourceCard: Lista Ostrzeżeń CERT Polska */}
      <div className="p-4 rounded-2xl bg-[#131424] border border-[#272540] space-y-3 shadow-md">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-[#22C55E] animate-pulse" />
            <h3 className="text-sm font-bold text-white">
              Lista Ostrzeżeń CERT Polska
            </h3>
          </div>
          <a
            href="https://cert.pl/lista-ostrzezen/"
            target="_blank"
            rel="noreferrer"
            className="text-[11px] text-[#A875FF] hover:underline flex items-center gap-1"
          >
            <span>CERT/NASK</span>
            <ExternalLink className="w-3 h-3" />
          </a>
        </div>

        <p
          className={`text-xs font-medium ${
            refreshingCert
              ? 'text-purple-300'
              : !certSnapshot
              ? 'text-[#FFD089]'
              : fresh
              ? 'text-[#22C55E]'
              : 'text-[#FFD089]'
          }`}
        >
          {refreshingCert
            ? 'Pobieranie aktualnej listy…'
            : !certSnapshot
            ? 'Brak pobranej listy. Sprawdzanie domen jest niedostępne.'
            : fresh
            ? `${certSnapshot.domains.length} domen · świeża kopia`
            : `${certSnapshot.domains.length} domen · kopia wymaga aktualizacji`}
        </p>

        {certSnapshot && (
          <p className="text-[11px] text-[#6C6F85]">
            Pobrano: {formatTimestamp(certSnapshot.fetchedAt)}
          </p>
        )}

        <div className="pt-1">
          <button
            onClick={() => refreshCert(true)}
            disabled={refreshingCert}
            className="w-full py-2 px-3 rounded-xl bg-[#1A1B30] hover:bg-[#22233C] border border-[#272540] text-xs font-semibold text-white/90 flex items-center justify-center gap-2 transition-all disabled:opacity-50"
          >
            <RefreshCw
              className={`w-3.5 h-3.5 ${refreshingCert ? 'animate-spin' : ''}`}
            />
            <span>{refreshingCert ? 'Aktualizowanie…' : 'Aktualizuj listę CERT'}</span>
          </button>
        </div>
      </div>

      {/* Input Field */}
      <div className="space-y-2">
        <div className="relative">
          <textarea
            value={inputText}
            onChange={e => handleInputChange(e.target.value)}
            placeholder="Wklej treść wiadomości lub adres strony..."
            data-testid="message_input"
            rows={5}
            className="w-full p-4 rounded-2xl bg-[#1A1B30]/90 border border-[#272540] text-sm text-white placeholder:text-[#6C6F85] focus:outline-none focus:border-[#7B4DFF] resize-none transition-colors"
          />
          {inputText && (
            <button
              onClick={() => handleInputChange('')}
              className="absolute top-3 right-3 text-xs text-[#9499AD] hover:text-white bg-[#131424] px-2 py-1 rounded-lg border border-[#272540]"
            >
              Wyczyść
            </button>
          )}
        </div>

        <button
          onClick={handleRunAnalysis}
          disabled={!inputText.trim()}
          data-testid="analyze"
          className="w-full py-3 px-4 rounded-2xl bg-[#7B4DFF] hover:bg-[#6a3ee0] disabled:opacity-40 text-white font-semibold text-sm flex items-center justify-center gap-2 shadow-lg transition-all"
        >
          <Search className="w-4 h-4" />
          <span>Sprawdź treść i domeny</span>
        </button>
      </div>

      {/* Analysis Result Card */}
      {analysis && (
        <div
          className={`p-5 rounded-3xl border space-y-4 shadow-xl transition-all ${
            matches.length > 0
              ? 'bg-[#281419] border-[#EF5350]/60'
              : 'bg-[#131424] border-[#272540]'
          }`}
        >
          <div className="flex items-start gap-3">
            {matches.length > 0 ? (
              <AlertTriangle className="w-6 h-6 text-[#EF5350] flex-shrink-0 mt-0.5" />
            ) : (
              <ShieldCheck className="w-6 h-6 text-[#22C55E] flex-shrink-0 mt-0.5" />
            )}
            <div>
              <h3
                data-testid="analysis_result"
                className={`text-base font-bold ${
                  matches.length > 0 ? 'text-[#FF8596]' : 'text-white'
                }`}
              >
                {!analysis.snapshotTime
                  ? 'Brak danych do sprawdzenia domen'
                  : matches.length > 0 && fresh
                  ? 'Ostrzeżenie CERT: wykryto domenę z listy'
                  : matches.length > 0
                  ? 'Domena figuruje w zapisanej kopii listy'
                  : analysis.findings.length === 0
                  ? 'Nie znaleziono rozpoznawalnego adresu'
                  : 'Nie znaleziono domen na liście CERT'}
              </h3>
              {!fresh && (
                <p className="text-xs text-[#FFD089] mt-1">
                  Lista nie jest aktualna lub nie została pobrana. Ten wynik nie opisuje bieżącego stanu CERT.
                </p>
              )}
            </div>
          </div>

          {/* Domain Findings */}
          {analysis.findings.length > 0 && (
            <div className="space-y-2 border-t border-white/10 pt-3">
              <span className="text-xs font-semibold text-[#9499AD] uppercase tracking-wider block">
                Znalezione adresy ({analysis.findings.length})
              </span>
              {analysis.findings.map((f, i) => (
                <div
                  key={i}
                  className="p-3 rounded-xl bg-black/30 border border-white/5 space-y-1"
                >
                  <p className="text-sm font-mono font-bold text-white">
                    {f.host}
                  </p>
                  <p
                    className={`text-xs ${
                      f.matchedDomain ? 'text-[#FF8596] font-semibold' : 'text-[#9499AD]'
                    }`}
                  >
                    {!analysis.snapshotTime
                      ? 'Nie sprawdzono — brak listy.'
                      : f.matchedDomain
                      ? `Wpis CERT: ${f.matchedDomain}`
                      : 'Brak wpisu w sprawdzonej kopii. To nie potwierdza bezpieczeństwa.'}
                  </p>
                </div>
              ))}
            </div>
          )}

          {/* Local signals */}
          {analysis.localSignals.length > 0 && (
            <div className="space-y-1.5 border-t border-white/10 pt-3">
              <span className="text-xs font-semibold text-amber-300 uppercase tracking-wider block">
                Wykryte sygnały językowe
              </span>
              {analysis.localSignals.map((sig, i) => (
                <div
                  key={i}
                  className="p-2.5 rounded-xl bg-amber-950/40 border border-amber-500/30 text-xs text-amber-200"
                >
                  {sig}
                </div>
              ))}
            </div>
          )}

          {matches.length > 0 && (
            <div className="p-3 rounded-xl bg-red-900/30 border border-red-500/30 text-xs text-red-200 font-medium">
              Nie otwieraj podejrzanego linku. Sprawdź sprawę w oficjalnej aplikacji lub niezależnie znalezionym kanale kontaktu.
            </div>
          )}

          <p className="text-[11px] text-[#6C6F85] pt-1">
            Analiza tekstu jest oparta na prostych regułach. Nie rozstrzyga, czy wiadomość jest oszustwem.
          </p>
        </div>
      )}

      {/* CERT 8080 report button */}
      {inputText.trim() && (
        <div className="space-y-1">
          <a
            href={`sms:8080?body=${encodeURIComponent(inputText)}`}
            className="w-full py-2.5 px-4 rounded-xl bg-[#1A1B30] hover:bg-[#22233C] border border-[#272540] text-xs font-semibold text-[#A875FF] hover:text-white flex items-center justify-center gap-2 transition-all"
          >
            <Send className="w-3.5 h-3.5" />
            <span>Przekaż podejrzany SMS do CERT (8080)</span>
          </a>
          <p className="text-[11px] text-[#6C6F85] text-center">
            Otworzy się Twoja aplikacja SMS. Samodzielnie zatwierdzasz wysyłkę.
          </p>
        </div>
      )}

      {/* InfoCard: Granice sprawdzania */}
      <div className="p-4 rounded-2xl bg-[#131424] border border-[#272540] space-y-1.5">
        <div className="flex items-center gap-2 text-white font-bold text-xs">
          <Info className="w-4 h-4 text-[#A875FF]" />
          <span>Granice sprawdzania</span>
        </div>
        <p className="text-xs text-[#9499AD] leading-relaxed">
          Lista CERT dotyczy domen phishingowych. Nie sprawdza tożsamości rozmówcy. Nowa niebezpieczna strona może jeszcze nie figurować na liście; przekierowania i skrócone linki nie są rozwijane.
        </p>
      </div>
    </div>
  );
}
