import React from 'react';
import { Palette, BookUser, User, PhoneCall } from 'lucide-react';
import { useApp } from '../context/AppContext';

export function Header() {
  const {
    activeTab,
    setActiveTab,
    smsConversationOpen,
    setAppearanceOpen,
    callSession
  } = useApp();

  const activeLiveCall = callSession.calls.find(c => c.state !== 'DISCONNECTED');

  // Hide top header when inside SMS thread conversation (matching Android spec)
  if (activeTab === 1 && smsConversationOpen) {
    return null;
  }

  return (
    <header className="sticky top-0 z-40 bg-[#0A0A12]/95 backdrop-blur-md border-b border-[#272540] px-4 py-3 max-w-xl mx-auto w-full">
      <div className="flex items-center justify-between">
        <div
          className="flex items-center gap-3 cursor-pointer"
          onClick={() => setActiveTab(0)}
        >
          <img
            src="/brand/logo-mark.svg"
            alt="Logo CzyToTy?"
            className="w-8 h-8 object-contain"
          />
          <h1 className="text-xl font-bold tracking-tight bg-gradient-to-r from-white via-purple-100 to-purple-300 bg-clip-text text-transparent">
            CzyToTy?
          </h1>
        </div>

        <div className="flex items-center gap-1 sm:gap-2">
          <button
            onClick={() => setAppearanceOpen(true)}
            data-testid="appearance"
            title="Dostosuj wygląd"
            className="p-2 text-purple-300 hover:text-white hover:bg-white/5 rounded-full transition-colors"
          >
            <Palette className="w-5 h-5" />
          </button>

          <button
            onClick={() => setActiveTab(3)}
            data-testid="tab_3"
            title="Numery i notatki"
            className={`p-2 rounded-full transition-colors ${
              activeTab === 3
                ? 'bg-[#7B4DFF]/20 text-[#A875FF]'
                : 'text-purple-300 hover:text-white hover:bg-white/5'
            }`}
          >
            <BookUser className="w-5 h-5" />
          </button>

          <button
            onClick={() => setActiveTab(5)}
            data-testid="tab_5"
            title="Konto"
            className={`p-2 rounded-full transition-colors ${
              activeTab === 5
                ? 'bg-[#7B4DFF]/20 text-[#A875FF]'
                : 'text-purple-300 hover:text-white hover:bg-white/5'
            }`}
          >
            <User className="w-5 h-5" />
          </button>
        </div>
      </div>

      {activeLiveCall && (
        <div className="mt-2.5">
          <button
            onClick={() => {
              // Click to focus call
            }}
            data-testid="return_to_call"
            className="w-full flex items-center justify-center gap-2 py-2 px-4 rounded-xl bg-gradient-to-r from-[#22C55E]/20 to-[#107E3E]/20 border border-[#22C55E]/40 text-[#4ADE80] font-medium text-sm animate-pulse hover:bg-[#22C55E]/30 transition-all"
          >
            <PhoneCall className="w-4 h-4 animate-bounce" />
            <span>Wróć do połączenia ({activeLiveCall.number})</span>
          </button>
        </div>
      )}
    </header>
  );
}
