import React, { useState } from 'react';
import {
  Cloud,
  Lock,
  Mail,
  Key,
  LogOut,
  Download,
  Trash2,
  ExternalLink,
  Info,
  CheckCircle2,
  Database,
  User,
  Heart,
  Phone,
  MessageSquare,
  ShieldAlert,
  Copy,
  Check,
  Edit3,
  Camera,
  Share2,
  Zap
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { UserBusinessCard } from '../types';
import { QuickShareSheet } from '../components/QuickShareSheet';

export function AccountTab() {
  const {
    cloudStatus,
    cloudUserEmail,
    cloudNotes,
    isCloudBusy,
    checkCloudConnection,
    authenticateCloud,
    signOutCloud,
    loadCloudNotes,
    deleteCloudNote,
    restoreToLocal,
    userCard,
    saveUserCard,
    callIceContact,
    smsIceContact,
    showNotice,
    triggerSmartArrowCall,
    appendDirectSms
  } = useApp();

  const [activeSubTab, setActiveSubTab] = useState<'CARD' | 'CLOUD'>('CARD');
  const [isEditingCard, setIsEditingCard] = useState<boolean>(false);
  const [copiedCard, setCopiedCard] = useState<boolean>(false);
  const [isShareSheetOpen, setIsShareSheetOpen] = useState<boolean>(false);

  // Card Form State
  const [fullName, setFullName] = useState(userCard.fullName);
  const [phone, setPhone] = useState(userCard.phone);
  const [avatarIcon, setAvatarIcon] = useState(userCard.avatarIcon || 'senior-m');
  const [avatarUrl, setAvatarUrl] = useState(userCard.avatarUrl || '');
  const [iceContactName, setIceContactName] = useState(userCard.iceContactName || '');
  const [iceContactPhone, setIceContactPhone] = useState(userCard.iceContactPhone || '');
  const [bloodType, setBloodType] = useState(userCard.bloodType || 'A Rh+');
  const [note, setNote] = useState(userCard.note || '');

  // Auth Form State
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const handleLogin = (isRegister: boolean) => {
    if (!email.includes('@') || password.length < 8) return;
    authenticateCloud(email, password, isRegister);
    setPassword('');
  };

  const handleSaveCard = (e: React.FormEvent) => {
    e.preventDefault();
    const updated: UserBusinessCard = {
      fullName: fullName.trim() || 'Użytkownik',
      phone: phone.trim() || '+48',
      avatarIcon,
      avatarUrl: avatarUrl.trim(),
      iceContactName: iceContactName.trim(),
      iceContactPhone: iceContactPhone.trim(),
      bloodType,
      note: note.trim(),
      updatedAt: Date.now()
    };
    saveUserCard(updated);
    setIsEditingCard(false);
  };

  const handleCopyCardText = () => {
    const text = `📇 WIZYTÓWKA UŻYTKOWNIKA (CzyToTy?):
Imię i nazwisko: ${userCard.fullName}
Telefon: ${userCard.phone}
Grupa krwi: ${userCard.bloodType || 'Brak danych'}
Kontakt ratunkowy ICE: ${userCard.iceContactName} (${userCard.iceContactPhone})
Informacje medyczne / notatka: ${userCard.note}`;

    if (navigator.clipboard) {
      navigator.clipboard.writeText(text);
      setCopiedCard(true);
      showNotice('Skopiowano treść wizytówki do schowka.');
      setTimeout(() => setCopiedCard(false), 2500);
    }
  };

  const avatarPresets = [
    { id: 'senior-m', emoji: '👨‍🦳', label: 'Senior Jan' },
    { id: 'senior-f', emoji: '👩‍🦳', label: 'Seniorka Maria' },
    { id: 'shield', emoji: '🛡️', label: 'Ochrona' },
    { id: 'business', emoji: '💼', label: 'Biznes' },
    { id: 'heart', emoji: '❤️', label: 'Zdrowie' },
    { id: 'smile', emoji: '😊', label: 'Uśmiech' }
  ];

  const getAvatarDisplay = () => {
    if (userCard.avatarUrl) {
      return (
        <img
          src={userCard.avatarUrl}
          alt={userCard.fullName}
          className="w-16 h-16 rounded-2xl object-cover border-2 border-[#7B4DFF]"
          onError={e => {
            (e.target as HTMLElement).style.display = 'none';
          }}
        />
      );
    }
    const preset = avatarPresets.find(p => p.id === userCard.avatarIcon);
    return (
      <div className="w-16 h-16 rounded-2xl bg-[#7B4DFF]/20 border-2 border-[#7B4DFF]/40 flex items-center justify-center text-3xl shadow-inner">
        {preset ? preset.emoji : '👤'}
      </div>
    );
  };

  return (
    <div className="flex flex-col h-full space-y-4 overflow-y-auto pr-1 pb-24">
      {/* Header & Sub-Tabs */}
      <div>
        <h2 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
          Profil & Wizytówka
        </h2>
        <p className="text-xs text-[#9499AD] mt-0.5">
          Twoja cyfrowa tożsamość, kontakt ratunkowy ICE i opcjonalna chmura
        </p>
      </div>

      {/* Segmented Switcher */}
      <div className="bg-[#1A1B30]/70 p-1 rounded-2xl flex border border-[#272540]">
        <button
          onClick={() => setActiveSubTab('CARD')}
          className={`flex-1 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all ${
            activeSubTab === 'CARD'
              ? 'bg-[#7B4DFF] text-white shadow-md'
              : 'text-[#9499AD] hover:text-white'
          }`}
        >
          Moja Wizytówka & ICE
        </button>
        <button
          onClick={() => setActiveSubTab('CLOUD')}
          className={`flex-1 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all ${
            activeSubTab === 'CLOUD'
              ? 'bg-[#7B4DFF] text-white shadow-md'
              : 'text-[#9499AD] hover:text-white'
          }`}
        >
          Konto i Chmura
        </button>
      </div>

      {/* SUBTAB 1: WIZYTÓWKA UŻYTKOWNIKA & KONTAKT ICE */}
      {activeSubTab === 'CARD' && (
        <div className="space-y-4">
          {/* Cyfrowa Karta Wizytówki (Live Card Preview) */}
          <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-[#1C1635] via-[#131424] to-[#0D0E1A] border-2 border-[#7B4DFF]/40 p-5 shadow-2xl space-y-4">
            <div className="flex items-start justify-between">
              <div className="flex items-center gap-3.5">
                {getAvatarDisplay()}
                <div>
                  <h3 className="text-xl font-black text-white tracking-tight">
                    {userCard.fullName}
                  </h3>
                  <p className="text-sm font-mono text-[#A875FF] font-semibold">
                    {userCard.phone}
                  </p>
                  {userCard.bloodType && (
                    <span className="inline-block mt-1 text-[10px] font-bold px-2 py-0.5 rounded-md bg-rose-950/60 text-rose-300 border border-rose-500/30">
                      🩸 Grupa krwi: {userCard.bloodType}
                    </span>
                  )}
                </div>
              </div>

              <button
                onClick={() => setIsEditingCard(prev => !prev)}
                className="py-1.5 px-3 rounded-xl bg-[#272540] hover:bg-[#343254] text-xs font-semibold text-white flex items-center gap-1.5 border border-[#443F6E] transition-all"
              >
                <Edit3 className="w-3.5 h-3.5 text-[#A875FF]" />
                <span>{isEditingCard ? 'Zamknij' : 'Edytuj'}</span>
              </button>
            </div>

            {/* Sekcja Kontaktu Ratunkowego ICE (In Case of Emergency) */}
            <div className="bg-[#1A172F]/90 rounded-2xl p-3.5 border border-rose-500/40 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-1.5 text-rose-400 font-extrabold text-xs">
                  <ShieldAlert className="w-4 h-4 text-rose-400" />
                  <span>KONTAKT W RAZIE WYPADKU (ICE)</span>
                </div>
                <span className="text-[10px] text-rose-300/80 font-mono">
                  Dla służb medycznych
                </span>
              </div>

              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-bold text-white">
                    {userCard.iceContactName || 'Brak wpisanego kontaktu'}
                  </p>
                  <p className="text-xs font-mono text-[#9499AD]">
                    {userCard.iceContactPhone || 'Uzupełnij numer telefonu'}
                  </p>
                </div>

                {userCard.iceContactPhone && (
                  <div className="flex items-center gap-2">
                    <button
                      onClick={callIceContact}
                      title="Zadzwoń do kontaktu ICE"
                      className="p-2.5 rounded-xl bg-[#8BE0BB] hover:bg-[#78cfab] text-[#10392A] font-bold shadow-md transition-all active:scale-95"
                    >
                      <Phone className="w-4 h-4 fill-current" />
                    </button>
                    <button
                      onClick={smsIceContact}
                      title="Napisz SMS do kontaktu ICE"
                      className="p-2.5 rounded-xl bg-[#7B4DFF] hover:bg-[#6a3ee0] text-white font-bold shadow-md transition-all active:scale-95"
                    >
                      <MessageSquare className="w-4 h-4" />
                    </button>
                  </div>
                )}
              </div>
            </div>

            {/* Osobista Notatka / Medyczna */}
            {userCard.note && (
              <div className="bg-[#131424]/80 p-3 rounded-2xl border border-[#272540] space-y-1">
                <span className="text-[10px] uppercase tracking-wider text-[#9499AD] font-bold">
                  Informacje zdrowotne / Notatka
                </span>
                <p className="text-xs text-white/90 leading-relaxed">
                  {userCard.note}
                </p>
              </div>
            )}

            {/* Przycisk Kopiuj / Udostępnij */}
            <div className="pt-1 flex items-center justify-between gap-2 flex-wrap">
              <span className="text-[10px] text-[#6C6F85]">
                Zaktualizowano: {new Date(userCard.updatedAt).toLocaleDateString('pl-PL')}
              </span>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setIsShareSheetOpen(true)}
                  className="py-1.5 px-3 rounded-xl bg-[#7B4DFF]/20 hover:bg-[#7B4DFF]/30 text-xs font-semibold text-[#CBB2FF] flex items-center gap-1.5 border border-[#7B4DFF]/40 transition-all shadow-sm"
                >
                  <Zap className="w-3.5 h-3.5 text-[#FFD089] fill-current" />
                  <span>Smart-Strzałka & QR</span>
                </button>
                <button
                  type="button"
                  onClick={handleCopyCardText}
                  className="py-1.5 px-3 rounded-xl bg-white/5 hover:bg-white/10 text-xs font-semibold text-[#A875FF] flex items-center gap-1.5 transition-all"
                >
                  {copiedCard ? (
                    <>
                      <Check className="w-3.5 h-3.5 text-emerald-400" />
                      <span className="text-emerald-400">Skopiowano</span>
                    </>
                  ) : (
                    <>
                      <Copy className="w-3.5 h-3.5" />
                      <span>Kopiuj</span>
                    </>
                  )}
                </button>
              </div>
            </div>
          </div>

          {/* Formularz Edycji Wizytówki */}
          {isEditingCard && (
            <form
              onSubmit={handleSaveCard}
              className="p-5 rounded-3xl bg-[#131424] border border-[#272540] space-y-4 shadow-xl animate-in fade-in-50 duration-200"
            >
              <div className="flex items-center justify-between border-b border-[#272540] pb-2">
                <h4 className="text-sm font-bold text-white flex items-center gap-2">
                  <User className="w-4 h-4 text-[#A875FF]" />
                  <span>Edycja Wizytówki i Danych ICE</span>
                </h4>
                <span className="text-[11px] text-[#9499AD]">Zapis na stałe</span>
              </div>

              {/* Wybór Awatara */}
              <div className="space-y-1.5">
                <label className="text-xs text-[#9499AD] font-semibold block">
                  Wybierz profilowe / awatar
                </label>
                <div className="grid grid-cols-3 sm:grid-cols-6 gap-2">
                  {avatarPresets.map(preset => (
                    <button
                      type="button"
                      key={preset.id}
                      onClick={() => {
                        setAvatarIcon(preset.id);
                        setAvatarUrl('');
                      }}
                      className={`p-2 rounded-xl flex flex-col items-center justify-center border transition-all ${
                        avatarIcon === preset.id && !avatarUrl
                          ? 'bg-[#7B4DFF]/30 border-[#A875FF] ring-2 ring-[#7B4DFF]'
                          : 'bg-[#1A1B30] border-[#272540] hover:bg-[#22233C]'
                      }`}
                    >
                      <span className="text-2xl">{preset.emoji}</span>
                      <span className="text-[10px] text-[#9499AD] mt-1 font-medium">
                        {preset.label}
                      </span>
                    </button>
                  ))}
                </div>

                <div className="pt-1">
                  <input
                    type="url"
                    value={avatarUrl}
                    onChange={e => setAvatarUrl(e.target.value)}
                    placeholder="Lub wklej link do zdjęcia profilowego (URL)"
                    className="w-full px-3 py-1.5 rounded-xl bg-[#1A1B30] border border-[#272540] text-xs text-white placeholder:text-[#6C6F85] focus:outline-none focus:border-[#7B4DFF]"
                  />
                </div>
              </div>

              {/* Imię i Nazwisko oraz Telefon */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-xs text-[#9499AD] font-semibold block mb-1">
                    Twoje imię i nazwisko
                  </label>
                  <input
                    type="text"
                    value={fullName}
                    onChange={e => setFullName(e.target.value)}
                    required
                    placeholder="np. Jan Kowalski"
                    className="w-full px-3 py-2 rounded-xl bg-[#1A1B30] border border-[#272540] text-sm text-white focus:outline-none focus:border-[#7B4DFF]"
                  />
                </div>

                <div>
                  <label className="text-xs text-[#9499AD] font-semibold block mb-1">
                    Twój numer telefonu
                  </label>
                  <input
                    type="tel"
                    value={phone}
                    onChange={e => setPhone(e.target.value)}
                    required
                    placeholder="+48 600 123 456"
                    className="w-full px-3 py-2 rounded-xl bg-[#1A1B30] border border-[#272540] text-sm text-white font-mono focus:outline-none focus:border-[#7B4DFF]"
                  />
                </div>
              </div>

              {/* Sekcja ICE (In Case of Emergency) */}
              <div className="p-3.5 rounded-2xl bg-rose-950/20 border border-rose-500/30 space-y-3">
                <span className="text-xs font-bold text-rose-300 flex items-center gap-1.5">
                  <ShieldAlert className="w-3.5 h-3.5 text-rose-400" />
                  Kontakt w razie nagłego wypadku (ICE)
                </span>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="text-[11px] text-[#9499AD] block mb-1">
                      Osoba najbliższa (Imię / Pokrewieństwo)
                    </label>
                    <input
                      type="text"
                      value={iceContactName}
                      onChange={e => setIceContactName(e.target.value)}
                      placeholder="np. Córka Anna"
                      className="w-full px-3 py-1.5 rounded-xl bg-[#131424] border border-[#272540] text-xs text-white focus:outline-none focus:border-rose-400"
                    />
                  </div>

                  <div>
                    <label className="text-[11px] text-[#9499AD] block mb-1">
                      Numer telefonu ICE
                    </label>
                    <input
                      type="tel"
                      value={iceContactPhone}
                      onChange={e => setIceContactPhone(e.target.value)}
                      placeholder="+48 601 234 567"
                      className="w-full px-3 py-1.5 rounded-xl bg-[#131424] border border-[#272540] text-xs text-white font-mono focus:outline-none focus:border-rose-400"
                    />
                  </div>
                </div>
              </div>

              {/* Grupa krwi i Notatka medyczna */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                <div>
                  <label className="text-xs text-[#9499AD] font-semibold block mb-1">
                    Grupa krwi
                  </label>
                  <select
                    value={bloodType}
                    onChange={e => setBloodType(e.target.value)}
                    className="w-full px-3 py-2 rounded-xl bg-[#1A1B30] border border-[#272540] text-sm text-white focus:outline-none focus:border-[#7B4DFF]"
                  >
                    <option value="A Rh+">A Rh+</option>
                    <option value="A Rh-">A Rh-</option>
                    <option value="0 Rh+">0 Rh+</option>
                    <option value="0 Rh-">0 Rh-</option>
                    <option value="B Rh+">B Rh+</option>
                    <option value="B Rh-">B Rh-</option>
                    <option value="AB Rh+">AB Rh+</option>
                    <option value="AB Rh-">AB Rh-</option>
                    <option value="Nieznana">Nieznana</option>
                  </select>
                </div>

                <div className="sm:col-span-2">
                  <label className="text-xs text-[#9499AD] font-semibold block mb-1">
                    Notatka medyczna / Informacje dla ratowników
                  </label>
                  <input
                    type="text"
                    value={note}
                    onChange={e => setNote(e.target.value)}
                    placeholder="np. Cukrzyca, alergia na penicylinę, rozrusznik serca..."
                    className="w-full px-3 py-2 rounded-xl bg-[#1A1B30] border border-[#272540] text-xs text-white focus:outline-none focus:border-[#7B4DFF]"
                  />
                </div>
              </div>

              <div className="flex gap-2 pt-2">
                <button
                  type="submit"
                  className="flex-1 py-2.5 rounded-xl bg-[#7B4DFF] hover:bg-[#6a3ee0] text-white text-xs font-bold shadow-lg transition-all"
                >
                  Zapisz wizytówkę na stałe
                </button>
                <button
                  type="button"
                  onClick={() => setIsEditingCard(false)}
                  className="py-2.5 px-4 rounded-xl bg-[#1A1B30] hover:bg-[#22233C] text-[#9499AD] text-xs font-semibold border border-[#272540]"
                >
                  Anuluj
                </button>
              </div>
            </form>
          )}
        </div>
      )}

      {/* SUBTAB 2: KONTO I SYNCHRONIZACJA W CHMURZE (Supabase) */}
      {activeSubTab === 'CLOUD' && (
        <div className="space-y-4">
          {/* Supabase Connection Status Card */}
          <div className="p-4 rounded-2xl bg-[#131424] border border-[#272540] space-y-3 shadow-md">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Database className="w-4 h-4 text-[#A875FF]" />
                <h3 className="text-sm font-bold text-white">
                  Supabase · czytoty-app
                </h3>
              </div>
              <span className="text-[11px] text-[#9499AD] font-mono">
                PostgreSQL + Auth
              </span>
            </div>

            <p className="text-xs text-[#9499AD]">{cloudStatus}</p>

            {isCloudBusy && (
              <div className="w-full bg-[#1A1B30] h-1.5 rounded-full overflow-hidden">
                <div className="bg-[#7B4DFF] h-full w-1/2 animate-pulse" />
              </div>
            )}

            <button
              onClick={checkCloudConnection}
              disabled={isCloudBusy}
              className="w-full py-2 px-3 rounded-xl bg-[#1A1B30] hover:bg-[#22233C] border border-[#272540] text-xs font-semibold text-white/90 flex items-center justify-center gap-2 transition-all disabled:opacity-50"
            >
              <span>Sprawdź połączenie z bazą</span>
            </button>
          </div>

          {/* Authentication Form or Logged In View */}
          {!cloudUserEmail ? (
            <div className="p-5 rounded-3xl bg-[#131424] border border-[#272540] space-y-4 shadow-xl">
              <div className="flex items-center gap-2">
                <Lock className="w-4 h-4 text-[#A875FF]" />
                <h3 className="text-sm font-bold text-white">Logowanie do konta</h3>
              </div>

              <div className="space-y-3">
                <div>
                  <label className="text-xs text-[#9499AD] block mb-1">
                    Adres e-mail
                  </label>
                  <div className="relative">
                    <Mail className="w-4 h-4 absolute left-3 top-3 text-[#9499AD]" />
                    <input
                      type="email"
                      value={email}
                      onChange={e => setEmail(e.target.value)}
                      placeholder="twoj@email.pl"
                      className="w-full pl-9 pr-3 py-2 rounded-xl bg-[#1A1B30] border border-[#272540] text-sm text-white focus:outline-none focus:border-[#7B4DFF]"
                    />
                  </div>
                </div>

                <div>
                  <label className="text-xs text-[#9499AD] block mb-1">
                    Hasło (minimum 8 znaków)
                  </label>
                  <div className="relative">
                    <Key className="w-4 h-4 absolute left-3 top-3 text-[#9499AD]" />
                    <input
                      type="password"
                      value={password}
                      onChange={e => setPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full pl-9 pr-3 py-2 rounded-xl bg-[#1A1B30] border border-[#272540] text-sm text-white focus:outline-none focus:border-[#7B4DFF]"
                    />
                  </div>
                </div>

                <div className="space-y-2 pt-2">
                  <button
                    onClick={() => handleLogin(false)}
                    disabled={isCloudBusy || !email.includes('@') || password.length < 8}
                    className="w-full py-2.5 px-4 rounded-xl bg-[#7B4DFF] hover:bg-[#6a3ee0] disabled:opacity-40 text-white text-xs font-semibold shadow-md transition-all"
                  >
                    Zaloguj się
                  </button>

                  <button
                    onClick={() => handleLogin(true)}
                    disabled={isCloudBusy || !email.includes('@') || password.length < 8}
                    className="w-full py-2.5 px-4 rounded-xl bg-[#1A1B30] hover:bg-[#22233C] border border-[#272540] disabled:opacity-40 text-white text-xs font-semibold transition-all"
                  >
                    Utwórz konto e-mail
                  </button>
                </div>

                <p className="text-[11px] text-[#6C6F85] leading-relaxed">
                  Rejestracja może wymagać potwierdzenia adresu przez wiadomość Supabase. Po potwierdzeniu wróć tutaj i zaloguj się.
                </p>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-[#131424] border border-[#272540] flex items-center justify-between">
                <div>
                  <span className="text-[11px] text-[#9499AD]">Zalogowano jako:</span>
                  <p className="text-sm font-bold text-white font-mono">
                    {cloudUserEmail}
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={loadCloudNotes}
                    disabled={isCloudBusy}
                    className="py-1.5 px-3 rounded-xl bg-[#1A1B30] hover:bg-[#22233C] border border-[#272540] text-xs font-semibold text-white/90 flex items-center gap-1.5 transition-all"
                  >
                    <Download className="w-3.5 h-3.5 text-[#A875FF]" />
                    <span>Pobierz</span>
                  </button>
                  <button
                    onClick={signOutCloud}
                    disabled={isCloudBusy}
                    className="py-1.5 px-3 rounded-xl bg-[#281419] hover:bg-[#3d1821] border border-[#EF5350]/30 text-xs font-semibold text-[#FF8596] flex items-center gap-1.5 transition-all"
                  >
                    <LogOut className="w-3.5 h-3.5" />
                    <span>Wyloguj</span>
                  </button>
                </div>
              </div>

              {/* Cloud Notes List */}
              <div className="space-y-3">
                <h3 className="text-base font-bold text-white">
                  Notatki na koncie · {cloudNotes.length} (do 100)
                </h3>

                {cloudNotes.length === 0 ? (
                  <p className="text-xs text-[#9499AD] py-6 text-center">
                    Brak pobranych notatek. Zapisz wybraną notatkę w zakładce Numery.
                  </p>
                ) : (
                  <div className="space-y-2.5">
                    {cloudNotes.map(n => (
                      <div
                        key={n.phone}
                        className="p-4 rounded-2xl bg-[#131424] border border-[#272540] space-y-2"
                      >
                        <div>
                          <h4 className="text-sm font-bold text-white">
                            {n.label || n.phone}
                          </h4>
                          <p className="text-xs font-mono text-[#9499AD]">{n.phone}</p>
                        </div>
                        {n.note && (
                          <p className="text-xs text-[#E8DEF8] bg-[#1A1B30]/60 p-2.5 rounded-xl border border-[#272540]/60">
                            {n.note}
                          </p>
                        )}
                        <div className="flex items-center gap-3 pt-1 text-xs">
                          <button
                            onClick={() => restoreToLocal(n)}
                            className="text-[#A875FF] hover:underline font-medium"
                          >
                            Kopiuj na urządzenie
                          </button>
                          <button
                            onClick={() => deleteCloudNote(n.phone)}
                            className="text-[#EF5350] hover:underline font-medium"
                          >
                            Usuń z chmury
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* InfoCard */}
          <div className="p-4 rounded-2xl bg-[#131424] border border-[#272540] space-y-1.5">
            <div className="flex items-center gap-2 text-white font-bold text-xs">
              <Info className="w-4 h-4 text-[#A875FF]" />
              <span>Co trafia do chmury?</span>
            </div>
            <p className="text-xs text-[#9499AD] leading-relaxed">
              Tylko numer, etykieta i notatka, które zapiszesz przyciskiem „Zapisz w chmurze”. Dane profilowe i wizytówka ICE są zapisywane lokalnie w Twojej przeglądarce i nie są wysyłane na serwer bez Twojej zgody.
            </p>
          </div>
        </div>
      )}

      {/* Footer Cert reference */}
      <div className="pt-2 text-center space-y-1">
        <a
          href="https://cert.pl/lista-ostrzezen/"
          target="_blank"
          rel="noreferrer"
          className="text-xs text-[#A875FF] hover:underline inline-flex items-center gap-1"
        >
          <span>Źródło danych bezpieczeństwa: CERT Polska / NASK</span>
          <ExternalLink className="w-3 h-3" />
        </a>
        <p className="text-[11px] text-[#6C6F85]">
          0.6.0 · CzyToTy? Senior Friendly Edition
        </p>
      </div>

      {/* Quick Share Sheet (Smart-Strzałka & Odwrócony QR) */}
      <QuickShareSheet
        isOpen={isShareSheetOpen}
        onClose={() => setIsShareSheetOpen(false)}
        dialedNumber={userCard.phone || ''}
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
    </div>
  );
}
