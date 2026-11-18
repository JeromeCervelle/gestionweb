package fr.limoges.valadon.gestionfioul.web.accueil;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Affiche la page d'accueil du site.
 *
 * <p>{@code @Controller} (et non {@code @RestController}) : la méthode renvoie le NOM d'une vue Thymeleaf,
 * pas des données JSON. Spring cherche alors le fichier {@code templates/accueil/index.html}.</p>
 */
@Controller
public class AccueilController {

    private final EtatApiClient etatApiClient;

    public AccueilController(EtatApiClient etatApiClient) {
        this.etatApiClient = etatApiClient;
    }

    /**
     * Page d'accueil : titre du site et état de l'API.
     *
     * @param model données transmises à la vue
     * @return le nom de la vue Thymeleaf {@code accueil/index}
     */
    @GetMapping("/")
    public String accueil(Model model) {
        // le Model transporte des données du contrôleur vers la vue
        model.addAttribute("titre", "Commandes groupées de fioul");
        model.addAttribute("etatApi", etatApiClient.consulter());
        return "accueil/index";
    }
}
