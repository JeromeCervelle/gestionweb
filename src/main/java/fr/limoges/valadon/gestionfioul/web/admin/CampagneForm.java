package fr.limoges.valadon.gestionfioul.web.admin;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.format.annotation.DateTimeFormat;

import fr.limoges.valadon.gestionfioul.web.campagne.CreerCampagneRequest;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Formulaire de création d'une campagne (administrateur).
 *
 * <p>Les règles simples sont des annotations Bean Validation ; l'ordre des deux dates est vérifié
 * par le contrôleur, car il porte sur deux champs.</p>
 */
public class CampagneForm {

    @NotBlank(message = "Le libellé est obligatoire")
    @Size(max = 100, message = "100 caractères au maximum")
    private String libelle;

    @NotNull(message = "La date d'ouverture est obligatoire")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dateOuverture;

    @NotNull(message = "La date limite est obligatoire")
    @Future(message = "La date limite doit être dans le futur")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime dateCloture;

    private String zoneLivraison;

    /**
     * Valeurs proposées : ouverture maintenant, clôture dans 30 jours à 23 h 59.
     *
     * @param maintenant date et heure actuelles (paramètre plutôt que {@code now()} : la méthode reste testable)
     * @return un formulaire prérempli
     */
    public static CampagneForm nouvelle(LocalDateTime maintenant) {
        CampagneForm form = new CampagneForm();
        form.dateOuverture = maintenant.truncatedTo(ChronoUnit.MINUTES);
        form.dateCloture = maintenant.plusDays(30).withHour(23).withMinute(59).truncatedTo(ChronoUnit.MINUTES);
        return form;
    }

    /**
     * Convertit la saisie en corps de requête pour l'API.
     *
     * @return la requête de création
     */
    public CreerCampagneRequest versRequete() {
        return new CreerCampagneRequest(libelle, dateOuverture, dateCloture, zoneLivraison);
    }

    public String getLibelle() {
        return libelle;
    }

    public void setLibelle(String libelle) {
        this.libelle = libelle;
    }

    public LocalDateTime getDateOuverture() {
        return dateOuverture;
    }

    public void setDateOuverture(LocalDateTime dateOuverture) {
        this.dateOuverture = dateOuverture;
    }

    public LocalDateTime getDateCloture() {
        return dateCloture;
    }

    public void setDateCloture(LocalDateTime dateCloture) {
        this.dateCloture = dateCloture;
    }

    public String getZoneLivraison() {
        return zoneLivraison;
    }

    public void setZoneLivraison(String zoneLivraison) {
        this.zoneLivraison = zoneLivraison;
    }
}
