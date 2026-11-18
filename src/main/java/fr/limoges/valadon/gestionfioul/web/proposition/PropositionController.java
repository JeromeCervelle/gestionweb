package fr.limoges.valadon.gestionfioul.web.proposition;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import fr.limoges.valadon.gestionfioul.web.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.web.campagne.CampagneClient;
import jakarta.validation.Valid;

/**
 * Espace fournisseur : proposer un barème sur une campagne ouverte, consulter ses propositions.
 *
 * <p>L'accès est réservé au rôle {@code FOURNISSEUR} dans
 * {@link fr.limoges.valadon.gestionfioul.web.security.SecurityConfig SecurityConfig}.</p>
 *
 * <p>Le formulaire a TROIS boutons de soumission, distingués par leur attribut {@code name} :</p>
 * <ul>
 *   <li>{@code ajouterPalier} : ajoute une ligne et réaffiche le formulaire (sans valider ni appeler l'API) ;</li>
 *   <li>{@code retirerPalier} : retire la ligne dont l'indice est la valeur du bouton ;</li>
 *   <li>aucun des deux : soumission réelle du barème.</li>
 * </ul>
 * <p>Spring choisit la méthode grâce à l'attribut {@code params} de {@code @PostMapping}.
 * Aucun JavaScript n'est nécessaire.</p>
 */
@Controller
public class PropositionController {

    private static final String VUE_FORMULAIRE = "proposition/formulaire";

    private final PropositionClient propositionClient;
    private final CampagneClient campagneClient;
    private final BaremeValidator baremeValidator;

    public PropositionController(PropositionClient propositionClient, CampagneClient campagneClient,
                                 BaremeValidator baremeValidator) {
        this.propositionClient = propositionClient;
        this.campagneClient = campagneClient;
        this.baremeValidator = baremeValidator;
    }

    // ------------------------------------------------------------------ formulaire

    /**
     * Formulaire de proposition : un seul palier, validité proposée 30 jours après la clôture.
     *
     * @param campagneId identifiant de la campagne
     * @param model      données transmises à la vue
     * @return la vue {@code proposition/formulaire}
     */
    @GetMapping("/campagnes/{campagneId}/proposer")
    public String formulaire(@PathVariable String campagneId, Model model) {
        Campagne campagne = campagneClient.trouver(campagneId);
        model.addAttribute("campagne", campagne);
        // validité proposée : 30 jours après la clôture (l'API exige qu'elle soit postérieure à la clôture)
        model.addAttribute("proposition",
                PropositionForm.nouveau(campagne.dateCloture().plusDays(30).truncatedTo(ChronoUnit.MINUTES)));
        return VUE_FORMULAIRE;
    }

    /**
     * Bouton « Ajouter un palier » : ajoute une ligne, sans valider la saisie.
     *
     * @param campagneId  identifiant de la campagne
     * @param proposition saisie en cours
     * @param erreurs     présent pour qu'une saisie non numérique ne fasse pas échouer la requête
     * @param model       données transmises à la vue
     * @return la vue {@code proposition/formulaire}
     */
    @PostMapping(value = "/campagnes/{campagneId}/proposer", params = "ajouterPalier")
    public String ajouterPalier(@PathVariable String campagneId,
                                @ModelAttribute("proposition") PropositionForm proposition,
                                BindingResult erreurs,   // présent pour ne pas échouer sur une saisie non numérique
                                Model model) {
        proposition.ajouterPalier();
        model.addAttribute("campagne", campagneClient.trouver(campagneId));
        return VUE_FORMULAIRE;
    }

    /**
     * Bouton « Retirer » d'une ligne : la valeur du bouton est l'indice de la ligne.
     *
     * @param campagneId    identifiant de la campagne
     * @param retirerPalier indice de la ligne à retirer
     * @param proposition   saisie en cours
     * @param erreurs       présent pour qu'une saisie non numérique ne fasse pas échouer la requête
     * @param model         données transmises à la vue
     * @return la vue {@code proposition/formulaire}
     */
    @PostMapping(value = "/campagnes/{campagneId}/proposer", params = "retirerPalier")
    public String retirerPalier(@PathVariable String campagneId,
                                @RequestParam int retirerPalier,
                                @ModelAttribute("proposition") PropositionForm proposition,
                                BindingResult erreurs,
                                Model model) {
        proposition.retirerPalier(retirerPalier);
        model.addAttribute("campagne", campagneClient.trouver(campagneId));
        return VUE_FORMULAIRE;
    }

    /**
     * Soumission du barème : Bean Validation, puis {@link BaremeValidator}, puis la règle de validité
     * qui dépend de la campagne, et enfin l'API.
     *
     * @param campagneId  identifiant de la campagne
     * @param proposition saisie, validée par Bean Validation
     * @param erreurs     erreurs de validation, complétées ici
     * @param model       données transmises à la vue en cas de réaffichage
     * @param redirection message « flash » affiché après la redirection
     * @return la redirection vers « Mes propositions », ou le formulaire réaffiché avec ses erreurs
     */
    @PostMapping("/campagnes/{campagneId}/proposer")
    public String soumettre(@PathVariable String campagneId,
                            @Valid @ModelAttribute("proposition") PropositionForm proposition,
                            BindingResult erreurs,
                            Model model,
                            RedirectAttributes redirection) {
        Campagne campagne = campagneClient.trouver(campagneId);

        // 1. règles d'enchaînement des paliers (validateur Spring, en plus de Bean Validation)
        baremeValidator.validate(proposition, erreurs);
        // 2. règle qui dépend de la campagne : impossible à vérifier dans le formulaire seul
        if (proposition.getValiditeJusqua() != null && !proposition.getValiditeJusqua().isAfter(campagne.dateCloture())) {
            erreurs.rejectValue("validiteJusqua", "validite",
                    "Votre offre doit rester valable après la clôture de la campagne");
        }

        if (!erreurs.hasErrors()) {
            try {
                propositionClient.soumettre(campagneId, proposition.versRequete());
                redirection.addFlashAttribute("info",
                        "Votre barème pour « " + campagne.libelle() + " » est enregistré.");
                return "redirect:/mes-propositions";
            } catch (ApiException e) {
                erreurs.reject("api", e.getMessage());
            }
        }
        model.addAttribute("campagne", campagne);
        return VUE_FORMULAIRE;
    }

    // ------------------------------------------------------------------ mes propositions

    /**
     * Propositions du fournisseur connecté, des plus récentes aux plus anciennes, avec leur campagne.
     *
     * @param model données transmises à la vue
     * @return la vue {@code proposition/mes-propositions}
     */
    @GetMapping("/mes-propositions")
    public String mesPropositions(Model model) {
        List<Proposition> propositions = propositionClient.mesPropositions();

        // même principe qu'à l'étape 5 : chaque campagne n'est demandée qu'une fois à l'API
        Map<String, Campagne> campagnes = propositions.stream()
                .map(Proposition::campagneId)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), campagneClient::trouver));

        List<LigneProposition> lignes = propositions.stream()
                .map(p -> new LigneProposition(p, campagnes.get(p.campagneId())))
                .sorted(Comparator.comparing((LigneProposition l) -> l.proposition().dateProposition()).reversed())
                .toList();

        model.addAttribute("lignes", lignes);
        return "proposition/mes-propositions";
    }
}
