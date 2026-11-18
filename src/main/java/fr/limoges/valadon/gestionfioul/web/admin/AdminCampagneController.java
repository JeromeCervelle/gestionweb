package fr.limoges.valadon.gestionfioul.web.admin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import fr.limoges.valadon.gestionfioul.web.campagne.ActionCampagne;
import fr.limoges.valadon.gestionfioul.web.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.web.campagne.CampagneClient;
import fr.limoges.valadon.gestionfioul.web.campagne.StatutCampagne;
import fr.limoges.valadon.gestionfioul.web.participation.Participation;
import fr.limoges.valadon.gestionfioul.web.participation.ParticipationClient;
import fr.limoges.valadon.gestionfioul.web.participation.StatutParticipation;
import fr.limoges.valadon.gestionfioul.web.proposition.LigneProposition;
import fr.limoges.valadon.gestionfioul.web.proposition.Proposition;
import fr.limoges.valadon.gestionfioul.web.proposition.PropositionClient;
import fr.limoges.valadon.gestionfioul.web.user.Utilisateur;
import fr.limoges.valadon.gestionfioul.web.user.UtilisateurClient;
import jakarta.validation.Valid;

/**
 * Espace administrateur : toutes les pages sous {@code /admin}, réservées au rôle {@code ADMIN}
 * dans {@link fr.limoges.valadon.gestionfioul.web.security.SecurityConfig SecurityConfig}.
 *
 * <p>Ce contrôleur ASSEMBLE des données venant de plusieurs fonctionnalités (campagnes, participations,
 * propositions, utilisateurs) : c'est le seul endroit du site qui a besoin de toutes à la fois.</p>
 */
@Controller
@RequestMapping("/admin/campagnes")
public class AdminCampagneController {

    private final CampagneClient campagneClient;
    private final ParticipationClient participationClient;
    private final PropositionClient propositionClient;
    private final UtilisateurClient utilisateurClient;

    public AdminCampagneController(CampagneClient campagneClient, ParticipationClient participationClient,
                                   PropositionClient propositionClient, UtilisateurClient utilisateurClient) {
        this.campagneClient = campagneClient;
        this.participationClient = participationClient;
        this.propositionClient = propositionClient;
        this.utilisateurClient = utilisateurClient;
    }

    // ------------------------------------------------------------------ liste par statut (onglets)

    /**
     * Liste des campagnes d'un statut, présentée en onglets.
     *
     * @param statut onglet choisi, {@code OUVERTE} par défaut
     * @param model  données transmises à la vue
     * @return la vue {@code admin/campagnes}
     */
    @GetMapping
    public String lister(@RequestParam(defaultValue = "OUVERTE") StatutCampagne statut, Model model) {
        model.addAttribute("statuts", StatutCampagne.values());
        model.addAttribute("statutChoisi", statut);
        model.addAttribute("campagnes", campagneClient.listerParStatut(statut));
        return "admin/campagnes";
    }

    // ------------------------------------------------------------------ création

    /**
     * Formulaire de création, prérempli avec des dates raisonnables.
     *
     * @param model données transmises à la vue
     * @return la vue {@code admin/nouvelle-campagne}
     */
    @GetMapping("/nouvelle")
    public String formulaire(Model model) {
        model.addAttribute("campagne", CampagneForm.nouvelle(LocalDateTime.now()));
        return "admin/nouvelle-campagne";
    }

    /**
     * Crée la campagne (au statut {@code BROUILLON}) puis affiche sa page.
     *
     * @param campagne    saisie, validée par Bean Validation
     * @param erreurs     erreurs de validation, complétées ici (ordre des dates, refus de l'API)
     * @param redirection message « flash » affiché après la redirection
     * @return la redirection vers la nouvelle campagne, ou le formulaire réaffiché avec ses erreurs
     */
    @PostMapping("/nouvelle")
    public String creer(@Valid @ModelAttribute("campagne") CampagneForm campagne,
                        BindingResult erreurs,
                        RedirectAttributes redirection) {
        if (campagne.getDateOuverture() != null && campagne.getDateCloture() != null
                && !campagne.getDateCloture().isAfter(campagne.getDateOuverture())) {
            erreurs.rejectValue("dateCloture", "dates", "La date limite doit être postérieure à l'ouverture");
        }
        if (erreurs.hasErrors()) {
            return "admin/nouvelle-campagne";
        }
        try {
            Campagne creee = campagneClient.creer(campagne.versRequete());
            redirection.addFlashAttribute("info", "Campagne créée en brouillon : vérifiez-la puis ouvrez-la.");
            // redirection vers la page de la NOUVELLE campagne : son id n'est connu qu'après la réponse de l'API
            return "redirect:/admin/campagnes/" + creee.id();
        } catch (ApiException e) {
            erreurs.reject("api", e.getMessage());
            return "admin/nouvelle-campagne";
        }
    }

    // ------------------------------------------------------------------ détail d'une campagne

    /**
     * Tableau de bord d'une campagne : participations, volume engagé, offres classées de la moins chère
     * à la plus chère, et boutons des actions possibles.
     *
     * @param id    identifiant de la campagne
     * @param model données transmises à la vue
     * @return la vue {@code admin/campagne}
     */
    @GetMapping("/{id}")
    public String detail(@PathVariable String id, Model model) {
        Campagne campagne = campagneClient.trouver(id);
        List<Participation> participations = participationClient.listerParCampagne(id);
        List<Proposition> propositions = propositionClient.listerParCampagne(id);

        // un seul appel à l'API par utilisateur différent (clients ET fournisseurs)
        Map<String, Utilisateur> utilisateurs = Stream.concat(
                        participations.stream().map(Participation::clientId),
                        propositions.stream().map(Proposition::fournisseurId))
                .distinct()
                .collect(Collectors.toMap(Function.identity(), utilisateurClient::trouver));

        // volume engagé à ce jour : l'API ne fige volumeTotal qu'à la clôture
        int volumeEngage = participations.stream()
                .filter(p -> p.statut() == StatutParticipation.CONFIRMEE)
                .mapToInt(Participation::volumeSouhaite)
                .sum();

        // offres classées : la moins chère pour le volume total d'abord (sinon par date de dépôt)
        List<LigneProposition> offres = propositions.stream()
                .map(p -> new LigneProposition(p, campagne))
                .sorted(Comparator.comparing(LigneProposition::prixAuVolumeTotal,
                                Comparator.nullsLast(Comparator.<BigDecimal>naturalOrder()))
                        .thenComparing(l -> l.proposition().dateProposition()))
                .toList();

        model.addAttribute("campagne", campagne);
        model.addAttribute("actions", campagne.statut().actionsPossibles());
        model.addAttribute("participations", participations.stream()
                .sorted(Comparator.comparing(Participation::dateParticipation)).toList());
        model.addAttribute("volumeEngage", volumeEngage);
        model.addAttribute("offres", offres);
        model.addAttribute("utilisateurs", utilisateurs);
        return "admin/campagne";
    }

    // ------------------------------------------------------------------ actions du cycle de vie

    /**
     * Exécute une action du cycle de vie.
     *
     * <p>Une seule route pour les cinq actions : le bouton envoie {@code action=OUVRIR}, {@code CLOTURER}...
     * Spring convertit ce texte en {@link ActionCampagne} ; une valeur inconnue donne une erreur 400.</p>
     *
     * @param id          identifiant de la campagne
     * @param action      action demandée
     * @param redirection message de réussite ou d'erreur, affiché après la redirection
     * @return la redirection vers la page de la campagne
     */
    @PostMapping("/{id}/action")
    public String executer(@PathVariable String id, @RequestParam ActionCampagne action,
                           RedirectAttributes redirection) {
        try {
            campagneClient.executer(id, action);
            redirection.addFlashAttribute("info", "Action effectuée : " + action.getLibelle() + ".");
        } catch (ApiException e) {
            // transition refusée (409), aucune proposition valable pour l'attribution...
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/admin/campagnes/" + id;
    }
}
