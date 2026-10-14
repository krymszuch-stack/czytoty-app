import React, { useState, useEffect, useMemo } from 'react';
import {
  Phone,
  MessageSquare,
  Delete,
  Search,
  Info,
  ChevronRight,
  AlertOctagon,
  ShieldAlert,
  Sparkles,
  Download,
  Check,
  Building,
  HeartHandshake,
  UserCheck,
  User,
  Flame,
  Ambulance,
  Siren,
  Sliders,
  Zap,
  QrCode
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { parsePhone, normalizeDialTarget } from '../utils/phone';
import { ContactAvatar } from '../components/ContactAvatar';
import { BusinessEntity, CallHistoryEntry, DeviceContact, EmergencyService } from '../types';
import { cleanPhoneDigits } from '../services/directory';
import { QuickShareSheet } from '../components/QuickShareSheet';
import { DialerDisplay, CallerPurposeInfo } from '../components/DialerDisplay';
import { evaluateWangiriRisk } from '../utils/wangiri';
import { analyzeRobocall } from '../utils/robocall';

export function DialerTab() {
  const {
    notes,
    dialPrefill,
    setDialPrefill,
    setSmsPrefill,
    setPhoneInput,
    setActiveTab,
    startCall,
    triggerSmartArrowCall,
    appendDirectSms,
    callHistory,
    contacts,
    appearance,
    updateAppearance,
    directory,
    searchDirectory,
    importDirectoryEntity,
    getEmergencyDetails,
    isEmergency,
    userCard,
    callIceContact,
    smsIceContact,
    showNotice
  } = useApp();

  const [section, setSection] = useState<number>(2); // 0: Ostatnie, 1: Kontakty, 2: Klawiatura
  const [number, setNumber] = useState<string>('');
  const [recentFilter, setRecentFilter] = useState<string>('');
  const [contactFilter, setContactFilter] = useState<string>('');
  const [missedOnly, setMissedOnly] = useState<boolean>(false);
  const [showInfoModal, setShowInfoModal] = useState<boolean>(false);
  const [isShareSheetOpen, setIsShareSheetOpen] = useState<boolean>(false);

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

  // Emergency Resolution
  const emergencyInfo = getEmergencyDetails(number);
  const isEmergencyNumber = emergencyInfo !== null || isEmergency(number);

  // Business Directory Search
  const matchingBusinesses = number.trim().length >= 2 ? searchDirectory(number) : [];
  const activeBusiness = matchingBusinesses.length > 0 ? matchingBusinesses[0] : null;

  // Contact Matching
  const matchedContact = number.trim()
    ? contacts.find(c => {
        const cClean = c.phone.replace(/\D/g, '');
        const numClean = number.replace(/\D/g, '');
        return cClean && numClean && (cClean.endsWith(numClean) || numClean.endsWith(cClean));
      })
    : null;

  // Construct structured lookup information for DialerDisplay
  const lookupData: CallerPurposeInfo | null = useMemo(() => {
    if (emergencyInfo) {
      return {
        name: emergencyInfo.name,
        department: `Służby ratunkowe: ${emergencyInfo.department}`,
        source: 'ALARM',
        category: 'EMERGENCY'
      };
    }
    if (activeBusiness) {
      return {
        name: activeBusiness.name,
        department: activeBusiness.address,
        source: activeBusiness.source,
        rating: activeBusiness.rating,
        category: activeBusiness.category.toUpperCase()
      };
    }
    if (matchedContact) {
      return {
        name: matchedContact.name,
        department: 'Zapisany kontakt w książce telefonicznej',
        source: 'KONTAKT',
        category: 'CONTACT'
      };
    }
    const cleanNum = number.replace(/\D/g, '');
    const cleanIce = userCard.iceContactPhone?.replace(/\D/g, '');
    if (cleanIce && cleanNum && (cleanIce.endsWith(cleanNum) || cleanNum.endsWith(cleanIce))) {
      return {
        name: `Kontakt ICE: ${userCard.iceContactName}`,
        department: 'Osoba do powiadomienia w razie nagłego wypadku',
        source: 'ICE',
        category: 'ICE'
      };
    }
    if (parsed) {
      return {
        name: parsed.formatted,
        department: `Połączenie telefoniczne (${parsed.country || 'Polska'})`,
        source: 'CERTIFIED',
        category: 'DEFAULT'
      };
    }
    return null;
  }, [emergencyInfo, activeBusiness, matchedContact, number, userCard, parsed]);

  // Background auto-import of verified business entity to permanent offline database
  useEffect(() => {
    if (activeBusiness && !activeBusiness.importedAt) {
      importDirectoryEntity(activeBusiness);
    }
  }, [activeBusiness, importDirectoryEntity]);

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
    if (emergencyInfo) {
      startCall(emergencyInfo.number);
      return;
    }
    if (target) {
      startCall(target);
    }
  };

  const handleInstantSms = () => {
    if (appearance.haptics && 'vibrate' in navigator) {
      navigator.vibrate(30);
    }
    if (parsed) {
      setSmsPrefill(parsed.e164);
    } else if (number.trim().length > 0) {
      setSmsPrefill(number.trim());
    }
    // Instant switch to SMS conversation / composer
    setActiveTab(1);
  };

  const toggleSeniorMode = () => {
    updateAppearance({
      ...appearance,
      largeText: !appearance.largeText,
      compact: false
    });
    showNotice(
      !appearance.largeText
        ? 'Włączono Tryb Seniora: powiększone cyfry i przyciski.'
        : 'Wyłączono powiększenie Trybu Seniora.'
    );
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
    <div className="flex flex-col h-full space-y-3 pb-20">
      {/* Top Title, Senior Mode Toggle & Info */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight flex items-center gap-2">
            Telefon & SMS
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-[#7B4DFF]/20 text-[#A875FF] border border-[#7B4DFF]/30">
              2w1
            </span>
          </h2>
          <p className="text-xs text-[#9499AD]">
            Zintegrowany dialer, baza jednostek i natychmiastowe wiadomości
          </p>
        </div>

        <div className="flex items-center gap-2">
          {/* Senior Mode Toggle Button */}
          <button
            onClick={toggleSeniorMode}
            title="Włącz/wyłącz tryb seniora z dużymi przyciskami"
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition-all border ${
              appearance.largeText
                ? 'bg-[#8BE0BB] text-[#10392A] border-[#8BE0BB] shadow-md shadow-[#8BE0BB]/20'
                : 'bg-[#1A1B30] text-[#9499AD] border-[#272540] hover:text-white'
            }`}
          >
            <span>{appearance.largeText ? '✓ Tryb Seniora' : 'Tryb Seniora'}</span>
          </button>

          <button
            onClick={() => setShowInfoModal(true)}
            className="p-2 text-[#9499AD] hover:text-white rounded-full hover:bg-white/5 transition-colors"
            title="O funkcjach telefonu i bazach danych"
          >
            <Info className="w-5 h-5" />
          </button>
        </div>
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
        <div className="flex-1 flex flex-col items-center justify-between py-1 space-y-3">
          {/* Okienko wyświetlacza dialera: 3 poziomy w pionie, sztywna siatka grid, debounce 220ms, ikony wektorowe SVG */}
          <DialerDisplay
            digits={number}
            onDigitsChange={setNumber}
            lookupData={lookupData}
            isEmergency={isEmergencyNumber}
            isBlocked={isBlocked}
            largeText={appearance.largeText}
          />

          {/* Keypad Grid (Ergonomiczny & Senior-friendly) */}
          <div
            className={`grid grid-cols-3 gap-x-5 gap-y-3 w-full mx-auto ${
              appearance.largeText ? 'max-w-sm' : 'max-w-xs'
            }`}
          >
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
                className={`rounded-full bg-[#1A1B30]/90 border border-[#272540] hover:bg-[#22233C] active:bg-[#7B4DFF]/40 active:scale-95 transition-all flex flex-col items-center justify-center mx-auto shadow-md ${
                  appearance.largeText ? 'w-20 h-20' : 'w-18 h-18'
                }`}
              >
                <span
                  className={`font-bold text-white leading-tight ${
                    appearance.largeText ? 'text-3xl' : 'text-2xl'
                  }`}
                >
                  {k.key}
                </span>
                {k.letters && (
                  <span
                    className={`tracking-widest text-[#9499AD] font-semibold uppercase ${
                      appearance.largeText ? 'text-xs' : 'text-[10px]'
                    }`}
                  >
                    {k.letters}
                  </span>
                )}
              </button>
            ))}
          </div>

          {/* Przyciski akcji pod klawiaturą: SMS, Połączenie i Kasowanie */}
          <div className="w-full max-w-sm pt-2 space-y-2">
            <div className="grid grid-cols-12 gap-2.5 items-center">
              {/* Łatwy dostęp do ikony SMS */}
              <button
                onClick={handleInstantSms}
                data-testid="dial_sms"
                title={number ? `Napisz wiadomość SMS do ${number}` : 'Otwórz wiadomości SMS'}
                className="col-span-4 h-16 rounded-2xl bg-[#1D1B36] hover:bg-[#28264A] active:scale-95 text-white flex flex-col items-center justify-center border border-[#7B4DFF]/40 shadow-md shadow-[#7B4DFF]/10 transition-all"
              >
                <MessageSquare className="w-6 h-6 text-[#A875FF]" />
                <span className="text-xs font-bold text-purple-200 mt-1">SMS</span>
              </button>

              {/* Główny przycisk Połączenia (Zwykły zielony lub Alarmowy czerwony) */}
              <button
                onClick={handleCall}
                disabled={!target && !emergencyInfo}
                data-testid="dial_call"
                className={`col-span-5 h-16 rounded-2xl active:scale-95 flex items-center justify-center gap-2 shadow-xl transition-all disabled:opacity-40 ${
                  emergencyInfo
                    ? 'bg-red-600 hover:bg-red-500 text-white shadow-red-600/40 animate-pulse'
                    : 'bg-[#8BE0BB] hover:bg-[#78cfab] text-[#10392A] shadow-emerald-900/30'
                }`}
              >
                <Phone className="w-6 h-6 fill-current shrink-0" />
                <div className="text-left leading-tight">
                  <span className="text-sm font-extrabold block">
                    {emergencyInfo ? 'WEZWIJ POMOC' : 'Zadzwoń'}
                  </span>
                  <span className="text-[10px] font-semibold opacity-80 block">
                    {emergencyInfo ? `Oddział ${emergencyInfo.number}` : 'Połączenie'}
                  </span>
                </div>
              </button>

              {/* Przycisk Kasowania (Backspace) */}
              <button
                onClick={() => setNumber(prev => prev.slice(0, -1))}
                onContextMenu={e => {
                  e.preventDefault();
                  setNumber('');
                }}
                disabled={!number}
                data-testid="dial_backspace"
                title="Usuń cyfrę (przytrzymaj, aby wyczyścić)"
                className="col-span-3 h-16 rounded-2xl bg-[#1A1B30] hover:bg-[#22233C] border border-[#272540] text-[#9499AD] hover:text-white flex flex-col items-center justify-center disabled:opacity-30 transition-all"
              >
                <Delete className="w-6 h-6" />
                <span className="text-[10px] text-[#9499AD] mt-0.5">Kasuj</span>
              </button>
            </div>

            {/* Przycisk Smart-Strzałki & Wizytówki QR */}
            <div className="pt-1 w-full">
              <button
                type="button"
                onClick={() => setIsShareSheetOpen(true)}
                data-testid="dial_smart_share"
                className="w-full py-2.5 px-3 rounded-2xl bg-gradient-to-r from-[#201D3A] via-[#1B1A33] to-[#161629] hover:from-[#29254C] hover:to-[#1F1E38] border border-[#7B4DFF]/35 hover:border-[#7B4DFF]/60 text-purple-200 hover:text-white flex items-center justify-center gap-2 text-xs font-semibold shadow-md transition-all active:scale-[0.98]"
              >
                <Zap className="w-4 h-4 text-[#FFD089] fill-current" />
                <span>Smart-Strzałka & Wizytówka QR</span>
                <span className="text-[10px] bg-[#7B4DFF]/30 text-[#CBB2FF] px-1.5 py-0.5 rounded-full font-bold ml-0.5">
                  Patent
                </span>
              </button>
            </div>

            {/* Dolne szybkie akcje przy wpisanym numerze */}
            {number && (
              <div className="flex items-center justify-center gap-3 text-xs pt-1 flex-wrap">
                <button
                  type="button"
                  onClick={() => setIsShareSheetOpen(true)}
                  className="text-[#FFD089] hover:underline font-bold flex items-center gap-1"
                >
                  <Zap className="w-3.5 h-3.5" />
                  <span>Wyślij wizytówkę</span>
                </button>
                <span className="text-[#272540]">•</span>
                <button
                  type="button"
                  onClick={() => {
                    if (parsed) {
                      setPhoneInput(parsed.e164);
                      setActiveTab(3);
                    }
                  }}
                  disabled={!parsed}
                  className="text-[#A875FF] hover:underline font-semibold disabled:opacity-40"
                >
                  Dodaj notatkę lub zablokuj
                </button>
                <span className="text-[#272540]">•</span>
                <button
                  type="button"
                  onClick={() => setNumber('')}
                  className="text-[#9499AD] hover:text-white font-medium"
                >
                  Wyczyść klawiaturę
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* SECTION 0: Ostatnie (Recent Calls) */}
      {section === 0 && (
        <div className="flex-1 flex flex-col space-y-3 overflow-hidden">
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3.5 top-3 text-[#9499AD]" />
            <input
              type="text"
              value={recentFilter}
              onChange={e => setRecentFilter(e.target.value)}
              placeholder="Szukaj w historii połączeń"
              className="w-full pl-10 pr-4 py-2 rounded-2xl bg-[#1A1B30]/80 border border-[#272540] text-sm text-white placeholder:text-[#6C6F85] focus:outline-none focus:border-[#7B4DFF]/50"
            />
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setMissedOnly(false)}
              className={`py-1 px-3 rounded-xl text-xs font-medium transition-all ${
                !missedOnly
                  ? 'bg-[#7B4DFF] text-white font-bold'
                  : 'bg-[#1A1B30] text-[#9499AD] hover:text-white'
              }`}
            >
              Wszystkie
            </button>
            <button
              onClick={() => setMissedOnly(true)}
              className={`py-1 px-3 rounded-xl text-xs font-medium transition-all ${
                missedOnly
                  ? 'bg-[#7B4DFF] text-white font-bold'
                  : 'bg-[#1A1B30] text-[#9499AD] hover:text-white'
              }`}
            >
              Nieodebrane
            </button>
          </div>

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
                const wangiriRisk = evaluateWangiriRisk(
                  entry.number,
                  entry.durationSec,
                  2,
                  entry.type === 3 || entry.type === 4
                );
                const roboAnalysis = analyzeRobocall(entry.number, callHistory, [], contacts);

                return (
                  <div
                    key={entry.id}
                    onClick={() => {
                      setNumber(entry.number);
                      setSection(2);
                    }}
                    className={`flex items-center justify-between p-3 rounded-2xl border cursor-pointer transition-all ${
                      wangiriRisk.isWangiri && (wangiriRisk.riskLevel === 'CRITICAL' || wangiriRisk.riskLevel === 'HIGH')
                        ? 'bg-[#251016]/80 hover:bg-[#32151D] border-red-500/40 shadow-sm'
                        : roboAnalysis.isRobocall && roboAnalysis.probabilityScore >= 60
                        ? 'bg-[#1C172E]/80 hover:bg-[#251E3D] border-[#7B4DFF]/35'
                        : 'bg-[#131424]/80 hover:bg-[#1A1B30] border-[#272540]'
                    }`}
                  >
                    <div className="flex items-center gap-3 min-w-0 flex-1 mr-2">
                      <ContactAvatar name={displayName} size="md" />
                      <div className="min-w-0 flex-1">
                        <div className="flex items-center gap-1.5 flex-wrap">
                          <p
                            className={`text-sm font-semibold truncate ${
                              wangiriRisk.isWangiri && (wangiriRisk.riskLevel === 'CRITICAL' || wangiriRisk.riskLevel === 'HIGH')
                                ? 'text-red-300 font-bold'
                                : isMissed
                                ? 'text-[#FF8596]'
                                : 'text-white'
                            }`}
                          >
                            {displayName}
                          </p>
                          {/* Wangiri Alert Badge */}
                          {wangiriRisk.isWangiri && (wangiriRisk.riskLevel === 'CRITICAL' || wangiriRisk.riskLevel === 'HIGH') && (
                            <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md bg-red-600/30 border border-red-500/40 text-[10px] font-bold text-red-200">
                              <span>{wangiriRisk.countryFlag}</span>
                              <span>WANGIRI ~{wangiriRisk.estimatedCostRange}</span>
                            </span>
                          )}
                          {/* Robocall Badge */}
                          {!wangiriRisk.isWangiri && roboAnalysis.isRobocall && roboAnalysis.probabilityScore >= 60 && (
                            <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md bg-[#7B4DFF]/25 border border-[#7B4DFF]/40 text-[10px] font-bold text-purple-200">
                              <span>🤖 {roboAnalysis.probabilityScore}% Bot</span>
                            </span>
                          )}
                        </div>

                        <p
                          className={`text-xs ${
                            isMissed ? 'text-[#FFD089]' : 'text-[#9499AD]'
                          }`}
                        >
                          {getCallTypeLabel(entry.type)}
                          {entry.durationSec > 0 && ` (${entry.durationSec}s)`}
                        </p>
                        <p className="text-[11px] text-[#6C6F85]">
                          {formatDateTime(entry.date)}
                        </p>
                      </div>
                    </div>

                    <div className="flex items-center gap-1 shrink-0">
                      {/* Direct Call Button (Intercepted safely by Wangiri Shield if risky) */}
                      <button
                        onClick={e => {
                          e.stopPropagation();
                          startCall(entry.number);
                        }}
                        className={`p-2 rounded-full transition-colors ${
                          wangiriRisk.isWangiri
                            ? 'text-red-400 hover:text-red-300 hover:bg-red-500/20'
                            : 'text-[#8BE0BB] hover:text-white hover:bg-emerald-500/15'
                        }`}
                        title={
                          wangiriRisk.isWangiri
                            ? 'Oddzwoń (Tarcza Wangiri przechwyci i zweryfikuje koszt)'
                            : `Zadzwoń do ${displayName}`
                        }
                      >
                        <Phone className="w-4 h-4 fill-current" />
                      </button>

                      <button
                        onClick={e => {
                          e.stopPropagation();
                          setSmsPrefill(entry.number);
                          setActiveTab(1);
                        }}
                        className="p-2 text-purple-300 hover:text-white hover:bg-white/5 rounded-full transition-colors"
                        title="Napisz natychmiastowy SMS"
                      >
                        <MessageSquare className="w-4 h-4" />
                      </button>
                      <ChevronRight className="w-4 h-4 text-[#6C6F85]" />
                    </div>
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
              placeholder="Szukaj kontaktu po nazwisku lub numerze"
              className="w-full pl-10 pr-4 py-2 rounded-2xl bg-[#1A1B30]/80 border border-[#272540] text-sm text-white placeholder:text-[#6C6F85] focus:outline-none focus:border-[#7B4DFF]/50"
            />
          </div>

          <div className="flex-1 overflow-y-auto space-y-2 pr-1">
            {filteredContacts.length === 0 ? (
              <div className="py-12 text-center text-[#9499AD] text-sm">
                Nie znaleziono kontaktów w książce telefonicznej.
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

                  <div className="flex items-center gap-1">
                    <button
                      onClick={e => {
                        e.stopPropagation();
                        setSmsPrefill(contact.phone);
                        setActiveTab(1);
                      }}
                      className="p-2 text-purple-300 hover:text-white hover:bg-white/5 rounded-full transition-colors"
                      title="Napisz natychmiastowy SMS"
                    >
                      <MessageSquare className="w-4 h-4" />
                    </button>
                    <ChevronRight className="w-4 h-4 text-[#6C6F85]" />
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      )}

      {/* Quick Share Sheet (Smart-Strzałka & Odwrócony QR) */}
      <QuickShareSheet
        isOpen={isShareSheetOpen}
        onClose={() => setIsShareSheetOpen(false)}
        dialedNumber={number}
        onTriggerDemoCall={(targetNum, durationMs) => {
          triggerSmartArrowCall(targetNum, durationMs);
        }}
        onAppendDemoSms={(targetNum, body, fromSim) => {
          appendDirectSms(targetNum, body, fromSim);
        }}
        onShowToast={(msg) => {
          showNotice(msg);
        }}
      />

      {/* Info Modal */}
      {showInfoModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4">
          <div className="bg-[#131424] border border-[#272540] rounded-3xl p-6 max-w-sm w-full space-y-4 shadow-2xl">
            <h3 className="text-lg font-bold text-white">
              Telefon & SMS 2w1 w CzyToTy?
            </h3>
            <p className="text-xs text-[#9499AD] leading-relaxed">
              Aplikacja łączy dialer, historię połączeń i SMS-y z automatycznym rozpoznawaniem służb ratunkowych (112, 999, 998, 997) oraz zweryfikowaną bazą punktów usługowych (piekarnie, apteki, przychodnie).
            </p>
            <div className="p-3 bg-[#1A1B30] rounded-xl border border-[#272540] text-xs text-white/90 space-y-1">
              <p className="font-semibold text-purple-300">
                • Tryb Seniora:
              </p>
              <p className="text-[#9499AD]">
                Powiększone cyfry i przyciski akcji, natychmiastowy SMS pod kciukiem.
              </p>
              <p className="font-semibold text-red-400 pt-1">
                • Numery ratunkowe:
              </p>
              <p className="text-[#9499AD]">
                Czerwone podświetlenie cyfr i jednoznaczne wskazanie oddziałów szybkiego reagowania.
              </p>
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
