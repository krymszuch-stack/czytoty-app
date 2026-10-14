import React from 'react';
import { X, Check } from 'lucide-react';
import { useApp } from '../context/AppContext';
import { AccentColor, ThemeMode } from '../types';

export function AppearanceSheet() {
  const { appearance, updateAppearance, appearanceOpen, setAppearanceOpen } = useApp();

  if (!appearanceOpen) return null;

  const modes: { id: ThemeMode; label: string }[] = [
    { id: 'SYSTEM', label: 'Systemowy' },
    { id: 'LIGHT', label: 'Jasny' },
    { id: 'DARK', label: 'Ciemny' }
  ];

  const accents: { id: AccentColor; label: string; color: string }[] = [
    { id: 'LAVENDER', label: 'Lawenda', color: '#A875FF' },
    { id: 'OCEAN', label: 'Ocean', color: '#3B82F6' },
    { id: 'MINT', label: 'Mięta', color: '#8BE0BB' }
  ];

  const resetDefault = () => {
    updateAppearance({
      mode: 'SYSTEM',
      accent: 'LAVENDER',
      dynamic: false,
      compact: false,
      largeText: false,
      rounded: true,
      haptics: true
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/60 backdrop-blur-sm p-0 sm:p-4 animate-in fade-in duration-200">
      <div
        className="w-full max-w-lg bg-[#131424] border border-[#272540] rounded-t-3xl sm:rounded-3xl p-6 shadow-2xl max-h-[90vh] overflow-y-auto"
        onClick={e => e.stopPropagation()}
      >
        <div className="flex items-center justify-between pb-3 border-b border-[#272540]">
          <div>
            <h2 className="text-xl font-bold text-white">Po Twojemu</h2>
            <p className="text-xs text-[#9499AD]">Wygląd telefonu i wiadomości</p>
          </div>
          <button
            onClick={() => setAppearanceOpen(false)}
            className="p-2 text-[#9499AD] hover:text-white rounded-full hover:bg-white/5 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-6 pt-4">
          {/* Theme Mode */}
          <div>
            <label className="text-sm font-semibold text-white/90 block mb-2">
              Motyw
            </label>
            <div className="flex gap-2">
              {modes.map(m => (
                <button
                  key={m.id}
                  onClick={() => updateAppearance({ ...appearance, mode: m.id })}
                  data-testid={`theme_${m.id}`}
                  className={`flex-1 py-2 px-3 rounded-xl text-sm font-medium transition-all ${
                    appearance.mode === m.id
                      ? 'bg-[#7B4DFF] text-white shadow-md'
                      : 'bg-[#1A1B30] text-[#9499AD] hover:text-white hover:bg-[#22233C]'
                  }`}
                >
                  {m.label}
                </button>
              ))}
            </div>
          </div>

          {/* Accent Color */}
          <div>
            <label className="text-sm font-semibold text-white/90 block mb-2">
              Kolor akcentu
            </label>
            <div className="flex gap-2">
              {accents.map(acc => (
                <button
                  key={acc.id}
                  onClick={() => updateAppearance({ ...appearance, accent: acc.id })}
                  className={`flex-1 flex items-center justify-center gap-2 py-2 px-3 rounded-xl text-sm font-medium border transition-all ${
                    appearance.accent === acc.id
                      ? 'border-[#7B4DFF] bg-[#231A45] text-white shadow-md'
                      : 'border-[#272540] bg-[#1A1B30] text-[#9499AD] hover:text-white'
                  }`}
                >
                  <span
                    className="w-3.5 h-3.5 rounded-full inline-block"
                    style={{ backgroundColor: acc.color }}
                  />
                  <span>{acc.label}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Message Preview Surface */}
          <div>
            <label className="text-sm font-semibold text-white/90 block mb-2">
              Podgląd
            </label>
            <div
              className={`p-4 rounded-2xl border transition-all ${
                appearance.accent === 'MINT'
                  ? 'bg-[#132B1E] border-[#22C55E]/30 text-[#8BE0BB]'
                  : appearance.accent === 'OCEAN'
                  ? 'bg-[#132338] border-[#3B82F6]/30 text-[#93C5FD]'
                  : 'bg-[#231A45] border-[#7B4DFF]/30 text-[#E8DEF8]'
              }`}
            >
              <p
                className={`font-medium ${
                  appearance.largeText ? 'text-lg leading-relaxed' : 'text-sm'
                }`}
              >
                Tak będą wyglądały Twoje wiadomości.
              </p>
            </div>
          </div>

          {/* Switches */}
          <div className="space-y-4 pt-1 border-t border-[#272540]">
            <label className="flex items-center justify-between cursor-pointer">
              <span className="text-sm text-white/90">Większy tekst wiadomości</span>
              <input
                type="checkbox"
                checked={appearance.largeText}
                onChange={e =>
                  updateAppearance({ ...appearance, largeText: e.target.checked })
                }
                className="w-5 h-5 accent-[#7B4DFF] rounded"
              />
            </label>

            <label className="flex items-center justify-between cursor-pointer">
              <span className="text-sm text-white/90">Zwarta lista rozmów</span>
              <input
                type="checkbox"
                checked={appearance.compact}
                onChange={e =>
                  updateAppearance({ ...appearance, compact: e.target.checked })
                }
                className="w-5 h-5 accent-[#7B4DFF] rounded"
              />
            </label>

            <label className="flex items-center justify-between cursor-pointer">
              <span className="text-sm text-white/90">Zaokrąglone dymki</span>
              <input
                type="checkbox"
                checked={appearance.rounded}
                onChange={e =>
                  updateAppearance({ ...appearance, rounded: e.target.checked })
                }
                className="w-5 h-5 accent-[#7B4DFF] rounded"
              />
            </label>

            <label className="flex items-center justify-between cursor-pointer">
              <span className="text-sm text-white/90">Wibracje klawiatury telefonu</span>
              <input
                type="checkbox"
                checked={appearance.haptics}
                onChange={e =>
                  updateAppearance({ ...appearance, haptics: e.target.checked })
                }
                className="w-5 h-5 accent-[#7B4DFF] rounded"
              />
            </label>
          </div>

          <div className="pt-2">
            <button
              onClick={resetDefault}
              className="w-full py-2.5 text-sm text-[#A875FF] hover:text-white font-medium text-center transition-colors"
            >
              Przywróć domyślny wygląd
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
