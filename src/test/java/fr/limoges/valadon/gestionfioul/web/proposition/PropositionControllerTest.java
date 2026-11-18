package fr.limoges.valadon.gestionfioul.web.proposition;

import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.admin;
import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.client;
import static fr.limoges.valadon.gestionfioul.web.security.UtilisateursDeTest.fournisseur;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import fr.limoges.valadon.gestionfioul.web.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.web.campagne.CampagneClient;
import fr.limoges.valadon.gestionfioul.web.campagne.StatutCampagne;
import fr.limoges.valadon.gestionfioul.web.security.SecurityConfig;

/**
 * BaremeValidator est un vrai @Component : on l'importe pour tester avec les vraies règles de barème.
 */
@WebMvcTest(PropositionController.class)
@Import({SecurityConfig.class, BaremeValidator.class})
class PropositionControllerTest {

    private static final LocalDateTime CLOTURE = LocalDateTime.now().plusMonths(1).truncatedTo(ChronoUnit.MINUTES);
    private static final String VALIDITE_OK = CLOTURE.plusDays(30).toString().substring(0, 16);   // yyyy-MM-ddTHH:mm

    private final Campagne hiver = new Campagne("c1", "Commande d'hiver",
            LocalDateTime.now().minusDays(5), CLOTURE, StatutCampagne.OUVERTE, 0, "87", null);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PropositionClient propositionClient;

    @MockitoBean
    private CampagneClient campagneClient;

    @BeforeEach
    void campagne() {
        when(campagneClient.trouver("c1")).thenReturn(hiver);
    }

    // barème valide à deux paliers : 1,10 € jusqu'à 5 000 L, puis 1,02 €
    private MockHttpServletRequestBuilder baremeValide() {
        return post("/campagnes/c1/proposer").with(fournisseur()).with(csrf())
                .param("validiteJusqua", VALIDITE_OK)
                .param("paliers[0].volumeMin", "0").param("paliers[0].volumeMax", "5000").param("paliers[0].prixLitre", "1.10")
                .param("paliers[1].volumeMin", "5000").param("paliers[1].volumeMax", "").param("paliers[1].prixLitre", "1.02");
    }

    private PropositionForm formulaireDuModele(MvcResult resultat) {
        return (PropositionForm) resultat.getModelAndView().getModel().get("proposition");
    }

    // ------------------------------------------------------------------ accès

    @Test
    void espace_fournisseur_interdit_aux_autres_roles() throws Exception {
        mockMvc.perform(get("/mes-propositions").with(client())).andExpect(status().isForbidden());
        mockMvc.perform(get("/mes-propositions").with(admin())).andExpect(status().isForbidden());
        mockMvc.perform(get("/campagnes/c1/proposer").with(client())).andExpect(status().isForbidden());

        verifyNoInteractions(propositionClient);
    }

    // ------------------------------------------------------------------ formulaire et paliers dynamiques

    @Test
    void formulaire_initial_un_palier_et_validite_trente_jours_apres_la_cloture() throws Exception {
        MvcResult resultat = mockMvc.perform(get("/campagnes/c1/proposer").with(fournisseur()))
                .andExpect(status().isOk())
                .andExpect(view().name("proposition/formulaire"))
                .andExpect(content().string(containsString("name=\"paliers[0].volumeMin\"")))
                .andExpect(content().string(containsString("value=\"" + VALIDITE_OK + "\"")))
                .andReturn();

        assertThat(formulaireDuModele(resultat).getPaliers()).hasSize(1);
    }

    @Test
    void ajouter_un_palier_qui_commence_a_la_fin_du_precedent_sans_appeler_l_api() throws Exception {
        MvcResult resultat = mockMvc.perform(post("/campagnes/c1/proposer").with(fournisseur()).with(csrf())
                        .param("ajouterPalier", "true")
                        .param("validiteJusqua", VALIDITE_OK)
                        .param("paliers[0].volumeMin", "0").param("paliers[0].volumeMax", "5000").param("paliers[0].prixLitre", "1.10"))
                .andExpect(view().name("proposition/formulaire"))
                .andExpect(content().string(containsString("name=\"paliers[1].volumeMin\"")))
                // aucune erreur affichée pendant la saisie
                .andExpect(model().hasNoErrors())
                .andReturn();

        List<PalierForm> paliers = formulaireDuModele(resultat).getPaliers();
        assertThat(paliers).hasSize(2);
        assertThat(paliers.get(1).getVolumeMin()).isEqualTo(5000);
        verifyNoInteractions(propositionClient);
    }

    @Test
    void retirer_le_palier_choisi() throws Exception {
        MvcResult resultat = mockMvc.perform(baremeValide().param("retirerPalier", "0"))
                .andExpect(view().name("proposition/formulaire"))
                .andReturn();

        List<PalierForm> paliers = formulaireDuModele(resultat).getPaliers();
        assertThat(paliers).singleElement().extracting(PalierForm::getVolumeMin).isEqualTo(5000);
        verifyNoInteractions(propositionClient);
    }

    // ------------------------------------------------------------------ soumission

    @Test
    void bareme_valide_envoye_a_l_api_puis_redirection() throws Exception {
        mockMvc.perform(baremeValide())
                .andExpect(redirectedUrl("/mes-propositions"))
                .andExpect(flash().attribute("info", "Votre barème pour « Commande d'hiver » est enregistré."));

        ArgumentCaptor<SoumettrePropositionRequest> requete = ArgumentCaptor.forClass(SoumettrePropositionRequest.class);
        verify(propositionClient).soumettre(eq("c1"), requete.capture());
        assertThat(requete.getValue().validiteJusqua()).isEqualTo(CLOTURE.plusDays(30));
        assertThat(requete.getValue().paliers()).containsExactly(
                new Palier(0, 5000, new BigDecimal("1.10")),
                new Palier(5000, null, new BigDecimal("1.02")));   // case vide -> null = sans limite
    }

    @Test
    void erreurs_de_bareme_affichees_sur_la_bonne_ligne() throws Exception {
        mockMvc.perform(post("/campagnes/c1/proposer").with(fournisseur()).with(csrf())
                        .param("validiteJusqua", VALIDITE_OK)
                        .param("paliers[0].volumeMin", "0").param("paliers[0].volumeMax", "5000").param("paliers[0].prixLitre", "1.10")
                        .param("paliers[1].volumeMin", "6000").param("paliers[1].volumeMax", "").param("paliers[1].prixLitre", "0"))
                .andExpect(view().name("proposition/formulaire"))
                .andExpect(model().attributeHasFieldErrors("proposition", "paliers[1].volumeMin", "paliers[1].prixLitre"))
                .andExpect(content().string(containsString("Doit valoir 5000 L (fin du palier précédent)")));

        verifyNoInteractions(propositionClient);
    }

    @Test
    void prix_non_numerique_et_trop_de_decimales() throws Exception {
        mockMvc.perform(post("/campagnes/c1/proposer").with(fournisseur()).with(csrf())
                        .param("validiteJusqua", VALIDITE_OK)
                        .param("paliers[0].volumeMin", "0").param("paliers[0].volumeMax", "5000").param("paliers[0].prixLitre", "cher")
                        .param("paliers[1].volumeMin", "5000").param("paliers[1].prixLitre", "1.0255"))
                .andExpect(model().attributeHasFieldErrorCode("proposition", "paliers[0].prixLitre", "typeMismatch"))
                .andExpect(model().attributeHasFieldErrorCode("proposition", "paliers[1].prixLitre", "Digits"))
                .andExpect(content().string(containsString("Ce champ doit être un nombre (exemple : 1.05)")));

        verifyNoInteractions(propositionClient);
    }

    @Test
    void validite_avant_la_cloture_de_la_campagne_refusee() throws Exception {
        String avantCloture = CLOTURE.minusDays(1).toString().substring(0, 16);

        mockMvc.perform(post("/campagnes/c1/proposer").with(fournisseur()).with(csrf())
                        .param("validiteJusqua", avantCloture)
                        .param("paliers[0].volumeMin", "0").param("paliers[0].prixLitre", "1.05"))
                .andExpect(model().attributeHasFieldErrors("proposition", "validiteJusqua"))
                .andExpect(content().string(containsString("Votre offre doit rester valable après la clôture de la campagne")));
    }

    @Test
    void refus_de_l_api_affiche_son_message() throws Exception {
        when(propositionClient.soumettre(anyString(), any())).thenThrow(new ApiException(HttpStatus.CONFLICT,
                "Ce fournisseur a déjà soumis une proposition pour cette campagne"));

        mockMvc.perform(baremeValide())
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ce fournisseur a déjà soumis une proposition pour cette campagne")));
    }

    // ------------------------------------------------------------------ mes propositions

    @Test
    void mes_propositions_avec_prix_et_montant_quand_la_campagne_est_cloturee() throws Exception {
        Campagne cloturee = new Campagne("c2", "Commande de printemps", LocalDateTime.now().minusMonths(2),
                LocalDateTime.now().minusMonths(1), StatutCampagne.ATTRIBUEE, 7000, "87", "pr2");
        when(campagneClient.trouver("c2")).thenReturn(cloturee);
        when(propositionClient.mesPropositions()).thenReturn(List.of(
                new Proposition("pr1", "c1", "fournisseur-1", LocalDateTime.now().minusDays(1), CLOTURE.plusDays(30),
                        StatutProposition.SOUMISE, List.of(new Palier(0, null, new BigDecimal("1.05")))),
                new Proposition("pr2", "c2", "fournisseur-1", LocalDateTime.now().minusMonths(2), LocalDateTime.now().plusDays(1),
                        StatutProposition.RETENUE, List.of(new Palier(0, 5000, new BigDecimal("1.10")),
                        new Palier(5000, null, new BigDecimal("1.02"))))));

        mockMvc.perform(get("/mes-propositions").with(fournisseur()))
                .andExpect(status().isOk())
                .andExpect(view().name("proposition/mes-propositions"))
                .andExpect(content().string(containsString("En attente")))
                .andExpect(content().string(containsString("Retenue")))
                .andExpect(content().string(containsString("de 0 à 5 000 L")))
                .andExpect(content().string(containsString("à partir de 5 000 L")))
                .andExpect(content().string(containsString("1,020 €/L")))
                // 7 000 L × 1,02 € = 7 140 €
                .andExpect(content().string(containsString("7 140,00 €")));
    }

    @Test
    void campagne_ouverte_pas_encore_de_montant() throws Exception {
        when(propositionClient.mesPropositions()).thenReturn(List.of(
                new Proposition("pr1", "c1", "fournisseur-1", LocalDateTime.now(), CLOTURE.plusDays(30),
                        StatutProposition.SOUMISE, List.of(new Palier(0, null, new BigDecimal("1.05"))))));

        mockMvc.perform(get("/mes-propositions").with(fournisseur()))
                .andExpect(content().string(not(containsString("Volume total de la campagne"))));
    }
}
