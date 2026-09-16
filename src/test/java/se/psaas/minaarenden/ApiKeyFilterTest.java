package se.psaas.minaarenden;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {"minaarenden.client-id=konsument", "minaarenden.client-secret=hemligt", "minaarenden.admin-api-key=adminnyckel"})
class ApiKeyFilterTest {

    @Autowired
    MockMvc mvc;

    private static final String FRAGA = """
            {"fraga":{"parter":[{"kund":{"identifierare":"199009090000","typ":"Personnummer"}}]},"anvandare":"199009090000"}
            """;

    @Test
    void fraganKraverClientIdOchSecretNarDeArKonfigurerade() throws Exception {
        mvc.perform(post("/kundhandelseFragaSynkron").contentType(MediaType.APPLICATION_JSON)
                        .header("skv_client_correlation_id", "x").content(FRAGA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Unauthorized")));
        mvc.perform(post("/kundhandelseFragaSynkron").contentType(MediaType.APPLICATION_JSON)
                        .header("skv_client_correlation_id", "x").header("client_id", "konsument").header("client_secret", "fel").content(FRAGA))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/kundhandelseFragaSynkron").contentType(MediaType.APPLICATION_JSON)
                        .header("skv_client_correlation_id", "x").header("client_id", "konsument").header("client_secret", "hemligt").content(FRAGA))
                .andExpect(status().isOk());
    }

    @Test
    void cachenKraverApiNyckel() throws Exception {
        mvc.perform(get("/kundhandelser")).andExpect(status().isUnauthorized());
        mvc.perform(get("/kundhandelser").header("X-Api-Key", "adminnyckel")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
