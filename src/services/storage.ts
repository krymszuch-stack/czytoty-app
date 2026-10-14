import {
  Appearance,
  BusinessEntity,
  CallCluster,
  CallHistoryEntry,
  CertSnapshot,
  ClusterSettings,
  DeviceContact,
  DeviceSms,
  PhoneNote,
  RobocallSettings,
  ScreeningEvent,
  SmsDraft,
  UserBusinessCard,
  WangiriSettings
} from '../types';
import { DEFAULT_CLUSTER_SETTINGS } from '../utils/cluster';
import { DEFAULT_WANGIRI_SETTINGS } from '../utils/wangiri';
import { DEFAULT_ROBOCALL_SETTINGS } from '../utils/robocall';
import { VERIFIED_SEED_DIRECTORY } from './directory';

const STORAGE_KEYS = {
  PHONE_NOTES: 'czytoty_phone_notes',
  SCREENING_EVENTS: 'czytoty_screening_events',
  PROTECTION_ENABLED: 'czytoty_protection_enabled',
  APPEARANCE: 'czytoty_appearance',
  CERT_SNAPSHOT: 'czytoty_cert_snapshot',
  DRAFTS: 'czytoty_drafts',
  PINNED_THREADS: 'czytoty_pinned_threads',
  CALL_HISTORY: 'czytoty_call_history',
  CONTACTS: 'czytoty_contacts',
  SMS_MESSAGES: 'czytoty_sms_messages',
  CLUSTERS: 'czytoty_call_clusters',
  CLUSTER_SETTINGS: 'czytoty_cluster_settings',
  USER_CARD: 'czytoty_user_card',
  DIRECTORY: 'czytoty_business_directory',
  WANGIRI_SETTINGS: 'czytoty_wangiri_settings',
  ROBOCALL_SETTINGS: 'czytoty_robocall_settings'
};

const DEFAULT_USER_CARD: UserBusinessCard = {
  fullName: 'Jan Kowalski',
  phone: '+48 600 123 456',
  avatarIcon: 'senior-m',
  iceContactName: 'Anna Kowalska (Córka)',
  iceContactPhone: '+48 601 234 567',
  note: 'Grupa krwi: A Rh+, cukrzyca typu 2. W razie nagłego wypadku proszę natychmiast dzwonić do córki Anny.',
  bloodType: 'A Rh+',
  updatedAt: Date.now()
};

const DEFAULT_APPEARANCE: Appearance = {
  mode: 'SYSTEM',
  accent: 'LAVENDER',
  dynamic: false,
  compact: false,
  largeText: false,
  rounded: true,
  haptics: true
};

// Seed realistic device contacts (Polish format)
const SEED_CONTACTS: DeviceContact[] = [
  { id: 'c1', name: 'Mama', phone: '+48601234567' },
  { id: 'c2', name: 'Tomasz Kowalski', phone: '+48502345678' },
  { id: 'c3', name: 'Anna Nowak', phone: '+48698112233' },
  { id: 'c4', name: 'Kurier InPost', phone: '+48722000111' },
  { id: 'c5', name: 'Marek z Pracy', phone: '+48511444555' }
];

// Seed realistic call history with Wangiri ping-calls and Robocall campaigns
const SEED_CALL_HISTORY: CallHistoryEntry[] = [
  {
    id: 'h_wangiri_1',
    number: '+247555123',
    name: null,
    type: 3, // Missed (1-ring ping)
    date: Date.now() - 1000 * 60 * 18, // 18 min ago
    durationSec: 0
  },
  {
    id: 'h_robo_2',
    number: '+48221664128',
    name: null,
    type: 3, // Missed (Robocall DDI rotation)
    date: Date.now() - 1000 * 60 * 28, // 28 min ago
    durationSec: 0
  },
  {
    id: 'h1',
    number: '+48601234567',
    name: 'Mama',
    type: 1, // Incoming
    date: Date.now() - 1000 * 60 * 45, // 45 min ago
    durationSec: 142
  },
  {
    id: 'h_robo_1',
    number: '+48221664120',
    name: null,
    type: 1, // Incoming answered, dropped after 2s (AMD silent call)
    date: Date.now() - 1000 * 60 * 55, // 55 min ago
    durationSec: 2
  },
  {
    id: 'h_wangiri_2',
    number: '+223701234',
    name: null,
    type: 3, // Missed (Mali, optical camouflage for Warsaw 022)
    date: Date.now() - 1000 * 60 * 110, // ~2h ago
    durationSec: 0
  },
  {
    id: 'h2',
    number: '+48221002030',
    name: 'Podejrzany Doradca',
    type: 3, // Missed
    date: Date.now() - 1000 * 60 * 180, // 3h ago
    durationSec: 0
  },
  {
    id: 'h3',
    number: '+48502345678',
    name: 'Tomasz Kowalski',
    type: 2, // Outgoing
    date: Date.now() - 1000 * 60 * 60 * 26, // Yesterday
    durationSec: 68
  },
  {
    id: 'h4',
    number: '+48799599888',
    name: null,
    type: 4, // Rejected / Blocked
    date: Date.now() - 1000 * 60 * 60 * 52,
    durationSec: 0
  }
];

// Seed realistic SMS messages (threads, legitimate and phishing alert)
const SEED_SMS_MESSAGES: DeviceSms[] = [
  {
    id: 101,
    thread: 1,
    address: '+48601234567',
    body: 'Cześć, kupiłeś chleb po drodze do domu?',
    time: Date.now() - 1000 * 60 * 40,
    type: 1, // Inbox
    read: true,
    incoming: true
  },
  {
    id: 102,
    thread: 1,
    address: '+48601234567',
    body: 'Tak, kupiłem orkiszowy. Będę za 15 minut!',
    time: Date.now() - 1000 * 60 * 38,
    type: 2, // Sent
    read: true,
    incoming: false
  },
  {
    id: 201,
    thread: 2,
    address: '+48732123456',
    body: 'PGE: Prosimy o natychmiastowa doplata 4.29 zl do faktury, aby uniknac odlaczenia pradu https://pge-faktura-zaleglosc.top',
    time: Date.now() - 1000 * 60 * 95,
    type: 1, // Inbox
    read: false,
    incoming: true
  },
  {
    id: 301,
    thread: 3,
    address: '+48502345678',
    body: 'Hej, widziałeś ten nowy projekt na jutrzejsze spotkanie?',
    time: Date.now() - 1000 * 60 * 60 * 18,
    type: 1, // Inbox
    read: true,
    incoming: true
  },
  {
    id: 302,
    thread: 3,
    address: '+48502345678',
    body: 'Tak, przejrzałem slajdy. Wygląda solidnie.',
    time: Date.now() - 1000 * 60 * 60 * 17,
    type: 2, // Sent
    read: true,
    incoming: false
  },
  {
    id: 401,
    thread: 4,
    address: 'InP0st',
    body: 'InP0st: Twoja paczka zostala wstrzymana z powodu bledu w adresie doreczenia. Zaktualizuj dane w ciagu 24h na: https://inpost-sledzenie-paczki.xyz',
    time: Date.now() - 1000 * 60 * 42,
    type: 1, // Inbox
    read: false,
    incoming: true
  },
  {
    id: 501,
    thread: 5,
    address: 'AlIegro',
    body: 'AlIegro: Wykryto nieautoryzowane logowanie z nowego urzadzenia Android. Jesli to nie Ty, natychmiast zabezpiecz konto pod adresem: https://allegro-weryfikacja-konta.info',
    time: Date.now() - 1000 * 60 * 15,
    type: 1, // Inbox
    read: false,
    incoming: true
  }
];

const SEED_NOTES: PhoneNote[] = [
  {
    phone: '+48221002030',
    label: 'Call center fotowoltaika',
    note: 'Dzwonili 3 razy oferując audyt, nie podają skąd mają numer.',
    blocked: true
  },
  {
    phone: '+48799599888',
    label: 'Oszustwo na kuriera',
    note: 'Wysyła SMS-y z linkami do fałszywych bramek płatności.',
    blocked: true
  }
];

const SEED_EVENTS: ScreeningEvent[] = [
  {
    id: 1,
    phone: '+48799599888',
    blocked: true,
    time: Date.now() - 1000 * 60 * 60 * 52
  },
  {
    id: 2,
    phone: '+48601234567',
    blocked: false,
    time: Date.now() - 1000 * 60 * 35
  }
];

export const LocalStore = {
  getNotes(): PhoneNote[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.PHONE_NOTES);
      if (!data) {
        localStorage.setItem(STORAGE_KEYS.PHONE_NOTES, JSON.stringify(SEED_NOTES));
        return SEED_NOTES;
      }
      return JSON.parse(data);
    } catch {
      return SEED_NOTES;
    }
  },

  saveNote(note: PhoneNote): void {
    const notes = this.getNotes().filter(n => n.phone !== note.phone);
    notes.push(note);
    notes.sort((a, b) => (a.label || a.phone).localeCompare(b.label || b.phone));
    localStorage.setItem(STORAGE_KEYS.PHONE_NOTES, JSON.stringify(notes));
  },

  deleteNote(phone: string): void {
    const notes = this.getNotes().filter(n => n.phone !== phone);
    localStorage.setItem(STORAGE_KEYS.PHONE_NOTES, JSON.stringify(notes));
  },

  isBlocked(phone: string): boolean {
    const notes = this.getNotes();
    return notes.some(n => n.phone === phone && n.blocked);
  },

  getProtectionEnabled(): boolean {
    try {
      const val = localStorage.getItem(STORAGE_KEYS.PROTECTION_ENABLED);
      return val === null ? true : val === 'true';
    } catch {
      return true;
    }
  },

  setProtectionEnabled(enabled: boolean): void {
    localStorage.setItem(STORAGE_KEYS.PROTECTION_ENABLED, String(enabled));
  },

  getScreeningEvents(): ScreeningEvent[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SCREENING_EVENTS);
      if (!data) {
        localStorage.setItem(STORAGE_KEYS.SCREENING_EVENTS, JSON.stringify(SEED_EVENTS));
        return SEED_EVENTS;
      }
      return JSON.parse(data);
    } catch {
      return SEED_EVENTS;
    }
  },

  recordScreeningEvent(phone: string, blocked: boolean): ScreeningEvent {
    const events = this.getScreeningEvents();
    const newEvent: ScreeningEvent = {
      id: Date.now(),
      phone,
      blocked,
      time: Date.now()
    };
    const updated = [newEvent, ...events].slice(0, 200);
    localStorage.setItem(STORAGE_KEYS.SCREENING_EVENTS, JSON.stringify(updated));
    return newEvent;
  },

  clearScreeningEvents(): void {
    localStorage.setItem(STORAGE_KEYS.SCREENING_EVENTS, JSON.stringify([]));
  },

  getAppearance(): Appearance {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.APPEARANCE);
      if (data) return { ...DEFAULT_APPEARANCE, ...JSON.parse(data) };
    } catch {
      // fallback
    }
    return DEFAULT_APPEARANCE;
  },

  saveAppearance(appearance: Appearance): void {
    localStorage.setItem(STORAGE_KEYS.APPEARANCE, JSON.stringify(appearance));
  },

  getDrafts(): Record<string, SmsDraft> {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.DRAFTS);
      return data ? JSON.parse(data) : {};
    } catch {
      return {};
    }
  },

  saveDraft(key: string, draft: SmsDraft): void {
    const drafts = this.getDrafts();
    if (!draft.body && !draft.address) {
      delete drafts[key];
    } else {
      drafts[key] = draft;
    }
    localStorage.setItem(STORAGE_KEYS.DRAFTS, JSON.stringify(drafts));
  },

  getPinnedThreads(): number[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.PINNED_THREADS);
      return data ? JSON.parse(data) : [];
    } catch {
      return [];
    }
  },

  togglePin(threadId: number): boolean {
    const pins = new Set(this.getPinnedThreads());
    let isPinned = false;
    if (pins.has(threadId)) {
      pins.delete(threadId);
    } else {
      pins.add(threadId);
      isPinned = true;
    }
    localStorage.setItem(STORAGE_KEYS.PINNED_THREADS, JSON.stringify(Array.from(pins)));
    return isPinned;
  },

  getContacts(): DeviceContact[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.CONTACTS);
      if (!data) {
        localStorage.setItem(STORAGE_KEYS.CONTACTS, JSON.stringify(SEED_CONTACTS));
        return SEED_CONTACTS;
      }
      return JSON.parse(data);
    } catch {
      return SEED_CONTACTS;
    }
  },

  getCallHistory(): CallHistoryEntry[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.CALL_HISTORY);
      if (!data) {
        localStorage.setItem(STORAGE_KEYS.CALL_HISTORY, JSON.stringify(SEED_CALL_HISTORY));
        return SEED_CALL_HISTORY;
      }
      return JSON.parse(data);
    } catch {
      return SEED_CALL_HISTORY;
    }
  },

  addCallHistory(entry: Omit<CallHistoryEntry, 'id'>): CallHistoryEntry {
    const history = this.getCallHistory();
    const newEntry: CallHistoryEntry = {
      ...entry,
      id: `call_${Date.now()}`
    };
    const updated = [newEntry, ...history].slice(0, 200);
    localStorage.setItem(STORAGE_KEYS.CALL_HISTORY, JSON.stringify(updated));
    return newEntry;
  },

  getSmsMessages(): DeviceSms[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.SMS_MESSAGES);
      if (!data) {
        localStorage.setItem(STORAGE_KEYS.SMS_MESSAGES, JSON.stringify(SEED_SMS_MESSAGES));
        return SEED_SMS_MESSAGES;
      }
      return JSON.parse(data);
    } catch {
      return SEED_SMS_MESSAGES;
    }
  },

  addSmsMessage(sms: Omit<DeviceSms, 'id'>): DeviceSms {
    const list = this.getSmsMessages();
    const newMsg: DeviceSms = {
      ...sms,
      id: Date.now()
    };
    const updated = [...list, newMsg];
    localStorage.setItem(STORAGE_KEYS.SMS_MESSAGES, JSON.stringify(updated));
    return newMsg;
  },

  markThreadRead(threadId: number): void {
    const list = this.getSmsMessages();
    const updated = list.map(m => (m.thread === threadId ? { ...m, read: true } : m));
    localStorage.setItem(STORAGE_KEYS.SMS_MESSAGES, JSON.stringify(updated));
  },

  getCertSnapshot(): CertSnapshot | null {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.CERT_SNAPSHOT);
      return data ? JSON.parse(data) : null;
    } catch {
      return null;
    }
  },

  saveCertSnapshot(snapshot: CertSnapshot): void {
    localStorage.setItem(STORAGE_KEYS.CERT_SNAPSHOT, JSON.stringify(snapshot));
  },

  getClusters(): CallCluster[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.CLUSTERS);
      if (!data) {
        const seedClusters: CallCluster[] = [
          {
            id: 'cluster_seed_1',
            prefix: '+48901234',
            mask: '+48 901 234 •••',
            numbers: ['+48901234102', '+48901234189'],
            callCount: 2,
            firstSeen: Date.now() - 1000 * 60 * 35, // 35 min ago
            lastSeen: Date.now() - 1000 * 60 * 15,  // 15 min ago (20 min interval!)
            status: 'ACTIVE_QUARANTINE',
            riskScore: 'HIGH',
            reason: 'Wykryto rotację 2 numerów z centrali SIP w oknie 20 minut (schemat windykacji/telemarketingu).'
          }
        ];
        localStorage.setItem(STORAGE_KEYS.CLUSTERS, JSON.stringify(seedClusters));
        return seedClusters;
      }
      return JSON.parse(data);
    } catch {
      return [];
    }
  },

  saveCluster(cluster: CallCluster): void {
    const list = this.getClusters().filter(c => c.id !== cluster.id);
    list.unshift(cluster);
    localStorage.setItem(STORAGE_KEYS.CLUSTERS, JSON.stringify(list));
  },

  deleteCluster(id: string): void {
    const list = this.getClusters().filter(c => c.id !== id);
    localStorage.setItem(STORAGE_KEYS.CLUSTERS, JSON.stringify(list));
  },

  getClusterSettings(): ClusterSettings {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.CLUSTER_SETTINGS);
      if (data) return { ...DEFAULT_CLUSTER_SETTINGS, ...JSON.parse(data) };
    } catch {
      // fallback
    }
    return DEFAULT_CLUSTER_SETTINGS;
  },

  saveClusterSettings(settings: ClusterSettings): void {
    localStorage.setItem(STORAGE_KEYS.CLUSTER_SETTINGS, JSON.stringify(settings));
  },

  getUserCard(): UserBusinessCard {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.USER_CARD);
      if (data) return { ...DEFAULT_USER_CARD, ...JSON.parse(data) };
    } catch {
      // fallback
    }
    return DEFAULT_USER_CARD;
  },

  saveUserCard(card: UserBusinessCard): void {
    const updated = { ...card, updatedAt: Date.now() };
    localStorage.setItem(STORAGE_KEYS.USER_CARD, JSON.stringify(updated));
  },

  getBusinessDirectory(): BusinessEntity[] {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.DIRECTORY);
      if (!data) {
        localStorage.setItem(STORAGE_KEYS.DIRECTORY, JSON.stringify(VERIFIED_SEED_DIRECTORY));
        return VERIFIED_SEED_DIRECTORY;
      }
      return JSON.parse(data);
    } catch {
      return VERIFIED_SEED_DIRECTORY;
    }
  },

  saveBusinessEntity(entity: BusinessEntity): void {
    const list = this.getBusinessDirectory().filter(e => e.id !== entity.id);
    list.unshift(entity);
    localStorage.setItem(STORAGE_KEYS.DIRECTORY, JSON.stringify(list));
  },

  importBusinessEntity(entity: BusinessEntity): BusinessEntity {
    const updatedEntity: BusinessEntity = {
      ...entity,
      importedAt: Date.now(),
      verified: true
    };
    this.saveBusinessEntity(updatedEntity);
    return updatedEntity;
  },

  getWangiriSettings(): WangiriSettings {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.WANGIRI_SETTINGS);
      if (data) return { ...DEFAULT_WANGIRI_SETTINGS, ...JSON.parse(data) };
    } catch {
      // fallback
    }
    return DEFAULT_WANGIRI_SETTINGS;
  },

  saveWangiriSettings(settings: WangiriSettings): void {
    localStorage.setItem(STORAGE_KEYS.WANGIRI_SETTINGS, JSON.stringify(settings));
  },

  getRobocallSettings(): RobocallSettings {
    try {
      const data = localStorage.getItem(STORAGE_KEYS.ROBOCALL_SETTINGS);
      if (data) return { ...DEFAULT_ROBOCALL_SETTINGS, ...JSON.parse(data) };
    } catch {
      // fallback
    }
    return DEFAULT_ROBOCALL_SETTINGS;
  },

  saveRobocallSettings(settings: RobocallSettings): void {
    localStorage.setItem(STORAGE_KEYS.ROBOCALL_SETTINGS, JSON.stringify(settings));
  }
};

