import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  useMemo,
  ReactNode
} from 'react';
import {
  Appearance,
  BusinessEntity,
  CallCluster,
  CallHistoryEntry,
  CallItem,
  CallSessionState,
  CallState,
  CertSnapshot,
  CloudNote,
  ClusterMatchResult,
  ClusterSettings,
  ClusterStatus,
  DeviceContact,
  DeviceSms,
  EmergencyService,
  MessageAnalysis,
  PhoneNote,
  RobocallAnalysis,
  RobocallCampaign,
  RobocallSettings,
  ScreeningEvent,
  SmsDraft,
  SmsSend,
  UserBusinessCard,
  WangiriRiskInfo,
  WangiriSettings
} from '../types';
import { LocalStore } from '../services/storage';
import { analyzeMessage, isSnapshotFresh } from '../utils/cert';
import { normalizeDialTarget, parsePhone } from '../utils/phone';
import { evaluateCallForCluster, DEFAULT_CLUSTER_SETTINGS } from '../utils/cluster';
import {
  evaluateWangiriRisk,
  checkOutgoingWangiriRisk,
  DEFAULT_WANGIRI_SETTINGS
} from '../utils/wangiri';
import {
  analyzeRobocall,
  aggregateRobocallCampaigns,
  DEFAULT_ROBOCALL_SETTINGS
} from '../utils/robocall';
import { CloudRepository } from '../services/supabase';
import { getEmergencyInfo, isEmergencyPrefix } from '../utils/emergency';
import { searchDirectoryEntities } from '../services/directory';

interface AppContextType {
  // Navigation
  activeTab: number;
  setActiveTab: (tab: number) => void;
  smsConversationOpen: boolean;
  setSmsConversationOpen: (open: boolean) => void;
  appearanceOpen: boolean;
  setAppearanceOpen: (open: boolean) => void;

  // Global inputs/prefills
  dialPrefill: string | null;
  setDialPrefill: (num: string | null) => void;
  smsPrefill: string;
  setSmsPrefill: (addr: string) => void;
  phoneInput: string;
  setPhoneInput: (num: string) => void;
  messageToCheck: string;
  setMessageToCheck: (text: string) => void;

  // Appearance
  appearance: Appearance;
  updateAppearance: (appearance: Appearance) => void;

  // CERT & Scanner
  certSnapshot: CertSnapshot | null;
  refreshingCert: boolean;
  refreshCert: (force?: boolean) => Promise<void>;
  analysis: MessageAnalysis | null;
  analyze: (text: string) => void;
  clearAnalysis: () => void;

  // Local Notes & Numbers
  notes: PhoneNote[];
  saveLocalNote: (note: PhoneNote) => void;
  deleteLocalNote: (phone: string) => void;
  blockNumber: (phone: string) => void;

  // Protection & Screening Events
  protectionEnabled: boolean;
  setProtectionEnabled: (enabled: boolean) => void;
  screeningEvents: ScreeningEvent[];
  clearScreeningEvents: () => void;
  simulateIncomingCall: (phone: string) => void;

  // Schematy i Klastry Rotacji
  clusters: CallCluster[];
  clusterSettings: ClusterSettings;
  updateClusterSettings: (settings: ClusterSettings) => void;
  updateClusterStatus: (id: string, status: ClusterStatus) => void;
  deleteCluster: (id: string) => void;
  evaluateCallCluster: (phone: string) => ClusterMatchResult;

  // Tarcza Wangiri (One-Ring & Drop)
  wangiriSettings: WangiriSettings;
  updateWangiriSettings: (settings: Partial<WangiriSettings>) => void;
  wangiriModalState: { isOpen: boolean; targetNumber: string; riskInfo: WangiriRiskInfo } | null;
  proceedOutgoingCall: (targetNumber: string) => void;
  blockAndDismissWangiri: (targetNumber: string) => void;
  closeWangiriModal: () => void;
  simulateWangiriPingCall: (targetNumber?: string) => void;

  // Robocall Intelligence & Kampanie Call Center
  robocallSettings: RobocallSettings;
  updateRobocallSettings: (settings: Partial<RobocallSettings>) => void;
  robocallCampaigns: RobocallCampaign[];
  simulateRobocallBurst: () => void;

  // Phone & Calls
  callHistory: CallHistoryEntry[];
  contacts: DeviceContact[];
  callSession: CallSessionState;
  startCall: (number: string) => void;
  triggerSmartArrowCall: (targetNumber: string, durationMs?: number) => void;
  answerCall: (id: string) => void;
  endCall: (id: string) => void;
  holdCall: (id: string) => void;
  toggleMute: () => void;
  setCallMuted: (muted: boolean) => void;
  setAudioChoice: (id: string) => void;
  sendDtmfTone: (id: string, digit: string | null) => void;
  lastEndedCallNumber: string | null;
  setLastEndedCallNumber: (num: string | null) => void;

  // SMS
  smsMessages: DeviceSms[];
  selectedThread: number | null;
  setSelectedThread: (thread: number | null) => void;
  drafts: Record<string, SmsDraft>;
  saveDraft: (key: string, draft: SmsDraft) => void;
  pinnedThreads: number[];
  togglePinThread: (threadId: number) => void;
  sendSms: (address: string, body: string, draftKey?: string) => Promise<string>;
  appendDirectSms: (targetNumber: string, body: string, fromSim?: 'PRIVATE' | 'WORK') => void;
  sends: SmsSend[];
  isSendingSms: boolean;
  markThreadRead: (threadId: number) => void;

  // Cloud / Supabase
  cloudStatus: string;
  cloudUserEmail: string | null;
  cloudNotes: CloudNote[];
  isCloudBusy: boolean;
  checkCloudConnection: () => Promise<void>;
  authenticateCloud: (email: string, pass: string, isRegister: boolean) => Promise<void>;
  signOutCloud: () => Promise<void>;
  loadCloudNotes: () => Promise<void>;
  saveCloudNote: (note: PhoneNote) => Promise<void>;
  deleteCloudNote: (phone: string) => Promise<void>;
  restoreToLocal: (cloudNote: CloudNote) => void;

  // Baza Firm i Usług (Smart Directory)
  directory: BusinessEntity[];
  searchDirectory: (query: string) => BusinessEntity[];
  importDirectoryEntity: (entity: BusinessEntity) => BusinessEntity;
  saveCustomBusinessEntity: (entity: Omit<BusinessEntity, 'id' | 'importedAt'>) => void;

  // Numery Alarmowe i Służby Szybkiego Reagowania
  getEmergencyDetails: (raw: string) => EmergencyService | null;
  isEmergency: (raw: string) => boolean;

  // Karta Użytkownika & Kontakt ICE
  userCard: UserBusinessCard;
  saveUserCard: (card: UserBusinessCard) => void;
  callIceContact: () => void;
  smsIceContact: () => void;

  // Toast / Notice
  notice: string | null;
  showNotice: (msg: string) => void;
  dismissNotice: () => void;
}

const AppContext = createContext<AppContextType | null>(null);

export function AppProvider({ children }: { children: ReactNode }) {
  // Navigation
  const [activeTab, setActiveTab] = useState<number>(0);
  const [smsConversationOpen, setSmsConversationOpen] = useState<boolean>(false);
  const [appearanceOpen, setAppearanceOpen] = useState<boolean>(false);

  // Prefills
  const [dialPrefill, setDialPrefill] = useState<string | null>(null);
  const [smsPrefill, setSmsPrefill] = useState<string>('');
  const [phoneInput, setPhoneInput] = useState<string>('');
  const [messageToCheck, setMessageToCheck] = useState<string>('');

  // Toast notice
  const [notice, setNotice] = useState<string | null>(null);
  const showNotice = useCallback((msg: string) => {
    setNotice(msg);
  }, []);
  const dismissNotice = useCallback(() => {
    setNotice(null);
  }, []);

  // Appearance
  const [appearance, setAppearance] = useState<Appearance>(() => LocalStore.getAppearance());
  const updateAppearance = (newApp: Appearance) => {
    setAppearance(newApp);
    LocalStore.saveAppearance(newApp);
  };

  // Sync dark class on document element
  useEffect(() => {
    const isDark =
      appearance.mode === 'DARK' ||
      (appearance.mode === 'SYSTEM' &&
        window.matchMedia('(prefers-color-scheme: dark)').matches);
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, [appearance.mode]);

  // CERT State
  const [certSnapshot, setCertSnapshot] = useState<CertSnapshot | null>(() =>
    LocalStore.getCertSnapshot()
  );
  const [refreshingCert, setRefreshingCert] = useState<boolean>(false);
  const [analysis, setAnalysis] = useState<MessageAnalysis | null>(null);

  const refreshCert = useCallback(
    async (force: boolean = false) => {
      setRefreshingCert(true);
      try {
        const res = await fetch(`/api/cert?force=${force}`);
        if (!res.ok) throw new Error('Serwer zwrócił błąd');
        const data = await res.json();
        if (data.success) {
          const snapshot: CertSnapshot = {
            fetchedAt: data.fetchedAt,
            domains: data.domains
          };
          setCertSnapshot(snapshot);
          LocalStore.saveCertSnapshot(snapshot);
        } else {
          showNotice('Nie udało się pobrać listy CERT. Zachowano poprzednią kopię.');
        }
      } catch (e: any) {
        showNotice('Nie udało się pobrać listy CERT. Zachowano poprzednią kopię, jeśli była dostępna.');
      } finally {
        setRefreshingCert(false);
      }
    },
    [showNotice]
  );

  useEffect(() => {
    // Initial fetch if needed
    if (!certSnapshot || !isSnapshotFresh(certSnapshot)) {
      refreshCert();
    }
  }, []);

  const analyze = useCallback(
    (text: string) => {
      if (!text.trim()) {
        showNotice('Wklej treść wiadomości lub adres strony.');
        return;
      }
      const result = analyzeMessage(text.slice(0, 20000), certSnapshot);
      setAnalysis(result);
    },
    [certSnapshot, showNotice]
  );

  const clearAnalysis = useCallback(() => {
    setAnalysis(null);
  }, []);

  // Notes & Phone Book
  const [notes, setNotes] = useState<PhoneNote[]>(() => LocalStore.getNotes());
  const saveLocalNote = (note: PhoneNote) => {
    LocalStore.saveNote(note);
    setNotes(LocalStore.getNotes());
    showNotice('Zapisano na tym urządzeniu.');
  };
  const deleteLocalNote = (phone: string) => {
    LocalStore.deleteNote(phone);
    setNotes(LocalStore.getNotes());
    showNotice('Usunięto z urządzenia.');
  };
  const blockNumber = (phone: string) => {
    const existing = notes.find(n => n.phone === phone) || {
      phone,
      label: '',
      note: '',
      blocked: false
    };
    saveLocalNote({ ...existing, blocked: true });
    showNotice('Dodano do Twojej listy blokad.');
  };

  // Protection & Screening
  const [protectionEnabled, setProtectionEnabledState] = useState<boolean>(() =>
    LocalStore.getProtectionEnabled()
  );
  const setProtectionEnabled = (val: boolean) => {
    setProtectionEnabledState(val);
    LocalStore.setProtectionEnabled(val);
  };
  const [screeningEvents, setScreeningEvents] = useState<ScreeningEvent[]>(() =>
    LocalStore.getScreeningEvents()
  );
  const clearScreeningEvents = () => {
    LocalStore.clearScreeningEvents();
    setScreeningEvents([]);
    showNotice('Wyczyszczono rejestr filtrowania.');
  };

  // Schematy i Klastry Rotacji
  const [clusters, setClusters] = useState<CallCluster[]>(() => LocalStore.getClusters());
  const [clusterSettings, setClusterSettings] = useState<ClusterSettings>(() =>
    LocalStore.getClusterSettings()
  );

  const updateClusterSettings = (newSettings: ClusterSettings) => {
    setClusterSettings(newSettings);
    LocalStore.saveClusterSettings(newSettings);
    showNotice('Zaktualizowano parametry wykrywania schematów.');
  };

  const updateClusterStatus = (id: string, status: ClusterStatus) => {
    const list = LocalStore.getClusters();
    const target = list.find(c => c.id === id);
    if (target) {
      const updated = { ...target, status };
      LocalStore.saveCluster(updated);
      setClusters(LocalStore.getClusters());
      showNotice(
        status === 'BLOCKED_PERMANENT'
          ? 'Pula została trwale zablokowana.'
          : status === 'ALLOWED_WHITELIST'
          ? 'Pula została oznaczona jako zaufana.'
          : 'Pula pozostaje w kwarantannie.'
      );
    }
  };

  const deleteCluster = (id: string) => {
    LocalStore.deleteCluster(id);
    setClusters(LocalStore.getClusters());
    showNotice('Usunięto schemat klastra.');
  };

  // Contacts & History
  const [contacts] = useState<DeviceContact[]>(() => LocalStore.getContacts());
  const [callHistory, setCallHistory] = useState<CallHistoryEntry[]>(() =>
    LocalStore.getCallHistory()
  );

  const evaluateCallCluster = useCallback(
    (phone: string): ClusterMatchResult => {
      const evalResult = evaluateCallForCluster(
        phone,
        clusters,
        callHistory,
        contacts,
        clusterSettings
      );
      if (evalResult.updatedCluster) {
        LocalStore.saveCluster(evalResult.updatedCluster);
        setClusters(LocalStore.getClusters());
      }
      return evalResult.result;
    },
    [clusters, callHistory, contacts, clusterSettings]
  );

  // In-Call Session
  const [callSession, setCallSession] = useState<CallSessionState>({
    calls: [],
    muted: false,
    audio: [
      { id: 'speaker', label: 'Głośnik telefonu' },
      { id: 'earpiece', label: 'Słuchawka telefonu', nearEar: true },
      { id: 'bluetooth', label: 'Zestaw Bluetooth' }
    ],
    selectedAudio: 'earpiece',
    canAdd: true,
    error: null
  });
  const [lastEndedCallNumber, setLastEndedCallNumber] = useState<string | null>(null);

  // Wangiri & Robocall State
  const [wangiriSettings, setWangiriSettings] = useState<WangiriSettings>(() =>
    LocalStore.getWangiriSettings()
  );
  const [robocallSettings, setRobocallSettings] = useState<RobocallSettings>(() =>
    LocalStore.getRobocallSettings()
  );
  const [wangiriModalState, setWangiriModalState] = useState<{
    isOpen: boolean;
    targetNumber: string;
    riskInfo: WangiriRiskInfo;
  } | null>(null);

  const robocallCampaigns = useMemo(() => {
    return aggregateRobocallCampaigns(callHistory, screeningEvents);
  }, [callHistory, screeningEvents]);

  const updateWangiriSettings = (partial: Partial<WangiriSettings>) => {
    setWangiriSettings(prev => {
      const updated = { ...prev, ...partial };
      LocalStore.saveWangiriSettings(updated);
      return updated;
    });
    showNotice('Zaktualizowano ustawienia Tarczy Wangiri.');
  };

  const updateRobocallSettings = (partial: Partial<RobocallSettings>) => {
    setRobocallSettings(prev => {
      const updated = { ...prev, ...partial };
      LocalStore.saveRobocallSettings(updated);
      return updated;
    });
    showNotice('Zaktualizowano parametry Robocall Intelligence.');
  };

  const closeWangiriModal = () => {
    setWangiriModalState(null);
  };

  const blockAndDismissWangiri = (targetNumber: string) => {
    const parsed = parsePhone(targetNumber);
    const e164 = parsed?.e164 || targetNumber;
    LocalStore.saveNote({
      phone: e164,
      label: 'Oszustwo Wangiri (Ping-Call)',
      note: 'Zablokowano przez Tarczę Wangiri przed kosztownym oddzwonieniem',
      blocked: true
    });
    setNotes(LocalStore.getNotes());
    LocalStore.recordScreeningEvent(e164, true);
    setScreeningEvents(LocalStore.getScreeningEvents());
    setWangiriModalState(null);
    showNotice('Tarcza Wangiri: Zablokowano numer i ochroniono portfel przed kosztami!');
  };

  const executeCallConnect = (lookupNumber: string) => {
    const isBlockedNum = LocalStore.isBlocked(lookupNumber);

    if (isBlockedNum && protectionEnabled) {
      // Log blocked event
      const event = LocalStore.recordScreeningEvent(lookupNumber, true);
      setScreeningEvents(LocalStore.getScreeningEvents());
      showNotice(`Połączenie zablokowane: numer znajduje się na Twojej liście blokad.`);
      return;
    }

    // Record allowed event
    LocalStore.recordScreeningEvent(lookupNumber, false);
    setScreeningEvents(LocalStore.getScreeningEvents());

    // Contact name lookup
    const contact = contacts.find(c => c.phone === lookupNumber);
    const callId = `call_${Date.now()}`;

    const newCall: CallItem = {
      id: callId,
      number: lookupNumber,
      state: 'DIALING',
      connectedAt: 0,
      canHold: true,
      canMerge: false,
      conference: false,
      accountChoices: []
    };

    setCallSession(prev => ({
      ...prev,
      calls: [...prev.calls.filter(c => c.state !== 'DISCONNECTED'), newCall]
    }));

    // Add to history
    const historyEntry = LocalStore.addCallHistory({
      number: lookupNumber,
      name: contact?.name || null,
      type: 2, // Outgoing
      date: Date.now(),
      durationSec: 0
    });
    setCallHistory(LocalStore.getCallHistory());

    // Simulate connection after 2 seconds
    setTimeout(() => {
      setCallSession(prev => ({
        ...prev,
        calls: prev.calls.map(c =>
          c.id === callId
            ? { ...c, state: 'ACTIVE' as CallState, connectedAt: Date.now() }
            : c
        )
      }));
    }, 2000);
  };

  const proceedOutgoingCall = (targetNumber: string) => {
    setWangiriModalState(null);
    executeCallConnect(targetNumber);
  };

  const startCall = (number: string) => {
    const target = normalizeDialTarget(number);
    if (!target) {
      showNotice('Nieprawidłowy numer telefonu.');
      return;
    }

    const parsed = parsePhone(target);
    const lookupNumber = parsed?.e164 || target;

    // Call-Back Interception by Wangiri Shield
    if (wangiriSettings.shieldEnabled && wangiriSettings.interceptOutgoingCall) {
      const risk = checkOutgoingWangiriRisk(lookupNumber);
      if (risk) {
        setWangiriSettings(prev => {
          const updated = {
            ...prev,
            interceptedAttemptsCount: (prev.interceptedAttemptsCount || 0) + 1
          };
          LocalStore.saveWangiriSettings(updated);
          return updated;
        });
        setWangiriModalState({
          isOpen: true,
          targetNumber: lookupNumber,
          riskInfo: risk
        });
        return;
      }
    }

    executeCallConnect(lookupNumber);
  };

  const simulateWangiriPingCall = (targetNum?: string) => {
    const num = targetNum || '+247555891';
    LocalStore.addCallHistory({
      number: num,
      name: null,
      type: 3, // Missed (1-ring ping)
      date: Date.now(),
      durationSec: 0
    });
    setCallHistory(LocalStore.getCallHistory());
    showNotice(`Tarcza Wangiri: Zarejestrowano 1-sekundowy ping z ${num} (Wyspa Wniebowstąpienia). Trafił do nieodebranych.`);
  };

  const simulateRobocallBurst = () => {
    const burstDdi = ['+48221664133', '+48221664137', '+48221664142'];
    burstDdi.forEach((n, idx) => {
      setTimeout(() => {
        LocalStore.addCallHistory({
          number: n,
          name: null,
          type: 3,
          date: Date.now() - idx * 25000,
          durationSec: 0
        });
        setCallHistory(LocalStore.getCallHistory());
      }, idx * 500);
    });
    showNotice('Robocall Intelligence: Zasymulowano burst dialera predykcyjnego (3 połączenia DDI ze wspólnym pniem).');
  };

  const simulateIncomingCall = (phone: string) => {
    const parsed = parsePhone(phone);
    const lookupNumber = parsed?.e164 || phone;

    // 1. Wangiri Shield check
    if (wangiriSettings.shieldEnabled) {
      const wRisk = evaluateWangiriRisk(lookupNumber, 0, 1.5, true);
      if (wRisk.isWangiri && wangiriSettings.autoBlockHighRiskPing && (wRisk.riskLevel === 'CRITICAL' || wRisk.riskLevel === 'HIGH')) {
        LocalStore.recordScreeningEvent(lookupNumber, true);
        setScreeningEvents(LocalStore.getScreeningEvents());
        showNotice(`Tarcza Wangiri: Zablokowano 1-sekundowy ping z ${wRisk.countryName} (${wRisk.estimatedCostRange})`);
        return;
      }
    }

    // 2. Robocall Intelligence check
    if (robocallSettings.enabled) {
      const roboAnalysis = analyzeRobocall(lookupNumber, callHistory, screeningEvents, contacts);
      if (roboAnalysis.isRobocall && roboAnalysis.probabilityScore >= robocallSettings.minScoreToBlock) {
        if (robocallSettings.action === 'REJECT') {
          LocalStore.recordScreeningEvent(lookupNumber, true);
          setScreeningEvents(LocalStore.getScreeningEvents());
          showNotice(`Robocall Intelligence: Odrzucono bota telemarketingowego (${roboAnalysis.botType} - ${roboAnalysis.probabilityScore}%)`);
          return;
        } else if (robocallSettings.action === 'SILENCE') {
          showNotice(`Robocall Intelligence: Wyciszono dzwonek bota (${roboAnalysis.probabilityScore}% prawdopodobieństwa)`);
        } else {
          showNotice(`Robocall Intelligence: Wykryto prawdopodobny automat (${roboAnalysis.probabilityScore}%)`);
        }
      }
    }

    // 3. Check pattern & rotation cluster detection
    const clusterEval = evaluateCallCluster(lookupNumber);
    if (clusterEval.shouldBlock && protectionEnabled) {
      LocalStore.recordScreeningEvent(lookupNumber, true);
      setScreeningEvents(LocalStore.getScreeningEvents());
      showNotice(
        `Filtrowanie schematów: odrzucono połączenie (${clusterEval.alertMessage || 'Pula rotacyjna'})`
      );
      return;
    }

    // 4. Check individual blocklist
    const isBlockedNum = LocalStore.isBlocked(lookupNumber);
    if (isBlockedNum && protectionEnabled) {
      LocalStore.recordScreeningEvent(lookupNumber, true);
      setScreeningEvents(LocalStore.getScreeningEvents());
      showNotice(`Filtrowanie: odrzucono połączenie od ${lookupNumber} (Twoja lista blokad).`);
      return;
    }

    if (clusterEval.isMatch && clusterEval.alertMessage) {
      showNotice(clusterEval.alertMessage);
    }

    LocalStore.recordScreeningEvent(lookupNumber, false);
    setScreeningEvents(LocalStore.getScreeningEvents());

    const contact = contacts.find(c => c.phone === lookupNumber);
    const callId = `call_${Date.now()}`;
    const newCall: CallItem = {
      id: callId,
      number: lookupNumber,
      state: 'RINGING',
      connectedAt: 0,
      canHold: true,
      canMerge: false,
      conference: false,
      accountChoices: []
    };

    setCallSession(prev => ({
      ...prev,
      calls: [...prev.calls.filter(c => c.state !== 'DISCONNECTED'), newCall]
    }));
  };

  const answerCall = (id: string) => {
    setCallSession(prev => ({
      ...prev,
      calls: prev.calls.map(c =>
        c.id === id
          ? { ...c, state: 'ACTIVE' as CallState, connectedAt: Date.now() }
          : c
      )
    }));
  };

  const endCall = (id: string) => {
    const call = callSession.calls.find(c => c.id === id);
    if (call && call.connectedAt > 0) {
      const duration = Math.round((Date.now() - call.connectedAt) / 1000);
      LocalStore.addCallHistory({
        number: call.number,
        name: contacts.find(c => c.phone === call.number)?.name || null,
        type: 1, // Incoming answered
        date: call.connectedAt,
        durationSec: duration
      });
      setCallHistory(LocalStore.getCallHistory());
    }

    if (call) {
      const isKnown = contacts.some(c => {
        const cleanC = c.phone.replace(/\D/g, '');
        const cleanCall = call.number.replace(/\D/g, '');
        return cleanC && cleanCall && (cleanC.endsWith(cleanCall) || cleanCall.endsWith(cleanC));
      });
      if (!isKnown && call.number.replace(/\D/g, '').length >= 4) {
        setLastEndedCallNumber(call.number);
      }
    }

    setCallSession(prev => ({
      ...prev,
      calls: prev.calls.map(c =>
        c.id === id ? { ...c, state: 'DISCONNECTED' as CallState } : c
      )
    }));
    setTimeout(() => {
      setCallSession(prev => ({
        ...prev,
        calls: prev.calls.filter(c => c.id !== id)
      }));
    }, 600);
  };

  const triggerSmartArrowCall = useCallback((targetNumber: string, durationMs = 2500) => {
    const callId = `call_${Date.now()}`;
    const newCall: CallItem = {
      id: callId,
      number: targetNumber,
      state: 'RINGING',
      connectedAt: 0,
      canHold: true,
      canMerge: false,
      conference: false,
      accountChoices: []
    };

    setCallSession(prev => ({
      ...prev,
      calls: [...prev.calls.filter(c => c.state !== 'DISCONNECTED'), newCall]
    }));

    setTimeout(() => {
      endCall(callId);
    }, durationMs);
  }, []);

  const holdCall = (id: string) => {
    setCallSession(prev => ({
      ...prev,
      calls: prev.calls.map(c =>
        c.id === id
          ? {
              ...c,
              state:
                c.state === 'HOLDING' ? ('ACTIVE' as CallState) : ('HOLDING' as CallState)
            }
          : c
      )
    }));
  };

  const toggleMute = () => {
    setCallSession(prev => ({ ...prev, muted: !prev.muted }));
  };

  const setCallMuted = (muted: boolean) => {
    setCallSession(prev => ({ ...prev, muted }));
  };

  const setAudioChoice = (id: string) => {
    setCallSession(prev => ({ ...prev, selectedAudio: id }));
  };

  const sendDtmfTone = (id: string, digit: string | null) => {
    // Plays sound / haptics if available
    if (digit && appearance.haptics && 'vibrate' in navigator) {
      navigator.vibrate(40);
    }
  };

  // SMS
  const [smsMessages, setSmsMessages] = useState<DeviceSms[]>(() =>
    LocalStore.getSmsMessages()
  );
  const [selectedThread, setSelectedThread] = useState<number | null>(null);
  const [drafts, setDrafts] = useState<Record<string, SmsDraft>>(() =>
    LocalStore.getDrafts()
  );
  const [pinnedThreads, setPinnedThreads] = useState<number[]>(() =>
    LocalStore.getPinnedThreads()
  );
  const [sends, setSends] = useState<SmsSend[]>([]);
  const [isSendingSms, setIsSendingSms] = useState<boolean>(false);

  const saveDraft = (key: string, draft: SmsDraft) => {
    LocalStore.saveDraft(key, draft);
    setDrafts(LocalStore.getDrafts());
  };

  const togglePinThread = (threadId: number) => {
    LocalStore.togglePin(threadId);
    setPinnedThreads(LocalStore.getPinnedThreads());
  };

  const markThreadRead = (threadId: number) => {
    LocalStore.markThreadRead(threadId);
    setSmsMessages(LocalStore.getSmsMessages());
  };

  const sendSms = async (
    address: string,
    body: string,
    draftKey: string = 'new'
  ): Promise<string> => {
    const parsed = parsePhone(address);
    if (!parsed) {
      throw new Error('Wpisz pełny numer odbiorcy z prefiksem kraju.');
    }
    if (!body.trim() || body.length > 2000) {
      throw new Error('Wiadomość musi mieć od 1 do 2000 znaków.');
    }

    setIsSendingSms(true);
    const sendId = `send_${Date.now()}`;
    const partsCount = Math.max(1, Math.ceil(body.length / 160));

    const newSend: SmsSend = {
      id: sendId,
      address: parsed.e164,
      parts: partsCount,
      completed: 0,
      failed: 0,
      started: Date.now(),
      error: ''
    };

    setSends(prev => [newSend, ...prev]);

    // Simulate sending transmission
    await new Promise(r => setTimeout(r, 600));

    // Determine thread ID
    const existing = smsMessages.find(m => m.address === parsed.e164);
    const threadId = existing ? existing.thread : Date.now();

    const createdMsg = LocalStore.addSmsMessage({
      thread: threadId,
      address: parsed.e164,
      body,
      time: Date.now(),
      type: 2, // Sent
      read: true,
      incoming: false
    });

    setSmsMessages(LocalStore.getSmsMessages());
    setSelectedThread(threadId);

    // Clear draft if it matches
    const currentDraft = drafts[draftKey];
    if (currentDraft && currentDraft.body === body) {
      saveDraft(draftKey, { address: parsed.e164, body: '' });
    }

    setSends(prev =>
      prev.map(s => (s.id === sendId ? { ...s, completed: partsCount } : s))
    );
    setIsSendingSms(false);
    showNotice('Operator potwierdził wysłanie wiadomości SMS.');
    return sendId;
  };

  const appendDirectSms = useCallback(
    (targetNumber: string, body: string, fromSim: 'PRIVATE' | 'WORK' = 'PRIVATE') => {
      const cleanTarget = targetNumber.replace(/\s+/g, '');
      const existing = smsMessages.find(m => m.address.replace(/\s+/g, '') === cleanTarget);
      const threadId = existing ? existing.thread : Date.now();

      LocalStore.addSmsMessage({
        thread: threadId,
        address: cleanTarget,
        body,
        time: Date.now(),
        type: 2, // Sent
        read: true,
        incoming: false
      });

      setSmsMessages(LocalStore.getSmsMessages());
    },
    [smsMessages]
  );

  // Cloud / Supabase
  const [cloudStatus, setCloudStatus] = useState<string>('Nie sprawdzono połączenia');
  const [cloudUserEmail, setCloudUserEmail] = useState<string | null>(null);
  const [cloudNotes, setCloudNotes] = useState<CloudNote[]>([]);
  const [isCloudBusy, setIsCloudBusy] = useState<boolean>(false);

  useEffect(() => {
    if (CloudRepository.isConfigured()) {
      CloudRepository.getUserEmail().then(email => {
        setCloudUserEmail(email);
        if (email) {
          setCloudStatus(`Zalogowano jako ${email}`);
        }
      });
    } else {
      setCloudStatus('Supabase nie jest skonfigurowane w zmiennych środowiskowych.');
    }
  }, []);

  const checkCloudConnection = async () => {
    setIsCloudBusy(true);
    try {
      if (!CloudRepository.isConfigured()) {
        setCloudStatus('Brak kluczy SUPABASE_URL / SUPABASE_PUBLISHABLE_KEY');
        showNotice('Supabase nie jest skonfigurowane.');
        return;
      }
      await CloudRepository.checkConnection();
      setCloudStatus('Połączono z bazą czytoty-app');
      showNotice('Połączono z bazą czytoty-app.');
    } catch (err: any) {
      setCloudStatus('Połączenie z bazą nie powiodło się');
      showNotice(err?.message || 'Brak połączenia z Supabase.');
    } finally {
      setIsCloudBusy(false);
    }
  };

  const authenticateCloud = async (
    email: string,
    pass: string,
    isRegister: boolean
  ) => {
    setIsCloudBusy(true);
    try {
      if (!CloudRepository.isConfigured()) {
        throw new Error('Supabase nie jest skonfigurowane.');
      }
      if (isRegister) {
        const { confirmed } = await CloudRepository.signUp(email, pass);
        showNotice(
          confirmed
            ? 'Utworzono konto.'
            : 'Sprawdź pocztę i potwierdź adres e-mail przed logowaniem.'
        );
      } else {
        const loggedIn = await CloudRepository.signIn(email, pass);
        setCloudUserEmail(loggedIn);
        showNotice('Zalogowano. Możesz teraz pobrać lub zapisać swoje notatki.');
      }
      const current = await CloudRepository.getUserEmail();
      setCloudUserEmail(current);
    } catch (err: any) {
      showNotice(
        err?.message ||
          'Operacja Supabase nie powiodła się. Sprawdź połączenie i dane.'
      );
    } finally {
      setIsCloudBusy(false);
    }
  };

  const signOutCloud = async () => {
    setIsCloudBusy(true);
    try {
      await CloudRepository.signOut();
      setCloudUserEmail(null);
      setCloudNotes([]);
      showNotice('Wylogowano z konta Supabase.');
    } finally {
      setIsCloudBusy(false);
    }
  };

  const loadCloudNotes = async () => {
    setIsCloudBusy(true);
    try {
      const data = await CloudRepository.loadNotes();
      setCloudNotes(data);
      showNotice(`Pobrano ${data.length} notatek z chmury.`);
    } catch (err: any) {
      showNotice(err?.message || 'Nie udało się pobrać notatek.');
    } finally {
      setIsCloudBusy(false);
    }
  };

  const saveCloudNote = async (note: PhoneNote) => {
    setIsCloudBusy(true);
    try {
      const saved = await CloudRepository.saveNote(note);
      setCloudNotes(prev => [
        ...prev.filter(n => n.phone !== saved.phone),
        saved
      ]);
      showNotice('Notatka została zapisana na Twoim koncie Supabase.');
    } catch (err: any) {
      showNotice(err?.message || 'Nie udało się zapisać w chmurze.');
    } finally {
      setIsCloudBusy(false);
    }
  };

  const deleteCloudNote = async (phone: string) => {
    setIsCloudBusy(true);
    try {
      await CloudRepository.deleteNote(phone);
      setCloudNotes(prev => prev.filter(n => n.phone !== phone));
      showNotice('Usunięto z chmury.');
    } catch (err: any) {
      showNotice(err?.message || 'Nie udało się usunąć z chmury.');
    } finally {
      setIsCloudBusy(false);
    }
  };

  const restoreToLocal = (cloudNote: CloudNote) => {
    const isBlocked = LocalStore.isBlocked(cloudNote.phone);
    saveLocalNote({
      phone: cloudNote.phone,
      label: cloudNote.label,
      note: cloudNote.note,
      blocked: isBlocked
    });
    showNotice('Skopiowano notatkę na urządzenie.');
  };

  // Karta Użytkownika & Kontakt ICE
  const [userCard, setUserCardState] = useState<UserBusinessCard>(() => LocalStore.getUserCard());
  const saveUserCard = useCallback((card: UserBusinessCard) => {
    LocalStore.saveUserCard(card);
    setUserCardState({ ...card, updatedAt: Date.now() });
    showNotice('Zapisano dane wizytówki i kontaktu ICE.');
  }, [showNotice]);

  const callIceContact = useCallback(() => {
    if (!userCard.iceContactPhone) {
      showNotice('Brak skonfigurowanego numeru ICE w wizytówce.');
      return;
    }
    startCall(userCard.iceContactPhone);
  }, [userCard.iceContactPhone, startCall, showNotice]);

  const smsIceContact = useCallback(() => {
    if (!userCard.iceContactPhone) {
      showNotice('Brak skonfigurowanego numeru ICE w wizytówce.');
      return;
    }
    setSmsPrefill(userCard.iceContactPhone);
    setActiveTab(1);
  }, [userCard.iceContactPhone, setSmsPrefill, setActiveTab, showNotice]);

  // Baza Firm i Usług (Smart Directory)
  const [directory, setDirectory] = useState<BusinessEntity[]>(() => LocalStore.getBusinessDirectory());

  const searchDirectory = useCallback((query: string) => {
    return searchDirectoryEntities(query, directory);
  }, [directory]);

  const importDirectoryEntity = useCallback((entity: BusinessEntity): BusinessEntity => {
    const imported = LocalStore.importBusinessEntity(entity);
    setDirectory(LocalStore.getBusinessDirectory());
    showNotice(`Pobrano i zapisano wizytówkę do bazy: ${entity.name}`);
    return imported;
  }, [showNotice]);

  const saveCustomBusinessEntity = useCallback((entity: Omit<BusinessEntity, 'id' | 'importedAt'>) => {
    const newEnt: BusinessEntity = {
      ...entity,
      id: `biz_custom_${Date.now()}`,
      importedAt: Date.now(),
      source: 'USER_IMPORT'
    };
    LocalStore.saveBusinessEntity(newEnt);
    setDirectory(LocalStore.getBusinessDirectory());
    showNotice(`Zapisano do bazy firm: ${newEnt.name}`);
  }, [showNotice]);

  // Numery Alarmowe
  const getEmergencyDetails = useCallback((raw: string) => {
    return getEmergencyInfo(raw);
  }, []);

  const isEmergency = useCallback((raw: string) => {
    return isEmergencyPrefix(raw);
  }, []);

  return (
    <AppContext.Provider
      value={{
        activeTab,
        setActiveTab,
        smsConversationOpen,
        setSmsConversationOpen,
        appearanceOpen,
        setAppearanceOpen,
        dialPrefill,
        setDialPrefill,
        smsPrefill,
        setSmsPrefill,
        phoneInput,
        setPhoneInput,
        messageToCheck,
        setMessageToCheck,
        appearance,
        updateAppearance,
        certSnapshot,
        refreshingCert,
        refreshCert,
        analysis,
        analyze,
        clearAnalysis,
        notes,
        saveLocalNote,
        deleteLocalNote,
        blockNumber,
        protectionEnabled,
        setProtectionEnabled,
        screeningEvents,
        clearScreeningEvents,
        simulateIncomingCall,
        clusters,
        clusterSettings,
        updateClusterSettings,
        updateClusterStatus,
        deleteCluster,
        evaluateCallCluster,
        wangiriSettings,
        updateWangiriSettings,
        wangiriModalState,
        proceedOutgoingCall,
        blockAndDismissWangiri,
        closeWangiriModal,
        simulateWangiriPingCall,
        robocallSettings,
        updateRobocallSettings,
        robocallCampaigns,
        simulateRobocallBurst,
        callHistory,
        contacts,
        callSession,
        startCall,
        triggerSmartArrowCall,
        answerCall,
        endCall,
        holdCall,
        toggleMute,
        setCallMuted,
        setAudioChoice,
        sendDtmfTone,
        lastEndedCallNumber,
        setLastEndedCallNumber,
        smsMessages,
        selectedThread,
        setSelectedThread,
        drafts,
        saveDraft,
        pinnedThreads,
        togglePinThread,
        sendSms,
        appendDirectSms,
        sends,
        isSendingSms,
        markThreadRead,
        cloudStatus,
        cloudUserEmail,
        cloudNotes,
        isCloudBusy,
        checkCloudConnection,
        authenticateCloud,
        signOutCloud,
        loadCloudNotes,
        saveCloudNote,
        deleteCloudNote,
        restoreToLocal,
        directory,
        searchDirectory,
        importDirectoryEntity,
        saveCustomBusinessEntity,
        getEmergencyDetails,
        isEmergency,
        userCard,
        saveUserCard,
        callIceContact,
        smsIceContact,
        notice,
        showNotice,
        dismissNotice
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useApp must be used within an AppProvider');
  }
  return context;
}
