package se.psaas.minaarenden;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import se.psaas.minaarenden.api.dto.Kund;
import se.psaas.minaarenden.api.dto.NyKundhandelse;
import se.psaas.minaarenden.api.dto.Part;
import se.psaas.minaarenden.service.SeedService;

class SeedServiceTest {

    @Test
    void prefixBytsBaraIIdOchTyp() {
        NyKundhandelse n = new NyKundhandelse("REFKOM-BYGG-1", new Part(new Kund("199009090000", null, "Personnummer"), null),
                "REFKOM nämns i rubriken", "Beskrivning", null, "2026-01-01T00:00:00Z", "REFKOM.BYGGLOV.BESLUT", false, true, null, null, null, null);
        NyKundhandelse m = SeedService.medPrefix(n, "TESTKOP");
        assertEquals("TESTKOP-BYGG-1", m.kundhandelseId());
        assertEquals("TESTKOP.BYGGLOV.BESLUT", m.kundhandelseTyp());
        assertEquals("REFKOM nämns i rubriken", m.rubrik());
    }
}
