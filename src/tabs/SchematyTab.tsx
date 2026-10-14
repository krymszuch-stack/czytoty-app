import React, { useState } from 'react';
import {
  Layers,
  ShieldAlert,
  ShieldCheck,
  Clock,
  Play,
  RotateCcw,
  Sliders,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Info,
  Trash2,
  Lock,
  Unlock,
  PhoneCall
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { CallCluster, ClusterStatus } from '../types';

export function SchematyTab() {
  const {
    clusters,
    clusterSettings,
    updateClusterSettings,
    updateClusterStatus,
    deleteCluster,
    simulateIncomingCall,
    contacts,
    protectionEnabled
  } = useApp();

  const [simPrefix, setSimPrefix] = useState('901234');
  const [simSuffix, setSimSuffix] = useState('789');
  const [activeStep, setActiveStep] = useState<number>(0);
  const [simLog, setSimLog] = useState<string[]>([]);

  const formatTimeAgo = (timestamp: number) => {
    const diffMin = Math.max(1, Math.floor((Date.now() - timestamp) / 60000));
    if (diffMin < 60) return `${diffMin} min temu`;
    const diffHours = Math.floor(diffMin / 60);
    return `${diffHours} godz. temu`;
  };

  const handleRunSimStep = (suffix: string, stepNum: number, delayLabel: string) => {
    const fullNumber = `+48${simPrefix}${suffix}`;
    setActiveStep(stepNum);

    const logEntry = `Próba #${stepNum} (${delayLabel}): połączenie z ${fullNumber}`;
    setSimLog(prev => [logEntry, ...prev]);

    // Triggers incoming call flow which invokes evaluateCallCluster
    simulateIncomingCall(fullNumber);
  };

  const handleResetSimulator = () => {
    setActiveStep(0);
    setSimLog([]);
  };

  const quarantinedCount = clusters.filter(c => c.status === 'ACTIVE_QUARANTINE').length;
  const blockedPoolsCount = clusters.filter(c => c.status === 'BLOCKED_PERMANENT').length;

  return (
    <div className="flex flex-col h-full space-y-6 overflow-y-auto pr-1 pb-20">
      {/* Top Header */}
      <div>
        <div className="flex items-center gap-2.5">
          <Layers className="w-7 h-7 text-[#A875FF]" />
          <h2 className="text-3xl font-extrabold text-white tracking-tight">
            Schematy połączeń
          </h2>
        </div>
        <p className="text-sm text-[#9499AD] mt-1.5 leading-relaxed">
          Wykrywanie rotacyjnych central SIP i systemów predictive dialer (np. windykacji) dzwoniących z tego samego prefiksu z różnymi końcówkami w oknie do 60 min.
        </p>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-3 gap-2.5">
        <div className="p-3.5 rounded-2xl bg-[#131424] border border-[#272540] text-center">
          <p className="text-xl font-bold font-mono text-[#FFD089]">{quarantinedCount}</p>
          <p className="text-[11px] text-[#9499AD] mt-0.5">W kwarantannie</p>
        </div>
        <div className="p-3.5 rounded-2xl bg-[#131424] border border-[#272540] text-center">
          <p className="text-xl font-bold font-mono text-[#EF5350]">{blockedPoolsCount}</p>
          <p className="text-[11px] text-[#9499AD] mt-0.5">Trwale zablokowane</p>
        </div>
        <div className="p-3.5 rounded-2xl bg-[#131424] border border-[#272540] text-center">
          <p className="text-xl font-bold font-mono text-[#22C55E]">100%</p>
          <p className="text-[11px] text-[#9499AD] mt-0.5">Ochrona kontaktów</p>
        </div>
      </div>

      {/* Interactive Simulator Card */}
      <div className="p-5 rounded-3xl bg-[#131424] border border-[#7B4DFF]/40 space-y-4 shadow-xl">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Play className="w-5 h-5 text-[#8BE0BB]" />
            <h3 className="text-base font-bold text-white">
              Symulator rotacji puli (Scenariusz 901...)
            </h3>
          </div>
          <button
            onClick={handleResetSimulator}
            className="text-xs text-[#9499AD] hover:text-white flex items-center gap-1"
            title="Resetuj symulator"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Reset</span>
          </button>
        </div>

        <p className="text-xs text-[#9499AD] leading-relaxed">
          Przetestuj na żywo przypadek opisany w pytaniu: centrala dzwoni z numeru o prefiksie <strong>901 234</strong>, a 20 minut później z inną końcówką.
        </p>

        {/* Step Buttons */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 pt-1">
          <button
            onClick={() => handleRunSimStep('101', 1, 'Start')}
            className={`py-3 px-3 rounded-2xl text-xs font-semibold flex flex-col items-center justify-center gap-1 border transition-all ${
              activeStep >= 1
                ? 'bg-[#22233C] border-[#7B4DFF] text-white shadow-md'
                : 'bg-[#1A1B30] border-[#272540] text-[#9499AD] hover:text-white'
            }`}
          >
            <span className="text-[10px] uppercase text-[#A875FF]">Krok 1</span>
            <span className="font-mono text-xs">+48 901 234 101</span>
            <span className="text-[10px] text-[#6C6F85]">Pierwsze połączenie</span>
          </button>

          <button
            onClick={() => handleRunSimStep('555', 2, '+20 min później')}
            className={`py-3 px-3 rounded-2xl text-xs font-semibold flex flex-col items-center justify-center gap-1 border transition-all ${
              activeStep >= 2
                ? 'bg-[#22233C] border-[#FFD089] text-white shadow-md'
                : 'bg-[#1A1B30] border-[#272540] text-[#9499AD] hover:text-white'
            }`}
          >
            <span className="text-[10px] uppercase text-[#FFD089]">Krok 2</span>
            <span className="font-mono text-xs">+48 901 234 555</span>
            <span className="text-[10px] text-[#FFD089]">Wykrycie klastra!</span>
          </button>

          <button
            onClick={() => handleRunSimStep('888', 3, '+35 min')}
            className={`py-3 px-3 rounded-2xl text-xs font-semibold flex flex-col items-center justify-center gap-1 border transition-all ${
              activeStep >= 3
                ? 'bg-[#281419] border-[#EF5350] text-white shadow-md'
                : 'bg-[#1A1B30] border-[#272540] text-[#9499AD] hover:text-white'
            }`}
          >
            <span className="text-[10px] uppercase text-[#EF5350]">Krok 3</span>
            <span className="font-mono text-xs">+48 901 234 888</span>
            <span className="text-[10px] text-[#FF8596]">Blokada klastrowa</span>
          </button>
        </div>

        {/* Live Simulator Logs */}
        {simLog.length > 0 && (
          <div className="p-3 rounded-2xl bg-black/40 border border-[#272540] space-y-1 font-mono text-[11px] text-purple-200">
            {simLog.map((log, i) => (
              <p key={i} className="flex items-center gap-2">
                <span className="text-[#8BE0BB]">✓</span>
                <span>{log}</span>
              </p>
            ))}
          </div>
        )}
      </div>

      {/* Detected Clusters List */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-bold text-white">
            Wykryte schematy i pule central ({clusters.length})
          </h3>
        </div>

        {clusters.length === 0 ? (
          <p className="text-xs text-[#9499AD] py-8 text-center bg-[#131424] rounded-2xl border border-[#272540]">
            Brak wykrytych klastrów. Schemat zostanie utworzony automatycznie po odnotowaniu $\ge$ 2 połączeń z tej samej puli w oknie {clusterSettings.windowMinutes} min.
          </p>
        ) : (
          <div className="space-y-3">
            {clusters.map(cluster => {
              const isQuarantine = cluster.status === 'ACTIVE_QUARANTINE';
              const isBlocked = cluster.status === 'BLOCKED_PERMANENT';
              const isAllowed = cluster.status === 'ALLOWED_WHITELIST';

              return (
                <div
                  key={cluster.id}
                  className={`p-4 rounded-3xl border space-y-3 shadow-md transition-all ${
                    isBlocked
                      ? 'bg-[#281419] border-[#EF5350]/60'
                      : isQuarantine
                      ? 'bg-[#1F1733] border-[#7B4DFF]/40'
                      : 'bg-[#132B1E] border-[#22C55E]/40'
                  }`}
                >
                  {/* Top line: Mask & Badge */}
                  <div className="flex items-start justify-between">
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="font-mono text-base font-extrabold text-white tracking-wide">
                          {cluster.mask}
                        </span>
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-white/10 text-white/90">
                          {cluster.callCount} próby
                        </span>
                      </div>
                      <p className="text-xs text-[#9499AD] mt-1 leading-relaxed">
                        {cluster.reason}
                      </p>
                    </div>

                    <span
                      className={`text-[11px] font-semibold px-2.5 py-1 rounded-full border ${
                        isBlocked
                          ? 'bg-red-950/70 text-[#FF8596] border-red-500/40'
                          : isQuarantine
                          ? 'bg-purple-950/70 text-[#D4B8FF] border-purple-500/40'
                          : 'bg-emerald-950/70 text-[#8BE0BB] border-emerald-500/40'
                      }`}
                    >
                      {isBlocked
                        ? 'Trwale zablokowana'
                        : isQuarantine
                        ? 'Aktywna kwarantanna'
                        : 'Zaufana pula'}
                    </span>
                  </div>

                  {/* Numbers breakdown in pool */}
                  <div className="space-y-1.5 pt-1 border-t border-white/10">
                    <span className="text-[11px] font-semibold text-[#9499AD] uppercase tracking-wider block">
                      Zarejestrowane końcówki z tej centrali ({cluster.numbers.length})
                    </span>
                    <div className="flex flex-wrap gap-1.5">
                      {cluster.numbers.map((num, idx) => (
                        <span
                          key={idx}
                          className="font-mono text-xs py-1 px-2.5 rounded-xl bg-black/40 border border-white/10 text-white/95"
                        >
                          {num}
                        </span>
                      ))}
                    </div>
                  </div>

                  {/* Timestamps */}
                  <div className="flex items-center justify-between text-[11px] text-[#9499AD]">
                    <span className="flex items-center gap-1">
                      <Clock className="w-3.5 h-3.5" />
                      <span>Ostatnia próba: {formatTimeAgo(cluster.lastSeen)}</span>
                    </span>
                    <span className="text-[10px] text-[#6C6F85]">
                      Wykryto: {new Date(cluster.firstSeen).toLocaleDateString('pl-PL')}
                    </span>
                  </div>

                  {/* Action Controls */}
                  <div className="flex items-center justify-between pt-2 border-t border-white/10 text-xs">
                    <div className="flex items-center gap-2">
                      {!isBlocked && (
                        <button
                          onClick={() => updateClusterStatus(cluster.id, 'BLOCKED_PERMANENT')}
                          className="py-1.5 px-3 rounded-xl bg-[#281419] hover:bg-[#3d1821] text-[#FF8596] border border-[#EF5350]/30 font-medium flex items-center gap-1 transition-all"
                        >
                          <Lock className="w-3 h-3" />
                          <span>Zablokuj całą pulę</span>
                        </button>
                      )}

                      {!isAllowed && (
                        <button
                          onClick={() => updateClusterStatus(cluster.id, 'ALLOWED_WHITELIST')}
                          className="py-1.5 px-3 rounded-xl bg-[#132B1E] hover:bg-[#1a3827] text-[#8BE0BB] border border-[#22C55E]/30 font-medium flex items-center gap-1 transition-all"
                        >
                          <Unlock className="w-3 h-3" />
                          <span>Zezwól na pulę</span>
                        </button>
                      )}

                      {isAllowed && (
                        <button
                          onClick={() => updateClusterStatus(cluster.id, 'ACTIVE_QUARANTINE')}
                          className="py-1.5 px-3 rounded-xl bg-[#1F1733] text-[#D4B8FF] border border-[#7B4DFF]/30 font-medium"
                        >
                          Przywróć kwarantannę
                        </button>
                      )}
                    </div>

                    <button
                      onClick={() => deleteCluster(cluster.id)}
                      className="p-1.5 text-[#9499AD] hover:text-[#EF5350] transition-colors"
                      title="Usuń ten schemat"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Settings & Algorithm Configuration */}
      <div className="p-5 rounded-3xl bg-[#131424] border border-[#272540] space-y-4 shadow-xl">
        <div className="flex items-center gap-2">
          <Sliders className="w-4 h-4 text-[#A875FF]" />
          <h3 className="text-sm font-bold text-white">
            Konfiguracja algorytmu detekcji
          </h3>
        </div>

        <div className="space-y-4 text-xs">
          {/* Prefix digits selector */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between text-white/90">
              <span>Długość analizowanego prefiksu</span>
              <span className="font-mono font-bold text-[#A875FF]">
                {clusterSettings.prefixDigits} cyfr (np. {clusterSettings.prefixDigits === 6 ? '901 234 xxx' : '901 234 5xx'})
              </span>
            </div>
            <div className="flex gap-2">
              {[6, 7].map(len => (
                <button
                  key={len}
                  onClick={() => updateClusterSettings({ ...clusterSettings, prefixDigits: len })}
                  className={`flex-1 py-2 rounded-xl font-medium transition-all ${
                    clusterSettings.prefixDigits === len
                      ? 'bg-[#7B4DFF] text-white shadow-sm'
                      : 'bg-[#1A1B30] text-[#9499AD] hover:text-white'
                  }`}
                >
                  {len} cyfr ({len === 6 ? 'szersza pula' : 'dokładniejsza pula'})
                </button>
              ))}
            </div>
          </div>

          {/* Time window selector */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between text-white/90">
              <span>Okno czasowe korelacji</span>
              <span className="font-mono font-bold text-[#A875FF]">
                {clusterSettings.windowMinutes} minut
              </span>
            </div>
            <div className="flex gap-2">
              {[15, 30, 60].map(mins => (
                <button
                  key={mins}
                  onClick={() => updateClusterSettings({ ...clusterSettings, windowMinutes: mins })}
                  className={`flex-1 py-2 rounded-xl font-medium transition-all ${
                    clusterSettings.windowMinutes === mins
                      ? 'bg-[#7B4DFF] text-white shadow-sm'
                      : 'bg-[#1A1B30] text-[#9499AD] hover:text-white'
                  }`}
                >
                  {mins} min
                </button>
              ))}
            </div>
          </div>

          {/* Auto-block toggle */}
          <div className="flex items-center justify-between p-3 rounded-2xl bg-[#1A1B30] border border-[#272540]">
            <div>
              <p className="font-semibold text-white">Automatyczna kwarantanna puli</p>
              <p className="text-[11px] text-[#9499AD] mt-0.5">
                Odrzuca kolejne próby z wykrytej centrali bez dzwonienia
              </p>
            </div>
            <input
              type="checkbox"
              checked={clusterSettings.autoBlockPool}
              onChange={e =>
                updateClusterSettings({ ...clusterSettings, autoBlockPool: e.target.checked })
              }
              className="w-5 h-5 accent-[#7B4DFF] rounded"
            />
          </div>
        </div>
      </div>

      {/* Info Card */}
      <div className="p-4 rounded-2xl bg-[#131424] border border-[#272540] space-y-1.5">
        <div className="flex items-center gap-2 text-white font-bold text-xs">
          <Info className="w-4 h-4 text-[#A875FF]" />
          <span>Dlaczego nie ma fałszywych blokad dla znajomych?</span>
        </div>
        <p className="text-xs text-[#9499AD] leading-relaxed">
          System stosuje regułę <strong>Whitelist-First</strong>: żaden numer zapisany w Twojej książce kontaktów nie jest analizowany pod kątem klastrowania. Pozwala to na agresywne wykrywanie central windykacyjnych bez ryzyka zablokowania bliskich.
        </p>
      </div>
    </div>
  );
}
