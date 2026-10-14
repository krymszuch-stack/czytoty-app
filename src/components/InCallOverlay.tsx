import React, { useState, useEffect } from 'react';
import {
  Phone,
  PhoneOff,
  Mic,
  MicOff,
  Volume2,
  Grid,
  Pause,
  Play,
  Minimize2
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { ContactAvatar } from './ContactAvatar';

export function InCallOverlay() {
  const {
    callSession,
    contacts,
    answerCall,
    endCall,
    holdCall,
    toggleMute,
    setAudioChoice,
    sendDtmfTone
  } = useApp();

  const [minimized, setMinimized] = useState(false);
  const [showKeypad, setShowKeypad] = useState(false);
  const [showAudioDialog, setShowAudioDialog] = useState(false);
  const [enteredDigits, setEnteredDigits] = useState('');
  const [timerSeconds, setTimerSeconds] = useState(0);

  const activeCall =
    callSession.calls.find(c => c.state === 'RINGING') ||
    callSession.calls.find(c => c.state !== 'DISCONNECTED');

  useEffect(() => {
    let interval: any;
    if (activeCall && activeCall.state === 'ACTIVE' && activeCall.connectedAt > 0) {
      interval = setInterval(() => {
        const secs = Math.max(0, Math.floor((Date.now() - activeCall.connectedAt) / 1000));
        setTimerSeconds(secs);
      }, 1000);
    } else {
      setTimerSeconds(0);
    }
    return () => clearInterval(interval);
  }, [activeCall?.state, activeCall?.connectedAt]);

  if (!activeCall) return null;

  const contact = contacts.find(c => c.phone === activeCall.number);
  const displayName = contact?.name || activeCall.number;
  const isRinging = activeCall.state === 'RINGING';
  const isActive = activeCall.state === 'ACTIVE';
  const isHolding = activeCall.state === 'HOLDING';

  const formatTimer = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  const handleDigit = (digit: string) => {
    setEnteredDigits(prev => (prev + digit).slice(-25));
    sendDtmfTone(activeCall.id, digit);
  };

  if (minimized) {
    return (
      <div
        onClick={() => setMinimized(false)}
        className="fixed bottom-20 right-4 z-50 bg-[#131424] border border-[#7B4DFF]/40 shadow-2xl rounded-2xl p-3 flex items-center gap-3 cursor-pointer animate-bounce hover:scale-105 transition-all"
      >
        <ContactAvatar name={displayName} size="sm" />
        <div>
          <p className="text-xs font-bold text-white max-w-[120px] truncate">
            {displayName}
          </p>
          <p className="text-[10px] text-[#22C55E]">
            {isActive ? formatTimer(timerSeconds) : 'Połączenie…'}
          </p>
        </div>
      </div>
    );
  }

  const stateLabels: Record<string, string> = {
    RINGING: 'Połączenie przychodzące',
    DIALING: 'Łączenie…',
    CONNECTING: 'Łączenie…',
    ACTIVE: 'Rozmowa trwa',
    HOLDING: 'Połączenie zawieszone',
    DISCONNECTING: 'Rozłączanie…',
    DISCONNECTED: 'Połączenie zakończone'
  };

  return (
    <div className="fixed inset-0 z-50 flex flex-col justify-between bg-gradient-to-b from-[#231A45] via-[#0A0A12] to-[#131424] text-white p-6 safe-top safe-bottom animate-in fade-in duration-300">
      {/* Top Bar with brand & minimize */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <img src="/brand/logo-mark.svg" alt="CzyToTy?" className="w-6 h-6" />
          <span className="text-xs font-bold tracking-wider uppercase text-[#A875FF]">
            CzyToTy?
          </span>
        </div>
        <button
          onClick={() => setMinimized(true)}
          className="p-2 text-[#9499AD] hover:text-white rounded-full bg-white/5 transition-colors"
          title="Minimalizuj okno rozmowy"
        >
          <Minimize2 className="w-5 h-5" />
        </button>
      </div>

      {/* Main Info */}
      <div className="flex flex-col items-center justify-center my-auto text-center space-y-4">
        <span
          data-testid="call_state"
          className="text-sm font-medium text-purple-200/80 bg-purple-900/30 px-3 py-1 rounded-full border border-purple-500/20"
        >
          {stateLabels[activeCall.state] || 'Połączenie'}
        </span>

        <ContactAvatar name={displayName} size="xl" className="shadow-2xl ring-4 ring-[#7B4DFF]/20 my-2" />

        <div>
          <h2 className="text-2xl font-bold text-white tracking-tight">
            {displayName}
          </h2>
          {contact && (
            <p className="text-sm text-[#9499AD] mt-0.5">{activeCall.number}</p>
          )}
        </div>

        {isActive && (
          <p
            data-testid="call_timer"
            className="text-xl font-mono tracking-wider font-semibold text-[#8BE0BB]"
          >
            {formatTimer(timerSeconds)}
          </p>
        )}

        {isHolding && (
          <p className="text-sm text-[#FFD089] font-medium bg-[#805000]/20 px-3 py-1 rounded-full border border-[#FFD089]/30">
            Rozmowa wstrzymana
          </p>
        )}

        {isRinging && (
          <div className="max-w-xs bg-amber-500/10 border border-amber-500/30 rounded-xl p-3 text-xs text-amber-200">
            Wyświetlany numer nie potwierdza tożsamości rozmówcy.
          </div>
        )}

        {/* DTMF Keypad View */}
        {showKeypad && (
          <div className="w-full max-w-xs bg-[#1A1B30]/90 backdrop-blur-md rounded-2xl p-4 border border-[#272540] my-4 animate-in slide-in-from-bottom duration-200">
            <div className="h-8 flex items-center justify-center font-mono text-xl tracking-widest text-[#A875FF] mb-3">
              {enteredDigits || 'Wpisz ton'}
            </div>
            <div className="grid grid-cols-3 gap-2.5">
              {['1', '2', '3', '4', '5', '6', '7', '8', '9', '*', '0', '#'].map(digit => (
                <button
                  key={digit}
                  onClick={() => handleDigit(digit)}
                  className="py-3 rounded-xl bg-[#22233C] text-lg font-bold text-white hover:bg-[#7B4DFF]/30 active:scale-95 transition-all"
                >
                  {digit}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Audio device dialog */}
      {showAudioDialog && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="bg-[#131424] border border-[#272540] rounded-2xl p-5 max-w-xs w-full space-y-3">
            <h3 className="text-base font-bold text-white">Dźwięk rozmowy</h3>
            <div className="space-y-2">
              {callSession.audio.map(choice => (
                <button
                  key={choice.id}
                  onClick={() => {
                    setAudioChoice(choice.id);
                    setShowAudioDialog(false);
                  }}
                  className={`w-full py-2.5 px-3 rounded-xl text-left text-sm font-medium transition-all ${
                    callSession.selectedAudio === choice.id
                      ? 'bg-[#7B4DFF] text-white'
                      : 'bg-[#1A1B30] text-[#9499AD] hover:text-white'
                  }`}
                >
                  {callSession.selectedAudio === choice.id ? '✓  ' : ''}
                  {choice.label}
                </button>
              ))}
            </div>
            <button
              onClick={() => setShowAudioDialog(false)}
              className="w-full py-2 text-xs text-[#9499AD] hover:text-white"
            >
              Zamknij
            </button>
          </div>
        </div>
      )}

      {/* Action Controls */}
      <div className="w-full max-w-sm mx-auto pb-4">
        {isRinging ? (
          <div className="flex items-center justify-around">
            <div className="flex flex-col items-center gap-2">
              <button
                onClick={() => endCall(activeCall.id)}
                data-testid="call_reject"
                className="w-18 h-18 rounded-full bg-[#E5697B] text-white flex items-center justify-center shadow-lg hover:scale-105 active:scale-95 transition-all"
              >
                <PhoneOff className="w-8 h-8" />
              </button>
              <span className="text-xs font-semibold text-[#FF8596]">Odrzuć</span>
            </div>

            <div className="flex flex-col items-center gap-2">
              <button
                onClick={() => answerCall(activeCall.id)}
                data-testid="call_answer"
                className="w-18 h-18 rounded-full bg-[#8BE0BB] text-[#10392A] flex items-center justify-center shadow-lg hover:scale-105 active:scale-95 transition-all animate-pulse"
              >
                <Phone className="w-8 h-8" />
              </button>
              <span className="text-xs font-semibold text-[#8BE0BB]">Odbierz</span>
            </div>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Top row controls */}
            <div className="flex items-center justify-around">
              <button
                onClick={toggleMute}
                data-testid="call_mute"
                className={`flex flex-col items-center gap-1.5 p-3 rounded-2xl transition-all ${
                  callSession.muted
                    ? 'bg-[#7B4DFF] text-white'
                    : 'bg-[#1A1B30] text-[#9499AD] hover:text-white'
                }`}
              >
                {callSession.muted ? (
                  <MicOff className="w-6 h-6" />
                ) : (
                  <Mic className="w-6 h-6" />
                )}
                <span className="text-[11px] font-medium">
                  {callSession.muted ? 'Wyciszony' : 'Mikrofon'}
                </span>
              </button>

              <button
                onClick={() => setShowAudioDialog(true)}
                data-testid="call_audio"
                className="flex flex-col items-center gap-1.5 p-3 rounded-2xl bg-[#1A1B30] text-[#9499AD] hover:text-white transition-all"
              >
                <Volume2 className="w-6 h-6" />
                <span className="text-[11px] font-medium">Dźwięk</span>
              </button>

              <button
                onClick={() => setShowKeypad(prev => !prev)}
                data-testid="call_keypad"
                className={`flex flex-col items-center gap-1.5 p-3 rounded-2xl transition-all ${
                  showKeypad
                    ? 'bg-[#7B4DFF] text-white'
                    : 'bg-[#1A1B30] text-[#9499AD] hover:text-white'
                }`}
              >
                <Grid className="w-6 h-6" />
                <span className="text-[11px] font-medium">Klawiatura</span>
              </button>

              <button
                onClick={() => holdCall(activeCall.id)}
                className={`flex flex-col items-center gap-1.5 p-3 rounded-2xl transition-all ${
                  isHolding
                    ? 'bg-[#FFD089] text-[#171321]'
                    : 'bg-[#1A1B30] text-[#9499AD] hover:text-white'
                }`}
              >
                {isHolding ? <Play className="w-6 h-6" /> : <Pause className="w-6 h-6" />}
                <span className="text-[11px] font-medium">
                  {isHolding ? 'Wznów' : 'Zawieś'}
                </span>
              </button>
            </div>

            {/* End call red button */}
            <div className="flex justify-center">
              <button
                onClick={() => endCall(activeCall.id)}
                data-testid="call_end"
                className="w-18 h-18 rounded-full bg-[#E5697B] text-white flex items-center justify-center shadow-xl hover:scale-105 active:scale-95 transition-all"
              >
                <PhoneOff className="w-8 h-8" />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
