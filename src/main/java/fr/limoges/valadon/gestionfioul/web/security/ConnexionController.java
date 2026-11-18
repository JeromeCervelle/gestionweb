package fr.limoges.valadon.gestionfioul.web.security;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttribute;

/**
 * Affiche la page de connexion.
 *
 * <p>Le traitement du formulaire ({@code POST /connexion}) n'est PAS ici : il est fait par le filtre
 * de Spring Security configuré dans {@link SecurityConfig} ({@code formLogin}).</p>
 */
@Controller
public class ConnexionController {

    /**
     * Page de connexion, avec le message d'échec ou de déconnexion éventuel.
     *
     * @param erreur      {@code true} après un échec (paramètre {@code ?erreur=true} ajouté par Spring Security)
     * @param deconnexion {@code true} après une déconnexion
     * @param echec       cause de l'échec, rangée en session par Spring Security
     * @param model       données transmises à la vue
     * @return la vue {@code connexion}
     */
    @GetMapping("/connexion")
    public String connexion(@RequestParam(defaultValue = "false") boolean erreur,
                            @RequestParam(defaultValue = "false") boolean deconnexion,
                            // après un échec, Spring Security range l'exception en session sous ce nom
                            @SessionAttribute(name = WebAttributes.AUTHENTICATION_EXCEPTION, required = false)
                            AuthenticationException echec,
                            Model model) {
        if (erreur) {
            model.addAttribute("erreur", echec != null ? echec.getMessage() : "Connexion impossible.");
        }
        if (deconnexion) {
            model.addAttribute("info", "Vous êtes déconnecté.");
        }
        return "connexion";
    }
}
