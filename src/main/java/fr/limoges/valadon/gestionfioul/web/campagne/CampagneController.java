package fr.limoges.valadon.gestionfioul.web.campagne;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Liste des campagnes ouvertes, visible par tout utilisateur connecté.
 */
@Controller
@RequestMapping("/campagnes")
public class CampagneController {

    private final CampagneClient campagneClient;

    public CampagneController(CampagneClient campagneClient) {
        this.campagneClient = campagneClient;
    }

    /**
     * Liste des campagnes ouvertes.
     *
     * <p>Aucune gestion d'erreur ici : si l'API refuse (401...) ou ne répond pas,
     * l'exception remonte jusqu'à {@link fr.limoges.valadon.gestionfioul.web.common.WebExceptionHandler WebExceptionHandler} qui affiche la page d'erreur.</p>
     *
     * @param model données transmises à la vue
     * @return la vue {@code campagne/liste}
     */
    @GetMapping
    public String lister(Model model) {
        model.addAttribute("campagnes", campagneClient.listerParStatut(StatutCampagne.OUVERTE));
        return "campagne/liste";
    }
}
