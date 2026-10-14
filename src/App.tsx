import React, { useState, useMemo } from 'react';
import {
  ShieldAlert,
  PhoneOff,
  MicOff,
  Lock,
  Unlock,
  AlertTriangle,
  X
} from 'lucide-react';
import { useApp } from './context/AppContext';
import { Header } from './components/Header';
import { BottomNav } from './components/BottomNav';
import { DialerTab } from './tabs/DialerTab';
import { SmsTab } from './tabs/SmsTab';
import { CheckTab } from './tabs/CheckTab';
import { NumbersTab } from './tabs/NumbersTab';
import { ProtectionTab } from './tabs/ProtectionTab';
import { AccountTab } from './tabs/AccountTab';
import { SchematyTab } from './tabs/SchematyTab';
import { InCallOverlay } from './components/InCallOverlay';
import { AppearanceSheet } from './components/AppearanceSheet';
import { Toast } from './components/Toast';
import { QuickCallNoteBar } from './components/QuickCallNoteBar';
import { WangiriInterceptModal } from './components/WangiriInterceptModal';

export function App() {
  const {
    activeTab,
    appearance,
    lastEndedCallNumber,
    setLastEndedCallNumber,
    callSession,
    contacts,
    smsMessages,
    endCall,
    setCallMuted,
    wangiriModalState,
    proceedOutgoingCall,
    blockAndDismissWangiri,
    closeWangiriModal
  } = useApp();

  // 1. Sprawdzenie trwającego połączenia (inCall === true)
  const activeLiveCall = callSession.calls.find(c => c.state === 'ACTIVE');
  const inCall = Boolean(activeLiveCall);

  // 2. Weryfikacja czy rozmówca to nieznany numer (spoza zapisanych kontaktów)
  const isUnknownNumber = useMemo(() => {
    if (!activeLiveCall) return false;
    const cleanActive = activeLiveCall.number.replace(/\D/g, '');
    const isKnown = contacts.some(c => {
      const cleanC = c.phone.replace(/\D/g, '');
      return cleanC && cleanActive && (cleanC.endsWith(cleanActive) || cleanActive.endsWith(cleanC));
    });
    return !isKnown;
  }, [activeLiveCall, contacts]);

  // 3. Stan maskowania kodu i zamknięcia alertu
  const [dismissedScamSmsId, setDismissedScamSmsId] = useState<number | null>(null);
  const [isMaskUnlocked, setIsMaskUnlocked] = useState(false);

  // Słowa kluczowe: kod, autoryzacja, przelew, BLIK
  const scamKeywords = ['kod', 'autoryzacj', 'przelew', 'blik'];

  // 4. Korelator scamu w App.tsx: łączy inCall z nieznanym numerem i przychodzący SMS z kodem
  const scamAlertData = useMemo(() => {
    if (!inCall || !isUnknownNumber || !activeLiveCall) return null;
    const windowStart = (activeLiveCall.connectedAt > 0 ? activeLiveCall.connectedAt : Date.now() - 180000) - 30000;

    const matchedSms = smsMessages.find(sms => {
      if (!sms.incoming || sms.time < windowStart) return false;
      const lower = sms.body.toLowerCase();
      return scamKeywords.some(kw => lower.includes(kw));
    });

    if (!matchedSms) return null;

    // Maskowanie kodu autoryzacyjnego w treści SMS
    const codeRegex = /(?:kod(?:em)?|hasło|autoryzacji|blik|kod to)[:\s]+([0-9A-Z]{4,8}|[0-9]{3}[-\s][0-9]{3})|\b([0-9]{3}[-][0-9]{3})\b|\b([0-9]{6})\b|\b([0-9]{4,8})\b/i;
    const match = codeRegex.exec(matchedSms.body);
    const extractedCode = match ? (match[1] || match[2] || match[3] || match[4]) : null;

    const maskedBody = extractedCode
      ? matchedSms.body.replace(extractedCode, '[ KOD UKRYTY: •••••• ]')
      : matchedSms.body.replace(/\b\d{4,8}\b/g, '[ KOD UKRYTY: •••••• ]');

    return {
      sms: matchedSms,
      extractedCode,
      maskedBody,
      callNumber: activeLiveCall.number,
      callId: activeLiveCall.id
    };
  }, [inCall, isUnknownNumber, activeLiveCall, smsMessages]);

  const showScamBanner = Boolean(scamAlertData && scamAlertData.sms.id !== dismissedScamSmsId);

  const accentColor =
    appearance.accent === 'MINT'
      ? '#22C55E'
      : appearance.accent === 'OCEAN'
      ? '#3B82F6'
      : '#A875FF';

  return (
    <div
      className="min-h-screen bg-[#0A0A12] text-white flex flex-col font-sans relative selection:bg-[#7B4DFF]/30"
      style={{
        // Custom variable for accent highlights
        ['--brand-accent' as any]: accentColor
      }}
    >
      {/* 🚨 KORELATOR SCAMU: Globalny alarmowy baner ostrzegawczy z maskowaniem SMS-a */}
      {showScamBanner && scamAlertData && (
        <div className="sticky top-0 z-50 w-full bg-gradient-to-r from-red-950 via-red-900 to-red-950 border-b-2 border-red-500 text-white p-3.5 shadow-2xl animate-in slide-in-from-top duration-300">
          <div className="max-w-xl mx-auto space-y-2.5">
            {/* Nagłówek alarmu */}
            <div className="flex items-start justify-between gap-2">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 rounded-xl bg-red-600 flex items-center justify-center text-white shrink-0 animate-pulse shadow-lg shadow-red-500/50">
                  <ShieldAlert className="w-5 h-5" />
                </div>
                <div>
                  <div className="flex items-center gap-1.5 flex-wrap">
                    <span className="text-xs font-black uppercase tracking-wider text-red-200">
                      Alarm: Próba wyłudzenia kodu na linii!
                    </span>
                    <span className="text-[9px] font-mono font-bold px-1.5 py-0.2 rounded bg-black/40 text-red-300 border border-red-500/40">
                      inCall 2w1
                    </span>
                  </div>
                  <p className="text-[11px] text-red-200">
                    Rozmawiasz z nieznanym numerem <strong className="text-white font-mono">{scamAlertData.callNumber}</strong> i w tej chwili nadszedł SMS autoryzacyjny!
                  </p>
                </div>
              </div>

              <button
                onClick={() => setDismissedScamSmsId(scamAlertData.sms.id)}
                className="p-1 text-red-300 hover:text-white rounded-lg hover:bg-white/10 shrink-0"
                title="Zamknij baner"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Zamaskowana zawartość SMS-a */}
            <div className="bg-black/60 rounded-xl p-2.5 border border-red-500/40 space-y-1.5 text-xs">
              <div className="flex items-center justify-between text-[10px] text-red-300 font-medium">
                <span>Nadawca SMS: <strong>{scamAlertData.sms.address}</strong></span>
                <span className="text-gray-400">Zawartość zamaskowana ze względów bezpieczeństwa</span>
              </div>

              <p className="font-mono text-gray-200 leading-relaxed break-words">
                {isMaskUnlocked ? (
                  <span className="text-white font-bold">{scamAlertData.sms.body}</span>
                ) : (
                  <span>{scamAlertData.maskedBody}</span>
                )}
              </p>

              {isMaskUnlocked && (
                <div className="p-1.5 bg-red-950/80 border border-red-500/60 rounded text-red-200 text-[10px] font-bold flex items-center gap-1">
                  <AlertTriangle className="w-3.5 h-3.5 text-red-400 shrink-0" />
                  <span>BANK NIGDY NIE PROSI O ODZYTANIE KODU PODCZAS ROZMOWY! NIE PODAWAJ GO!</span>
                </div>
              )}
            </div>

            {/* Przyciski natychmiastowej reakcji */}
            <div className="flex items-center gap-2 pt-0.5 flex-wrap">
              <button
                onClick={() => endCall(scamAlertData.callId)}
                className="py-2 px-3.5 rounded-xl bg-red-600 hover:bg-red-500 text-white font-bold text-xs flex items-center gap-1.5 shadow-md shadow-red-600/40 active:scale-95 transition-all"
              >
                <PhoneOff className="w-3.5 h-3.5" />
                <span>Rozłącz oszusta (Chroń konto)</span>
              </button>

              <button
                onClick={() => setCallMuted(true)}
                className="py-2 px-3 rounded-xl bg-white/10 hover:bg-white/15 text-gray-200 text-xs font-semibold flex items-center gap-1.5 transition-colors"
              >
                <MicOff className="w-3.5 h-3.5" />
                <span>Wycisz mikrofon</span>
              </button>

              <button
                onClick={() => setIsMaskUnlocked(prev => !prev)}
                className="py-2 px-3 rounded-xl bg-white/5 hover:bg-white/10 text-red-300 hover:text-white text-xs font-medium flex items-center gap-1.5 transition-colors ml-auto"
              >
                {isMaskUnlocked ? <Lock className="w-3.5 h-3.5" /> : <Unlock className="w-3.5 h-3.5" />}
                <span>{isMaskUnlocked ? 'Zamaskuj kod ponownie' : 'Pokaż zamaskowany kod'}</span>
              </button>
            </div>
          </div>
        </div>
      )}

      <Header />

      <main className="flex-1 max-w-xl mx-auto w-full p-4 flex flex-col overflow-hidden">
        {activeTab === 0 && <DialerTab />}
        {activeTab === 1 && <SmsTab />}
        {activeTab === 2 && <CheckTab />}
        {activeTab === 3 && <NumbersTab />}
        {activeTab === 4 && <ProtectionTab />}
        {activeTab === 5 && <AccountTab />}
        {activeTab === 6 && <SchematyTab />}
      </main>

      <BottomNav />
      <InCallOverlay />
      {lastEndedCallNumber && (
        <QuickCallNoteBar
          phoneNumber={lastEndedCallNumber}
          onDismiss={() => setLastEndedCallNumber(null)}
        />
      )}
      <AppearanceSheet />
      <Toast />
      {wangiriModalState && (
        <WangiriInterceptModal
          isOpen={wangiriModalState.isOpen}
          targetNumber={wangiriModalState.targetNumber}
          riskInfo={wangiriModalState.riskInfo}
          onBlockAndDismiss={blockAndDismissWangiri}
          onConfirmCall={proceedOutgoingCall}
          onCancel={closeWangiriModal}
        />
      )}
    </div>
  );
}
export default App;
