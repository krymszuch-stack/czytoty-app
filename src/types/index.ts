export type ThemeMode = 'SYSTEM' | 'LIGHT' | 'DARK';
export type AccentColor = 'LAVENDER' | 'OCEAN' | 'MINT';

export interface Appearance {
  mode: ThemeMode;
  accent: AccentColor;
  dynamic: boolean;
  compact: boolean;
  largeText: boolean;
  rounded: boolean;
  haptics: boolean;
}

export interface PhoneNote {
  phone: string; // E.164 format
  label: string;
  note: string;
  blocked: boolean;
}

export interface ScreeningEvent {
  id: number;
  phone: string;
  blocked: boolean;
  time: number;
}

export interface CertSnapshot {
  fetchedAt: number;
  domains: string[];
}

export interface DomainFinding {
  host: string;
  matchedDomain: string | null;
}

export interface MessageAnalysis {
  findings: DomainFinding[];
  localSignals: string[];
  snapshotTime: number | null;
}

export interface ParsedPhone {
  e164: string;
  formatted: string;
  country: string;
}

export interface DeviceContact {
  id: string;
  name: string;
  phone: string;
}

export interface CallHistoryEntry {
  id: string;
  number: string;
  name: string | null;
  type: number; // 1: Incoming, 2: Outgoing, 3: Missed, 4: Rejected, 6: Blocked
  date: number;
  durationSec: number;
}

export interface DeviceSms {
  id: number;
  thread: number;
  address: string;
  body: string;
  time: number;
  type: number; // 1: Inbox, 2: Sent, 4: Outbox, 5: Failed, 6: Queued
  read: boolean;
  incoming: boolean;
}

export interface SmsDraft {
  address: string;
  body: string;
}

export interface SmsSend {
  id: string;
  address: string;
  parts: number;
  completed: number;
  failed: number;
  started: number;
  error: string;
}

export type CallState =
  | 'RINGING'
  | 'DIALING'
  | 'CONNECTING'
  | 'ACTIVE'
  | 'HOLDING'
  | 'DISCONNECTING'
  | 'DISCONNECTED';

export interface CallItem {
  id: string;
  number: string;
  state: CallState;
  connectedAt: number;
  canHold: boolean;
  canMerge: boolean;
  conference: boolean;
  accountChoices: { index: number; label: string }[];
}

export interface AudioChoice {
  id: string;
  label: string;
  nearEar?: boolean;
}

export interface CallSessionState {
  calls: CallItem[];
  muted: boolean;
  audio: AudioChoice[];
  selectedAudio: string | null;
  canAdd: boolean;
  error: string | null;
}

export interface CloudNote {
  userId: string;
  phone: string;
  label: string;
  note: string;
}
