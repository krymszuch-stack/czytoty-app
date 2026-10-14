import React, { useState, useEffect } from 'react';

export interface CallerPurposeInfo {
  name: string;        // np. "Apteka Gemini – Punkt Recepturowy"
  department: string;  // np. "Dział zamówień i dostępności leków rzadkich"
  source?: 'GOOGLE_MAPS' | 'CEIDG' | 'CERTIFIED' | 'VERIFIED_DATABASE' | 'USER_IMPORT' | 'ALARM' | 'KONTAKT' | 'ICE' | string;
  rating?: number;     // np. 4.7
  category?: 'PHARMACY' | 'MEDICAL' | 'BAKERY' | 'POST' | 'BANK' | 'SERVICE' | 'TAXI' | 'GROCERY' | 'EMERGENCY' | 'CONTACT' | 'ICE' | 'DEFAULT' | string;
}

interface DialerDisplayProps {
  digits: string;
  onDigitsChange?: (val: string) => void;
  lookupData: CallerPurposeInfo | null;
  isEmergency?: boolean;
  isBlocked?: boolean;
  largeText?: boolean;
}

const SOURCE_LABELS: Record<string, string> = {
  GOOGLE_MAPS: 'Google',
  VERIFIED_DATABASE: 'Rejestr Firm',
  CEIDG: 'Rejestr Firm',
  CERTIFIED: 'CzyToTy ID',
  USER_IMPORT: 'Import własny',
  ALARM: 'Służby Ratunkowe',
  KONTAKT: 'Kontakty',
  ICE: 'Kontakt ICE'
};

function CategoryIcon({ category }: { category?: string }) {
  const norm = (category || 'DEFAULT').toUpperCase();

  switch (norm) {
    case 'PHARMACY':
      return (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            backgroundColor: 'rgba(110, 231, 183, 0.1)',
            border: '1px solid rgba(110, 231, 183, 0.22)',
            color: '#6EE7B7',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
          title="Apteka / Punkt recepturowy"
        >
          {/* Monochromatyczny wektor pigułki SVG */}
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z" />
            <path d="m8.5 8.5 7 7" />
          </svg>
        </div>
      );

    case 'MEDICAL':
    case 'CLINIC':
      return (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            backgroundColor: 'rgba(147, 197, 253, 0.1)',
            border: '1px solid rgba(147, 197, 253, 0.22)',
            color: '#93C5FD',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
          title="Przychodnia / Szpital"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 4v16m-8-8h16" />
            <rect x="3" y="3" width="18" height="18" rx="4" />
          </svg>
        </div>
      );

    case 'BAKERY':
      return (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            backgroundColor: 'rgba(251, 191, 36, 0.1)',
            border: '1px solid rgba(251, 191, 36, 0.22)',
            color: '#FCD34D',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
          title="Piekarnia / Cukiernia"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M4 14c0 3 3 5 8 5s8-2 8-5a6 6 0 0 0-16 0Z" />
            <path d="M8 12c1-2 2-3 4-3s3 1 4 3" />
          </svg>
        </div>
      );

    case 'POST':
      return (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            backgroundColor: 'rgba(251, 146, 60, 0.1)',
            border: '1px solid rgba(251, 146, 60, 0.22)',
            color: '#FDBA74',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
          title="Poczta / Paczkomat"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect width="20" height="16" x="2" y="4" rx="2" />
            <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
          </svg>
        </div>
      );

    case 'BANK':
      return (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            backgroundColor: 'rgba(52, 211, 153, 0.1)',
            border: '1px solid rgba(52, 211, 153, 0.22)',
            color: '#6EE7B7',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
          title="Bank / Finanse"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <line x1="3" x2="21" y1="22" y2="22" />
            <line x1="6" x2="6" y1="18" y2="11" />
            <line x1="10" x2="10" y1="18" y2="11" />
            <line x1="14" x2="14" y1="18" y2="11" />
            <line x1="18" x2="18" y1="18" y2="11" />
            <polygon points="12 2 20 7 4 7" />
          </svg>
        </div>
      );

    case 'SERVICE':
      return (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            backgroundColor: 'rgba(167, 139, 250, 0.1)',
            border: '1px solid rgba(167, 139, 250, 0.22)',
            color: '#C4B5FD',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
          title="Usługi / Pogotowie techniczne"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
          </svg>
        </div>
      );

    case 'TAXI':
      return (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            backgroundColor: 'rgba(250, 204, 21, 0.1)',
            border: '1px solid rgba(250, 204, 21, 0.22)',
            color: '#FDE047',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
          title="Taxi / Transport"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2" />
            <circle cx="7" cy="17" r="2" />
            <path d="M9 17h6" />
            <circle cx="17" cy="17" r="2" />
          </svg>
        </div>
      );

    case 'GROCERY':
      return (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            backgroundColor: 'rgba(45, 212, 191, 0.1)',
            border: '1px solid rgba(45, 212, 191, 0.22)',
            color: '#5EEAD4',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
          title="Sklep spożywczy / Market"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <circle cx="8" cy="21" r="1" />
            <circle cx="19" cy="21" r="1" />
            <path d="M2.05 2.05h2l2.66 12.42a2 2 0 0 0 2 1.58h9.78a2 2 0 0 0 1.95-1.57l1.65-7.43H5.12" />
          </svg>
        </div>
      );

    case 'EMERGENCY':
      return (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            backgroundColor: 'rgba(239, 68, 68, 0.18)',
            border: '1px solid rgba(239, 68, 68, 0.35)',
            color: '#F87171',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
          title="Służba Ratownicza"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
            <line x1="12" x2="12" y1="8" y2="12" />
            <line x1="12" x2="12.01" y1="16" y2="16" />
          </svg>
        </div>
      );

    case 'ICE':
      return (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            backgroundColor: 'rgba(244, 63, 94, 0.15)',
            border: '1px solid rgba(244, 63, 94, 0.3)',
            color: '#FB7185',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
          title="Kontakt ICE"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z" />
            <path d="M12 9v4" />
            <path d="M10 11h4" />
          </svg>
        </div>
      );

    case 'CONTACT':
      return (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            backgroundColor: 'rgba(6, 182, 212, 0.12)',
            border: '1px solid rgba(6, 182, 212, 0.25)',
            color: '#67E8F9',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
          title="Kontakt z książki telefonicznej"
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
        </div>
      );

    default:
      return (
        <div
          style={{
            width: 32,
            height: 32,
            borderRadius: 10,
            backgroundColor: 'rgba(167, 243, 208, 0.1)',
            border: '1px solid rgba(167, 243, 208, 0.2)',
            color: '#A7F3D0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect width="16" height="20" x="4" y="2" rx="2" />
            <line x1="8" x2="16" y1="6" y2="6" />
            <line x1="8" x2="16" y1="10" y2="10" />
            <line x1="8" x2="12" y1="14" y2="14" />
          </svg>
        </div>
      );
  }
}

export const DialerDisplay: React.FC<DialerDisplayProps> = ({
  digits,
  onDigitsChange,
  lookupData,
  isEmergency = false,
  isBlocked = false,
  largeText = false
}) => {
  const [visibleInfo, setVisibleInfo] = useState<CallerPurposeInfo | null>(null);
  const [isAnimatingIn, setIsAnimatingIn] = useState(false);

  const cleanDigits = digits.replace(/\s+/g, '');

  useEffect(() => {
    // Poniżej 3 cyfr nie pokazujemy informatora (chyba że numer alarmowy o długości 3 cyfr)
    if (cleanDigits.length < 3 || !lookupData) {
      setIsAnimatingIn(false);
      const exitTimer = window.setTimeout(() => setVisibleInfo(null), 100);
      return () => window.clearTimeout(exitTimer);
    }

    // 9 cyfr = pełny numer (0ms opóźnienia) lub natychmiast dla numerów alarmowych. 3-8 cyfr = 220ms debounce na szybkie pisanie
    const delay = cleanDigits.length >= 9 || isEmergency ? 0 : 220;

    const timer = window.setTimeout(() => {
      setVisibleInfo(lookupData);
      requestAnimationFrame(() => setIsAnimatingIn(true));
    }, delay);

    return () => window.clearTimeout(timer);
  }, [cleanDigits, lookupData, isEmergency]);

  return (
    <div
      style={{
        height: 122, // Sztywna wysokość = zero skakania klawiatury pod spodem
        width: '100%',
        maxWidth: 380,
        margin: '0 auto 14px',
        padding: '12px 18px',
        borderRadius: 22,
        background: isEmergency
          ? 'linear-gradient(180deg, rgba(69, 10, 10, 0.82) 0%, rgba(30, 8, 8, 0.95) 100%)'
          : 'linear-gradient(180deg, rgba(30, 22, 54, 0.75) 0%, rgba(19, 14, 36, 0.9) 100%)',
        border: isEmergency
          ? '1px solid rgba(239, 68, 68, 0.7)'
          : visibleInfo
          ? '1px solid rgba(167, 243, 208, 0.25)' // Subtelny miętowy obrys, gdy rozpoznano cel
          : '1px solid rgba(139, 92, 246, 0.18)',
        boxShadow: isEmergency
          ? '0 0 24px rgba(239, 68, 68, 0.25), inset 0 1px 0 0 rgba(255, 255, 255, 0.08)'
          : visibleInfo
          ? 'inset 0 1px 0 0 rgba(255, 255, 255, 0.06), 0 12px 32px -8px rgba(0, 0, 0, 0.5), 0 0 18px rgba(110, 231, 183, 0.08)'
          : 'inset 0 1px 0 0 rgba(255, 255, 255, 0.06), 0 12px 32px -8px rgba(0, 0, 0, 0.5)',
        display: 'grid',
        gridTemplateRows: '44px 1fr',
        alignItems: 'center',
        transition: 'border-color 0.25s ease, box-shadow 0.25s ease, background 0.25s ease',
        boxSizing: 'border-box',
        overflow: 'hidden',
      }}
    >
      {/* GÓRNA STREFA: Informator o celu połączenia (zawsze zarezerwowane 44px) */}
      <div style={{ minWidth: 0, height: 44, display: 'flex', alignItems: 'center' }}>
        {visibleInfo ? (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: 10,
              width: '100%',
              minWidth: 0,
              opacity: isAnimatingIn ? 1 : 0,
              transform: isAnimatingIn ? 'translateY(0) scale(1)' : 'translateY(-6px) scale(0.98)',
              filter: isAnimatingIn ? 'blur(0px)' : 'blur(4px)',
              transition: isAnimatingIn
                ? 'opacity 220ms cubic-bezier(0.22, 1, 0.36, 1), transform 220ms cubic-bezier(0.22, 1, 0.36, 1), filter 220ms ease-out'
                : 'opacity 100ms ease-in, transform 100ms ease-in',
            }}
          >
            {/* Monochromatyczna ikona wektorowa SVG w kwadracie z tłem akcentu */}
            <CategoryIcon category={visibleInfo.category} />

            {/* Kaskadowy tekst: 1 linia = Nazwa + Badge, 2 linia = Cel/Dział */}
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8 }}>
                <span
                  style={{
                    fontSize: 13.5,
                    fontWeight: 600,
                    color: isEmergency ? '#FCA5A5' : '#F8FAFC',
                    letterSpacing: '-0.01em',
                    whiteSpace: 'nowrap',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                  }}
                  title={visibleInfo.name}
                >
                  {visibleInfo.name}
                </span>

                {/* Czysty badge źródła i oceny zamiast "(GOOGLE_MAPS ★ 4.7)" */}
                {(visibleInfo.rating || visibleInfo.source) && (
                  <span
                    style={{
                      display: 'inline-flex',
                      alignItems: 'center',
                      gap: 4,
                      padding: '2px 7px',
                      borderRadius: 99,
                      backgroundColor: isEmergency ? 'rgba(239, 68, 68, 0.16)' : 'rgba(251, 191, 36, 0.12)',
                      border: isEmergency ? '1px solid rgba(239, 68, 68, 0.3)' : '1px solid rgba(251, 191, 36, 0.22)',
                      color: isEmergency ? '#EF4444' : '#FBBF24',
                      fontSize: 10.5,
                      fontWeight: 600,
                      fontVariantNumeric: 'tabular-nums',
                      flexShrink: 0,
                    }}
                  >
                    {visibleInfo.rating && <span>★ {visibleInfo.rating.toFixed(1)}</span>}
                    {visibleInfo.source && (
                      <span
                        style={{
                          color: isEmergency ? '#F87171' : 'rgba(251, 191, 36, 0.78)',
                          fontWeight: 500,
                        }}
                      >
                        {visibleInfo.rating ? `· ${SOURCE_LABELS[visibleInfo.source] || visibleInfo.source}` : (SOURCE_LABELS[visibleInfo.source] || visibleInfo.source)}
                      </span>
                    )}
                  </span>
                )}
              </div>

              {/* Cel połączenia / Dział (maksymalnie 1 linia) */}
              <div
                style={{
                  fontSize: 12,
                  fontWeight: 400,
                  color: isEmergency ? 'rgba(254, 202, 202, 0.8)' : 'rgba(226, 232, 240, 0.62)',
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  marginTop: 2,
                }}
                title={visibleInfo.department}
              >
                {visibleInfo.department}
              </div>
            </div>
          </div>
        ) : (
          /* Subtelny placeholder, gdy brak dopasowania */
          <div
            style={{
              width: '100%',
              textAlign: 'center',
              fontSize: 12,
              fontWeight: 500,
              color: 'rgba(148, 163, 184, 0.35)',
              letterSpacing: '0.02em',
            }}
          >
            {cleanDigits.length > 0 ? 'Weryfikacja numeru w bazie CzyToTy...' : 'Wprowadź numer telefonu'}
          </div>
        )}
      </div>

      {/* DOLNA STREFA: Główny numer telefonu (zawsze w tej samej osi Y) */}
      <div style={{ position: 'relative', width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {isEmergency && (
          <span
            className="text-red-500 font-black text-2xl select-none animate-pulse mr-2"
            title="Połączenie alarmowe"
          >
            !
          </span>
        )}

        <input
          type="text"
          value={digits}
          onChange={e => onDigitsChange?.(e.target.value.slice(0, 40))}
          placeholder="••• ••• •••"
          data-testid="dial_input"
          autoComplete="off"
          spellCheck={false}
          style={{
            width: '100%',
            textAlign: 'center',
            fontSize: cleanDigits.length > 9 ? (largeText ? 30 : 26) : (largeText ? 38 : 34),
            fontWeight: 700,
            color: isEmergency
              ? '#EF4444'
              : cleanDigits.length > 0
              ? '#F8FAFC'
              : 'rgba(167, 139, 250, 0.25)',
            letterSpacing: '0.06em',
            fontVariantNumeric: 'tabular-nums',
            lineHeight: 1,
            padding: '2px 0',
            background: 'transparent',
            border: 'none',
            outline: 'none',
            boxShadow: 'none',
            fontFamily: 'inherit',
            transition: 'font-size 0.15s ease, color 0.15s ease',
            caretColor: '#6EE7B7',
          }}
        />

        {isBlocked && (
          <span
            style={{
              position: 'absolute',
              bottom: -10,
              fontSize: 10,
              fontWeight: 600,
              color: '#FBBF24',
              backgroundColor: 'rgba(251, 191, 36, 0.15)',
              border: '1px solid rgba(251, 191, 36, 0.3)',
              borderRadius: 99,
              padding: '1px 8px',
            }}
          >
            ⚠️ Zablokowany
          </span>
        )}
      </div>
    </div>
  );
};
