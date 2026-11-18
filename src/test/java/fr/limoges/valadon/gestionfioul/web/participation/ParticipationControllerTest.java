package fr.limoges.valadon.gestionfioul.web.participation;

import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.admin;
import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.client;
import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.fournisseur;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
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

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import fr.limoges.valadon.gestionfioul.web.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.web.campagne.CampagneClient;
import fr.limoges.valadon.gestionfioul.web.campagne.StatutCampagne;
import fr.limoges.valadon.gestionfioul.web.security.SecurityConfig;

@WebMvcTest(ParticipationController.class)
@Import(SecurityConfig.class)
class ParticipationControllerTest {

    private static final LocalDateTime DANS_UN_MOIS = LocalDateTime.now().plusMonths(1);

    private final Campagne hiver = new Campagne("c1", "Commande d'hiver",
            LocalDateTime.now().minusDays(5), DANS_UN_MOIS, StatutCampagne.OUVERTE, 0, "87", null);

    private final Campagne printemps = new Campagne("c2", "Commande de printemps",
            LocalDateTime.now().minusMonths(3), LocalDateTime.now().minusMonths(2), StatutCampagne.LIVREE, 9000, "87", "pr1");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParticipationClient participationClient;

    @MockitoBean
    private CampagneClient campagneClient;

    @BeforeEach
    void campagnes() {
        when(campagneClient.trouver("c1")).thenReturn(hiver);
        when(campagneClient.trouver("c2")).thenReturn(printemps);
    }

    // ------------------------------------------------------------------ accès

    @Test
    void espace_client_interdit_aux_autres_roles() throws Exception {
        mockMvc.perform(get("/mes-participations").with(fournisseur())).andExpect(status().isForbidden());
        mockMvc.perform(get("/mes-participations").with(admin())).andExpect(status().isForbidden());
        mockMvc.perform(get("/campagnes/c1/participer").with(fournisseur())).andExpect(status().isForbidden());
        mockMvc.perform(post("/participations/p1/annuler").with(fournisseur()).with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(participationClient);
    }

    @Test
    void visiteur_redirige_vers_la_connexion() throws Exception {
        mockMvc.perform(get("/mes-participations")).andExpect(redirectedUrl("/connexion"));
    }

    // ------------------------------------------------------------------ participer

    @Test
    void formulaire_affiche_la_campagne() throws Exception {
        mockMvc.perform(get("/campagnes/c1/participer").with(client()))
                .andExpect(status().isOk())
                .andExpect(view().name("participation/formulaire"))
                .andExpect(content().string(containsString("Commande d&#39;hiver")))
                .andExpect(content().string(containsString("action=\"/campagnes/c1/participer\"")));
    }

    @Test
    void campagne_inconnue_affiche_la_page_d_erreur_404() throws Exception {
        when(campagneClient.trouver("inconnue"))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "Campagne introuvable : inconnue"));

        mockMvc.perform(get("/campagnes/inconnue/participer").with(client()))
                .andExpect(status().isNotFound())
                .andExpect(view().name("erreur"));
    }

    @Test
    void participation_valide_redirige_vers_mes_participations() throws Exception {
        mockMvc.perform(post("/campagnes/c1/participer").with(client()).with(csrf())
                        .param("volumeSouhaite", "3000").param("adresseLivraison", "1 rue A"))
                .andExpect(redirectedUrl("/mes-participations"))
                .andExpect(flash().attribute("info", "Votre participation de 3000 L est enregistrée."));

        verify(participationClient).participer("c1", new ParticiperRequest(3000, "1 rue A"));
    }

    @Test
    void volume_absent_nul_ou_negatif_refuse_sans_appeler_l_api() throws Exception {
        for (String volume : new String[] {"", "0", "-5"}) {
            mockMvc.perform(post("/campagnes/c1/participer").with(client()).with(csrf()).param("volumeSouhaite", volume))
                    .andExpect(view().name("participation/formulaire"))
                    .andExpect(model().attributeHasFieldErrors("participation", "volumeSouhaite"));
        }
        verifyNoInteractions(participationClient);
    }

    @Test
    void volume_non_numerique_donne_le_message_de_messages_properties() throws Exception {
        mockMvc.perform(post("/campagnes/c1/participer").with(client()).with(csrf()).param("volumeSouhaite", "beaucoup"))
                .andExpect(model().attributeHasFieldErrorCode("participation", "volumeSouhaite", "typeMismatch"))
                .andExpect(content().string(containsString("Le volume doit être un nombre entier de litres")))
                // la campagne est rechargée pour réafficher la page
                .andExpect(content().string(containsString("Commande d&#39;hiver")));

        verifyNoInteractions(participationClient);
    }

    @Test
    void refus_de_l_api_affiche_son_message_sur_le_formulaire() throws Exception {
        when(participationClient.participer(anyString(), any())).thenThrow(new ApiException(HttpStatus.CONFLICT,
                "Ce client participe déjà à la campagne : annulez sa participation pour la refaire"));

        mockMvc.perform(post("/campagnes/c1/participer").with(client()).with(csrf()).param("volumeSouhaite", "3000"))
                .andExpect(status().isOk())
                .andExpect(view().name("participation/formulaire"))
                .andExpect(content().string(containsString("Ce client participe déjà à la campagne")));
    }

    @Test
    void participation_sans_jeton_csrf_refusee() throws Exception {
        mockMvc.perform(post("/campagnes/c1/participer").with(client()).param("volumeSouhaite", "3000"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(participationClient);
    }

    // ------------------------------------------------------------------ mes participations

    @Test
    void mes_participations_avec_le_libelle_des_campagnes_chargees_une_seule_fois() throws Exception {
        when(participationClient.mesParticipations()).thenReturn(List.of(
                new Participation("p1", "c1", "client-1", 4000, "1 rue A", StatutParticipation.ANNULEE,
                        LocalDateTime.now().minusDays(3)),
                new Participation("p2", "c1", "client-1", 3000, "1 rue A", StatutParticipation.CONFIRMEE,
                        LocalDateTime.now().minusDays(1)),
                new Participation("p3", "c2", "client-1", 2500, null, StatutParticipation.CONFIRMEE,
                        LocalDateTime.now().minusMonths(3))));

        mockMvc.perform(get("/mes-participations").with(client()))
                .andExpect(status().isOk())
                .andExpect(view().name("participation/mes-participations"))
                .andExpect(content().string(containsString("Commande d&#39;hiver")))
                .andExpect(content().string(containsString("Commande de printemps")))
                .andExpect(content().string(containsString("3 000 L")))
                // un seul bouton Annuler : p2 (p1 est annulée, la campagne de p3 est livrée)
                .andExpect(content().string(containsString("action=\"/participations/p2/annuler\"")))
                .andExpect(content().string(not(containsString("/participations/p1/annuler"))))
                .andExpect(content().string(not(containsString("/participations/p3/annuler"))));

        // c1 apparaît deux fois mais n'est demandée qu'une fois à l'API
        verify(campagneClient, times(1)).trouver("c1");
        verify(campagneClient, times(1)).trouver("c2");
    }

    @Test
    void aucune_participation() throws Exception {
        when(participationClient.mesParticipations()).thenReturn(List.of());

        mockMvc.perform(get("/mes-participations").with(client()))
                .andExpect(content().string(containsString("Vous ne participez encore à aucune campagne")));
    }

    // ------------------------------------------------------------------ annuler

    @Test
    void annulation_reussie() throws Exception {
        when(participationClient.annuler("p2")).thenReturn(new Participation("p2", "c1", "client-1", 3000, "1 rue A",
                StatutParticipation.ANNULEE, LocalDateTime.now()));

        mockMvc.perform(post("/participations/p2/annuler").with(client()).with(csrf()))
                .andExpect(redirectedUrl("/mes-participations"))
                .andExpect(flash().attribute("info", "Votre participation de 3000 L a été annulée."));
    }

    @Test
    void annulation_refusee_par_l_api_message_d_erreur_apres_redirection() throws Exception {
        when(participationClient.annuler("p2")).thenThrow(new ApiException(HttpStatus.CONFLICT,
                "La campagne n'accepte plus de modifications de participation"));

        mockMvc.perform(post("/participations/p2/annuler").with(client()).with(csrf()))
                .andExpect(redirectedUrl("/mes-participations"))
                .andExpect(flash().attribute("erreur", "La campagne n'accepte plus de modifications de participation"));
    }
}
