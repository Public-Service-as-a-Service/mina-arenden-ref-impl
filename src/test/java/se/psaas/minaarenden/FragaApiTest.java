package se.psaas.minaarenden;

import static org.hamcrest.Matchers.hasSize;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
class FragaApiTest {

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
    void openApiOchHalsaFinns() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andExpect(jsonPath("$.paths['/kundhandelseFragaSynkron'].post.operationId", is("fraga")));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status", is("UP")));
    }
}
