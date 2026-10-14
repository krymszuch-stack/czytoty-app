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
  seniorMode?: boolean;
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

export type ClusterStatus = 'ACTIVE_QUARANTINE' | 'BLOCKED_PERMANENT' | 'ALLOWED_WHITELIST';

export interface CallCluster {
  id: string;
  prefix: string;            // e.g. "+48901234"
  mask: string;              // e.g. "+48 901 234 •••"
  numbers: string[];         // list of unique caller numbers seen in this pool
  callCount: number;
  firstSeen: number;
  lastSeen: number;
  status: ClusterStatus;
  riskScore: 'HIGH' | 'SUSPECTED';
  reason: string;
}

export interface ClusterSettings {
  prefixDigits: number;       // default: 7 (or 6)
  windowMinutes: number;      // default: 60
  minAttemptsToTrigger: number; // default: 2
  autoBlockPool: boolean;     // default: true
}

export interface ClusterMatchResult {
  isMatch: boolean;
  cluster: CallCluster | null;
  shouldBlock: boolean;
  alertMessage: string | null;
}

export interface EmergencyService {
  number: string;
  name: string;
  department: string;
  description: string;
  priority: 'CRITICAL';
  freeAndDirect: boolean;
}

export type BusinessCategory =
  | 'bakery'
  | 'pharmacy'
  | 'clinic'
  | 'post'
  | 'bank'
  | 'service'
  | 'grocery'
  | 'taxi'
  | 'other';

export interface BusinessEntity {
  id: string;
  name: string;
  phone: string;
  formattedPhone: string;
  category: BusinessCategory;
  categoryIcon: string;       // e.g. '🥖', '💊', '🏥', '📯', '🏦', '🔧', '🛒', '🚕'
  address: string;
  rating?: number;
  reviewCount?: number;
  source: 'GOOGLE_MAPS' | 'VERIFIED_DATABASE' | 'USER_IMPORT';
  verified: boolean;
  importedAt?: number;
}

export interface UserBusinessCard {
  fullName: string;
  phone: string;
  avatarIcon: string;         // 'user' | 'senior-m' | 'senior-f' | 'shield' | 'heart' | 'smile'
  avatarUrl?: string;
  iceContactName: string;
  iceContactPhone: string;
  note: string;
  bloodType?: string;
  updatedAt: number;
}

// ==========================================
// WANGIRI SHIELD (ONE-RING & DROP)
// ==========================================
export type WangiriRiskLevel = 'CRITICAL' | 'HIGH' | 'SUSPECTED' | 'SAFE';

export interface WangiriRiskInfo {
  isWangiri: boolean;
  riskLevel: WangiriRiskLevel;
  countryName: string;
  countryFlag: string;
  prefix: string;
  matchedPrefix: string;
  estimatedCostPlnMin: number;
  estimatedCostRange: string;
  isOneRing: boolean;
  durationSec: number;
  ringDurationSec: number;
  isCamouflage: boolean;
  camouflageTarget: string | null;
  explanation: string;
  recommendation: string;
}

export interface WangiriSettings {
  shieldEnabled: boolean;
  autoBlockHighRiskPing: boolean;
  interceptOutgoingCall: boolean;
  alertOnCamouflage: boolean;
  interceptedAttemptsCount: number;
}

// ==========================================
// ROBOCALL INTELLIGENCE & CALL SCREENING
// ==========================================
export type RobocallConfidence = 'CRITICAL' | 'HIGH' | 'MODERATE' | 'LOW';

export interface RobocallIndicator {
  id: string;
  label: string;
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'INFO';
  detail: string;
}

export interface RobocallAnalysis {
  isRobocall: boolean;
  probabilityScore: number; // 0 - 100%
  confidence: RobocallConfidence;
  indicators: RobocallIndicator[];
  botType: string;
  campaignId: string | null;
  campaignName: string | null;
  carrierGuess: string | null;
  telecomAction: 'REJECT' | 'SILENCE' | 'WARN' | 'ALLOW';
  telecomReason: string;
}

export interface RobocallCampaign {
  id: string;
  name: string;
  trunkPrefix: string;
  botType: string;
  detectedDialer: string;
  totalAttempts: number;
  abandonmentRate: number; // % (np. 78%)
  averageRingDurationSec: number;
  firstDetected: number;
  lastDetected: number;
  status: 'ACTIVE_BLOCKED' | 'MONITORING' | 'DISMISSED';
  sampleNumbers: string[];
  riskScore: number;
}

export interface RobocallSettings {
  enabled: boolean;
  action: 'SILENCE' | 'REJECT' | 'WARN';
  minScoreToBlock: number;
  autoClusterDdi: boolean;
  amdSilenceFilter: boolean;
}

