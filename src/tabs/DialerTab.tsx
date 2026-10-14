import React, { useState, useEffect } from 'react';
import {
  Phone,
  MessageSquare,
  Delete,
  Search,
  Info,
  ChevronRight,
  RotateCcw
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { parsePhone, normalizeDialTarget } from '../utils/phone';
import { ContactAvatar } from '../components/ContactAvatar';
import { CallHistoryEntry, DeviceContact } from '../types';

export function DialerTab() {
  const {
    notes,
    dialPrefill,
    setDialPrefill,
    setSmsPrefill,
    setPhoneInput,
    setActiveTab,
    startCall,
    callHistory,
    contacts,
    appearance
  } = useApp();

  const [section, setSection] = useState<number>(2); // 0: Ostatnie, 1: Kontakty, 2: Klawiatura
  const [number, setNumber] = useState<string>('');
  const [recentFilter, setRecentFilter] = useState<string>('');
  const [contactFilter, setContactFilter] = useState<string>('');
  const [missedOnly, setMissedOnly] = useState<boolean>(false);
  const [showInfoModal, setShowInfoModal] = useState<boolean>(false);

  useEffect(() => {
    if (dialPrefill) {
      setNumber(dialPrefill);
      setSection(2);
      setDialPrefill(null);
    }
  }, [dialPrefill, setDialPrefill]);

  const parsed = parsePhone(number);
  const target = normalizeDialTarget(number);
  const isBlocked = parsed ? notes.some(n => n.phone === parsed.e164 && n.blocked) : false;

  const handleKeyPress = (key: string) => {
    if (appearance.haptics && 'vibrate' in navigator) {
      navigator.vibrate(30);
    }
    setNumber(prev => (prev + key).slice(0, 40));
  };

  const handleLongPressZero = () => {
    if (appearance.haptics && 'vibrate' in navigator) {
      navigator.vibrate(50);
    }
    setNumber(prev => (prev + '+').slice(0, 40));
  };

  const handleCall = () => {
    if (target) {
      startCall(target);
    }
  };

  const handleGoToSms = () => {
    if (parsed) {
      setSmsPrefill(parsed.e164);
      setActiveTab(1);
    }
  };

  const handleGoToNote = () => {
    if (parsed) {
      setPhoneInput(parsed.e164);
      setActiveTab(3);
    }
  };

  const keys = [
    { key: '1', letters: '' },
    { key: '2', letters: 'ABC' },
    { key: '3', letters: 'DEF' },
    { key: '4', letters: 'GHI' },
    { key: '5', letters: 'JKL' },
    { key: '6', letters: 'MNO' },
    { key: '7', letters: 'PQRS' },
    { key: '8', letters: 'TUV' },
    { key: '9', letters: 'WXYZ' },
    { key: '*', letters: '' },
    { key: '0', letters: '+' },
    { key: '#', letters: '' }
  ];

  // Filtering recent calls
  const filteredHistory = callHistory.filter(item => {
    if (missedOnly && item.type !== 3) return false;
    if (recentFilter) {
      const q = recentFilter.toLowerCase();
      const matchName = item.name?.toLowerCase().includes(q);
      const matchNumber = item.number.includes(q);
      return matchName || matchNumber;
    }
    return true;
  });

  // Filtering contacts
  const filteredContacts = contacts.filter(c => {
    if (contactFilter) {
      const q = contactFilter.toLowerCase();
      return c.name.toLowerCase().includes(q) || c.phone.includes(q);
    }
    return true;
  });

  const getCallTypeLabel = (type: number) => {
    switch (type) {
      case 1:
        return 'Połączenie przychodzące';
      case 2:
        return 'Połączenie wychodzące';
      case 3:
        return 'Nieodebrane';
      case 4:
        return 'Odrzucone';
      case 6:
        return 'Zablokowane przez system';
      default:
        return 'Połączenie';
    }
  };

  const formatDateTime = (timestamp: number) => {
    const d = new Date(timestamp);
    return `${d.toLocaleDateString('pl-PL')} ${d.toLocaleTimeString('pl-PL', {
      hour: '2-digit',
      minute: '2-digit'
    })}`;
  };

  return (
    <div className="flex flex-col h-full space-y-4">
      {/* Top Title & Info */}
      <div className="flex items-center justify-between">
        <h2 className="text-3xl font-extrabold text-white tracking-tight">
          Telefon
        </h2>
        <button
          onClick={() => setShowInfoModal(true)}
          className="p-2 text-[#9499AD] hover:text-white rounded-full hover:bg-white/5 transition-colors"
          title="O funkcjach telefonu"
        >
          <Info className="w-5 h-5" />
        </button>
      </div>

      {/* Segmented Bar */}
      <div className="bg-[#1A1B30]/60 p-1 rounded-2xl flex border border-[#272540]">
        {['Ostatnie', 'Kontakty', 'Klawiatura'].map((label, idx) => (
          <button
            key={label}
            onClick={() => setSection(idx)}
            data-testid={`dial_section_${idx}`}
            className={`flex-1 py-2 rounded-xl text-sm font-medium transition-all ${
              section === idx
                ? 'bg-[#22233C] text-white shadow-sm font-semibold'
                : 'text-[#9499AD] hover:text-white'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {/* SECTION 2: Klawiatura */}
      {section === 2 && (
        <div className="flex-1 flex flex-col items-center justify-between py-2 space-y-4">
          {/* Display & parsed format */}
          <div className="w-full text-center space-y-1 min-h-[72px] flex flex-col justify-center">
            <input
              type="text"
              value={number}
              onChange={e => setNumber(e.target.value.slice(0, 40))}
              placeholder="Wybierz numer"
              data-testid="dial_input"
              className="w-full text-center text-3xl font-bold bg-transparent text-white focus:outline-none placeholder:text-[#6C6F85] tracking-tight"
            />
            {parsed && (
              <p className="text-sm text-[#9499AD] font-medium">
                {parsed.formatted}
              </p>
            )}
            {isBlocked && (
              <p className="text-xs font-semibold text-[#FFD089] bg-[#805000]/20 py-0.5 px-2.5 rounded-full inline-block mx-auto border border-[#FFD089]/30">
                Na Twojej liście blokad
              </p>
            )}
          </div>

          {/* Keypad Grid */}
          <div className="grid grid-cols-3 gap-x-6 gap-y-3.5 max-w-xs w-full">
            {keys.map(k => (
              <button
                key={k.key}
                onClick={() => handleKeyPress(k.key)}
                onContextMenu={e => {
                  if (k.key === '0') {
                    e.preventDefault();
                    handleLongPressZero();
                  }
                }}
                data-testid={`dial_key_${k.key}`}
                className="w-18 h-18 rounded-full bg-[#1A1B30]/90 border border-[#272540] hover:bg-[#22233C] active:bg-[#7B4DFF]/30 active:scale-90 transition-all flex flex-col items-center justify-center mx-auto shadow-md"
              >
                <span className="text-2xl font-semibold text-white leading-tight">
                  {k.key}
                </span>
                {k.letters && (
                  <span className="text-[10px] tracking-widest text-[#9499AD] font-medium uppercase">
                    {k.letters}
                  </span>
                )}
              </button>
            ))}
          </div>

          {/* Action Row */}
          <div className="flex items-center justify-around w-full max-w-xs pt-2">
            <button
              onClick={handleGoToSms}
              disabled={!parsed}
              data-testid="dial_sms"
              title="Napisz SMS"
              className="p-4 rounded-full text-purple-300 hover:text-white hover:bg-white/5 disabled:opacity-30 disabled:hover:bg-transparent transition-all"
            >
              <MessageSquare className="w-6 h-6" />
            </button>

            <button
              onClick={handleCall}
              disabled={!target}
              data-testid="dial_call"
              className="w-24 h-16 rounded-3xl bg-[#8BE0BB] hover:bg-[#78cfab] active:scale-95 text-[#10392A] flex items-center justify-center shadow-lg disabled:opacity-40 disabled:hover:bg-[#8BE0BB] transition-all"
            >
              <Phone className="w-7 h-7 fill-current" />
            </button>

            <button
              onClick={() => setNumber(prev => prev.slice(0, -1))}
              disabled={!number}
              data-testid="dial_backspace"
              title="Usuń cyfrę"
              className="p-4 rounded-full text-[#9499AD] hover:text-white hover:bg-white/5 disabled:opacity-30 disabled:hover:bg-transparent transition-all"
            >
              <Delete className="w-6 h-6" />
            </button>
          </div>

          {/* Quick links when number is entered */}
          {number && (
            <div className="flex items-center gap-4 text-xs">
              <button
                onClick={handleGoToNote}
                disabled={!parsed}
                className="text-[#A875FF] hover:underline disabled:opacity-40"
              >
                Notatka / blokada
              </button>
              <span className="text-[#272540]">•</span>
              <button
                onClick={() => setNumber('')}
                className="text-[#9499AD] hover:text-white"
              >
                Wyczyść
              </button>
            </div>
          )}
        </div>
      )}

      {/* SECTION 0: Ostatnie (Recent Calls) */}
      {section === 0 && (
        <div className="flex-1 flex flex-col space-y-3 overflow-hidden">
          {/* Search & filters */}
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3.5 top-3 text-[#9499AD]" />
            <input
              type="text"
              value={recentFilter}
              onChange={e => setRecentFilter(e.target.value)}
              placeholder="Szukaj połączenia"
              className="w-full pl-10 pr-4 py-2 rounded-2xl bg-[#1A1B30]/80 border border-[#272540] text-sm text-white placeholder:text-[#6C6F85] focus:outline-none focus:border-[#7B4DFF]/50"
            />
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setMissedOnly(false)}
              className={`py-1 px-3 rounded-xl text-xs font-medium transition-all ${
                !missedOnly
                  ? 'bg-[#7B4DFF] text-white'
                  : 'bg-[#1A1B30] text-[#9499AD] hover:text-white'
              }`}
            >
              Wszystkie
            </button>
            <button
              onClick={() => setMissedOnly(true)}
              className={`py-1 px-3 rounded-xl text-xs font-medium transition-all ${
                missedOnly
                  ? 'bg-[#7B4DFF] text-white'
                  : 'bg-[#1A1B30] text-[#9499AD] hover:text-white'
              }`}
            >
              Nieodebrane
            </button>
          </div>

          {/* List */}
          <div className="flex-1 overflow-y-auto space-y-2 pr-1">
            {filteredHistory.length === 0 ? (
              <div className="py-12 text-center text-[#9499AD] text-sm">
                {missedOnly
                  ? 'Brak nieodebranych połączeń.'
                  : recentFilter
                  ? 'Brak pasujących połączeń.'
                  : 'Brak historii połączeń.'}
              </div>
            ) : (
              filteredHistory.map(entry => {
                const displayName = entry.name || entry.number || 'Numer prywatny';
                const isMissed = entry.type === 3;

                return (
                  <div
                    key={entry.id}
                    onClick={() => {
                      setNumber(entry.number);
                      setSection(2);
                    }}
                    className="flex items-center justify-between p-3 rounded-2xl bg-[#131424]/80 hover:bg-[#1A1B30] border border-[#272540] cursor-pointer transition-colors"
                  >
                    <div className="flex items-center gap-3">
                      <ContactAvatar name={displayName} size="md" />
                      <div>
                        <p
                          className={`text-sm font-semibold ${
                            isMissed ? 'text-[#FF8596]' : 'text-white'
                          }`}
                        >
                          {displayName}
                        </p>
                        <p
                          className={`text-xs ${
                            isMissed ? 'text-[#FFD089]' : 'text-[#9499AD]'
                          }`}
                        >
                          {getCallTypeLabel(entry.type)}
                        </p>
                        <p className="text-[11px] text-[#6C6F85]">
                          {formatDateTime(entry.date)}
                        </p>
                      </div>
                    </div>

                    <button
                      onClick={e => {
                        e.stopPropagation();
                        setSmsPrefill(entry.number);
                        setActiveTab(1);
                      }}
                      className="p-2 text-purple-300 hover:text-white hover:bg-white/5 rounded-full transition-colors"
                      title="Napisz SMS"
                    >
                      <MessageSquare className="w-4 h-4" />
                    </button>
                  </div>
                );
              })
            )}
          </div>
        </div>
      )}

      {/* SECTION 1: Kontakty */}
      {section === 1 && (
        <div className="flex-1 flex flex-col space-y-3 overflow-hidden">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3.5 top-3 text-[#9499AD]" />
            <input
              type="text"
              value={contactFilter}
              onChange={e => setContactFilter(e.target.value)}
              placeholder="Szukaj kontaktu"
              className="w-full pl-10 pr-4 py-2 rounded-2xl bg-[#1A1B30]/80 border border-[#272540] text-sm text-white placeholder:text-[#6C6F85] focus:outline-none focus:border-[#7B4DFF]/50"
            />
          </div>

          <div className="flex-1 overflow-y-auto space-y-2 pr-1">
            {filteredContacts.length === 0 ? (
              <div className="py-12 text-center text-[#9499AD] text-sm">
                Nie znaleziono kontaktów.
              </div>
            ) : (
              filteredContacts.map(contact => (
                <div
                  key={contact.id}
                  onClick={() => {
                    setNumber(contact.phone);
                    setSection(2);
                  }}
                  className="flex items-center justify-between p-3 rounded-2xl bg-[#131424]/80 hover:bg-[#1A1B30] border border-[#272540] cursor-pointer transition-colors"
                >
                  <div className="flex items-center gap-3">
                    <ContactAvatar name={contact.name} size="md" />
                    <div>
                      <p className="text-sm font-semibold text-white">
                        {contact.name}
                      </p>
                      <p className="text-xs text-[#9499AD]">{contact.phone}</p>
                    </div>
                  </div>
                  <ChevronRight className="w-4 h-4 text-[#6C6F85]" />
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Info Modal */}
      {showInfoModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-[#131424] border border-[#272540] rounded-3xl p-6 max-w-sm w-full space-y-4 shadow-2xl">
            <h3 className="text-lg font-bold text-white">Telefon w CzyToTy?</h3>
            <p className="text-xs text-[#9499AD] leading-relaxed">
              CzyToTy? łączy codzienny dialer, historię połączeń i książkę kontaktów z filtrowaniem połączeń oraz bazą ostrzeżeń.
            </p>
            <div className="p-3 bg-[#1A1B30] rounded-xl border border-[#272540] text-xs text-white/90">
              Numer nie potwierdza tożsamości rozmówcy. Zapisane notatki i blokady: {notes.length}.
            </div>
            <button
              onClick={() => setShowInfoModal(false)}
              className="w-full py-2.5 rounded-xl bg-[#7B4DFF] text-white text-sm font-semibold hover:bg-[#6a3ee0] transition-colors"
            >
              Rozumiem
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
