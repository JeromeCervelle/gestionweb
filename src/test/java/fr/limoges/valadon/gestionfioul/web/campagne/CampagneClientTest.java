package fr.limoges.valadon.gestionfioul.web.campagne;

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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import fr.limoges.valadon.gestionfioul.web.api.ApiConfig;
import fr.limoges.valadon.gestionfioul.web.api.ApiException;

/**
 * @RestClientTest : charge le client testé + le RestClient.Builder de Spring Boot, et remplace le réseau
 * par MockRestServiceServer. Aucune API réelle n'est appelée : on décrit la requête attendue et la réponse à rendre.
 * @Import(ApiConfig.class) : on teste avec NOTRE configuration (adresse de l'API, gestion des erreurs).
 */
@RestClientTest(CampagneClient.class)
@Import(ApiConfig.class)
class CampagneClientTest {

    private static final String API = "http://localhost:9999";

    @Autowired
    private CampagneClient campagneClient;

    @Autowired
    private MockRestServiceServer api;

    @Test
    void creer_poste_la_campagne_et_renvoie_son_id() {
        api.expect(requestTo(API + "/api/campagnes"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"libelle":"Hiver","dateOuverture":"2026-12-01T08:00:00",
                         "dateCloture":"2026-12-31T23:59:00","zoneLivraison":"87"}
                        """))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"id":"c9","libelle":"Hiver","dateOuverture":"2026-12-01T08:00:00","dateCloture":"2026-12-31T23:59:00",
                         "statut":"BROUILLON","volumeTotal":0,"zoneLivraison":"87","propositionRetenueId":null}
                        """));

        Campagne creee = campagneClient.creer(new CreerCampagneRequest("Hiver",
                LocalDateTime.of(2026, 12, 1, 8, 0), LocalDateTime.of(2026, 12, 31, 23, 59), "87"));

        assertThat(creee.id()).isEqualTo("c9");
        assertThat(creee.statut()).isEqualTo(StatutCampagne.BROUILLON);
        api.verify();
    }

    // une invocation par action : chacune doit appeler SA route de l'API
    @ParameterizedTest
    @CsvSource({"OUVRIR, ouvrir", "CLOTURER, cloturer", "ATTRIBUER, attribution", "LIVRER, livrer", "ANNULER, annuler"})
    void executer_poste_sur_la_route_de_l_action(ActionCampagne action, String segment) {
        api.expect(requestTo(API + "/api/campagnes/c1/" + segment))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        campagneClient.executer("c1", action);
        api.verify();
    }

    @Test
    void transition_refusee_par_l_api() {
        api.expect(requestTo(API + "/api/campagnes/c1/livrer"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON).body("""
                        {"status":409,"detail":"La campagne est OUVERTE, elle devrait être ATTRIBUEE"}
                        """));

        assertThatThrownBy(() -> campagneClient.executer("c1", ActionCampagne.LIVRER))
                .isInstanceOf(ApiException.class)
                .hasMessage("La campagne est OUVERTE, elle devrait être ATTRIBUEE");
    }

    @Test
    void lister_convertit_le_json_en_campagnes() {
        api.expect(requestTo(API + "/api/campagnes?statut=OUVERTE"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":"c1","libelle":"Commande d'hiver",
                          "dateOuverture":"2026-09-01T08:00:00","dateCloture":"2026-12-31T23:59:00",
                          "statut":"OUVERTE","volumeTotal":0,"zoneLivraison":"87","propositionRetenueId":null}]
                        """, MediaType.APPLICATION_JSON));

        List<Campagne> campagnes = campagneClient.listerParStatut(StatutCampagne.OUVERTE);

        assertThat(campagnes).hasSize(1);
        Campagne campagne = campagnes.get(0);
        assertThat(campagne.id()).isEqualTo("c1");
        assertThat(campagne.libelle()).isEqualTo("Commande d'hiver");
        assertThat(campagne.statut()).isEqualTo(StatutCampagne.OUVERTE);
        assertThat(campagne.dateCloture()).isEqualTo(LocalDateTime.of(2026, 12, 31, 23, 59));
        api.verify();
    }

    @Test
    void erreur_avec_problem_detail_reprend_le_message_de_l_api() {
        api.expect(requestTo(API + "/api/campagnes/c1"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("""
                                {"type":"about:blank","title":"Not Found","status":404,
                                 "detail":"Campagne introuvable : c1","instance":"/api/campagnes/c1"}
                                """));

        assertThatThrownBy(() -> campagneClient.trouver("c1"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Campagne introuvable : c1")
                .extracting(e -> ((ApiException) e).getStatut().value())
                .isEqualTo(404);
    }

    @Test
    void erreur_401_sans_corps_donne_un_message_par_defaut() {
        // Spring Security répond 401 sans ProblemDetail
        api.expect(requestTo(API + "/api/campagnes?statut=OUVERTE"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> campagneClient.listerParStatut(StatutCampagne.OUVERTE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Authentification refusée");
    }

    @Test
    void trouver_une_campagne_par_son_id() {
        // cas trouvé grâce au rapport JaCoCo : trouver() n'était testé que dans les contrôleurs, où il est simulé
        api.expect(requestTo(API + "/api/campagnes/c1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"c1","libelle":"Commande d'hiver",
                         "dateOuverture":"2026-09-01T08:00:00","dateCloture":"2026-12-31T23:59:00",
                         "statut":"ATTRIBUEE","volumeTotal":7000,"zoneLivraison":"87","propositionRetenueId":"pr1"}
                        """, MediaType.APPLICATION_JSON));

        Campagne campagne = campagneClient.trouver("c1");

        assertThat(campagne.statut()).isEqualTo(StatutCampagne.ATTRIBUEE);
        assertThat(campagne.volumeTotal()).isEqualTo(7000);
        assertThat(campagne.propositionRetenueId()).isEqualTo("pr1");
        api.verify();
    }

    @Test
    void erreur_403_sans_corps_donne_un_message_par_defaut() {
        api.expect(requestTo(API + "/api/campagnes/c1"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> campagneClient.trouver("c1"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Vous n'avez pas les droits nécessaires pour cette action.");
    }

    @Test
    void erreur_avec_un_corps_qui_n_est_pas_du_json_donne_un_message_par_defaut() {
        api.expect(requestTo(API + "/api/campagnes/c1"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY)
                        .contentType(MediaType.TEXT_HTML)
                        .body("<html><body>Bad Gateway</body></html>"));

        assertThatThrownBy(() -> campagneClient.trouver("c1"))
                .isInstanceOf(ApiException.class)
                .hasMessage("L'API a renvoyé une erreur (code 502).");
    }
}
