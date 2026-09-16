package se.psaas.minaarenden;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class FragaApiTestBase {

    @Autowired
    MockMvc mvc;

    private MockHttpServletRequestBuilder fraga(String body) {
        return post("/kundhandelseFragaSynkron")
                .contentType(MediaType.APPLICATION_JSON)
                .header("skv_client_correlation_id", "0002aa29-49f2-4baf-be51-c7c39c9824b4")
                .header("Accept-Language", "sv")
                .content(body);
    }

    @Test
    void exempeldataLasesInVidStart() throws Exception {
        mvc.perform(get("/kundhandelser")).andExpect(status().isOk()).andExpect(jsonPath("$.antal", is(5)));
    }

    @Test
    void fragaPaKundGerAllaKundensHandelserNyastForst() throws Exception {
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}]},"anvandare":"199009090000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser", hasSize(1)))
                .andExpect(jsonPath("$.kundhandelser[0].totaltAntalKundhandelser", is(3)))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart", hasSize(3)))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[0].kundhandelseId", is("TESTKOP-FSK-2026-00777-1")))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[0].producent", is("Testköpings kommun")))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[0].version", is("6.1")))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[0].tidpunkt", is("2026-09-10T07:30:00+02:00")))
                .andExpect(jsonPath("$.metadata.onskatSprak", is("sv")))
                .andExpect(jsonPath("$.delfragor", hasSize(1)))
                .andExpect(jsonPath("$.delfragor[0].producent", is("Testköpings kommun")))
                .andExpect(jsonPath("$.delfragor[0].status", is("OK")))
                .andExpect(jsonPath("$.delfragor[0].httpCode", is(200)));
    }

    @Test
    void fragaPaKundOchArendeSmalnarAv() throws Exception {
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"},"arende":{"identifierare":"BYGG-2026-00123","typ":"Diarienummer"}}]},"anvandare":"199009090000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser[0].totaltAntalKundhandelser", is(2)))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[0].kundhandelseTyp", is("TESTKOP.BYGGLOV.KOMPLETTERING_BEGARD")))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[0].producentarendetKraverKundatgard", is(true)))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[0].utokadInformation.senastDatum", is("2026-09-30")));
    }

    @Test
    void kundhandelseTypMatcharPrefixOchExaktTyp() throws Exception {
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],"kundhandelseTyper":["TESTKOP.BYGGLOV"]},"anvandare":"199009090000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser[0].totaltAntalKundhandelser", is(2)));
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],"kundhandelseTyper":["TESTKOP.BYGGLOV.ANSOKAN_MOTTAGEN"]},"anvandare":"199009090000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser[0].totaltAntalKundhandelser", is(1)));
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],"kundhandelseTyper":["TESTKOP.BYGG"]},"anvandare":"199009090000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser[0].totaltAntalKundhandelser", is(0)));
    }

    @Test
    void taggarDatumSorteringOchPagineringFungerar() throws Exception {
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],"taggar":["kundatgard"]},"anvandare":"199009090000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser[0].totaltAntalKundhandelser", is(2)));
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],"startDatum":"2026-09-01","slutDatum":"2026-09-02"},"anvandare":"199009090000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser[0].totaltAntalKundhandelser", is(1)))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[0].kundhandelseId", is("TESTKOP-BYGG-2026-00123-2")));
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],
                 "behandling":{"sortering":[{"attribut":"TIDPUNKT","stigande":true}],"paginering":{"offset":1,"limit":1}}},"anvandare":"199009090000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser[0].totaltAntalKundhandelser", is(3)))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart", hasSize(1)))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[0].kundhandelseId", is("TESTKOP-BYGG-2026-00123-2")))
                .andExpect(jsonPath("$.metadata.behandling.paginering.limit", is(1)));
    }

    @Test
    void fleraParterBesvarasVarForSig() throws Exception {
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}},{"kund":{"identifierare":"165560001234","typ":"Organisationsnummer"}},{"arende":{"identifierare":"FINNS-INTE","typ":"Diarienummer"}}]},"anvandare":"199009090000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser", hasSize(3)))
                .andExpect(jsonPath("$.kundhandelser[1].totaltAntalKundhandelser", is(2)))
                .andExpect(jsonPath("$.kundhandelser[2].totaltAntalKundhandelser", is(0)))
                .andExpect(jsonPath("$.delfragor", hasSize(3)));
    }

    @Test
    void felaktigaFragorGer400MedMessage() throws Exception {
        mvc.perform(post("/kundhandelseFragaSynkron").contentType(MediaType.APPLICATION_JSON).content("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}]},"anvandare":"199009090000"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Bad request: headern skv_client_correlation_id saknas")));
        mvc.perform(fraga("""
                {"fraga":{"parter":[{}]},"anvandare":"199009090000"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Bad request: Varje part måste innehålla kund, ärende eller båda")));
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"12345","typ":"Personnummer"}}]},"anvandare":"abc"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Bad request: anvandare: anvandare ska vara 12 siffror; fraga.parter[0].kund.identifierare: kund.identifierare ska vara 12 siffror")));
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],"behandling":{"paginering":{"offset":0}}},"anvandare":"199009090000"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Bad request: Paginering kräver att sortering anges")));
        mvc.perform(fraga("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cachenKanUppdaterasOchRensas() throws Exception {
        String ny = """
                [{"kundhandelseId":"TEST-1","part":{"kund":{"identifierare":"198001010101","typ":"Personnummer"}},
                  "rubrik":"Testhändelse","beskrivning":"Beskrivning","tidpunkt":"2026-09-15 12:00:00",
                  "kundhandelseTyp":"TESTKOP.TEST.SKAPAD","producentarendetKraverKundatgard":false,"producentarendetKlart":false}]
                """;
        mvc.perform(post("/kundhandelser").contentType(MediaType.APPLICATION_JSON).content(ny))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.antal", is(1)));
        mvc.perform(get("/kundhandelser/TEST-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelse.tidpunkt", is("2026-09-15T12:00:00+02:00")))
                .andExpect(jsonPath("$.kundhandelse.sprak", is("sv")))
                .andExpect(jsonPath("$.kundhandelse.version", is("6.1")));
        // Samma id igen ersätter i stället för att dubblera.
        mvc.perform(post("/kundhandelser").contentType(MediaType.APPLICATION_JSON).content(ny.replace("Testhändelse", "Uppdaterad")))
                .andExpect(status().isCreated());
        mvc.perform(get("/kundhandelser/TEST-1")).andExpect(jsonPath("$.kundhandelse.rubrik", is("Uppdaterad")));
        mvc.perform(delete("/kundhandelser/TEST-1")).andExpect(status().isNoContent());
        mvc.perform(get("/kundhandelser/TEST-1")).andExpect(status().isNotFound()).andExpect(jsonPath("$.message", is("Not found")));
        mvc.perform(post("/kundhandelser").contentType(MediaType.APPLICATION_JSON).content("""
                [{"kundhandelseId":"X","part":{},"rubrik":"r","beskrivning":"b","tidpunkt":"2026-01-01T00:00:00Z","kundhandelseTyp":"TESTKOP.A.B","producentarendetKraverKundatgard":false,"producentarendetKlart":false}]
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ensamtProducentprefixMatcharAllaTyper() throws Exception {
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],"kundhandelseTyper":["TESTKOP"]},"anvandare":"199009090000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser[0].totaltAntalKundhandelser", is(3)));
    }

    @Test
    void sorteringOchPagineringUtanLimitGerRestenAvListan() throws Exception {
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],
                 "behandling":{"sortering":[{"attribut":"RUBRIK","stigande":true}],"paginering":{"offset":1}}},"anvandare":"199009090000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser[0].totaltAntalKundhandelser", is(3)))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart", hasSize(2)))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[0].rubrik", is("Ansökan om bygglov mottagen")))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[1].rubrik", is("Plats i förskola erbjuden")));
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],
                 "behandling":{"sortering":[{"attribut":"TIDPUNKT","stigande":false}],"paginering":{"offset":10,"limit":5}}},"anvandare":"199009090000"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser[0].totaltAntalKundhandelser", is(3)))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart", hasSize(0)));
    }

    @Test
    void tidpunktSorterasKronologisktAvenNarOffsetSkiljer() throws Exception {
        // Timmen 02:00-03:00 den 25 oktober 2026 finns två gånger i svensk tid. Som text sorterar "+01:00" före
        // "+02:00", men kronologiskt kommer +02:00 (sommartid) först. Sorteringen ska gå på tidpunkten, inte texten.
        mvc.perform(post("/kundhandelser").contentType(MediaType.APPLICATION_JSON).content("""
                [{"kundhandelseId":"DST-VINTER","part":{"kund":{"identifierare":"197001010001","typ":"Personnummer"}},
                  "rubrik":"Efter omställning","beskrivning":"b","tidpunkt":"2026-10-25T02:30:00+01:00",
                  "kundhandelseTyp":"TESTKOP.TEST.SKAPAD","producentarendetKraverKundatgard":false,"producentarendetKlart":false},
                 {"kundhandelseId":"DST-SOMMAR","part":{"kund":{"identifierare":"197001010001","typ":"Personnummer"}},
                  "rubrik":"Före omställning","beskrivning":"b","tidpunkt":"2026-10-25T02:30:00+02:00",
                  "kundhandelseTyp":"TESTKOP.TEST.SKAPAD","producentarendetKraverKundatgard":false,"producentarendetKlart":false}]
                """))
                .andExpect(status().isCreated());
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"197001010001","typ":"Personnummer"}}],
                 "behandling":{"sortering":[{"attribut":"TIDPUNKT","stigande":true}]}},"anvandare":"197001010001"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[0].kundhandelseId", is("DST-SOMMAR")))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[0].tidpunkt", is("2026-10-25T02:30:00+02:00")))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[1].kundhandelseId", is("DST-VINTER")))
                .andExpect(jsonPath("$.kundhandelser[0].kundhandelserForPart[1].tidpunkt", is("2026-10-25T02:30:00+01:00")));
        mvc.perform(delete("/kundhandelser/DST-SOMMAR")).andExpect(status().isNoContent());
        mvc.perform(delete("/kundhandelser/DST-VINTER")).andExpect(status().isNoContent());
    }

    @Test
    void granserForFraganGer400() throws Exception {
        String parter = java.util.stream.IntStream.range(0, 101)
                .mapToObj(i -> "{\"kund\":{\"identifierare\":\"199009090000\",\"typ\":\"Personnummer\"}}")
                .collect(java.util.stream.Collectors.joining(","));
        mvc.perform(fraga("{\"fraga\":{\"parter\":[" + parter + "]},\"anvandare\":\"199009090000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("högst 100 parter")));
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],
                 "behandling":{"sortering":[{"attribut":"TIDPUNKT","stigande":true}],"paginering":{"offset":0,"limit":5000}}},"anvandare":"199009090000"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("paginering.limit får vara högst 1000")));
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],"kundhandelseTyper":["testkop.bygglov"]},"anvandare":"199009090000"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("kundhandelseTyper har ogiltigt format")));
    }

    @Test
    void correlationIdMasteVaraIckeTomOchSkrivbar() throws Exception {
        String body = """
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}]},"anvandare":"199009090000"}
                """;
        mvc.perform(post("/kundhandelseFragaSynkron").contentType(MediaType.APPLICATION_JSON).header("skv_client_correlation_id", "   ").content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", startsWith("Bad request: skv_client_correlation_id")));
        mvc.perform(post("/kundhandelseFragaSynkron").contentType(MediaType.APPLICATION_JSON).header("skv_client_correlation_id", "x".repeat(201)).content(body))
                .andExpect(status().isBadRequest());
        // API-definitionen tillåter valfritt format, så ett id som inte är UUID accepteras.
        mvc.perform(post("/kundhandelseFragaSynkron").contentType(MediaType.APPLICATION_JSON).header("skv_client_correlation_id", "anrop-4711").content(body))
                .andExpect(status().isOk());
    }

    @Test
    void felmeddelandenAvslojarIntePaketOchKlassnamn() throws Exception {
        mvc.perform(fraga("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Bad request: meddelandet kan inte tolkas som JSON")));
        mvc.perform(fraga("""
                {"fraga":{"parter":"fel typ"},"anvandare":"199009090000"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Bad request: fältet fraga.parter har fel typ eller format")));
        mvc.perform(fraga("""
                {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}],"startDatum":"2026-13-45"},"anvandare":"199009090000"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Bad request: ogiltigt datum eller ogiltig tidpunkt")));
    }

    @Test
    void inlasningValiderasMotDomanOchDatabasregler() throws Exception {
        String mall = """
                [{"kundhandelseId":"VAL-1","part":{"kund":{"identifierare":"198001010101","typ":"Personnummer"}},
                  "rubrik":"%s","beskrivning":"b","tidpunkt":"2026-09-15T12:00:00+02:00",
                  "kundhandelseTyp":"%s","producentarendetKraverKundatgard":false,"producentarendetKlart":false}]
                """;
        // Fel producentprefix.
        mvc.perform(post("/kundhandelser").contentType(MediaType.APPLICATION_JSON).content(mall.formatted("r", "ANNAN.TEST.SKAPAD")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("måste börja med producentprefixet TESTKOP.")));
        // Tomt segment och för få delar.
        mvc.perform(post("/kundhandelser").contentType(MediaType.APPLICATION_JSON).content(mall.formatted("r", "TESTKOP..SKAPAD")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("kundhandelseTyp")));
        mvc.perform(post("/kundhandelser").contentType(MediaType.APPLICATION_JSON).content(mall.formatted("r", "TESTKOP.SKAPAD")))
                .andExpect(status().isBadRequest());
        // För lång rubrik ger 400, inte databasfel.
        mvc.perform(post("/kundhandelser").contentType(MediaType.APPLICATION_JSON).content(mall.formatted("x".repeat(256), "TESTKOP.TEST.SKAPAD")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("rubrik")));
        // Saknat obligatoriskt fält i ett listelement.
        mvc.perform(post("/kundhandelser").contentType(MediaType.APPLICATION_JSON).content("""
                [{"kundhandelseId":"VAL-2","part":{"kund":{"identifierare":"198001010101","typ":"Personnummer"}},"beskrivning":"b",
                  "tidpunkt":"2026-09-15T12:00:00+02:00","kundhandelseTyp":"TESTKOP.TEST.SKAPAD","producentarendetKraverKundatgard":false,"producentarendetKlart":false}]
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("rubrik")));
        // Tom lista och för stor lista.
        mvc.perform(post("/kundhandelser").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isBadRequest());
        String en = mall.formatted("r", "TESTKOP.TEST.SKAPAD").trim();
        String element = en.substring(1, en.length() - 1);
        String forManga = "[" + java.util.Collections.nCopies(1001, element).stream().collect(java.util.stream.Collectors.joining(",")) + "]";
        mvc.perform(post("/kundhandelser").contentType(MediaType.APPLICATION_JSON).content(forManga))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("högst 1000 kundhändelser")));
        mvc.perform(get("/kundhandelser/VAL-1")).andExpect(status().isNotFound());
    }

    @Test
    void openApiOchHalsaFinns() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/kundhandelseFragaSynkron'].post.operationId", is("fraga")))
                .andExpect(jsonPath("$.components.securitySchemes.client_id.in", is("header")))
                .andExpect(jsonPath("$.components.securitySchemes['X-Api-Key'].in", is("header")))
                .andExpect(jsonPath("$.paths['/kundhandelser'].post.security[0]['X-Api-Key']").exists());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status", is("UP")));
    }
}
