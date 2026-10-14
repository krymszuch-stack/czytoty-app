import React from 'react';
import { Phone, MessageSquare, Search, ShieldCheck, Layers } from 'lucide-react';
import { useApp } from '../context/AppContext';

export function BottomNav() {
  const { activeTab, setActiveTab, smsConversationOpen, appearance, smsMessages } = useApp();

  // Hide bottom navigation in conversation view matching Android spec
  if (activeTab === 1 && smsConversationOpen) {
    return null;
  }

  const unreadSmsCount = smsMessages ? smsMessages.filter(m => !m.read).length : 0;

  const items = [
    { index: 0, label: 'Telefon', icon: Phone },
    { index: 1, label: 'SMS', icon: MessageSquare, badge: unreadSmsCount },
    { index: 2, label: 'Sprawdź', icon: Search },
    { index: 6, label: 'Schematy', icon: Layers },
    { index: 4, label: 'Ochrona', icon: ShieldCheck }
  ];

  const handleTabClick = (idx: number) => {
    if (appearance.haptics && 'vibrate' in navigator) {
      navigator.vibrate(35);
    }
    setActiveTab(idx);
  };

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-[#0A0A12]/95 backdrop-blur-xl border-t border-[#272540] py-2 px-3 safe-bottom shadow-2xl">
      <div className="max-w-xl mx-auto flex items-center justify-around gap-1">
        {items.map(item => {
          const isSelected = activeTab === item.index;
          const IconComponent = item.icon;

          return (
            <button
              key={item.index}
              onClick={() => handleTabClick(item.index)}
              data-testid={`tab_${item.index}`}
              title={item.label}
              className={`flex-1 flex flex-col items-center justify-center py-1.5 px-1 rounded-2xl transition-all duration-150 active:scale-95 ${
                isSelected ? 'text-[#A875FF]' : 'text-[#9499AD] hover:text-white'
              }`}
            >
              <div
                className={`p-2 rounded-2xl transition-all flex items-center justify-center relative ${
                  isSelected
                    ? 'bg-[#7B4DFF]/25 text-white shadow-md shadow-[#7B4DFF]/30'
                    : item.index === 1
                    ? 'bg-[#1E1C38] text-purple-300 hover:bg-[#2A2750] border border-[#7B4DFF]/30'
                    : 'bg-transparent'
                }`}
              >
                <IconComponent className="w-5 h-5" />
                {Boolean(item.badge && item.badge > 0) && (
                  <span className="absolute -top-1 -right-1 min-w-[18px] h-[18px] px-1 bg-[#8BE0BB] text-[#0A0A12] text-[10px] font-extrabold rounded-full flex items-center justify-center ring-2 ring-[#0A0A12]">
                    {item.badge}
                  </span>
                )}
              </div>
              <span
                className={`text-[11px] mt-1 font-semibold tracking-tight ${
                  isSelected
                    ? 'text-white font-bold'
                    : item.index === 1
                    ? 'text-purple-200'
                    : 'text-[#8A8F9E]'
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
