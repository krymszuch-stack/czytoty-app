import { createClient, SupabaseClient } from '@supabase/supabase-js';
import { CloudNote, PhoneNote } from '../types';

const SUPABASE_URL = (import.meta as any).env?.VITE_SUPABASE_URL || '';
const SUPABASE_KEY = (import.meta as any).env?.VITE_SUPABASE_PUBLISHABLE_KEY || '';

let client: SupabaseClient | null = null;

export const CloudRepository = {
  isConfigured(): boolean {
    return Boolean(
      SUPABASE_URL &&
      SUPABASE_URL.startsWith('https://') &&
      SUPABASE_KEY &&
      SUPABASE_KEY.length > 10
    );
  },

  getClient(): SupabaseClient {
    if (!client) {
      if (!this.isConfigured()) {
        throw new Error('Brak konfiguracji Supabase w tej kompilacji.');
      }
      client = createClient(SUPABASE_URL, SUPABASE_KEY);
    }
    return client;
  },

  async getUserEmail(): Promise<string | null> {
    if (!this.isConfigured()) return null;
    try {
      const { data } = await this.getClient().auth.getUser();
      return data.user?.email || null;
    } catch {
      return null;
    }
  },

  async checkConnection(): Promise<any[]> {
    if (!this.isConfigured()) {
      throw new Error('Brak konfiguracji Supabase.');
    }
    const { data, error } = await this.getClient().from('data_sources').select('*');
    if (error) throw error;
    return data || [];
  },

  async signIn(email: string, password: string): Promise<string> {
    const { data, error } = await this.getClient().auth.signInWithPassword({
      email: email.trim(),
      password
    });
    if (error) throw error;
    return data.user?.email || email;
  },

  async signUp(email: string, password: string): Promise<{ confirmed: boolean }> {
    const { data, error } = await this.getClient().auth.signUp({
      email: email.trim(),
      password
    });
    if (error) throw error;
    const hasSession = Boolean(data.session);
    return { confirmed: hasSession };
  },

  async signOut(): Promise<void> {
    if (!this.isConfigured()) return;
    await this.getClient().auth.signOut();
  },

  async saveNote(note: PhoneNote): Promise<CloudNote> {
    const { data: userData } = await this.getClient().auth.getUser();
    const user = userData?.user;
    if (!user) throw new Error('Zaloguj się, aby zapisać notatkę w chmurze.');

    const cloudNote: CloudNote = {
      userId: user.id,
      phone: note.phone,
      label: note.label,
      note: note.note
    };

    const { error } = await this.getClient()
      .from('private_notes')
      .upsert({
        user_id: user.id,
        phone: note.phone,
        label: note.label,
        note: note.note
      });

    if (error) throw error;
    return cloudNote;
  },

  async loadNotes(): Promise<CloudNote[]> {
    const { data: userData } = await this.getClient().auth.getUser();
    const user = userData?.user;
    if (!user) throw new Error('Brak zalogowanego użytkownika.');

    const { data, error } = await this.getClient()
      .from('private_notes')
      .select('*')
      .eq('user_id', user.id)
      .limit(100);

    if (error) throw error;

    return (data || []).map((row: any) => ({
      userId: row.user_id,
      phone: row.phone,
      label: row.label || '',
      note: row.note || ''
    }));
  },

  async deleteNote(phone: string): Promise<void> {
    const { data: userData } = await this.getClient().auth.getUser();
    const user = userData?.user;
    if (!user) throw new Error('Brak zalogowanego użytkownika.');

    const { error } = await this.getClient()
      .from('private_notes')
      .delete()
      .eq('user_id', user.id)
      .eq('phone', phone);

    if (error) throw error;
  }
};
