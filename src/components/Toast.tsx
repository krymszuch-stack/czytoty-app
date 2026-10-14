import React, { useEffect } from 'react';
import { X, AlertCircle } from 'lucide-react';
import { useApp } from '../context/AppContext';

export function Toast() {
  const { notice, dismissNotice } = useApp();

  useEffect(() => {
    if (!notice) return;
    const timer = setTimeout(() => {
      dismissNotice();
    }, 4500);
    return () => clearTimeout(timer);
  }, [notice, dismissNotice]);

  if (!notice) return null;

  return (
    <div className="fixed bottom-20 left-4 right-4 z-50 flex justify-center pointer-events-none animate-in slide-in-from-bottom duration-200">
      <div className="bg-[#1A1B30]/95 backdrop-blur-md border border-[#7B4DFF]/40 text-white shadow-2xl py-2.5 px-4 rounded-2xl flex items-center justify-between gap-3 max-w-md w-full pointer-events-auto">
        <div className="flex items-center gap-2.5 overflow-hidden">
          <AlertCircle className="w-4 h-4 text-[#A875FF] flex-shrink-0" />
          <p className="text-xs font-medium text-white/95 truncate">{notice}</p>
        </div>
        <button
          onClick={dismissNotice}
          className="p-1 hover:bg-white/10 rounded-full text-[#9499AD] hover:text-white flex-shrink-0"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}
