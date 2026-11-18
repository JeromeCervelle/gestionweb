package fr.limoges.valadon.gestionfioul.web.participation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import fr.limoges.valadon.gestionfioul.web.api.ApiConfig;
import fr.limoges.valadon.gestionfioul.web.api.ApiException;

@RestClientTest(ParticipationClient.class)
@Import(ApiConfig.class)
class ParticipationClientTest {

    private static final String API = "http://localhost:9999";

    private static final String PARTICIPATION_JSON = """
            {"id":"p1","campagneId":"c1","clientId":"client-1","volumeSouhaite":3000,
             "adresseLivraison":"1 rue A","statut":"%s","dateParticipation":"2026-11-14T10:30:00"}
            """;

    @Autowired
    private ParticipationClient participationClient;

    @Autowired
    private MockRestServiceServer api;

    @Test
    void participer_poste_le_volume_et_l_adresse_sur_la_campagne() {
        api.expect(requestTo(API + "/api/campagnes/c1/participations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"volumeSouhaite":3000,"adresseLivraison":"1 rue A"}
                        """))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(PARTICIPATION_JSON.formatted("CONFIRMEE")));

        Participation participation = participationClient.participer("c1", new ParticiperRequest(3000, "1 rue A"));

        assertThat(participation.id()).isEqualTo("p1");
        assertThat(participation.statut()).isEqualTo(StatutParticipation.CONFIRMEE);
        assertThat(participation.dateParticipation()).isEqualTo(LocalDateTime.of(2026, 11, 14, 10, 30));
        api.verify();
    }

    @Test
    void participer_refuse_par_l_api_leve_une_api_exception_409() {
        api.expect(requestTo(API + "/api/campagnes/c1/participations"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON).body("""
                        {"status":409,"detail":"Ce client participe déjà à la campagne : annulez sa participation pour la refaire"}
                        """));

        assertThatThrownBy(() -> participationClient.participer("c1", new ParticiperRequest(3000, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageStartingWith("Ce client participe déjà");
    }

    @Test
    void mes_participations() {
        api.expect(requestTo(API + "/api/moi/participations"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[" + PARTICIPATION_JSON.formatted("ANNULEE") + "]", MediaType.APPLICATION_JSON));

        List<Participation> participations = participationClient.mesParticipations();

        assertThat(participations).singleElement()
                .extracting(Participation::statut).isEqualTo(StatutParticipation.ANNULEE);
    }

    @Test
    void lister_les_participations_d_une_campagne() {
        api.expect(requestTo(API + "/api/campagnes/c1/participations"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[" + PARTICIPATION_JSON.formatted("CONFIRMEE") + "]", MediaType.APPLICATION_JSON));

        assertThat(participationClient.listerParCampagne("c1")).singleElement()
                .extracting(Participation::volumeSouhaite).isEqualTo(3000);
    }

    @Test
    void annuler_poste_sur_la_route_d_annulation() {
        api.expect(requestTo(API + "/api/participations/p1/annuler"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(PARTICIPATION_JSON.formatted("ANNULEE"), MediaType.APPLICATION_JSON));

        assertThat(participationClient.annuler("p1").statut()).isEqualTo(StatutParticipation.ANNULEE);
        api.verify();
    }
}
