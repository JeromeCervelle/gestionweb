package fr.limoges.valadon.gestionfioul.web.admin;

import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.admin;
import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.client;
import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.fournisseur;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import fr.limoges.valadon.gestionfioul.web.campagne.ActionCampagne;
import fr.limoges.valadon.gestionfioul.web.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.web.campagne.CampagneClient;
import fr.limoges.valadon.gestionfioul.web.campagne.CreerCampagneRequest;
import fr.limoges.valadon.gestionfioul.web.campagne.StatutCampagne;
import fr.limoges.valadon.gestionfioul.web.participation.Participation;
import fr.limoges.valadon.gestionfioul.web.participation.ParticipationClient;
import fr.limoges.valadon.gestionfioul.web.participation.StatutParticipation;
import fr.limoges.valadon.gestionfioul.web.proposition.Palier;
import fr.limoges.valadon.gestionfioul.web.proposition.Proposition;
import fr.limoges.valadon.gestionfioul.web.proposition.PropositionClient;
import fr.limoges.valadon.gestionfioul.web.proposition.StatutProposition;
import fr.limoges.valadon.gestionfioul.web.security.SecurityConfig;
import fr.limoges.valadon.gestionfioul.web.user.Role;
import fr.limoges.valadon.gestionfioul.web.user.Utilisateur;
import fr.limoges.valadon.gestionfioul.web.user.UtilisateurClient;

@WebMvcTest(AdminCampagneController.class)
@Import(SecurityConfig.class)
class AdminCampagneControllerTest {

    private static final LocalDateTime DANS_UN_MOIS = LocalDateTime.now().plusMonths(1);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CampagneClient campagneClient;

    @MockitoBean
    private ParticipationClient participationClient;

    @MockitoBean
    private PropositionClient propositionClient;

    @MockitoBean
    private UtilisateurClient utilisateurClient;

    private static Campagne campagne(StatutCampagne statut, int volumeTotal, String retenue) {
        return new Campagne("c1", "Commande d'hiver", LocalDateTime.now().minusDays(10), DANS_UN_MOIS,
                statut, volumeTotal, "87", retenue);
    }

    // ------------------------------------------------------------------ accès

    @Test
    void administration_reservee_a_l_admin() throws Exception {
        mockMvc.perform(get("/admin/campagnes").with(client())).andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/campagnes/c1").with(fournisseur())).andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/campagnes/c1/action").param("action", "ANNULER").with(client()).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/campagnes")).andExpect(redirectedUrl("/connexion"));

        verifyNoInteractions(campagneClient);
    }

    // ------------------------------------------------------------------ liste par statut

    @Test
    void liste_par_statut_ouverte_par_defaut() throws Exception {
        when(campagneClient.listerParStatut(StatutCampagne.OUVERTE)).thenReturn(List.of(campagne(StatutCampagne.OUVERTE, 0, null)));

        mockMvc.perform(get("/admin/campagnes").with(admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/campagnes"))
                .andExpect(model().attribute("statutChoisi", StatutCampagne.OUVERTE))
                .andExpect(content().string(containsString("href=\"/admin/campagnes/c1\"")))
                // un onglet par statut
                .andExpect(content().string(containsString("href=\"/admin/campagnes?statut=BROUILLON\"")))
                .andExpect(content().string(containsString("href=\"/admin/campagnes?statut=ANNULEE\"")));
    }

    @Test
    void onglet_choisi() throws Exception {
        when(campagneClient.listerParStatut(StatutCampagne.BROUILLON)).thenReturn(List.of());

        mockMvc.perform(get("/admin/campagnes").param("statut", "BROUILLON").with(admin()))
                .andExpect(model().attribute("statutChoisi", StatutCampagne.BROUILLON))
                .andExpect(content().string(containsString("Aucune campagne dans ce statut")));
    }

    // ------------------------------------------------------------------ création

    @Test
    void formulaire_de_creation_pre_rempli() throws Exception {
        mockMvc.perform(get("/admin/campagnes/nouvelle").with(admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/nouvelle-campagne"))
                .andExpect(content().string(containsString("name=\"dateCloture\"")))
                .andExpect(content().string(containsString("T23:59\"")));
    }

    @Test
    void creation_valide_redirige_vers_la_page_de_la_nouvelle_campagne() throws Exception {
        when(campagneClient.creer(any())).thenReturn(campagne(StatutCampagne.BROUILLON, 0, null));

        mockMvc.perform(post("/admin/campagnes/nouvelle").with(admin()).with(csrf())
                        .param("libelle", "Commande d'hiver")
                        .param("dateOuverture", "2026-12-01T08:00")
                        .param("dateCloture", DANS_UN_MOIS.toString().substring(0, 16))
                        .param("zoneLivraison", "87"))
                .andExpect(redirectedUrl("/admin/campagnes/c1"))
                .andExpect(flash().attribute("info", "Campagne créée en brouillon : vérifiez-la puis ouvrez-la."));

        ArgumentCaptor<CreerCampagneRequest> requete = ArgumentCaptor.forClass(CreerCampagneRequest.class);
        verify(campagneClient).creer(requete.capture());
        assertThat(requete.getValue().dateOuverture()).isEqualTo(LocalDateTime.of(2026, 12, 1, 8, 0));
    }

    @Test
    void creation_refusee_par_l_api_formulaire_reaffiche_avec_son_message() throws Exception {
        // cas trouvé grâce au rapport de couverture JaCoCo : le bloc catch n'était jamais exécuté
        when(campagneClient.creer(any()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "La date d'ouverture doit être dans le futur"));

        mockMvc.perform(post("/admin/campagnes/nouvelle").with(admin()).with(csrf())
                        .param("libelle", "Commande d'hiver")
                        .param("dateOuverture", "2026-12-01T08:00")
                        .param("dateCloture", DANS_UN_MOIS.toString().substring(0, 16)))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/nouvelle-campagne"))
                .andExpect(model().attributeHasErrors("campagne"))
                // message de l'API en erreur globale, et la saisie est conservée
                .andExpect(content().string(containsString("La date d&#39;ouverture doit être dans le futur")))
                .andExpect(content().string(containsString("value=\"Commande d&#39;hiver\"")));
    }

    @Test
    void creation_invalide_sans_appeler_l_api() throws Exception {
        String demain = LocalDateTime.now().plusDays(1).toString().substring(0, 16);
        String apresDemain = LocalDateTime.now().plusDays(2).toString().substring(0, 16);

        mockMvc.perform(post("/admin/campagnes/nouvelle").with(admin()).with(csrf())
                        .param("libelle", "")
                        .param("dateOuverture", apresDemain)
                        .param("dateCloture", demain))
                .andExpect(view().name("admin/nouvelle-campagne"))
                .andExpect(model().attributeHasFieldErrors("campagne", "libelle", "dateCloture"))
                .andExpect(content().string(containsString("La date limite doit être postérieure à l&#39;ouverture")));

        verifyNoInteractions(campagneClient);
    }

    // ------------------------------------------------------------------ détail

    @Test
    void detail_d_une_campagne_ouverte() throws Exception {
        when(campagneClient.trouver("c1")).thenReturn(campagne(StatutCampagne.OUVERTE, 0, null));
        when(participationClient.listerParCampagne("c1")).thenReturn(List.of(
                new Participation("p1", "c1", "u1", 3000, "1 rue A", StatutParticipation.CONFIRMEE, LocalDateTime.now().minusDays(3)),
                new Participation("p2", "c1", "u1", 1000, "1 rue A", StatutParticipation.ANNULEE, LocalDateTime.now().minusDays(4)),
                new Participation("p3", "c1", "u2", 4000, null, StatutParticipation.CONFIRMEE, LocalDateTime.now().minusDays(2))));
        when(propositionClient.listerParCampagne("c1")).thenReturn(List.of(
                new Proposition("pr1", "c1", "f1", LocalDateTime.now().minusDays(1), DANS_UN_MOIS.plusDays(30),
                        StatutProposition.SOUMISE, List.of(new Palier(0, null, new BigDecimal("1.05"))))));
        when(utilisateurClient.trouver("u1")).thenReturn(new Utilisateur("u1", "alice@test.fr", "Durand", "Alice", null, Role.CLIENT, null, null));
        when(utilisateurClient.trouver("u2")).thenReturn(new Utilisateur("u2", "bob@test.fr", "Martin", "Bob", null, Role.CLIENT, null, null));
        when(utilisateurClient.trouver("f1")).thenReturn(new Utilisateur("f1", "fioul@test.fr", "Leroy", null, null, Role.FOURNISSEUR, null, "Fioul Limousin"));

        mockMvc.perform(get("/admin/campagnes/c1").with(admin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/campagne"))
                .andExpect(model().attribute("actions", List.of(ActionCampagne.CLOTURER, ActionCampagne.ANNULER)))
                // volume engagé = participations CONFIRMEES seulement (3 000 + 4 000)
                .andExpect(model().attribute("volumeEngage", 7000))
                .andExpect(content().string(containsString("7 000 L")))
                // seuls les boutons permis par le statut
                .andExpect(content().string(containsString("value=\"CLOTURER\"")))
                .andExpect(content().string(containsString("value=\"ANNULER\"")))
                .andExpect(content().string(not(containsString("value=\"OUVRIR\""))))
                .andExpect(content().string(not(containsString("value=\"LIVRER\""))))
                // message de confirmation passé par un attribut data-*
                .andExpect(content().string(containsString("data-confirmation=\"Clôturer la campagne ?")))
                .andExpect(content().string(containsString("Alice Durand")))
                .andExpect(content().string(containsString("Fioul Limousin")));

        // u1 apparaît dans deux participations mais n'est demandé qu'une fois
        verify(utilisateurClient, times(1)).trouver("u1");
    }

    @Test
    void detail_apres_attribution_offres_classees_et_retenue_mise_en_valeur() throws Exception {
        when(campagneClient.trouver("c1")).thenReturn(campagne(StatutCampagne.ATTRIBUEE, 7000, "pr2"));
        when(participationClient.listerParCampagne("c1")).thenReturn(List.of());
        when(propositionClient.listerParCampagne("c1")).thenReturn(List.of(
                new Proposition("pr1", "c1", "f1", LocalDateTime.now().minusDays(5), DANS_UN_MOIS,
                        StatutProposition.REJETEE, List.of(new Palier(0, null, new BigDecimal("1.05")))),
                new Proposition("pr2", "c1", "f2", LocalDateTime.now().minusDays(4), DANS_UN_MOIS,
                        StatutProposition.RETENUE, List.of(new Palier(0, 5000, new BigDecimal("1.10")),
                        new Palier(5000, null, new BigDecimal("1.02"))))));
        when(utilisateurClient.trouver("f1")).thenReturn(new Utilisateur("f1", "a@t.fr", "A", null, null, Role.FOURNISSEUR, null, "Énergie 87"));
        when(utilisateurClient.trouver("f2")).thenReturn(new Utilisateur("f2", "b@t.fr", "B", null, null, Role.FOURNISSEUR, null, "Fioul Limousin"));

        String page = mockMvc.perform(get("/admin/campagnes/c1").with(admin()))
                .andExpect(model().attribute("actions", List.of(ActionCampagne.LIVRER, ActionCampagne.ANNULER)))
                .andExpect(content().string(containsString("7 140,00 €")))
                .andExpect(content().string(containsString("7 350,00 €")))
                .andReturn().getResponse().getContentAsString();

        // la moins chère (1,02 €/L, Fioul Limousin) est classée AVANT l'autre (1,05 €/L)
        assertThat(page.indexOf("Fioul Limousin")).isLessThan(page.indexOf("Énergie 87"));
    }

    @Test
    void campagne_livree_aucune_action() throws Exception {
        when(campagneClient.trouver("c1")).thenReturn(campagne(StatutCampagne.LIVREE, 7000, null));
        when(participationClient.listerParCampagne("c1")).thenReturn(List.of());
        when(propositionClient.listerParCampagne("c1")).thenReturn(List.of());

        mockMvc.perform(get("/admin/campagnes/c1").with(admin()))
                .andExpect(content().string(containsString("Campagne terminée : aucune action possible.")));
    }

    // ------------------------------------------------------------------ actions

    @Test
    void action_executee_puis_retour_sur_la_campagne() throws Exception {
        mockMvc.perform(post("/admin/campagnes/c1/action").param("action", "CLOTURER").with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/admin/campagnes/c1"))
                .andExpect(flash().attribute("info", "Action effectuée : Clôturer."));

        verify(campagneClient).executer("c1", ActionCampagne.CLOTURER);
    }

    @Test
    void action_refusee_par_l_api_message_d_erreur() throws Exception {
        doThrow(new ApiException(HttpStatus.CONFLICT, "Aucune proposition valable pour cette campagne"))
                .when(campagneClient).executer("c1", ActionCampagne.ATTRIBUER);

        mockMvc.perform(post("/admin/campagnes/c1/action").param("action", "ATTRIBUER").with(admin()).with(csrf()))
                .andExpect(redirectedUrl("/admin/campagnes/c1"))
                .andExpect(flash().attribute("erreur", "Aucune proposition valable pour cette campagne"));
    }

    @Test
    void action_inconnue_refusee_par_spring() throws Exception {
        mockMvc.perform(post("/admin/campagnes/c1/action").param("action", "SUPPRIMER").with(admin()).with(csrf()))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(campagneClient);
    }
}
