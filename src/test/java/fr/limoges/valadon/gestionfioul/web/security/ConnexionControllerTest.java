package fr.limoges.valadon.gestionfioul.web.security;

import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.client;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.WebAttributes;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import fr.limoges.valadon.gestionfioul.web.user.Role;
import fr.limoges.valadon.gestionfioul.web.user.Utilisateur;
import fr.limoges.valadon.gestionfioul.web.user.UtilisateurClient;

/**
 * Parcours de connexion complet côté site : vraie configuration de sécurité, vrai ApiAuthenticationProvider,
 * seul l'appel à l'API (UtilisateurClient) est simulé.
 */
@WebMvcTest(ConnexionController.class)
@Import({SecurityConfig.class, ApiAuthenticationProvider.class})
class ConnexionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UtilisateurClient utilisateurClient;

    @Test
    void page_de_connexion_publique_avec_jeton_csrf() throws Exception {
        mockMvc.perform(get("/connexion"))
                .andExpect(status().isOk())
                .andExpect(view().name("connexion"))
                // champ caché ajouté automatiquement par th:action
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void connexion_reussie_redirige_vers_l_accueil_avec_le_bon_role() throws Exception {
        when(utilisateurClient.moi("alice@test.fr", "motdepasse1")).thenReturn(
                new Utilisateur("u1", "alice@test.fr", "Durand", "Alice", null, Role.CLIENT, null, null));

        // formLogin() poste le formulaire avec un jeton CSRF valide
        mockMvc.perform(formLogin("/connexion")
                        .userParameter("email").user("alice@test.fr")
                        .passwordParam("motDePasse").password("motdepasse1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withRoles("CLIENT"));
    }

    @Test
    void mauvais_identifiants_renvoient_vers_la_page_de_connexion() throws Exception {
        when(utilisateurClient.moi("alice@test.fr", "faux"))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Authentification refusée"));

        mockMvc.perform(formLogin("/connexion")
                        .userParameter("email").user("alice@test.fr")
                        .passwordParam("motDePasse").password("faux"))
                .andExpect(redirectedUrl("/connexion?erreur=true"))
                .andExpect(unauthenticated());
    }

    @Test
    void message_d_erreur_affiche_apres_un_echec() throws Exception {
        mockMvc.perform(get("/connexion?erreur=true")
                        .sessionAttr(WebAttributes.AUTHENTICATION_EXCEPTION,
                                new BadCredentialsException("Email ou mot de passe incorrect.")))
                .andExpect(model().attribute("erreur", "Email ou mot de passe incorrect."))
                .andExpect(content().string(containsString("Email ou mot de passe incorrect.")));
    }

    @Test
    void formulaire_poste_sans_jeton_csrf_est_refuse() throws Exception {
        mockMvc.perform(post("/connexion").param("email", "alice@test.fr").param("motDePasse", "motdepasse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deconnexion_redirige_avec_un_message() throws Exception {
        mockMvc.perform(post("/deconnexion").with(client()).with(csrf()))
                .andExpect(redirectedUrl("/connexion?deconnexion=true"))
                .andExpect(unauthenticated());

        mockMvc.perform(get("/connexion?deconnexion=true"))
                .andExpect(content().string(containsString("Vous êtes déconnecté.")));
    }

    @Test
    void apres_une_inscription_message_et_email_pre_rempli() throws Exception {
        // flashAttr simule les attributs flash posés par InscriptionController avant sa redirection
        mockMvc.perform(get("/connexion")
                        .flashAttr("info", "Votre compte a été créé : vous pouvez vous connecter.")
                        .flashAttr("email", "alice@test.fr"))
                .andExpect(content().string(containsString("Votre compte a été créé")))
                .andExpect(content().string(containsString("value=\"alice@test.fr\"")));
    }
}
