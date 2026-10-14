import React, { useState, useEffect } from 'react';
import {
  Phone,
  MessageSquare,
  ShieldAlert,
  Save,
  Trash2,
  Edit2,
  CloudUpload,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { parsePhone } from '../utils/phone';
import { PhoneNote } from '../types';

export function NumbersTab() {
  const {
    notes,
    saveLocalNote,
    deleteLocalNote,
    phoneInput,
    setPhoneInput,
    setDialPrefill,
    setSmsPrefill,
    setActiveTab,
    protectionEnabled,
    cloudUserEmail,
    saveCloudNote,
    isCloudBusy
  } = useApp();

  const [inputVal, setInputVal] = useState(phoneInput || '');
  const [label, setLabel] = useState('');
  const [noteText, setNoteText] = useState('');
  const [isBlocked, setIsBlocked] = useState(false);

  const parsed = parsePhone(inputVal);

  // Sync existing note if number matches
  useEffect(() => {
    if (parsed) {
      const existing = notes.find(n => n.phone === parsed.e164);
      if (existing) {
        setLabel(existing.label);
        setNoteText(existing.note);
        setIsBlocked(existing.blocked);
      } else {
        setLabel('');
        setNoteText('');
        setIsBlocked(false);
      }
    }
  }, [parsed?.e164, notes]);

  useEffect(() => {
    if (phoneInput && phoneInput !== inputVal) {
      setInputVal(phoneInput);
    }
  }, [phoneInput]);

  const handleSaveLocal = () => {
    if (!parsed) return;
    const newNote: PhoneNote = {
      phone: parsed.e164,
      label: label.slice(0, 80),
      note: noteText.slice(0, 500),
      blocked: isBlocked
    };
    saveLocalNote(newNote);
  };

  const handleSaveCloud = async () => {
    if (!parsed) return;
    const noteToSave: PhoneNote = {
      phone: parsed.e164,
      label: label.slice(0, 80),
      note: noteText.slice(0, 500),
      blocked: isBlocked
    };
    await saveCloudNote(noteToSave);
  };

  const handleOpenDialer = () => {
    if (parsed) {
      setDialPrefill(parsed.e164);
      setActiveTab(0);
    }
  };

  const handleOpenSms = () => {
    if (parsed) {
      setSmsPrefill(parsed.e164);
      setActiveTab(1);
    }
  };

  return (
    <div className="flex flex-col h-full space-y-6 overflow-y-auto pr-1 pb-20">
      <div>
        <h2 className="text-3xl font-extrabold text-white tracking-tight">
          Twoje numery
        </h2>
        <p className="text-sm text-[#9499AD] mt-1">
          Sprawdź format, dodaj własną notatkę lub blokadę. Numery bez prefiksu kraju traktujemy jako polskie.
        </p>
      </div>

      {/* Phone Number Input */}
      <div className="space-y-1.5">
        <label className="text-xs font-semibold text-[#9499AD] uppercase tracking-wider block">
          Numer telefonu
        </label>
        <input
          type="text"
          value={inputVal}
          onChange={e => {
            const val = e.target.value.slice(0, 40);
            setInputVal(val);
            setPhoneInput(val);
          }}
          placeholder="Wpisz numer, np. +48 601 234 567"
          data-testid="phone_input"
          className="w-full py-3 px-4 rounded-2xl bg-[#1A1B30] border border-[#272540] text-base text-white placeholder:text-[#6C6F85] focus:outline-none focus:border-[#7B4DFF]"
        />
        {inputVal.trim() && !parsed && (
          <p className="text-xs text-[#FFD089] flex items-center gap-1.5 pt-1">
            <AlertCircle className="w-3.5 h-3.5" />
            <span>Wpisz pełny, prawidłowy numer, np. z prefiksem +48.</span>
          </p>
        )}
      </div>

      {/* Phone Editor Card */}
      {parsed && (
        <div className="p-5 rounded-3xl bg-[#131424] border border-[#272540] space-y-4 shadow-xl">
          <div>
            <h3 className="text-xl font-bold text-white tracking-tight">
              {parsed.formatted}
            </h3>
            <p className="text-xs text-[#A875FF] font-semibold mt-0.5">
              Plan numeracji: {parsed.country} · poprawny format
            </p>
            <p className="text-[11px] text-[#6C6F85] mt-1">
              Format sprawdzono na podstawie Google libphonenumber. To nie potwierdza aktywności numeru, jego właściciela ani reputacji.
            </p>
          </div>

          <div className="space-y-3 pt-2">
            <div>
              <label className="text-xs text-[#9499AD] block mb-1">
                Twoja etykieta
              </label>
              <input
                type="text"
                value={label}
                onChange={e => setLabel(e.target.value.slice(0, 80))}
                placeholder="Np. Kurier, Doradca bankowy"
                className="w-full py-2 px-3 rounded-xl bg-[#1A1B30] border border-[#272540] text-sm text-white focus:outline-none focus:border-[#7B4DFF]"
              />
            </div>

            <div>
              <label className="text-xs text-[#9499AD] block mb-1">
                Prywatna notatka
              </label>
              <textarea
                value={noteText}
                onChange={e => setNoteText(e.target.value.slice(0, 500))}
                placeholder="Notatka widoczna tylko dla Ciebie na tym urządzeniu..."
                data-testid="note_input"
                rows={3}
                className="w-full p-3 rounded-xl bg-[#1A1B30] border border-[#272540] text-sm text-white focus:outline-none focus:border-[#7B4DFF] resize-none"
              />
            </div>

            {/* Block switch */}
            <div className="flex items-center justify-between p-3 rounded-xl bg-[#1A1B30] border border-[#272540]">
              <div className="flex items-center gap-2">
                <ShieldAlert className="w-4 h-4 text-[#FF8596]" />
                <span className="text-xs font-semibold text-white">
                  Dodaj do mojej listy blokad
                </span>
              </div>
              <input
                type="checkbox"
                checked={isBlocked}
                onChange={e => setIsBlocked(e.target.checked)}
                data-testid="blocked_switch"
                className="w-5 h-5 accent-[#EF5350] rounded"
              />
            </div>

            {isBlocked && !protectionEnabled && (
              <p className="text-xs text-[#FFD089] bg-[#805000]/20 p-2.5 rounded-xl border border-[#FFD089]/30">
                Blokada zacznie działać po zapisaniu i włączeniu filtrowania w zakładce Ochrona.
              </p>
            )}

            {/* Action buttons */}
            <div className="space-y-2 pt-2">
              <button
                onClick={handleSaveLocal}
                data-testid="save_local"
                className="w-full py-2.5 px-4 rounded-xl bg-[#7B4DFF] hover:bg-[#6a3ee0] text-white text-sm font-semibold flex items-center justify-center gap-2 shadow-md transition-all"
              >
                <Save className="w-4 h-4" />
                <span>Zapisz na urządzeniu</span>
              </button>

              {cloudUserEmail && (
                <button
                  onClick={handleSaveCloud}
                  disabled={isCloudBusy}
                  className="w-full py-2.5 px-4 rounded-xl bg-[#1A1B30] hover:bg-[#22233C] border border-[#272540] text-white text-xs font-semibold flex items-center justify-center gap-2 transition-all disabled:opacity-50"
                >
                  <CloudUpload className="w-4 h-4 text-[#A875FF]" />
                  <span>Zapisz w chmurze</span>
                </button>
              )}
            </div>

            {/* Quick shortcuts */}
            <div className="flex items-center justify-around pt-1 text-xs">
              <button
                onClick={handleOpenDialer}
                className="text-[#A875FF] hover:underline flex items-center gap-1.5"
              >
                <Phone className="w-3.5 h-3.5" />
                <span>Otwórz telefon</span>
              </button>
              <span className="text-[#272540]">•</span>
              <button
                onClick={handleOpenSms}
                className="text-[#A875FF] hover:underline flex items-center gap-1.5"
              >
                <MessageSquare className="w-3.5 h-3.5" />
                <span>Otwórz SMS</span>
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Saved Notes Section */}
      <div className="space-y-3 pt-2">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-bold text-white">
            Zapisane lokalnie · {notes.length}
          </h3>
        </div>

        {notes.length === 0 ? (
          <p className="text-xs text-[#9499AD] py-6 text-center">
            Nie masz jeszcze zapisanych numerów. Lista zaczyna się pusta.
          </p>
        ) : (
          <div className="space-y-2.5">
            {notes.map(note => (
              <div
                key={note.phone}
                className="p-4 rounded-2xl bg-[#131424] border border-[#272540] space-y-2"
              >
                <div className="flex items-start justify-between">
                  <div>
                    <h4 className="text-sm font-bold text-white">
                      {note.label || note.phone}
                    </h4>
                    {note.label && (
                      <p className="text-xs font-mono text-[#9499AD]">
                        {note.phone}
                      </p>
                    )}
                  </div>
                  <span
                    className={`text-[11px] font-semibold px-2 py-0.5 rounded-full border ${
                      note.blocked
                        ? 'bg-[#805000]/20 text-[#FFD089] border-[#FFD089]/30'
                        : 'bg-[#1A1B30] text-[#9499AD] border-[#272540]'
                    }`}
                  >
                    {note.blocked ? 'Na Twojej liście blokad' : 'Twoja notatka'}
                  </span>
                </div>

                {note.note && (
                  <p className="text-xs text-[#E8DEF8] bg-[#1A1B30]/60 p-2.5 rounded-xl border border-[#272540]/60">
                    {note.note}
                  </p>
                )}

                <div className="flex items-center gap-3 pt-1 text-xs">
                  <button
                    onClick={() => {
                      setInputVal(note.phone);
                      setPhoneInput(note.phone);
                    }}
                    className="text-[#A875FF] hover:underline flex items-center gap-1 font-medium"
                  >
                    <Edit2 className="w-3.5 h-3.5" />
                    <span>Edytuj</span>
                  </button>
                  <button
                    onClick={() => deleteLocalNote(note.phone)}
                    className="text-[#EF5350] hover:underline flex items-center gap-1 font-medium"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Usuń lokalnie</span>
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
