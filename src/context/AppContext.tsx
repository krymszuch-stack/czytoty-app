import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  ReactNode
} from 'react';
import {
  Appearance,
  CallHistoryEntry,
  CallItem,
  CallSessionState,
  CallState,
  CertSnapshot,
  CloudNote,
  DeviceContact,
  DeviceSms,
  MessageAnalysis,
  PhoneNote,
  ScreeningEvent,
  SmsDraft,
  SmsSend
} from '../types';
import { LocalStore } from '../services/storage';
import { analyzeMessage, isSnapshotFresh } from '../utils/cert';
import { normalizeDialTarget, parsePhone } from '../utils/phone';
import { CloudRepository } from '../services/supabase';

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

  // Phone & Calls
  callHistory: CallHistoryEntry[];
  contacts: DeviceContact[];
  callSession: CallSessionState;
  startCall: (number: string) => void;
  answerCall: (id: string) => void;
  endCall: (id: string) => void;
  holdCall: (id: string) => void;
  toggleMute: () => void;
  setAudioChoice: (id: string) => void;
  sendDtmfTone: (id: string, digit: string | null) => void;

  // SMS
  smsMessages: DeviceSms[];
  selectedThread: number | null;
  setSelectedThread: (thread: number | null) => void;
  drafts: Record<string, SmsDraft>;
  saveDraft: (key: string, draft: SmsDraft) => void;
  pinnedThreads: number[];
  togglePinThread: (threadId: number) => void;
  sendSms: (address: string, body: string, draftKey?: string) => Promise<string>;
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

  // Contacts & History
  const [contacts] = useState<DeviceContact[]>(() => LocalStore.getContacts());
  const [callHistory, setCallHistory] = useState<CallHistoryEntry[]>(() =>
    LocalStore.getCallHistory()
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

  const startCall = (number: string) => {
    const target = normalizeDialTarget(number);
    if (!target) {
      showNotice('Nieprawidłowy numer telefonu.');
      return;
    }

    const parsed = parsePhone(target);
    const lookupNumber = parsed?.e164 || target;
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

  const simulateIncomingCall = (phone: string) => {
    const parsed = parsePhone(phone);
    const lookupNumber = parsed?.e164 || phone;
    const isBlockedNum = LocalStore.isBlocked(lookupNumber);

    if (isBlockedNum && protectionEnabled) {
      LocalStore.recordScreeningEvent(lookupNumber, true);
      setScreeningEvents(LocalStore.getScreeningEvents());
      showNotice(`Filtrowanie: odrzucono połączenie od ${lookupNumber} (Twoja lista blokad).`);
      return;
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
        callHistory,
        contacts,
        callSession,
        startCall,
        answerCall,
        endCall,
        holdCall,
        toggleMute,
        setAudioChoice,
        sendDtmfTone,
        smsMessages,
        selectedThread,
        setSelectedThread,
        drafts,
        saveDraft,
        pinnedThreads,
        togglePinThread,
        sendSms,
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
