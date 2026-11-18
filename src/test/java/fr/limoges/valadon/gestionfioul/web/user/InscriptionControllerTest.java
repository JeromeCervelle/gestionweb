package fr.limoges.valadon.gestionfioul.web.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import fr.limoges.valadon.gestionfioul.web.security.SecurityConfig;

@WebMvcTest(InscriptionController.class)
@Import(SecurityConfig.class)
class InscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UtilisateurClient utilisateurClient;

    /**
     * Un formulaire client valide, dont on peut REMPLACER des champs : inscriptionClient("role", "FOURNISSEUR").
     * Attention : appeler .param("role", ...) une seconde fois sur la requête AJOUTERAIT une valeur
     * (role=CLIENT&role=FOURNISSEUR) au lieu de la remplacer, et Spring retiendrait la première.
     */
    private MockHttpServletRequestBuilder inscriptionClient(String... champsModifies) {
        Map<String, String> champs = new LinkedHashMap<>();
        champs.put("role", "CLIENT");
        champs.put("nom", "Durand");
        champs.put("prenom", "Alice");
        champs.put("email", "alice@test.fr");
        champs.put("motDePasse", "motdepasse1");
        champs.put("confirmation", "motdepasse1");
        champs.put("telephone", "0600000000");
        champs.put("adresse", "1 rue A, 87000 Limoges");
        for (int i = 0; i < champsModifies.length; i += 2) {
            champs.put(champsModifies[i], champsModifies[i + 1]);
        }

        MockHttpServletRequestBuilder requete = post("/inscription").with(csrf());
        champs.forEach(requete::param);
        return requete;
    }

    // ---------------------------------------------------------------- affichage

    @Test
    void formulaire_public_avec_les_roles_client_et_fournisseur_seulement() throws Exception {
        mockMvc.perform(get("/inscription"))
                .andExpect(status().isOk())
                .andExpect(view().name("inscription"))
                .andExpect(model().attribute("roles", List.of(Role.CLIENT, Role.FOURNISSEUR)))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("value=\"FOURNISSEUR\"")))
                .andExpect(content().string(not(containsString("value=\"ADMIN\""))));
    }

    // ---------------------------------------------------------------- succès

    @Test
    void inscription_valide_appelle_l_api_puis_redirige_vers_la_connexion() throws Exception {
        mockMvc.perform(inscriptionClient())
                .andExpect(redirectedUrl("/connexion"))
                .andExpect(flash().attribute("info", "Votre compte a été créé : vous pouvez vous connecter."))
                .andExpect(flash().attribute("email", "alice@test.fr"));

        ArgumentCaptor<InscriptionRequest> requete = ArgumentCaptor.forClass(InscriptionRequest.class);
        verify(utilisateurClient).inscrire(requete.capture());
        assertThat(requete.getValue()).isEqualTo(new InscriptionRequest("alice@test.fr", "motdepasse1",
                "Durand", "Alice", "0600000000", Role.CLIENT, "1 rue A, 87000 Limoges", null));
    }

    @Test
    void raison_sociale_saisie_par_un_client_n_est_pas_transmise() throws Exception {
        mockMvc.perform(inscriptionClient("raisonSociale", "Société oubliée"))
                .andExpect(redirectedUrl("/connexion"));

        ArgumentCaptor<InscriptionRequest> requete = ArgumentCaptor.forClass(InscriptionRequest.class);
        verify(utilisateurClient).inscrire(requete.capture());
        assertThat(requete.getValue().raisonSociale()).isNull();
    }

    @Test
    void inscription_d_un_fournisseur_avec_sa_raison_sociale() throws Exception {
        mockMvc.perform(inscriptionClient("role", "FOURNISSEUR", "raisonSociale", "Fioul Limousin"))
                .andExpect(redirectedUrl("/connexion"));

        ArgumentCaptor<InscriptionRequest> requete = ArgumentCaptor.forClass(InscriptionRequest.class);
        verify(utilisateurClient).inscrire(requete.capture());
        assertThat(requete.getValue().role()).isEqualTo(Role.FOURNISSEUR);
        assertThat(requete.getValue().raisonSociale()).isEqualTo("Fioul Limousin");
    }

    // ---------------------------------------------------------------- erreurs de saisie

    @Test
    void champs_obligatoires_vides_reaffichent_le_formulaire_sans_appeler_l_api() throws Exception {
        mockMvc.perform(post("/inscription").with(csrf()).param("role", "CLIENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("inscription"))
                .andExpect(model().attributeHasFieldErrors("inscription", "email", "motDePasse", "nom"))
                .andExpect(content().string(containsString("L&#39;email est obligatoire")));

        verifyNoInteractions(utilisateurClient);
    }

    @Test
    void email_invalide_et_mot_de_passe_trop_court() throws Exception {
        mockMvc.perform(inscriptionClient("email", "pas-un-email",
                        "motDePasse", "court", "confirmation", "court"))
                .andExpect(model().attributeHasFieldErrorCode("inscription", "email", "Email"))
                .andExpect(model().attributeHasFieldErrorCode("inscription", "motDePasse", "Size"))
                .andExpect(content().string(containsString("au moins 8 caractères")));

        verifyNoInteractions(utilisateurClient);
    }

    @Test
    void confirmation_differente_du_mot_de_passe() throws Exception {
        mockMvc.perform(inscriptionClient("confirmation", "autrechose"))
                .andExpect(view().name("inscription"))
                .andExpect(model().attributeHasFieldErrors("inscription", "confirmation"));

        verifyNoInteractions(utilisateurClient);
    }

    @Test
    void fournisseur_sans_raison_sociale() throws Exception {
        mockMvc.perform(inscriptionClient("role", "FOURNISSEUR"))
                .andExpect(model().attributeHasFieldErrors("inscription", "raisonSociale"))
                .andExpect(content().string(containsString("La raison sociale est obligatoire")));

        verifyNoInteractions(utilisateurClient);
    }

    @Test
    void role_admin_force_dans_la_requete_est_refuse() throws Exception {
        // le bouton n'existe pas dans la page, mais n'importe qui peut forger la requête
        mockMvc.perform(inscriptionClient("role", "ADMIN"))
                .andExpect(model().attributeHasFieldErrors("inscription", "role"));

        verifyNoInteractions(utilisateurClient);
    }

    @Test
    void le_mot_de_passe_n_est_jamais_renvoye_dans_la_page() throws Exception {
        mockMvc.perform(inscriptionClient("nom", ""))
                .andExpect(view().name("inscription"))
                .andExpect(content().string(not(containsString("motdepasse1"))))
                // les autres saisies sont conservées
                .andExpect(content().string(containsString("value=\"alice@test.fr\"")));
    }

    // ---------------------------------------------------------------- refus de l'API

    @Test
    void refus_de_l_api_affiche_son_message_sur_le_formulaire() throws Exception {
        when(utilisateurClient.inscrire(any()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "Un compte existe déjà avec cet email"));

        mockMvc.perform(inscriptionClient())
                .andExpect(status().isOk())
                .andExpect(view().name("inscription"))
                .andExpect(model().attributeHasErrors("inscription"))
                .andExpect(content().string(containsString("Un compte existe déjà avec cet email")))
                .andExpect(content().string(not(containsString("motdepasse1"))));
    }

    @Test
    void formulaire_poste_sans_jeton_csrf_est_refuse() throws Exception {
        mockMvc.perform(post("/inscription").param("email", "alice@test.fr"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(utilisateurClient);
    }
}
