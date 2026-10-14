import React from 'react';
import { Phone, MessageSquare, Search, ShieldCheck } from 'lucide-react';
import { useApp } from '../context/AppContext';

export function BottomNav() {
  const { activeTab, setActiveTab, smsConversationOpen } = useApp();

  // Hide bottom navigation in conversation view matching Android spec
  if (activeTab === 1 && smsConversationOpen) {
    return null;
  }

  const items = [
    { index: 0, label: 'Telefon', icon: Phone },
    { index: 1, label: 'SMS', icon: MessageSquare },
    { index: 2, label: 'Sprawdź', icon: Search },
    { index: 4, label: 'Ochrona', icon: ShieldCheck }
  ];

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-[#0A0A12]/95 backdrop-blur-lg border-t border-[#272540] py-2 px-6 safe-bottom">
      <div className="max-w-xl mx-auto flex items-center justify-around">
        {items.map(item => {
          const isSelected = activeTab === item.index;
          const IconComponent = item.icon;

          return (
            <button
              key={item.index}
              onClick={() => setActiveTab(item.index)}
              data-testid={`tab_${item.index}`}
              className={`flex flex-col items-center justify-center py-1 px-3 rounded-2xl transition-all duration-200 ${
                isSelected
                  ? 'text-[#A875FF]'
                  : 'text-[#9499AD] hover:text-white'
              }`}
            >
              <div
                className={`p-1.5 rounded-full transition-all ${
                  isSelected ? 'bg-[#7B4DFF]/20 scale-105' : 'bg-transparent'
                }`}
              >
                <IconComponent className="w-5 h-5" />
              </div>
              <span
                className={`text-[11px] mt-0.5 font-medium tracking-tight ${
                  isSelected ? 'text-[#E8DEF8] font-semibold' : 'text-[#6C6F85]'
                }`}
              >
                {item.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
