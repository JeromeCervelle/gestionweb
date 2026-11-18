package fr.limoges.valadon.gestionfioul.web.accueil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import fr.limoges.valadon.gestionfioul.web.api.ApiConfig;

@RestClientTest(EtatApiClient.class)
@Import(ApiConfig.class)
class EtatApiClientTest {

    @Autowired
    private EtatApiClient etatApiClient;

    @Autowired
    private MockRestServiceServer api;

    @Test
    void api_joignable_donne_son_nom_et_sa_version() {
        api.expect(requestTo("http://localhost:9999/v3/api-docs"))
                .andRespond(withSuccess("""
                        {"openapi":"3.1.0","info":{"title":"Gestion Fioul","version":"1.0"},"paths":{}}
                        """, MediaType.APPLICATION_JSON));

        assertThat(etatApiClient.consulter()).isEqualTo(EtatApi.disponible("Gestion Fioul", "1.0"));
    }

    @Test
    void api_arretee_donne_indisponible_sans_exception() {
        // simule "connexion refusée" : RestClient la transforme en ResourceAccessException
        api.expect(requestTo("http://localhost:9999/v3/api-docs"))
                .andRespond(withException(new ConnectException("Connection refused")));

        assertThat(etatApiClient.consulter().disponible()).isFalse();
    }

    @Test
    void documentation_absente_donne_indisponible() {
        api.expect(requestTo("http://localhost:9999/v3/api-docs"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(etatApiClient.consulter().disponible()).isFalse();
    }
}
