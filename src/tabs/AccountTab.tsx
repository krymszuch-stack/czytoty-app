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
  Database
} from 'lucide-react';
import { useApp } from '../context/AppContext';

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
    restoreToLocal
  } = useApp();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const handleLogin = (isRegister: boolean) => {
    if (!email.includes('@') || password.length < 8) return;
    authenticateCloud(email, password, isRegister);
    setPassword('');
  };

  return (
    <div className="flex flex-col h-full space-y-6 overflow-y-auto pr-1 pb-20">
      <div>
        <h2 className="text-3xl font-extrabold text-white tracking-tight">
          Prywatne notatki w chmurze
        </h2>
        <p className="text-sm text-[#9499AD] mt-1">
          Konto jest opcjonalne. Sprawdzanie treści i blokady działają bez logowania.
        </p>
      </div>

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

      {/* InfoCard: Co trafia do chmury? */}
      <div className="p-4 rounded-2xl bg-[#131424] border border-[#272540] space-y-1.5">
        <div className="flex items-center gap-2 text-white font-bold text-xs">
          <Info className="w-4 h-4 text-[#A875FF]" />
          <span>Co trafia do chmury?</span>
        </div>
        <p className="text-xs text-[#9499AD] leading-relaxed">
          Tylko numer, etykieta i notatka, które zapiszesz przyciskiem „Zapisz w chmurze”, oraz dane konta. Notatki są prywatne, dostępne po zalogowaniu. SMS-y, sprawdzane linki, blokady i rejestr połączeń pozostają na urządzeniu.
        </p>
      </div>

      <div className="pt-2 text-center space-y-1">
        <a
          href="https://cert.pl/lista-ostrzezen/"
          target="_blank"
          rel="noreferrer"
          className="text-xs text-[#A875FF] hover:underline inline-flex items-center gap-1"
        >
          <span>Źródło danych: CERT Polska / NASK</span>
          <ExternalLink className="w-3 h-3" />
        </a>
        <p className="text-[11px] text-[#6C6F85]">
          0.6.0-prototype · CzyToTy? Web
        </p>
      </div>
    </div>
  );
}
