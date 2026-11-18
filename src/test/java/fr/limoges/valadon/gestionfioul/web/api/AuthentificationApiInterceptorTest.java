package fr.limoges.valadon.gestionfioul.web.api;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.client.MockRestServiceServer;

import fr.limoges.valadon.gestionfioul.web.campagne.CampagneClient;
import fr.limoges.valadon.gestionfioul.web.campagne.StatutCampagne;
import fr.limoges.valadon.gestionfioul.web.security.UtilisateurConnecte;
import fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest;

/**
 * Vérifie, à travers un client quelconque (CampagneClient), que l'intercepteur ajoute
 * les identifiants de l'utilisateur connecté à la requête envoyée à l'API.
 */
@RestClientTest(CampagneClient.class)
@Import(ApiConfig.class)
class AuthentificationApiInterceptorTest {

    @Autowired
    private CampagneClient campagneClient;

    @Autowired
    private MockRestServiceServer api;

    // le contexte de sécurité est lié au thread : on le vide pour ne pas polluer les autres tests
    @AfterEach
    void viderLeContexteDeSecurite() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void utilisateur_connecte_ses_identifiants_sont_envoyes_a_l_api() {
        UtilisateurConnecte alice = UtilisateursDeTest.unClient();
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(alice, null, alice.autorites()));
        String attendu = "Basic " + Base64.getEncoder()
                .encodeToString("alice@test.fr:motdepasse1".getBytes(StandardCharsets.UTF_8));

        api.expect(requestTo("http://localhost:9999/api/campagnes?statut=OUVERTE"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, attendu))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        campagneClient.listerParStatut(StatutCampagne.OUVERTE);
        api.verify();
    }

    @Test
    void personne_n_est_connecte_aucun_en_tete_authorization() {
        api.expect(requestTo("http://localhost:9999/api/campagnes?statut=OUVERTE"))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        campagneClient.listerParStatut(StatutCampagne.OUVERTE);
        api.verify();
    }
}
