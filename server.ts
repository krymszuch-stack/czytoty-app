import express, { Request, Response } from 'express';
import { createServer as createViteServer } from 'vite';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const app = express();
const PORT = parseInt(process.env.PORT || '3000', 10);

app.use(express.json());

// In-memory CERT Cache
const CERT_SOURCE = 'https://hole.cert.pl/domains/v2/domains.txt';
const CERT_MAX_AGE_MS = 5 * 60 * 1000;

interface CertCache {
  fetchedAt: number;
  domains: string[];
}

let certCache: CertCache | null = null;
let isRefreshing = false;

async function fetchCertDomains(): Promise<CertCache> {
  if (certCache && Date.now() - certCache.fetchedAt < CERT_MAX_AGE_MS) {
    return certCache;
  }

  isRefreshing = true;
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 15000);
    const res = await fetch(CERT_SOURCE, {
      signal: controller.signal,
      headers: {
        'User-Agent': 'CzyToTy-Prototype-Web/0.6.0'
      }
    });
    clearTimeout(timeout);

    if (!res.ok) {
      throw new Error(`CERT server returned HTTP ${res.status}`);
    }

    const text = await res.text();
    const rawLines = text.split('\n');
    const domainSet = new Set<string>();

    for (const raw of rawLines) {
      const line = raw.trim().toLowerCase().replace(/\.$/, '');
      if (line && line.length <= 253 && line.includes('.')) {
        domainSet.add(line);
      }
    }

    certCache = {
      fetchedAt: Date.now(),
      domains: Array.from(domainSet)
    };
    return certCache;
  } catch (err) {
    if (certCache) {
      // Return stale cache if available
      return certCache;
    }
    // Minimal fallback seed if network unavailable
    return {
      fetchedAt: Date.now(),
      domains: [
        'doplata-paczka-inpost.info',
        'pge-faktura-zaleglosc.top',
        'sms-autoryzacja-mbank.cc',
        'bezpieczny-blik-odbior.pw'
      ]
    };
  } finally {
    isRefreshing = false;
  }
}

// CERT API Endpoint
app.get('/api/cert', async (_req: Request, res: Response) => {
  try {
    const force = _req.query.force === 'true';
    if (force || !certCache || Date.now() - certCache.fetchedAt >= CERT_MAX_AGE_MS) {
      await fetchCertDomains();
    }
    res.json({
      success: true,
      fetchedAt: certCache?.fetchedAt || Date.now(),
      domains: certCache?.domains || [],
      isFresh: certCache ? Date.now() - certCache.fetchedAt <= CERT_MAX_AGE_MS : false,
      count: certCache?.domains.length || 0
    });
  } catch (error: any) {
    res.status(500).json({
      success: false,
      error: error?.message || 'Nie udało się pobrać listy CERT'
    });
  }
});

// Cloud status ping
app.get('/api/health', (_req: Request, res: Response) => {
  res.json({
    status: 'ok',
    version: '0.6.0-prototype',
    timestamp: Date.now()
  });
});

async function startServer() {
  const isProduction = process.env.NODE_ENV === 'production';

  if (!isProduction) {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa'
    });
    app.use(vite.middlewares);
  } else {
    app.use(express.static(path.resolve(__dirname, 'dist')));
    app.get('*', (_req: Request, res: Response) => {
      res.sendFile(path.resolve(__dirname, 'dist', 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`CzyToTy? server running at http://0.0.0.0:${PORT}`);
  });
}

startServer();
