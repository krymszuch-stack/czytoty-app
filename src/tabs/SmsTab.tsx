import React, { useState, useEffect, useMemo } from 'react';
import {
  Search,
  Pin,
  PinOff,
  Send,
  ArrowLeft,
  Phone,
  Shield,
  Edit,
  MoreVertical,
  CheckCircle2,
  AlertCircle
} from 'lucide-react';
import { useApp } from '../context/AppContext';
import { ContactAvatar } from '../components/ContactAvatar';
import { parsePhone } from '../utils/phone';
import { analyzeMessage } from '../utils/cert';
import { DeviceSms, SmsDraft } from '../types';

export function SmsTab() {
  const {
    smsMessages,
    selectedThread,
    setSelectedThread,
    drafts,
    saveDraft,
    pinnedThreads,
    togglePinThread,
    sendSms,
    sends,
    isSendingSms,
    markThreadRead,
    smsPrefill,
    setSmsPrefill,
    setDialPrefill,
    setMessageToCheck,
    setActiveTab,
    certSnapshot,
    contacts,
    appearance,
    setSmsConversationOpen
  } = useApp();

  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState<'ALL' | 'UNREAD' | 'PINNED'>('ALL');
  const [composing, setComposing] = useState(false);
  const [newRecipient, setNewRecipient] = useState('');
  const [messageBody, setMessageBody] = useState('');
  const [showConfirmSend, setShowConfirmSend] = useState(false);
  const [showInfoModal, setShowInfoModal] = useState(false);

  const isConversationOpen = Boolean(composing || selectedThread !== null);

  useEffect(() => {
    setSmsConversationOpen(isConversationOpen);
    return () => setSmsConversationOpen(false);
  }, [isConversationOpen, setSmsConversationOpen]);

  // Handle incoming SMS prefill from other tabs
  useEffect(() => {
    if (smsPrefill) {
      setNewRecipient(smsPrefill);
      setComposing(true);
      setSelectedThread(null);
      setSmsPrefill('');
    }
  }, [smsPrefill, setSmsPrefill, setSelectedThread]);

  // Sync draft when thread changes
  useEffect(() => {
    if (selectedThread !== null) {
      const threadDraft = drafts[String(selectedThread)];
      setMessageBody(threadDraft?.body || '');
      markThreadRead(selectedThread);
    } else if (composing) {
      const newDraft = drafts['new'];
      setNewRecipient(newDraft?.address || newRecipient);
      setMessageBody(newDraft?.body || '');
    }
  }, [selectedThread, composing]);

  // Group messages by thread
  const threadGroups = useMemo(() => {
    const map = new Map<number, DeviceSms[]>();
    for (const msg of smsMessages) {
      const list = map.get(msg.thread) || [];
      list.push(msg);
      map.set(msg.thread, list);
    }
    return map;
  }, [smsMessages]);

  const contactMap = useMemo(() => {
    const map = new Map<string, string>();
    for (const c of contacts) {
      map.set(c.phone, c.name);
    }
    return map;
  }, [contacts]);

  const getContactName = (phone: string) => {
    const parsed = parsePhone(phone);
    const lookup = parsed?.e164 || phone;
    return contactMap.get(lookup) || phone;
  };

  // Build sorted threads list
  const visibleThreads = useMemo(() => {
    const result: {
      threadId: number;
      latestMsg: DeviceSms;
      messages: DeviceSms[];
      name: string;
      hasUnread: boolean;
      isPinned: boolean;
      draftText: string;
    }[] = [];

    for (const [threadId, messages] of threadGroups.entries()) {
      messages.sort((a, b) => b.time - a.time);
      const latestMsg = messages[0];
      const name = getContactName(latestMsg.address);
      const hasUnread = messages.some(m => m.incoming && !m.read);
      const isPinned = pinnedThreads.includes(threadId);
      const draftText = drafts[String(threadId)]?.body || '';

      // Filters
      if (filter === 'UNREAD' && !hasUnread) continue;
      if (filter === 'PINNED' && !isPinned) continue;

      if (query) {
        const q = query.toLowerCase();
        const matchName = name.toLowerCase().includes(q);
        const matchAddress = latestMsg.address.toLowerCase().includes(q);
        const matchBody = messages.some(m => m.body.toLowerCase().includes(q));
        if (!matchName && !matchAddress && !matchBody) continue;
      }

      result.push({
        threadId,
        latestMsg,
        messages,
        name,
        hasUnread,
        isPinned,
        draftText
      });
    }

    // Sort: pinned first, then latest message time
    return result.sort((a, b) => {
      if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
      return b.latestMsg.time - a.latestMsg.time;
    });
  }, [threadGroups, contactMap, pinnedThreads, drafts, filter, query]);

  // Active conversation messages
  const activeThreadMessages = useMemo(() => {
    if (selectedThread === null) return [];
    const list = threadGroups.get(selectedThread) || [];
    return [...list].sort((a, b) => a.time - b.time);
  }, [selectedThread, threadGroups]);

  const activeThreadLatest = activeThreadMessages[activeThreadMessages.length - 1];
  const activeAddress = activeThreadLatest?.address || newRecipient;
  const activeDisplayName = getContactName(activeAddress);

  const handleOpenThread = (threadId: number) => {
    setSelectedThread(threadId);
    setComposing(false);
  };

  const handleBackToInbox = () => {
    // Save draft if any
    const draftKey = selectedThread !== null ? String(selectedThread) : 'new';
    saveDraft(draftKey, {
      address: activeAddress,
      body: messageBody
    });
    setSelectedThread(null);
    setComposing(false);
    setMessageBody('');
    setNewRecipient('');
  };

  const handleDraftChange = (text: string) => {
    setMessageBody(text);
    const draftKey = selectedThread !== null ? String(selectedThread) : 'new';
    saveDraft(draftKey, {
      address: activeAddress,
      body: text
    });
  };

  const handleSend = async () => {
    if (!messageBody.trim() || !activeAddress.trim()) return;
    try {
      const draftKey = selectedThread !== null ? String(selectedThread) : 'new';
      await sendSms(activeAddress, messageBody, draftKey);
      setMessageBody('');
      if (composing) {
        setComposing(false);
      }
    } catch {
      // error handled in context
    }
  };

  const handleCheckText = (text: string) => {
    setMessageToCheck(text);
    setActiveTab(2);
  };

  const handleCallRecipient = () => {
    const parsed = parsePhone(activeAddress);
    if (parsed) {
      setDialPrefill(parsed.e164);
      setActiveTab(0);
    }
  };

  const formatDateLabel = (timestamp: number) => {
    const date = new Date(timestamp);
    return date.toLocaleDateString('pl-PL', {
      day: 'numeric',
      month: 'long',
      year: 'numeric'
    });
  };

  const formatTime = (timestamp: number) => {
    const date = new Date(timestamp);
    const isToday = new Date().toDateString() === date.toDateString();
    if (isToday) {
      return date.toLocaleTimeString('pl-PL', { hour: '2-digit', minute: '2-digit' });
    }
    return date.toLocaleDateString('pl-PL', { day: 'numeric', month: 'short' });
  };

  const smsPartsCount = Math.max(1, Math.ceil((messageBody.length || 1) / 160));

  return (
    <div className="flex flex-col h-full overflow-hidden">
      {/* VIEW 1: INBOX / THREADS LIST */}
      {!isConversationOpen && (
        <div className="flex-1 flex flex-col space-y-3 overflow-hidden">
          {/* Header */}
          <div className="flex items-center justify-between">
            <h2 className="text-3xl font-extrabold text-white tracking-tight">
              Wiadomości
            </h2>
            <button
              onClick={() => setShowInfoModal(true)}
              className="p-2 text-[#9499AD] hover:text-white rounded-full hover:bg-white/5 transition-colors"
            >
              <MoreVertical className="w-5 h-5" />
            </button>
          </div>

          {/* Search Bar */}
          <div className="relative">
            <Search className="w-4 h-4 absolute left-3.5 top-3 text-[#9499AD]" />
            <input
              type="text"
              value={query}
              onChange={e => setQuery(e.target.value)}
              placeholder="Szukaj rozmowy lub treści"
              className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-[#1A1B30]/80 border border-[#272540] text-sm text-white placeholder:text-[#6C6F85] focus:outline-none focus:border-[#7B4DFF]/50"
            />
          </div>

          {/* Filter Chips */}
          <div className="flex items-center gap-2">
            {[
              { id: 'ALL', label: 'Wszystkie' },
              { id: 'UNREAD', label: 'Nieprzeczytane' },
              { id: 'PINNED', label: 'Przypięte' }
            ].map(f => (
              <button
                key={f.id}
                onClick={() => setFilter(f.id as any)}
                className={`py-1.5 px-3.5 rounded-xl text-xs font-medium transition-all ${
                  filter === f.id
                    ? 'bg-[#7B4DFF] text-white shadow-sm font-semibold'
                    : 'bg-[#1A1B30] text-[#9499AD] hover:text-white'
                }`}
              >
                {f.label}
              </button>
            ))}
          </div>

          {/* Threads List */}
          <div
            data-testid="sms_list"
            className="flex-1 overflow-y-auto space-y-1.5 pr-1 pb-20"
          >
            {visibleThreads.length === 0 ? (
              <div className="py-16 text-center text-[#9499AD] text-sm space-y-2">
                <p className="text-base font-semibold text-white">
                  {query
                    ? 'Brak wyników'
                    : filter === 'PINNED'
                    ? 'Brak przypiętych rozmów'
                    : filter === 'UNREAD'
                    ? 'Wszystko przeczytane'
                    : 'Miejsce na Twoje rozmowy'}
                </p>
                <p className="text-xs text-[#6C6F85]">
                  Nową wiadomość napiszesz przyciskiem poniżej.
                </p>
              </div>
            ) : (
              visibleThreads.map(t => (
                <div
                  key={t.threadId}
                  onClick={() => handleOpenThread(t.threadId)}
                  className={`flex items-center justify-between p-3 rounded-2xl transition-all cursor-pointer border ${
                    t.hasUnread
                      ? 'bg-[#231A45]/50 border-[#7B4DFF]/30 hover:bg-[#231A45]/80'
                      : 'bg-[#131424]/80 border-[#272540] hover:bg-[#1A1B30]'
                  }`}
                >
                  <div className="flex items-center gap-3 overflow-hidden flex-1 mr-2">
                    <div className="relative">
                      <ContactAvatar name={t.name} size="md" />
                      {t.hasUnread && (
                        <span className="w-3 h-3 rounded-full bg-[#7B4DFF] absolute -bottom-0.5 -right-0.5 ring-2 ring-[#0A0A12]" />
                      )}
                    </div>
                    <div className="overflow-hidden flex-1">
                      <div className="flex items-center justify-between">
                        <p
                          className={`text-sm truncate ${
                            t.hasUnread
                              ? 'font-bold text-white'
                              : 'font-semibold text-white/90'
                          }`}
                        >
                          {t.name}
                        </p>
                        <span className="text-[11px] text-[#6C6F85] ml-2 flex-shrink-0">
                          {formatTime(t.latestMsg.time)}
                        </span>
                      </div>
                      <p
                        className={`text-xs truncate mt-0.5 ${
                          t.draftText
                            ? 'text-[#A875FF] font-medium'
                            : 'text-[#9499AD]'
                        }`}
                      >
                        {t.draftText
                          ? `Szkic · ${t.draftText}`
                          : t.latestMsg.body}
                      </p>
                    </div>
                  </div>

                  <button
                    onClick={e => {
                      e.stopPropagation();
                      togglePinThread(t.threadId);
                    }}
                    className="p-2 text-[#9499AD] hover:text-[#A875FF] transition-colors"
                    title={t.isPinned ? 'Odepnij rozmowę' : 'Przypnij rozmowę'}
                  >
                    {t.isPinned ? (
                      <Pin className="w-4 h-4 fill-current text-[#A875FF]" />
                    ) : (
                      <PinOff className="w-4 h-4" />
                    )}
                  </button>
                </div>
              ))
            )}
          </div>

          {/* Floating Action Button (New SMS) */}
          <div className="fixed bottom-20 right-6 z-30">
            <button
              onClick={() => {
                setComposing(true);
                setSelectedThread(null);
                setNewRecipient('');
                setMessageBody('');
              }}
              data-testid="sms_new"
              className="flex items-center gap-2 py-3.5 px-5 rounded-2xl bg-[#7B4DFF] hover:bg-[#6a3ee0] text-white font-semibold text-sm shadow-xl active:scale-95 transition-all"
            >
              <Edit className="w-5 h-5" />
              <span>Nowy SMS</span>
            </button>
          </div>
        </div>
      )}

      {/* VIEW 2: CONVERSATION VIEW */}
      {isConversationOpen && (
        <div className="flex-1 flex flex-col h-full overflow-hidden bg-[#0A0A12]">
          {/* Conversation Top Header */}
          <div className="flex items-center justify-between py-3 px-2 border-b border-[#272540] bg-[#131424]/90 backdrop-blur-md">
            <div className="flex items-center gap-2 overflow-hidden">
              <button
                onClick={handleBackToInbox}
                className="p-2 rounded-full hover:bg-white/5 text-purple-200"
                title="Wróć do wiadomości"
              >
                <ArrowLeft className="w-5 h-5" />
              </button>
              <ContactAvatar
                name={composing && !newRecipient ? '?' : activeDisplayName}
                size="sm"
              />
              <div className="overflow-hidden">
                <h3 className="text-sm font-bold text-white truncate">
                  {composing && !newRecipient ? 'Nowy SMS' : activeDisplayName}
                </h3>
                {activeDisplayName !== activeAddress && activeAddress && (
                  <p className="text-[11px] text-[#9499AD] truncate">
                    {activeAddress}
                  </p>
                )}
              </div>
            </div>

            <div className="flex items-center gap-1">
              {activeAddress && (
                <button
                  onClick={handleCallRecipient}
                  className="p-2 rounded-full text-purple-300 hover:text-white hover:bg-white/5"
                  title="Otwórz numer w telefonie"
                >
                  <Phone className="w-5 h-5" />
                </button>
              )}
              <button
                onClick={() => setShowInfoModal(true)}
                className="p-2 rounded-full text-[#9499AD] hover:text-white hover:bg-white/5"
              >
                <MoreVertical className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* New SMS recipient field */}
          {composing && (
            <div className="p-3 bg-[#1A1B30]/90 border-b border-[#272540]">
              <input
                type="text"
                value={newRecipient}
                onChange={e => setNewRecipient(e.target.value)}
                placeholder="Do: numer telefonu (np. +48...)"
                data-testid="sms_address"
                className="w-full py-1.5 px-3 rounded-xl bg-[#131424] border border-[#272540] text-sm text-white placeholder:text-[#6C6F85] focus:outline-none focus:border-[#7B4DFF]"
              />
            </div>
          )}

          {/* Transmission Receipts */}
          {sends.length > 0 && (
            <div className="px-4 py-1.5 bg-[#1A1B30]/50 text-[11px] text-purple-300 flex items-center gap-2 border-b border-[#272540]/50">
              <CheckCircle2 className="w-3.5 h-3.5 text-[#22C55E]" />
              <span>Operator potwierdził wysłanie. Doręczenie nie jest potwierdzone.</span>
            </div>
          )}

          {/* Messages Bubbles List */}
          <div
            data-testid="sms_list"
            className="flex-1 overflow-y-auto p-4 space-y-4"
          >
            {activeThreadMessages.length === 0 ? (
              <div className="py-20 flex flex-col items-center justify-center text-center space-y-3 text-[#9499AD]">
                <ContactAvatar name={activeDisplayName || '?'} size="lg" />
                <p className="text-base font-bold text-white">Zacznij rozmowę</p>
                <p className="text-xs text-[#6C6F85]">
                  Napisz pierwszą wiadomość poniżej.
                </p>
              </div>
            ) : (
              activeThreadMessages.map((sms, idx) => {
                const prev = activeThreadMessages[idx - 1];
                const showDate =
                  !prev ||
                  new Date(prev.time).toDateString() !==
                    new Date(sms.time).toDateString();

                const analysis = analyzeMessage(sms.body, certSnapshot);
                const hasPhishingHit = analysis.findings.some(
                  f => f.matchedDomain !== null
                );
                const hasSignals = analysis.localSignals.length > 0;

                const bubbleRadius = appearance.rounded
                  ? 'rounded-3xl'
                  : 'rounded-xl';

                return (
                  <React.Fragment key={sms.id}>
                    {showDate && (
                      <div className="flex justify-center my-3">
                        <span className="text-[11px] font-medium text-[#6C6F85] bg-[#1A1B30]/70 px-3 py-1 rounded-full border border-[#272540]">
                          {formatDateLabel(sms.time)}
                        </span>
                      </div>
                    )}

                    <div
                      className={`flex w-full ${
                        sms.incoming ? 'justify-start' : 'justify-end'
                      }`}
                    >
                      <div
                        className={`max-w-[85%] sm:max-w-md ${bubbleRadius} p-3.5 space-y-2 shadow-md transition-all ${
                          sms.incoming
                            ? 'bg-[#1A1B30] text-white border border-[#272540]'
                            : 'bg-[#231A45] text-[#E8DEF8] border border-[#7B4DFF]/30'
                        }`}
                      >
                        <p
                          className={`break-words select-text ${
                            appearance.largeText
                              ? 'text-base leading-relaxed'
                              : 'text-sm'
                          }`}
                        >
                          {sms.body}
                        </p>

                        {/* CERT phishing alert banner */}
                        {hasPhishingHit && (
                          <div className="flex items-center gap-1.5 p-2 rounded-xl bg-red-950/60 border border-red-500/40 text-red-300 text-xs font-semibold">
                            <AlertCircle className="w-4 h-4 text-red-400 flex-shrink-0" />
                            <span>Uwaga · domena z listy CERT</span>
                          </div>
                        )}

                        {/* Keyword signals alert */}
                        {!hasPhishingHit && hasSignals && (
                          <div className="flex items-center gap-1.5 p-1.5 rounded-lg bg-amber-950/50 border border-amber-500/30 text-amber-300 text-xs">
                            <AlertCircle className="w-3.5 h-3.5 text-amber-400 flex-shrink-0" />
                            <span>Warto sprawdzić tę treść</span>
                          </div>
                        )}

                        {/* Footer with time & Shield check shortcut */}
                        <div className="flex items-center justify-between text-[10px] text-[#9499AD] pt-1">
                          <span>
                            {new Date(sms.time).toLocaleTimeString('pl-PL', {
                              hour: '2-digit',
                              minute: '2-digit'
                            })}
                            {!sms.incoming && ' · wysłano'}
                          </span>

                          <button
                            onClick={() => handleCheckText(sms.body)}
                            className="flex items-center gap-1 text-purple-300 hover:text-white px-1.5 py-0.5 rounded hover:bg-white/5 transition-colors"
                            title="Sprawdź treść wiadomości w skanerze"
                          >
                            <Shield className="w-3.5 h-3.5 text-[#A875FF]" />
                            <span>Sprawdź</span>
                          </button>
                        </div>
                      </div>
                    </div>
                  </React.Fragment>
                );
              })
            )}
          </div>

          {/* Bottom Compose Input */}
          <div className="p-3 bg-[#131424] border-t border-[#272540] space-y-2">
            <div className="flex items-end gap-2 bg-[#1A1B30] border border-[#272540] rounded-2xl p-2 focus-within:border-[#7B4DFF]">
              <textarea
                value={messageBody}
                onChange={e => handleDraftChange(e.target.value.slice(0, 2000))}
                placeholder="Wiadomość SMS"
                data-testid="sms_body"
                rows={1}
                className="flex-1 bg-transparent text-white text-sm focus:outline-none resize-none max-h-28 placeholder:text-[#6C6F85] px-2 py-1"
              />
              <button
                onClick={() => setShowConfirmSend(true)}
                disabled={!messageBody.trim() || !activeAddress.trim() || isSendingSms}
                data-testid="sms_send"
                className="w-10 h-10 rounded-xl bg-[#7B4DFF] hover:bg-[#6a3ee0] disabled:opacity-30 disabled:hover:bg-[#7B4DFF] text-white flex items-center justify-center transition-all flex-shrink-0 shadow-md"
              >
                <Send className="w-4 h-4" />
              </button>
            </div>

            {messageBody.length > 0 && (
              <div className="flex items-center justify-between text-xs text-[#9499AD] px-2">
                <button
                  onClick={() => handleCheckText(messageBody)}
                  className="text-[#A875FF] hover:underline font-medium"
                >
                  Sprawdź przed wysłaniem
                </button>
                <span>
                  {smsPartsCount} SMS · {messageBody.length}/2000
                </span>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Confirm Send Modal */}
      {showConfirmSend && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-[#131424] border border-[#272540] rounded-3xl p-6 max-w-sm w-full space-y-4 shadow-2xl">
            <h3 className="text-lg font-bold text-white">Wyślij SMS?</h3>
            <div className="p-3 bg-[#1A1B30] rounded-2xl text-xs space-y-1 text-white/90">
              <p className="font-semibold text-[#A875FF]">Do: {activeAddress}</p>
              <p className="text-[#9499AD] italic break-words">{messageBody}</p>
            </div>
            <p className="text-xs text-[#9499AD]">
              Wysyłka przez domyślną kartę SIM. Dłuższy tekst może zostać podzielony na kilka płatnych SMS-ów według taryfy operatora.
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setShowConfirmSend(false)}
                className="flex-1 py-2.5 rounded-xl bg-[#1A1B30] text-[#9499AD] hover:text-white text-xs font-semibold"
              >
                Wróć do edycji
              </button>
              <button
                onClick={() => {
                  setShowConfirmSend(false);
                  handleSend();
                }}
                className="flex-1 py-2.5 rounded-xl bg-[#7B4DFF] text-white text-xs font-semibold hover:bg-[#6a3ee0]"
              >
                Wyślij przez operatora
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Settings / Info Modal */}
      {showInfoModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-[#131424] border border-[#272540] rounded-3xl p-6 max-w-sm w-full space-y-4 shadow-2xl">
            <h3 className="text-lg font-bold text-white">Wiadomości i ochrona</h3>
            <p className="text-xs text-[#9499AD] leading-relaxed">
              SMS-y pozostają na urządzeniu. Ostrzeżenia korzystają z zapisanej listy CERT; brak ostrzeżenia nie potwierdza bezpieczeństwa.
            </p>
            <div className="text-xs text-white/80 bg-[#1A1B30] p-3 rounded-xl border border-[#272540]">
              Widok obejmuje historię wiadomości oraz wykrywanie podejrzanych linków i sygnałów wyłudzeń.
            </div>
            <button
              onClick={() => setShowInfoModal(false)}
              className="w-full py-2.5 rounded-xl bg-[#7B4DFF] text-white text-sm font-semibold hover:bg-[#6a3ee0]"
            >
              Zamknij
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
