package fr.limoges.valadon.gestionfioul.web.accueil;

import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.admin;
import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.client;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.limoges.valadon.gestionfioul.web.security.SecurityConfig;

/**
 * @WebMvcTest charge uniquement la couche web (ce contrôleur + Thymeleaf), sans démarrer de serveur.
 * EtatApiClient appelle l'API : il est remplacé par un mock (@MockitoBean).
 * @Import(SecurityConfig.class) : l'accueil doit rester public avec NOTRE configuration de sécurité.
 */
@WebMvcTest(AccueilController.class)
@Import(SecurityConfig.class)
class AccueilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtatApiClient etatApiClient;

    @BeforeEach
    void apiDisponible() {
        when(etatApiClient.consulter()).thenReturn(EtatApi.disponible("Gestion Fioul", "1.0"));
    }

    @Test
    void accueil_affiche_la_vue_avec_le_titre() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("accueil/index"))
                .andExpect(model().attribute("titre", "Commandes groupées de fioul"))
                // le gabarit commun a bien été appliqué (en-tête + contenu de la page)
                .andExpect(content().string(containsString("Gestion Fioul")))
                .andExpect(content().string(containsString("Commandes groupées de fioul")));
    }

    @Test
    void accueil_indique_que_l_api_est_connectee() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("Connecté à l'API")))
                .andExpect(content().string(containsString("1.0")));
    }

    @Test
    void accueil_reste_affiche_quand_l_api_est_injoignable() throws Exception {
        when(etatApiClient.consulter()).thenReturn(EtatApi.indisponible());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("L'API est injoignable")));
    }

    @Test
    void visiteur_voit_le_lien_de_connexion() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString(">Connexion</a>")))
                .andExpect(content().string(not(containsString("Déconnexion"))));
    }

    @Test
    void utilisateur_connecte_voit_son_nom_son_role_et_la_deconnexion() throws Exception {
        mockMvc.perform(get("/").with(client()))
                .andExpect(content().string(containsString("Alice Durand")))
                .andExpect(content().string(containsString(">CLIENT<")))
                .andExpect(content().string(containsString("Déconnexion")))
                .andExpect(content().string(not(containsString(">Connexion</a>"))));
    }

    @Test
    void le_gabarit_utilise_la_feuille_de_style_generee_et_plus_le_cdn() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("<link rel=\"stylesheet\" href=\"/css/app.css\">")))
                .andExpect(content().string(not(containsString("cdn.jsdelivr.net"))));
    }

    @Test
    void la_feuille_de_style_est_servie_sans_etre_connecte() throws Exception {
        // sans .requestMatchers("/css/**").permitAll(), un visiteur serait redirigé vers /connexion (302)
        mockMvc.perform(get("/css/app.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                // une classe utilisée seulement dans un fragment : le CLI a bien lu fragments/badges.html
                .andExpect(content().string(containsString(".bg-violet-100")));
    }

    @Test
    void sans_prenom_seul_le_nom_est_affiche() throws Exception {
        mockMvc.perform(get("/").with(admin()))
                .andExpect(content().string(containsString(">Administrateur<")))
                .andExpect(content().string(containsString(">ADMIN<")));
    }
}
