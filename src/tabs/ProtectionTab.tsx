import React, { useState } from 'react';
import {
  Shield,
  ShieldAlert,
  ShieldCheck,
  Bot,
  Globe,
  Coins,
  AlertTriangle,
  Trash2,
  Play,
  PhoneCall,
  PhoneOff,
  Sliders,
  CheckCircle2,
  XCircle,
  Info,
  Lock,
  Layers,
  Radio,
  Sparkles,
  ArrowRight
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { WANGIRI_HIGH_RISK_DESTINATIONS } from '../utils/wangiri';

export function ProtectionTab() {
  const {
    protectionEnabled,
    setProtectionEnabled,
    notes,
    screeningEvents,
    clearScreeningEvents,
    simulateIncomingCall,
    wangiriSettings,
    updateWangiriSettings,
    simulateWangiriPingCall,
    robocallSettings,
    updateRobocallSettings,
    robocallCampaigns,
    simulateRobocallBurst,
    startCall,
    saveLocalNote
  } = useApp();

  // Active view: 'wangiri' | 'robocall' | 'my_blocks'
  const [activeSubTab, setActiveSubTab] = useState<'wangiri' | 'robocall' | 'my_blocks'>('wangiri');
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
    <div className="flex flex-col h-full space-y-4 overflow-y-auto pr-1 pb-24 text-white">
      {/* Header */}
      <div>
        <div className="flex items-center gap-2">
          <Shield className="w-7 h-7 text-[#A875FF]" />
          <h2 className="text-2xl sm:text-3xl font-extrabold tracking-tight">
            Centrum Ochrony
          </h2>
          <span className="px-2 py-0.5 rounded-full bg-[#7B4DFF]/20 text-[#A875FF] border border-[#7B4DFF]/30 text-xs font-bold">
            Telecom Pro
          </span>
        </div>
        <p className="text-xs sm:text-sm text-[#9499AD] mt-1">
          Natywne reguły Android CallScreeningService: Wangiri Shield, analiza dialerów predykcyjnych i własna lista blokad.
        </p>
      </div>

      {/* Segmented Controller (3 Tabs) */}
      <div className="bg-[#1A1B30]/75 p-1 rounded-2xl flex border border-[#272540] shadow-sm">
        <button
          onClick={() => setActiveSubTab('wangiri')}
          className={`flex-1 py-2 px-1 rounded-xl text-xs sm:text-sm font-semibold transition-all flex items-center justify-center gap-1.5 ${
            activeSubTab === 'wangiri'
              ? 'bg-[#22233C] text-white shadow-md border border-red-500/30'
              : 'text-[#9499AD] hover:text-white'
          }`}
        >
          <Globe className="w-4 h-4 text-red-400" />
          <span>Tarcza Wangiri</span>
        </button>

        <button
          onClick={() => setActiveSubTab('robocall')}
          className={`flex-1 py-2 px-1 rounded-xl text-xs sm:text-sm font-semibold transition-all flex items-center justify-center gap-1.5 ${
            activeSubTab === 'robocall'
              ? 'bg-[#22233C] text-white shadow-md border border-[#7B4DFF]/40'
              : 'text-[#9499AD] hover:text-white'
          }`}
        >
          <Bot className="w-4 h-4 text-[#A875FF]" />
          <span>Robocall Intelligence</span>
        </button>

        <button
          onClick={() => setActiveSubTab('my_blocks')}
          className={`flex-1 py-2 px-1 rounded-xl text-xs sm:text-sm font-semibold transition-all flex items-center justify-center gap-1.5 ${
            activeSubTab === 'my_blocks'
              ? 'bg-[#22233C] text-white shadow-md border border-emerald-500/30'
              : 'text-[#9499AD] hover:text-white'
          }`}
        >
          <ShieldCheck className="w-4 h-4 text-emerald-400" />
          <span>Moja lista ({blockedCount})</span>
        </button>
      </div>

      {/* ==================================================== */}
      {/* SUBTAB 1: TARCZA WANGIRI (ONE-RING & DROP)            */}
      {/* ==================================================== */}
      {activeSubTab === 'wangiri' && (
        <div className="space-y-4 animate-in fade-in duration-200">
          {/* Main Status & KPI Cards */}
          <div className="p-4 rounded-3xl bg-gradient-to-br from-[#231218] via-[#171322] to-[#12111D] border border-red-500/40 shadow-xl space-y-4">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-2xl bg-red-600/25 border border-red-500/40 text-red-400">
                  <ShieldAlert className="w-6 h-6 animate-pulse" />
                </div>
                <div>
                  <h3 className="text-base font-extrabold text-white flex items-center gap-2">
                    Tarcza Oszustw Wangiri
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-red-600/30 text-red-200 border border-red-500/40 font-bold uppercase">
                      One-Ring & Drop
                    </span>
                  </h3>
                  <p className="text-xs text-[#9499AD]">
                    Blokowanie naciągania na kosztowne oddzwanianie na numery zagraniczne i satelitarne.
                  </p>
                </div>
              </div>

              {/* Master toggle */}
              <input
                type="checkbox"
                checked={wangiriSettings.shieldEnabled}
                onChange={e => updateWangiriSettings({ shieldEnabled: e.target.checked })}
                className="w-5 h-5 accent-red-500 rounded cursor-pointer mt-1"
                title="Włącz/Wyłącz Tarczę Wangiri"
              />
            </div>

            {/* Metrics */}
            <div className="grid grid-cols-3 gap-2 pt-1 border-t border-white/10">
              <div className="p-2.5 rounded-2xl bg-black/40 border border-white/5 text-center">
                <p className="text-lg font-bold font-mono text-red-400">
                  {wangiriSettings.interceptedAttemptsCount || 1}
                </p>
                <p className="text-[10px] text-[#9499AD]">Przechwycone próby</p>
              </div>
              <div className="p-2.5 rounded-2xl bg-black/40 border border-white/5 text-center">
                <p className="text-lg font-bold font-mono text-[#FFD089]">19 stref</p>
                <p className="text-[10px] text-[#9499AD]">Baza ITU High-Risk</p>
              </div>
              <div className="p-2.5 rounded-2xl bg-black/40 border border-white/5 text-center">
                <p className="text-lg font-bold font-mono text-emerald-400">~24 zł/min</p>
                <p className="text-[10px] text-[#9499AD]">Średnia stawka oszustw</p>
              </div>
            </div>

            {/* Key Switches */}
            <div className="space-y-2.5 pt-2 border-t border-white/10 text-xs">
              <label className="flex items-center justify-between cursor-pointer">
                <div>
                  <span className="font-semibold text-white block">
                    Przechwytuj próby oddzwonienia (Call-back Interceptor)
                  </span>
                  <span className="text-[11px] text-[#9499AD]">
                    Zatrzymuje połączenie wychodzące przed zestawieniem i wyświetla kalkulację kosztów.
                  </span>
                </div>
                <input
                  type="checkbox"
                  checked={wangiriSettings.interceptOutgoingCall}
                  onChange={e => updateWangiriSettings({ interceptOutgoingCall: e.target.checked })}
                  className="w-4 h-4 accent-red-500 rounded ml-2"
                />
              </label>

              <label className="flex items-center justify-between cursor-pointer">
                <div>
                  <span className="font-semibold text-white block">
                    Automatyczna blokada 1-sekundowych pingów
                  </span>
                  <span className="text-[11px] text-[#9499AD]">
                    Odrzuca natychmiast połączenia z ryzykownych destynacji trwające tylko 1 sygnał.
                  </span>
                </div>
                <input
                  type="checkbox"
                  checked={wangiriSettings.autoBlockHighRiskPing}
                  onChange={e => updateWangiriSettings({ autoBlockHighRiskPing: e.target.checked })}
                  className="w-4 h-4 accent-red-500 rounded ml-2"
                />
              </label>

              <label className="flex items-center justify-between cursor-pointer">
                <div>
                  <span className="font-semibold text-white block">
                    Wykrywaj kamuflaż optyczny (+22x vs Warszawa)
                  </span>
                  <span className="text-[11px] text-[#9499AD]">
                    Identyfikuje prefiksy z Afryki Zachodniej imitujące stacjonarne numery ze stolicy (022).
                  </span>
                </div>
                <input
                  type="checkbox"
                  checked={wangiriSettings.alertOnCamouflage}
                  onChange={e => updateWangiriSettings({ alertOnCamouflage: e.target.checked })}
                  className="w-4 h-4 accent-red-500 rounded ml-2"
                />
              </label>
            </div>
          </div>

          {/* Interactive Wangiri Simulator */}
          <div className="p-4 rounded-3xl bg-[#141424] border border-[#7B4DFF]/35 space-y-3 shadow-lg">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-purple-300 flex items-center gap-1.5">
                <Play className="w-4 h-4 text-[#8BE0BB]" />
                Symulator Ataków Wangiri (One-Ring Tester)
              </h3>
              <span className="text-[10px] bg-[#7B4DFF]/25 text-purple-200 px-2 py-0.5 rounded-full font-bold">
                Bezpieczny test
              </span>
            </div>
            <p className="text-xs text-[#9499AD] leading-relaxed">
              Zasymuluj przychodzący 1-sekundowy ping z zagranicy. Zobaczysz jak trafia do historii z czerwoną tarczą, a próba oddzwonienia odpala barierę ochronną z kalkulacją kosztów.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
              <button
                type="button"
                onClick={() => simulateWangiriPingCall('+247555891')}
                className="py-2.5 px-3 rounded-2xl bg-[#1E192D] hover:bg-[#28213D] border border-red-500/30 text-xs font-bold text-red-200 flex items-center justify-center gap-2 transition-all active:scale-95 text-left"
              >
                <span>🇦🇨</span>
                <div>
                  <span>Wyspa Wniebowstąpienia</span>
                  <span className="block text-[10px] text-red-300/80 font-normal">
                    +247 555 ••• (~32 zł/min)
                  </span>
                </div>
              </button>

              <button
                type="button"
                onClick={() => simulateWangiriPingCall('+223791244')}
                className="py-2.5 px-3 rounded-2xl bg-[#1E192D] hover:bg-[#28213D] border border-amber-500/30 text-xs font-bold text-amber-200 flex items-center justify-center gap-2 transition-all active:scale-95 text-left"
              >
                <span>🇲🇱</span>
                <div>
                  <span>Mali (Kamuflaż Warszawy 22)</span>
                  <span className="block text-[10px] text-amber-300/80 font-normal">
                    +223 791 ••• (~21 zł/min)
                  </span>
                </div>
              </button>
            </div>
          </div>

          {/* Database of High-Risk Destinations & Tariff Rates */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Coins className="w-4 h-4 text-[#FFD089]" />
                Kierunki Podwyższonego Ryzyka i Taryfikacja ITU
              </h3>
              <span className="text-xs text-[#9499AD]">
                {WANGIRI_HIGH_RISK_DESTINATIONS.length} monitorowanych stref
              </span>
            </div>

            <div className="space-y-2 max-h-72 overflow-y-auto pr-1">
              {WANGIRI_HIGH_RISK_DESTINATIONS.slice(0, 10).map(dest => (
                <div
                  key={dest.prefix}
                  className="p-3 rounded-2xl bg-[#131424] border border-[#272540] flex items-center justify-between text-xs"
                >
                  <div className="flex items-center gap-3">
                    <span className="text-xl">{dest.flag}</span>
                    <div>
                      <div className="flex items-center gap-1.5">
                        <span className="font-bold text-white">{dest.country}</span>
                        <span className="font-mono text-[11px] px-1.5 py-0.2 bg-black/40 rounded text-purple-200 border border-white/5 font-semibold">
                          {dest.prefix}
                        </span>
                      </div>
                      <p className="text-[11px] text-[#9499AD] mt-0.5">{dest.notes}</p>
                    </div>
                  </div>

                  <div className="text-right shrink-0 ml-3">
                    <span className="font-mono font-bold text-red-300 block">
                      ~{dest.costRange}
                    </span>
                    <span className="text-[10px] text-[#6C6F85]">szacowany koszt</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ==================================================== */}
      {/* SUBTAB 2: ROBOCALL INTELLIGENCE & CALL SCREENING     */}
      {/* ==================================================== */}
      {activeSubTab === 'robocall' && (
        <div className="space-y-4 animate-in fade-in duration-200">
          {/* Main Status & Configuration */}
          <div className="p-4 rounded-3xl bg-gradient-to-br from-[#1B1633] via-[#141224] to-[#0D0B14] border border-[#7B4DFF]/40 shadow-xl space-y-4">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3">
                <div className="p-2.5 rounded-2xl bg-[#7B4DFF]/25 border border-[#7B4DFF]/50 text-purple-300">
                  <Bot className="w-6 h-6 animate-pulse" />
                </div>
                <div>
                  <h3 className="text-base font-extrabold text-white flex items-center gap-2">
                    Robocall Intelligence
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-[#7B4DFF]/30 text-purple-200 border border-[#7B4DFF]/40 font-bold uppercase">
                      Predictive Dialer AI
                    </span>
                  </h3>
                  <p className="text-xs text-[#9499AD]">
                    Wykrywanie rotacji pni DDI/DID, botów AMD (cichy telefon) i kampanii telemarketingowych.
                  </p>
                </div>
              </div>

              {/* Master toggle */}
              <input
                type="checkbox"
                checked={robocallSettings.enabled}
                onChange={e => updateRobocallSettings({ enabled: e.target.checked })}
                className="w-5 h-5 accent-[#7B4DFF] rounded cursor-pointer mt-1"
                title="Włącz/Wyłącz Robocall Intelligence"
              />
            </div>

            {/* Telecom Action Selector (CallScreeningService Mode) */}
            <div className="p-3 rounded-2xl bg-black/40 border border-white/5 space-y-2">
              <span className="text-xs font-bold text-purple-200 block">
                Działanie Android CallScreeningService przy wykryciu bota:
              </span>
              <div className="grid grid-cols-3 gap-1.5">
                {[
                  {
                    key: 'SILENCE',
                    label: 'Ciche wyciszenie',
                    desc: 'setSilenceCall(true)'
                  },
                  {
                    key: 'REJECT',
                    label: 'Twarde odrzucenie',
                    desc: 'setRejectCall(true)'
                  },
                  {
                    key: 'WARN',
                    label: 'Ostrzeżenie na żywo',
                    desc: 'Heads-up alert'
                  }
                ].map(opt => (
                  <button
                    key={opt.key}
                    type="button"
                    onClick={() => updateRobocallSettings({ action: opt.key as any })}
                    className={`p-2 rounded-xl text-center transition-all border ${
                      robocallSettings.action === opt.key
                        ? 'bg-[#7B4DFF]/30 border-[#7B4DFF] text-white shadow-md'
                        : 'bg-white/5 border-transparent text-[#9499AD] hover:text-white'
                    }`}
                  >
                    <span className="text-xs font-bold block">{opt.label}</span>
                    <span className="text-[9px] font-mono opacity-70 block">{opt.desc}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Sensitivity Slider */}
            <div className="space-y-1.5 pt-1">
              <div className="flex items-center justify-between text-xs">
                <span className="text-[#9499AD] font-semibold">
                  Próg pewności bota do wyzwolenia blokady:
                </span>
                <span className="font-mono font-bold text-purple-300">
                  {robocallSettings.minScoreToBlock}%
                </span>
              </div>
              <input
                type="range"
                min="50"
                max="90"
                step="5"
                value={robocallSettings.minScoreToBlock}
                onChange={e => updateRobocallSettings({ minScoreToBlock: Number(e.target.value) })}
                className="w-full accent-[#7B4DFF] cursor-pointer"
              />
            </div>

            {/* Heuristics toggles */}
            <div className="space-y-2 pt-2 border-t border-white/10 text-xs">
              <label className="flex items-center justify-between cursor-pointer">
                <div>
                  <span className="font-semibold text-white block">
                    Filtr ciszy detektora mowy (AMD Silent Call)
                  </span>
                  <span className="text-[11px] text-[#9499AD]">
                    Wykrywa natychmiastowe rozłączenie bota po 1.5–3s (gdy żaden konsultant nie jest wolny).
                  </span>
                </div>
                <input
                  type="checkbox"
                  checked={robocallSettings.amdSilenceFilter}
                  onChange={e => updateRobocallSettings({ amdSilenceFilter: e.target.checked })}
                  className="w-4 h-4 accent-[#7B4DFF] rounded ml-2"
                />
              </label>

              <label className="flex items-center justify-between cursor-pointer">
                <div>
                  <span className="font-semibold text-white block">
                    Klastrowanie pni DDI / DID operatorów VoIP
                  </span>
                  <span className="text-[11px] text-[#9499AD]">
                    Automatycznie grupuje połączenia ze wspólnym pniem centrali (+48 22 166 •••).
                  </span>
                </div>
                <input
                  type="checkbox"
                  checked={robocallSettings.autoClusterDdi}
                  onChange={e => updateRobocallSettings({ autoClusterDdi: e.target.checked })}
                  className="w-4 h-4 accent-[#7B4DFF] rounded ml-2"
                />
              </label>
            </div>
          </div>

          {/* Interactive Robocall Simulator */}
          <div className="p-4 rounded-3xl bg-[#141424] border border-[#7B4DFF]/35 space-y-3 shadow-lg">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-purple-300 flex items-center gap-1.5">
                <Play className="w-4 h-4 text-[#8BE0BB]" />
                Symulator Kampanii Robocall (Burst Dialing)
              </h3>
              <span className="text-[10px] bg-[#7B4DFF]/25 text-purple-200 px-2 py-0.5 rounded-full font-bold">
                Test DDI
              </span>
            </div>
            <p className="text-xs text-[#9499AD] leading-relaxed">
              Zasymuluj sekwencję 3 szybkich połączeń z rotacją końcówek ze zidentyfikowanego pnia VoIP (+48 22 166 •••). Zobaczysz, jak algorytm wykrywa kampanię predykcyjną.
            </p>

            <button
              type="button"
              onClick={simulateRobocallBurst}
              className="w-full py-2.5 px-4 rounded-2xl bg-[#201B36] hover:bg-[#2B244A] border border-[#7B4DFF]/40 text-purple-200 font-bold text-xs flex items-center justify-center gap-2 transition-all active:scale-98 shadow-md"
            >
              <Sparkles className="w-4 h-4 text-[#FFD089]" />
              <span>Zasymuluj burst predykcyjny (3 numery DDI)</span>
            </button>
          </div>

          {/* Identified Call Center Campaigns List */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Layers className="w-4 h-4 text-[#A875FF]" />
                Zidentyfikowane Kampanie Call Center ({robocallCampaigns.length})
              </h3>
              <span className="text-xs text-[#9499AD]">Agregacja w locie</span>
            </div>

            <div className="space-y-2.5">
              {robocallCampaigns.map(camp => (
                <div
                  key={camp.id}
                  className="p-3.5 rounded-2xl bg-[#131424] border border-[#272540] space-y-2.5"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-extrabold text-white">{camp.name}</span>
                        <span className="px-1.5 py-0.5 rounded-md bg-[#7B4DFF]/20 text-[#A875FF] text-[10px] font-mono font-bold border border-[#7B4DFF]/30">
                          {camp.riskScore}% pewności bota
                        </span>
                      </div>
                      <p className="text-[11px] text-[#9499AD] mt-0.5">
                        {camp.detectedDialer} · Pień: <span className="text-white font-mono">{camp.trunkPrefix}</span>
                      </p>
                    </div>

                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-red-600/20 text-red-300 border border-red-500/30">
                      ZABLOKOWANA
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-2 text-[11px] pt-1 border-t border-white/5">
                    <div>
                      <span className="text-[#9499AD]">Wskaźnik porzucenia (AMD):</span>
                      <span className="font-mono font-bold text-amber-300 ml-1.5">
                        {camp.abandonmentRate}%
                      </span>
                    </div>
                    <div>
                      <span className="text-[#9499AD]">Algorytm bota:</span>
                      <span className="font-semibold text-purple-200 ml-1.5 truncate block">
                        {camp.botType}
                      </span>
                    </div>
                  </div>

                  <div className="flex items-center justify-between pt-1">
                    <span className="text-[10px] text-[#6C6F85]">
                      Próbki: {camp.sampleNumbers.join(', ')}
                    </span>
                    <button
                      type="button"
                      onClick={() => {
                        camp.sampleNumbers.forEach(n => {
                          saveLocalNote({
                            phone: n,
                            label: camp.name,
                            note: `Zablokowano pień kampanii robocall ${camp.trunkPrefix}`,
                            blocked: true
                          });
                        });
                      }}
                      className="text-[11px] text-[#A875FF] hover:underline font-bold"
                    >
                      Zablokuj numery pnia
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* ==================================================== */}
      {/* SUBTAB 3: MOJA LISTA BLOKAD & REJESTR                */}
      {/* ==================================================== */}
      {activeSubTab === 'my_blocks' && (
        <div className="space-y-4 animate-in fade-in duration-200">
          {/* Status Card */}
          <div className="p-4 rounded-3xl bg-[#131424] border border-[#272540] space-y-3 shadow-md">
            <div className="flex items-center gap-3">
              {protectionEnabled ? (
                <ShieldCheck className="w-6 h-6 text-[#22C55E]" />
              ) : (
                <ShieldAlert className="w-6 h-6 text-[#FFD089]" />
              )}
              <div>
                <h3 className="text-sm font-bold text-white">
                  {protectionEnabled
                    ? 'Blokowanie Twojej listy jest aktywne'
                    : 'Blokowanie jest nieaktywne'}
                </h3>
                <p className="text-xs text-[#9499AD] mt-0.5 leading-relaxed">
                  Lokalne filtrowanie połączeń przychodzących na podstawie Twojej własnej listy numerów.
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
          <div className="p-4 rounded-3xl bg-[#1A1B30]/70 border border-[#272540] space-y-3">
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
              Zdarzenia odebrane przez tę aplikację, maksymalnie 200. Android może pomijać numery z kontaktów i połączenia o ograniczonej prezentacji numeru.
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
                            ? 'Odrzucono według reguł CallScreening'
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
        </div>
      )}
    </div>
  );
}
