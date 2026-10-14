import React, { useState, useEffect } from 'react';
import { Tag, Check, X, Clock } from 'lucide-react';
import { useApp } from '../context/AppContext';

interface QuickCallNoteBarProps {
  phoneNumber: string;
  onDismiss: () => void;
}

const PRESET_TAGS = [
  { label: '📦 Kurier', note: 'Dostawa paczki / kurier' },
  { label: '🔧 Serwis', note: 'Hydraulik / serwis / naprawa' },
  { label: '💼 Klient', note: 'Kontakt biznesowy / klient' },
  { label: '🏥 Przychodnia', note: 'Rejestracja medyczna / wizyta' }
];

export function QuickCallNoteBar({ phoneNumber, onDismiss }: QuickCallNoteBarProps) {
  const { saveLocalNote, showNotice } = useApp();
  const [customText, setCustomText] = useState('');
  const [secondsLeft, setSecondsLeft] = useState(4);

  useEffect(() => {
    const timer = setInterval(() => {
      setSecondsLeft(prev => {
        if (prev <= 1) {
          clearInterval(timer);
          onDismiss();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [onDismiss]);

  const handleApplyTag = (tagLabel: string, defaultNote: string) => {
    const noteText = customText.trim() || defaultNote;
    saveLocalNote({
      phone: phoneNumber,
      label: tagLabel,
      note: noteText,
      blocked: false
    });
    showNotice(`Zapisano notatkę „${tagLabel}” dla tego numeru. Wyświetli się przy kolejnym telefonie!`);
    onDismiss();
  };

  return (
    <div className="fixed bottom-20 left-4 right-4 z-40 max-w-sm mx-auto animate-in slide-in-from-bottom duration-300">
      <div className="bg-[#181A2E]/95 backdrop-blur-md border border-[#3E4068] rounded-2xl p-3 shadow-2xl text-left space-y-2">
        {/* Pasek postępu odliczania 4s */}
        <div className="w-full bg-white/10 h-1 rounded-full overflow-hidden">
          <div
            className="bg-[#7B4DFF] h-full transition-all duration-1000 ease-linear rounded-full"
            style={{ width: `${(secondsLeft / 4) * 100}%` }}
          />
        </div>

        <div className="flex items-center justify-between">
          <div className="flex items-center gap-1.5">
            <Tag className="w-3.5 h-3.5 text-[#8BE0BB]" />
            <span className="text-xs font-bold text-white">
              Pamięć Złotej Rybki: Oznacz numer
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span className="text-[10px] text-gray-400 font-mono flex items-center gap-1">
              <Clock className="w-3 h-3" />
              {secondsLeft}s
            </span>
            <button
              onClick={onDismiss}
              className="p-1 text-gray-400 hover:text-white rounded-lg hover:bg-white/10"
              title="Zamknij"
            >
              <X className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        <p className="text-[10px] text-gray-400">
          Zapisz szybki tag dla <span className="font-mono text-gray-200">{phoneNumber}</span> bez zaśmiecania kontaktów:
        </p>

        {/* Gotowe tagi jednym kliknięciem */}
        <div className="grid grid-cols-2 gap-1.5 pt-0.5">
          {PRESET_TAGS.map(tag => (
            <button
              key={tag.label}
              onClick={() => handleApplyTag(tag.label, tag.note)}
              className="px-2 py-1.5 rounded-xl bg-[#242642] hover:bg-[#7B4DFF]/30 active:scale-95 text-xs text-white text-left font-medium border border-white/5 transition-all truncate"
            >
              {tag.label}
            </button>
          ))}
        </div>
      </div>
    </div>
  );
}
