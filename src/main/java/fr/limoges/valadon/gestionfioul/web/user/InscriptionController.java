package fr.limoges.valadon.gestionfioul.web.user;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import jakarta.validation.Valid;

/**
 * Inscription d'un nouveau client ou fournisseur (page publique).
 *
 * <p>Illustre le schéma Post / Redirect / Get et les trois niveaux de validation :
 * annotations Bean Validation, règles entre champs vérifiées ici, puis refus éventuel de l'API.</p>
 */
@Controller
@RequestMapping("/inscription")
public class InscriptionController {

    private final UtilisateurClient utilisateurClient;

    public InscriptionController(UtilisateurClient utilisateurClient) {
        this.utilisateurClient = utilisateurClient;
    }

    /**
     * Rôles proposés dans le formulaire, ajoutés au modèle de chaque requête de ce contrôleur.
     *
     * @return client et fournisseur, jamais administrateur (l'API le refuserait de toute façon)
     */
    @ModelAttribute("roles")
    public List<Role> roles() {
        return List.of(Role.CLIENT, Role.FOURNISSEUR);
    }

    /**
     * Formulaire d'inscription vide.
     *
     * @param model données transmises à la vue
     * @return la vue {@code inscription}
     */
    @GetMapping
    public String formulaire(Model model) {
        model.addAttribute("inscription", new InscriptionForm());
        return "inscription";
    }

    /**
     * Crée le compte auprès de l'API puis redirige vers la page de connexion.
     *
     * <p>{@code @Valid} déclenche les contrôles des annotations de {@link InscriptionForm} ;
     * {@code BindingResult} (juste après le paramètre validé) reçoit les erreurs au lieu de lever une exception.</p>
     *
     * @param inscription saisie, validée par Bean Validation
     * @param erreurs     erreurs de validation, complétées par les règles entre champs et le refus de l'API
     * @param redirection message et email transmis à la page de connexion (attributs « flash »)
     * @return la redirection vers la connexion, ou le formulaire réaffiché avec ses erreurs
     */
    @PostMapping
    public String inscrire(@Valid @ModelAttribute("inscription") InscriptionForm inscription,
                           BindingResult erreurs,
                           RedirectAttributes redirection) {
        verifierReglesEntreChamps(inscription, erreurs);

        if (erreurs.hasErrors()) {
            inscription.effacerMotsDePasse();
            return "inscription";   // même vue : le formulaire est réaffiché avec les valeurs et les erreurs
        }

        try {
            utilisateurClient.inscrire(inscription.versRequete());
        } catch (ApiException e) {
            // l'API reste juge en dernier ressort (email déjà utilisé...) : son message devient une erreur globale
            erreurs.reject("api", e.getMessage());
            inscription.effacerMotsDePasse();
            return "inscription";
        }

        // Post / Redirect / Get : après un POST réussi, on redirige (un F5 ne recréerait pas le compte)
        // les attributs "flash" survivent à UNE redirection, puis disparaissent
        redirection.addFlashAttribute("info", "Votre compte a été créé : vous pouvez vous connecter.");
        redirection.addFlashAttribute("email", inscription.getEmail());
        return "redirect:/connexion";
    }

    private void verifierReglesEntreChamps(InscriptionForm inscription, BindingResult erreurs) {
        if (inscription.getMotDePasse() != null
                && !inscription.getMotDePasse().equals(inscription.getConfirmation())) {
            erreurs.rejectValue("confirmation", "confirmation", "Les deux mots de passe ne correspondent pas");
        }
        if (inscription.getRole() == Role.ADMIN) {
            erreurs.rejectValue("role", "role", "Ce type de compte ne peut pas être demandé");
        }
        if (inscription.getRole() == Role.FOURNISSEUR
                && (inscription.getRaisonSociale() == null || inscription.getRaisonSociale().isBlank())) {
            erreurs.rejectValue("raisonSociale", "raisonSociale", "La raison sociale est obligatoire pour un fournisseur");
        }
    }
}
