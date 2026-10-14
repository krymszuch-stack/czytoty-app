import React from 'react';
import { useApp } from './context/AppContext';
import { Header } from './components/Header';
import { BottomNav } from './components/BottomNav';
import { DialerTab } from './tabs/DialerTab';
import { SmsTab } from './tabs/SmsTab';
import { CheckTab } from './tabs/CheckTab';
import { NumbersTab } from './tabs/NumbersTab';
import { ProtectionTab } from './tabs/ProtectionTab';
import { AccountTab } from './tabs/AccountTab';
import { InCallOverlay } from './components/InCallOverlay';
import { AppearanceSheet } from './components/AppearanceSheet';
import { Toast } from './components/Toast';

export function App() {
  const { activeTab, appearance } = useApp();

  const accentColor =
    appearance.accent === 'MINT'
      ? '#22C55E'
      : appearance.accent === 'OCEAN'
      ? '#3B82F6'
      : '#A875FF';

  return (
    <div
      className="min-h-screen bg-[#0A0A12] text-white flex flex-col font-sans relative selection:bg-[#7B4DFF]/30"
      style={{
        // Custom variable for accent highlights
        ['--brand-accent' as any]: accentColor
      }}
    >
      <Header />

      <main className="flex-1 max-w-xl mx-auto w-full p-4 flex flex-col overflow-hidden">
        {activeTab === 0 && <DialerTab />}
        {activeTab === 1 && <SmsTab />}
        {activeTab === 2 && <CheckTab />}
        {activeTab === 3 && <NumbersTab />}
        {activeTab === 4 && <ProtectionTab />}
        {activeTab === 5 && <AccountTab />}
      </main>

      <BottomNav />
      <InCallOverlay />
      <AppearanceSheet />
      <Toast />
    </div>
  );
}
export default App;
