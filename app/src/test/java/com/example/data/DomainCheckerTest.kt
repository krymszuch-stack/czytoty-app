package com.example.data

import org.junit.Assert.*
import org.junit.Test

class DomainCheckerTest {
    @Test fun subdomainsMatchButParentsAndLookalikesDoNot() {
        val feed = setOf("scam.example", "bad.example.org")
        assertEquals("scam.example", DomainChecker.match("login.scam.example", feed))
        assertNull(DomainChecker.match("notscam.example", feed))
        assertNull(DomainChecker.match("example.org", feed))
        assertNull(DomainChecker.match("scam.example.safe.example.org", feed))
    }
    @Test fun parsesRealUrlAuthorityInsteadOfDisplayedBrand() {
        assertEquals(listOf("evil.example"), DomainChecker.extractHosts("https://bank.pl@evil.example/pay"))
        assertEquals(listOf("bank.pl.evil.example"), DomainChecker.extractHosts("https://bank.pl.evil.example/"))
        assertEquals(listOf("evil.example"), DomainChecker.extractHosts("https://evil.example/?redirect=https://bank.pl"))
    }
    @Test fun handlesPunctuationUnicodeAndBareDomains() {
        assertEquals(listOf("example.org", "example.pl"), DomainChecker.extractHosts("Link (https://EXAMPLE.org/pay). Drugi: example.pl."))
        assertEquals("xn--bcher-kva.example", DomainChecker.normalizeDomain("BÜCHER.example."))
        assertEquals(listOf("xn--bcher-kva.example"), DomainChecker.extractHosts("https://bücher.example/test"))
    }
    @Test fun rejectsMalformedFeedsInsteadOfReplacingGoodCache() {
        listOf("", "<html>upstream unavailable</html>", "scam.example\nnot a domain").forEach {
            try { DomainChecker.parseFeed(it); fail("Malformed feed accepted") } catch (_: IllegalArgumentException) {}
            catch (_: IllegalStateException) {}
        }
    }
    @Test fun replacingFeedRemovesWithdrawnEntries() {
        val first = DomainChecker.parseFeed("old.example\ncurrent.example")
        val second = DomainChecker.parseFeed("current.example\n")
        assertNotNull(DomainChecker.match("old.example", first))
        assertNull(DomainChecker.match("old.example", second))
    }
    @Test fun noFeedIsUnknownAndNeverClaimsSafe() {
        val result = DomainChecker.analyze("https://example.org", null)
        assertNull(result.snapshotTime)
        assertNull(result.findings.single().matchedDomain)
    }
    @Test fun staleAndFutureSnapshotsAreNotFresh() {
        val snapshot = CertSnapshot(1_000_000, setOf("scam.example"))
        assertTrue(snapshot.isFresh(1_000_001))
        assertFalse(snapshot.isFresh(1_000_000 + CERT_MAX_AGE_MS + 1))
        assertFalse(snapshot.isFresh(999_999))
    }
    @Test fun localSignalsAreSeparateFromCertEvidence() {
        val result = DomainChecker.analyze("Pilne, podaj kod BLIK https://example.org", CertSnapshot(0, emptySet()))
        assertEquals(2, result.localSignals.size)
        assertNull(result.findings.single().matchedDomain)
    }
}
