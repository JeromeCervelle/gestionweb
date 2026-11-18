package fr.limoges.valadon.gestionfioul.web.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restclient.test.autoconfigure.RestClientTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;

import fr.limoges.valadon.gestionfioul.web.api.ApiConfig;
import fr.limoges.valadon.gestionfioul.web.api.ApiException;

@RestClientTest(UtilisateurClient.class)
@Import(ApiConfig.class)
class UtilisateurClientTest {

    @Autowired
    private UtilisateurClient utilisateurClient;

    @Autowired
    private MockRestServiceServer api;

    private static String basic(String email, String motDePasse) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((email + ":" + motDePasse).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void trouver_un_utilisateur_par_son_id() {
        api.expect(requestTo("http://localhost:9999/api/users/f1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"f1","email":"fioul@test.fr","nom":"Leroy","prenom":"Paul","role":"FOURNISSEUR","raisonSociale":"Fioul Limousin"}
                        """, MediaType.APPLICATION_JSON));

        Utilisateur fournisseur = utilisateurClient.trouver("f1");

        assertThat(fournisseur.designation()).isEqualTo("Fioul Limousin");
    }

    @Test
    void designation_d_un_client_avec_ou_sans_prenom() {
        assertThat(new Utilisateur("u1", "a@t.fr", "Durand", "Alice", null, Role.CLIENT, null, null).designation())
                .isEqualTo("Alice Durand");
        assertThat(new Utilisateur("u2", "b@t.fr", "Martin", null, null, Role.CLIENT, null, null).designation())
                .isEqualTo("Martin");
    }

    @Test
    void inscrire_poste_le_json_sans_identifiants_et_renvoie_le_profil_cree() {
        api.expect(requestTo("http://localhost:9999/api/users"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // comparaison "lenient" : l'ordre des champs et les espaces n'importent pas
                .andExpect(content().json("""
                        {"email":"fioul@test.fr","motDePasse":"motdepasse1","nom":"Leroy","prenom":null,
                         "telephone":null,"role":"FOURNISSEUR","adresse":null,"raisonSociale":"Fioul Limousin"}
                        """))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body("""
                        {"id":"f1","email":"fioul@test.fr","nom":"Leroy","role":"FOURNISSEUR","raisonSociale":"Fioul Limousin"}
                        """));

        Utilisateur cree = utilisateurClient.inscrire(new InscriptionRequest("fioul@test.fr", "motdepasse1",
                "Leroy", null, null, Role.FOURNISSEUR, null, "Fioul Limousin"));

        assertThat(cree.id()).isEqualTo("f1");
        api.verify();
    }

    @Test
    void inscrire_refuse_par_l_api_leve_une_api_exception_avec_son_message() {
        api.expect(requestTo("http://localhost:9999/api/users"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON).body("""
                        {"title":"Bad Request","status":400,"detail":"Un compte existe déjà avec cet email"}
                        """));

        assertThatThrownBy(() -> utilisateurClient.inscrire(new InscriptionRequest("alice@test.fr", "motdepasse1",
                "Durand", null, null, Role.CLIENT, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Un compte existe déjà avec cet email");
    }

    @Test
    void moi_envoie_les_identifiants_en_basic_et_renvoie_le_profil() {
        api.expect(requestTo("http://localhost:9999/api/users/moi"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basic("alice@test.fr", "motdepasse1")))
                .andRespond(withSuccess("""
                        {"id":"u1","email":"alice@test.fr","nom":"Durand","prenom":"Alice",
                         "telephone":null,"role":"CLIENT","adresse":"1 rue A","raisonSociale":null}
                        """, MediaType.APPLICATION_JSON));

        Utilisateur utilisateur = utilisateurClient.moi("alice@test.fr", "motdepasse1");

        assertThat(utilisateur.id()).isEqualTo("u1");
        assertThat(utilisateur.role()).isEqualTo(Role.CLIENT);
        api.verify();
    }

    @Test
    void moi_encode_un_mot_de_passe_accentue_en_utf8() {
        // l'API (Spring Security) décode l'en-tête Basic en UTF-8
        api.expect(requestTo("http://localhost:9999/api/users/moi"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, basic("alice@test.fr", "hélène-1234")))
                .andRespond(withSuccess("""
                        {"id":"u1","email":"alice@test.fr","role":"CLIENT"}
                        """, MediaType.APPLICATION_JSON));

        utilisateurClient.moi("alice@test.fr", "hélène-1234");

        api.verify();
    }

    @Test
    void moi_avec_de_mauvais_identifiants_leve_une_api_exception_401() {
        api.expect(requestTo("http://localhost:9999/api/users/moi"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> utilisateurClient.moi("alice@test.fr", "faux"))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatut().value())
                .isEqualTo(401);
    }
}
