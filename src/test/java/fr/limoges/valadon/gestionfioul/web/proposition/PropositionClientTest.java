package fr.limoges.valadon.gestionfioul.web.proposition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
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

@RestClientTest(PropositionClient.class)
@Import(ApiConfig.class)
class PropositionClientTest {

    private static final String API = "http://localhost:9999";

    @Autowired
    private PropositionClient propositionClient;

    @Autowired
    private MockRestServiceServer api;

    @Test
    void soumettre_envoie_la_validite_et_les_paliers_en_json() {
        api.expect(requestTo(API + "/api/campagnes/c1/propositions"))
                .andExpect(method(HttpMethod.POST))
                // date au format ISO, volumeMax null pour le dernier palier, prix en nombre décimal
                .andExpect(content().json("""
                        {"validiteJusqua":"2027-01-31T18:00:00",
                         "paliers":[{"volumeMin":0,"volumeMax":5000,"prixLitre":1.10},
                                    {"volumeMin":5000,"volumeMax":null,"prixLitre":1.02}]}
                        """))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"id":"pr1","campagneId":"c1","fournisseurId":"f1","dateProposition":"2026-11-14T10:00:00",
                         "validiteJusqua":"2027-01-31T18:00:00","statut":"SOUMISE",
                         "paliers":[{"volumeMin":0,"volumeMax":5000,"prixLitre":1.10},
                                    {"volumeMin":5000,"volumeMax":null,"prixLitre":1.02}]}
                        """));

        Proposition proposition = propositionClient.soumettre("c1", new SoumettrePropositionRequest(
                LocalDateTime.of(2027, 1, 31, 18, 0),
                List.of(new Palier(0, 5000, new BigDecimal("1.10")), new Palier(5000, null, new BigDecimal("1.02")))));

        assertThat(proposition.statut()).isEqualTo(StatutProposition.SOUMISE);
        assertThat(proposition.paliers()).hasSize(2);
        assertThat(proposition.paliers().get(1).volumeMax()).isNull();
        // BigDecimal : comparer la VALEUR (1.1 et 1.10 sont égaux en valeur, pas pour equals())
        assertThat(proposition.paliers().get(0).prixLitre()).isEqualByComparingTo("1.10");
        api.verify();
    }

    @Test
    void bareme_refuse_par_l_api_leve_une_api_exception_avec_son_message() {
        api.expect(requestTo(API + "/api/campagnes/c1/propositions"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON).body("""
                        {"status":400,"detail":"Les paliers doivent se suivre sans trou ni chevauchement"}
                        """));

        assertThatThrownBy(() -> propositionClient.soumettre("c1",
                new SoumettrePropositionRequest(LocalDateTime.of(2027, 1, 31, 18, 0), List.of())))
                .isInstanceOf(ApiException.class)
                .hasMessage("Les paliers doivent se suivre sans trou ni chevauchement");
    }

    @Test
    void lister_les_propositions_d_une_campagne() {
        api.expect(requestTo(API + "/api/campagnes/c1/propositions"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [{"id":"pr1","campagneId":"c1","fournisseurId":"f1","dateProposition":"2026-11-14T10:00:00",
                          "validiteJusqua":"2027-01-31T18:00:00","statut":"SOUMISE",
                          "paliers":[{"volumeMin":0,"volumeMax":null,"prixLitre":1.05}]},
                         {"id":"pr2","campagneId":"c1","fournisseurId":"f2","dateProposition":"2026-11-14T11:00:00",
                          "validiteJusqua":"2027-01-31T18:00:00","statut":"SOUMISE",
                          "paliers":[{"volumeMin":0,"volumeMax":null,"prixLitre":1.03}]}]
                        """, MediaType.APPLICATION_JSON));

        assertThat(propositionClient.listerParCampagne("c1")).extracting(Proposition::fournisseurId)
                .containsExactly("f1", "f2");
    }

    @Test
    void mes_propositions() {
        api.expect(requestTo(API + "/api/moi/propositions"))
                .andRespond(withSuccess("""
                        [{"id":"pr1","campagneId":"c1","fournisseurId":"f1","dateProposition":"2026-11-14T10:00:00",
                          "validiteJusqua":"2027-01-31T18:00:00","statut":"RETENUE",
                          "paliers":[{"volumeMin":0,"volumeMax":null,"prixLitre":1.05}]}]
                        """, MediaType.APPLICATION_JSON));

        assertThat(propositionClient.mesPropositions()).singleElement()
                .extracting(Proposition::statut).isEqualTo(StatutProposition.RETENUE);
    }
}
