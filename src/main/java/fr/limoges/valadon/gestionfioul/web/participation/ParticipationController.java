package fr.limoges.valadon.gestionfioul.web.participation;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import fr.limoges.valadon.gestionfioul.web.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.web.campagne.CampagneClient;
import jakarta.validation.Valid;

/**
 * Espace client : participer à une campagne, consulter et annuler ses participations.
 *
 * <p>L'accès est réservé au rôle {@code CLIENT} dans
 * {@link fr.limoges.valadon.gestionfioul.web.security.SecurityConfig SecurityConfig}.</p>
 */
@Controller
public class ParticipationController {

    private final ParticipationClient participationClient;
    private final CampagneClient campagneClient;

    public ParticipationController(ParticipationClient participationClient, CampagneClient campagneClient) {
        this.participationClient = participationClient;
        this.campagneClient = campagneClient;
    }

    // ------------------------------------------------------------------ participer

    /**
     * Formulaire de participation à une campagne.
     *
     * @param campagneId identifiant de la campagne
     * @param model      données transmises à la vue
     * @return la vue {@code participation/formulaire}
     */
    @GetMapping("/campagnes/{campagneId}/participer")
    public String formulaire(@PathVariable String campagneId, Model model) {
        // campagne inconnue : ApiException 404, affichée par WebExceptionHandler
        model.addAttribute("campagne", campagneClient.trouver(campagneId));
        model.addAttribute("participation", new ParticipationForm());
        return "participation/formulaire";
    }

    /**
     * Enregistre la participation auprès de l'API.
     *
     * @param campagneId    identifiant de la campagne
     * @param participation saisie, validée par Bean Validation
     * @param erreurs       erreurs de validation, complétées par le refus éventuel de l'API
     * @param model         données transmises à la vue en cas de réaffichage
     * @param redirection   message « flash » affiché après la redirection
     * @return la redirection vers « Mes participations », ou le formulaire réaffiché avec ses erreurs
     */
    @PostMapping("/campagnes/{campagneId}/participer")
    public String participer(@PathVariable String campagneId,
                             @Valid @ModelAttribute("participation") ParticipationForm participation,
                             BindingResult erreurs,
                             Model model,
                             RedirectAttributes redirection) {
        if (!erreurs.hasErrors()) {
            try {
                participationClient.participer(campagneId, participation.versRequete());
                redirection.addFlashAttribute("info",
                        "Votre participation de " + participation.getVolumeSouhaite() + " L est enregistrée.");
                return "redirect:/mes-participations";
            } catch (ApiException e) {
                // campagne fermée, déjà inscrit... (409) : message de l'API en haut du formulaire
                erreurs.reject("api", e.getMessage());
            }
        }
        // réaffichage : la vue a aussi besoin de la campagne (le modèle de la requête GET est perdu)
        model.addAttribute("campagne", campagneClient.trouver(campagneId));
        return "participation/formulaire";
    }

    // ------------------------------------------------------------------ mes participations

    /**
     * Participations du client connecté, des plus récentes aux plus anciennes, avec leur campagne.
     *
     * @param model données transmises à la vue
     * @return la vue {@code participation/mes-participations}
     */
    @GetMapping("/mes-participations")
    public String mesParticipations(Model model) {
        List<Participation> participations = participationClient.mesParticipations();

        // l'API ne renvoie que campagneId : on charge chaque campagne UNE seule fois, même si elle revient plusieurs fois
        Map<String, Campagne> campagnes = participations.stream()
                .map(Participation::campagneId)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), campagneClient::trouver));

        List<LigneParticipation> lignes = participations.stream()
                .map(p -> new LigneParticipation(p, campagnes.get(p.campagneId())))
                .sorted(Comparator.comparing((LigneParticipation l) -> l.participation().dateParticipation()).reversed())
                .toList();

        model.addAttribute("lignes", lignes);
        return "participation/mes-participations";
    }

    // ------------------------------------------------------------------ annuler

    /**
     * Annule une participation.
     *
     * @param id          identifiant de la participation
     * @param redirection message de réussite ou d'erreur, affiché après la redirection
     * @return la redirection vers « Mes participations »
     */
    @PostMapping("/participations/{id}/annuler")
    public String annuler(@PathVariable String id, RedirectAttributes redirection) {
        try {
            Participation annulee = participationClient.annuler(id);
            redirection.addFlashAttribute("info",
                    "Votre participation de " + annulee.volumeSouhaite() + " L a été annulée.");
        } catch (ApiException e) {
            redirection.addFlashAttribute("erreur", e.getMessage());
        }
        return "redirect:/mes-participations";
    }
}
