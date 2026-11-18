package fr.limoges.valadon.gestionfioul.web.campagne;

import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.admin;
import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.client;
import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.fournisseur;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import fr.limoges.valadon.gestionfioul.web.security.SecurityConfig;

/**
 * @WebMvcTest charge le contrôleur et les @ControllerAdvice (donc WebExceptionHandler).
 * Le CampagneClient est remplacé par un mock : on ne teste ici que la couche web.
 * @Import(SecurityConfig.class) : on teste avec NOTRE configuration de sécurité (page protégée).
 */
@WebMvcTest(CampagneController.class)
@Import(SecurityConfig.class)
class CampagneControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampagneClient campagneClient;

    @Test
    void liste_des_campagnes_ouvertes() throws Exception {
        Campagne campagne = new Campagne("c1", "Commande d'hiver",
                LocalDateTime.of(2026, 9, 1, 8, 0), LocalDateTime.of(2026, 12, 31, 23, 59),
                StatutCampagne.OUVERTE, 0, "87", null);
        when(campagneClient.listerParStatut(StatutCampagne.OUVERTE)).thenReturn(List.of(campagne));

        mockMvc.perform(get("/campagnes").with(client()))
                .andExpect(status().isOk())
                .andExpect(view().name("campagne/liste"))
                .andExpect(model().attribute("campagnes", List.of(campagne)))
                // donnée insérée par th:text : l'apostrophe est échappée (&#39;) ; la date est formatée par #temporals
                .andExpect(content().string(containsString("Commande d&#39;hiver")))
                .andExpect(content().string(containsString("31/12/2026 23:59")));
    }

    @Test
    void boutons_et_menus_selon_le_role() throws Exception {
        Campagne campagne = new Campagne("c1", "Commande d'hiver", LocalDateTime.of(2026, 9, 1, 8, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59), StatutCampagne.OUVERTE, 0, "87", null);
        when(campagneClient.listerParStatut(StatutCampagne.OUVERTE)).thenReturn(List.of(campagne));

        mockMvc.perform(get("/campagnes").with(client()))
                .andExpect(content().string(containsString("href=\"/campagnes/c1/participer\"")))
                .andExpect(content().string(containsString("href=\"/mes-participations\"")))
                .andExpect(content().string(not(containsString("/proposer"))))
                .andExpect(content().string(not(containsString("/mes-propositions"))));

        mockMvc.perform(get("/campagnes").with(fournisseur()))
                .andExpect(content().string(containsString("href=\"/campagnes/c1/proposer\"")))
                .andExpect(content().string(containsString("href=\"/mes-propositions\"")))
                .andExpect(content().string(not(containsString("/campagnes/c1/participer"))))
                .andExpect(content().string(not(containsString("/mes-participations"))));

        mockMvc.perform(get("/campagnes").with(admin()))
                .andExpect(content().string(not(containsString("href=\"/campagnes/c1/participer\""))))
                .andExpect(content().string(not(containsString("href=\"/campagnes/c1/proposer\""))));
    }

    @Test
    void liste_vide_affiche_un_message() throws Exception {
        when(campagneClient.listerParStatut(StatutCampagne.OUVERTE)).thenReturn(List.of());

        mockMvc.perform(get("/campagnes").with(client()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Aucune campagne n'est ouverte")));
    }

    @Test
    void refus_de_l_api_affiche_la_page_d_erreur_avec_son_message() throws Exception {
        when(campagneClient.listerParStatut(StatutCampagne.OUVERTE))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Authentification refusée par l'API"));

        mockMvc.perform(get("/campagnes").with(client()))
                .andExpect(status().isUnauthorized())
                .andExpect(view().name("erreur"))
                .andExpect(model().attribute("statut", 401))
                .andExpect(model().attribute("message", "Authentification refusée par l'API"));
    }

    @Test
    void api_injoignable_affiche_la_page_d_erreur_503() throws Exception {
        when(campagneClient.listerParStatut(StatutCampagne.OUVERTE))
                .thenThrow(new ResourceAccessException("Connection refused"));

        mockMvc.perform(get("/campagnes").with(client()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(view().name("erreur"))
                .andExpect(model().attribute("statut", 503));
    }

    @Test
    void visiteur_non_connecte_est_redirige_vers_la_connexion() throws Exception {
        mockMvc.perform(get("/campagnes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/connexion"));

        // l'API n'est même pas appelée
        verifyNoInteractions(campagneClient);
    }
}
