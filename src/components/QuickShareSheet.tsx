import React, { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import {
  X,
  Zap,
  RotateCw,
  Share2,
  MessageSquare,
  ShieldCheck,
  Briefcase,
  User,
  Copy,
  Check,
  Download,
  PhoneForwarded,
  Sparkles
} from 'lucide-react';
import { useApp } from '../context/AppContext';

export type SimProfileType = 'PRIVATE' | 'WORK';

export interface UserShareProfile {
  firstName: string;
  lastName: string;
  phone: string;
  company?: string;
  role?: string;
  verifiedBadge?: string;
}

export interface QuickShareSheetProps {
  isOpen: boolean;
  onClose: () => void;
  dialedNumber: string;
  onTriggerDemoCall?: (targetNumber: string, durationMs: number) => void;
  onAppendDemoSms?: (targetNumber: string, body: string, fromSim: SimProfileType) => void;
  onShowToast?: (message: string) => void;
}

export const QuickShareSheet: React.FC<QuickShareSheetProps> = ({
  isOpen,
  onClose,
  dialedNumber,
  onTriggerDemoCall,
  onAppendDemoSms,
  onShowToast
}) => {
  const { userCard, appearance, showNotice } = useApp();

  const [activeSim, setActiveSim] = useState<SimProfileType>('PRIVATE');
  const [isUpsideDown, setIsUpsideDown] = useState<boolean>(true);
  const [isShootingArrow, setIsShootingArrow] = useState<boolean>(false);
  const [copiedVCard, setCopiedVCard] = useState<boolean>(false);
  const [activeFormat, setActiveFormat] = useState<'vcard' | 'tel'>('vcard');

  if (!isOpen) return null;

  // Extract names from userCard if customized
  const [userFirstName = 'Jan', ...restLastName] = (userCard.fullName || 'Jan Kowalski').split(' ');
  const userLastName = restLastName.join(' ') || 'Kowalski';

  const profiles: Record<SimProfileType, UserShareProfile> = {
    PRIVATE: {
      firstName: userFirstName,
      lastName: userLastName,
      phone: userCard.phone || '+48 500 100 200',
      verifiedBadge: 'CzyToTy-ID: #A9F2'
    },
    WORK: {
      firstName: userFirstName,
      lastName: userLastName,
      phone: '+48 600 900 800',
      company: 'Ochrona Połączeń Sp. z o.o.',
      role: 'Architekt Systemów',
      verifiedBadge: 'CzyToTy-PRO: #B4E1'
    }
  };

  const profile = profiles[activeSim];
  const targetNumber = dialedNumber.trim() || '+48 501 234 567';
  const cleanTarget = targetNumber.replace(/\s+/g, '');

  const toast = onShowToast || showNotice;

  // Generowanie vCard 3.0 z sygnaturą certyfikatu CzyToTy
  const generateVCard = (p: UserShareProfile): string => {
    const cleanPhone = p.phone.replace(/\s+/g, '');
    const lines = [
      'BEGIN:VCARD',
      'VERSION:3.0',
      `N;CHARSET=UTF-8:${p.lastName};${p.firstName};;;`,
      `FN;CHARSET=UTF-8:${p.firstName} ${p.lastName}`,
      `TEL;TYPE=CELL:${cleanPhone}`
    ];
    if (p.company) lines.push(`ORG;CHARSET=UTF-8:${p.company}`);
    if (p.role) lines.push(`TITLE;CHARSET=UTF-8:${p.role}`);
    if (p.verifiedBadge) {
      lines.push(`NOTE;CHARSET=UTF-8:Zweryfikowano w systemie CzyToTy (${p.verifiedBadge})`);
    }
    lines.push('END:VCARD');
    return lines.join('\n');
  };

  const buildSmsText = (p: UserShareProfile): string => {
    const orgPart = p.company ? ` (${p.company})` : '';
    const certPart = p.verifiedBadge ? ` [${p.verifiedBadge}]` : '';
    return `Cześć, tu ${p.firstName} ${p.lastName}${orgPart}. Zapisz mój numer: ${p.phone}.${certPart}`;
  };

  const vCardData = generateVCard(profile);
  const smsText = buildSmsText(profile);
  const qrPayload = activeFormat === 'vcard' ? vCardData : `tel:${profile.phone.replace(/\s+/g, '')}`;

  // 1. "Smart-Strzałka" (Symulacja w InCallOverlay + wpis do SmsTab)
  const handleSmartArrowDemo = () => {
    const digitsOnly = cleanTarget.replace(/\D/g, '');
    if (digitsOnly.length < 3) {
      toast('Wpisz najpierw numer odbiorcy na klawiaturze');
      return;
    }

    setIsShootingArrow(true);

    // Odpalamy InCallOverlay na 2.5 sekundy (symulacja 1 dzwonka i rozłączenia)
    onTriggerDemoCall?.(cleanTarget, 2500);

    setTimeout(() => {
      setIsShootingArrow(false);
      onAppendDemoSms?.(cleanTarget, smsText, activeSim);
      toast(`Smart-Strzałka (2,5s) + wizytówka SMS wysłana na ${targetNumber}`);
      onClose();
    }, 2500);
  };

  // 2. Natywny fallback PWA: SMS URI
  const handleNativeSms = () => {
    try {
      const smsUrl = `sms:${cleanTarget}?body=${encodeURIComponent(smsText)}`;
      window.location.href = smsUrl;
      toast('Otwieram systemowy program wiadomości SMS...');
    } catch {
      toast('Nie udało się otworzyć aplikacji SMS.');
    }
  };

  // 3. Udostępnianie .vcf (Web Share API z plikiem lub pobieranie vCard)
  const handleShareVCard = async () => {
    const cleanPhone = profile.phone.replace(/\s+/g, '');
    const fileName = `${profile.firstName}_${profile.lastName}_CzyToTy.vcf`;

    try {
      const blob = new Blob([vCardData], { type: 'text/vcard;charset=utf-8' });
      const file = new File([blob], fileName, { type: 'text/vcard' });

      if (navigator.canShare && navigator.canShare({ files: [file] })) {
        await navigator.share({
          files: [file],
          title: `Wizytówka: ${profile.firstName} ${profile.lastName}`,
          text: `Wizytówka kontaktowa z certyfikatem CzyToTy: ${profile.phone}`
        });
        toast('Udostępniono plik wizytówki .vcf');
        return;
      }
    } catch {
      // Ignored user cancel or fallback
    }

    // Fallback: pobieranie bezpośrednie pliku .vcf
    try {
      const blob = new Blob([vCardData], { type: 'text/vcard;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      toast('Pobrano plik wizytówki vCard (.vcf) na dysk');
    } catch {
      toast('Błąd podczas generowania pliku vCard.');
    }
  };

  const handleCopyVCardText = () => {
    if (navigator.clipboard) {
      navigator.clipboard.writeText(vCardData);
      setCopiedVCard(true);
      toast('Skopiowano treść vCard do schowka.');
      setTimeout(() => setCopiedVCard(false), 2000);
    }
  };

  const accentColor =
    appearance.accent === 'MINT'
      ? '#8BE0BB'
      : appearance.accent === 'OCEAN'
      ? '#60A5FA'
      : '#A875FF';

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/70 backdrop-blur-sm p-0 sm:p-4 animate-in fade-in duration-200">
      <div
        className="w-full max-w-lg bg-[#131424] border border-[#272540] rounded-t-3xl sm:rounded-3xl p-5 sm:p-6 shadow-2xl max-h-[92vh] overflow-y-auto"
        onClick={e => e.stopPropagation()}
      >
        {/* Top Header */}
        <div className="flex items-center justify-between pb-3 border-b border-[#272540]">
          <div className="flex items-center gap-2.5">
            <div className="w-9 h-9 rounded-xl bg-[#7B4DFF]/20 border border-[#7B4DFF]/40 flex items-center justify-center text-[#A875FF]">
              <Zap className="w-5 h-5 fill-current" />
            </div>
            <div>
              <h2 className="text-lg font-bold text-white flex items-center gap-2">
                Smart-Strzałka & Wizytówka
                <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-[#8BE0BB]/20 text-[#8BE0BB] border border-[#8BE0BB]/30">
                  Patent
                </span>
              </h2>
              <p className="text-xs text-[#9499AD]">
                Błyskawiczna wymiana numeru z certyfikatem CzyToTy
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 text-[#9499AD] hover:text-white rounded-full hover:bg-white/5 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-5 pt-4">
          {/* SIM Profile Switcher */}
          <div>
            <div className="flex items-center justify-between mb-2">
              <label className="text-xs font-bold uppercase tracking-wider text-[#9499AD]">
                Profil nadawcy (Dual SIM)
              </label>
              <span className="text-xs font-semibold text-[#A875FF] flex items-center gap-1">
                <ShieldCheck className="w-3.5 h-3.5" />
                {profile.verifiedBadge}
              </span>
            </div>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setActiveSim('PRIVATE')}
                className={`p-3 rounded-2xl border text-left transition-all ${
                  activeSim === 'PRIVATE'
                    ? 'border-[#7B4DFF] bg-[#231A45] text-white shadow-lg shadow-[#7B4DFF]/15'
                    : 'border-[#272540] bg-[#1A1B30] text-[#9499AD] hover:text-white'
                }`}
              >
                <div className="flex items-center gap-2 mb-1">
                  <User className="w-4 h-4 text-[#A875FF]" />
                  <span className="text-xs font-bold text-white">SIM 1 · Prywatny</span>
                </div>
                <p className="text-sm font-semibold truncate">
                  {profiles.PRIVATE.firstName} {profiles.PRIVATE.lastName}
                </p>
                <p className="text-[11px] font-mono text-[#9499AD]">{profiles.PRIVATE.phone}</p>
              </button>

              <button
                type="button"
                onClick={() => setActiveSim('WORK')}
                className={`p-3 rounded-2xl border text-left transition-all ${
                  activeSim === 'WORK'
                    ? 'border-[#7B4DFF] bg-[#231A45] text-white shadow-lg shadow-[#7B4DFF]/15'
                    : 'border-[#272540] bg-[#1A1B30] text-[#9499AD] hover:text-white'
                }`}
              >
                <div className="flex items-center gap-2 mb-1">
                  <Briefcase className="w-4 h-4 text-[#8BE0BB]" />
                  <span className="text-xs font-bold text-white">SIM 2 · Służbowy</span>
                </div>
                <p className="text-sm font-semibold truncate">
                  {profiles.WORK.company || 'Firma'}
                </p>
                <p className="text-[11px] font-mono text-[#9499AD]">{profiles.WORK.phone}</p>
              </button>
            </div>
          </div>

          {/* Sekcja 1: Smart-Strzałka (Demo Call 2.5s + Auto-SMS) */}
          <div className="p-4 rounded-2xl bg-gradient-to-br from-[#1C1A38] to-[#121324] border border-[#7B4DFF]/30 space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-xs font-extrabold uppercase tracking-wider text-purple-300 flex items-center gap-1.5">
                <Sparkles className="w-3.5 h-3.5 text-[#FFD089]" />
                Tryb 1: Smart-Strzałka (2,5s Sygnał + SMS)
              </span>
              <span className="text-[11px] text-[#9499AD]">Odbiorca: {targetNumber}</span>
            </div>

            <p className="text-xs text-purple-200/90 leading-relaxed">
              Zestawia 1 krótki sygnał (2,5 sekundy), po czym natychmiast dostarcza Twoją
              certyfikowaną wizytówkę SMS do skrzynki odbiorcy.
            </p>

            <button
              type="button"
              onClick={handleSmartArrowDemo}
              disabled={isShootingArrow}
              className="w-full py-3.5 px-4 rounded-xl bg-gradient-to-r from-[#7B4DFF] to-[#9955FF] hover:from-[#895cf8] hover:to-[#a768ff] active:scale-[0.98] text-white font-bold text-sm shadow-xl shadow-[#7B4DFF]/25 flex items-center justify-center gap-2 transition-all disabled:opacity-50"
            >
              {isShootingArrow ? (
                <>
                  <PhoneForwarded className="w-5 h-5 animate-spin text-[#FFD089]" />
                  <span>Trwa 1 sygnał (2,5s) & wysyłka SMS...</span>
                </>
              ) : (
                <>
                  <Zap className="w-5 h-5 fill-current text-[#FFD089]" />
                  <span>Wyślij Smart-Strzałkę na {targetNumber}</span>
                </>
              )}
            </button>
          </div>

          {/* Sekcja 2: Odwrócony Kod QR (dla rozmówcy naprzeciwko) */}
          <div className="p-4 rounded-2xl bg-[#1A1B30]/90 border border-[#272540] flex flex-col items-center text-center space-y-3">
            <div className="w-full flex items-center justify-between">
              <span className="text-xs font-extrabold uppercase tracking-wider text-[#9499AD]">
                Tryb 2: Odwrócony kod QR
              </span>
              <button
                type="button"
                onClick={() => setIsUpsideDown(prev => !prev)}
                className="flex items-center gap-1.5 py-1 px-2.5 rounded-lg text-xs font-semibold bg-[#22233C] text-purple-300 hover:text-white hover:bg-[#282a4d] transition-all"
                title="Obróć kod o 180 stopni"
              >
                <RotateCw className="w-3.5 h-3.5" />
                <span>{isUpsideDown ? 'Odwróć dla siebie' : 'Odwróć dla rozmówcy'}</span>
              </button>
            </div>

            {/* Informacja o orientacji */}
            <p className="text-[11px] text-[#9499AD] max-w-xs">
              {isUpsideDown
                ? '🔄 Kod jest obrócony o 180° – osoba siedząca naprzeciwko może od razu nakierować aparat.'
                : 'Kod wyświetlany jest w standardowej orientacji.'}
            </p>

            {/* Kontener z QR kodem z obrotem */}
            <div className="p-4 bg-white rounded-2xl shadow-xl flex items-center justify-center my-1 transition-transform duration-300">
              <div
                style={{
                  transform: isUpsideDown ? 'rotate(180deg)' : 'none',
                  transition: 'transform 0.3s ease'
                }}
              >
                <QRCodeSVG
                  value={qrPayload}
                  size={170}
                  level="M"
                  includeMargin={false}
                  bgColor="#FFFFFF"
                  fgColor="#0F101A"
                />
              </div>
            </div>

            {/* Przełącznik formatu QR */}
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => setActiveFormat('vcard')}
                className={`py-1 px-3 rounded-lg text-xs font-semibold transition-all ${
                  activeFormat === 'vcard'
                    ? 'bg-[#7B4DFF] text-white'
                    : 'bg-[#22233C] text-[#9499AD] hover:text-white'
                }`}
              >
                Format vCard 3.0
              </button>
              <button
                type="button"
                onClick={() => setActiveFormat('tel')}
                className={`py-1 px-3 rounded-lg text-xs font-semibold transition-all ${
                  activeFormat === 'tel'
                    ? 'bg-[#7B4DFF] text-white'
                    : 'bg-[#22233C] text-[#9499AD] hover:text-white'
                }`}
              >
                Format Bezpośredni tel:
              </button>
            </div>
          </div>

          {/* Sekcja 3: Fizyczne kanały PWA / Systemowe */}
          <div className="space-y-2">
            <label className="text-xs font-bold uppercase tracking-wider text-[#9499AD] block">
              Tryb 3: Udostępnianie systemowe (PWA & Plik .vcf)
            </label>

            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={handleShareVCard}
                className="p-3 rounded-xl bg-[#1A1B30] hover:bg-[#22233C] border border-[#272540] text-white flex items-center justify-center gap-2 text-xs font-semibold transition-all"
              >
                <Share2 className="w-4 h-4 text-[#8BE0BB]" />
                <span>Udostępnij wizytówkę (.vcf)</span>
              </button>

              <button
                type="button"
                onClick={handleNativeSms}
                className="p-3 rounded-xl bg-[#1A1B30] hover:bg-[#22233C] border border-[#272540] text-white flex items-center justify-center gap-2 text-xs font-semibold transition-all"
              >
                <MessageSquare className="w-4 h-4 text-[#60A5FA]" />
                <span>Natywny SMS (sms:)</span>
              </button>
            </div>

            <button
              type="button"
              onClick={handleCopyVCardText}
              className="w-full py-2 px-3 rounded-xl bg-[#1A1B30]/60 hover:bg-[#1A1B30] border border-[#272540]/60 text-[#9499AD] hover:text-white flex items-center justify-center gap-2 text-xs font-medium transition-all"
            >
              {copiedVCard ? (
                <>
                  <Check className="w-3.5 h-3.5 text-[#8BE0BB]" />
                  <span className="text-[#8BE0BB]">Skopiowano tekst vCard</span>
                </>
              ) : (
                <>
                  <Copy className="w-3.5 h-3.5" />
                  <span>Kopiuj dane vCard do schowka</span>
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
