package fr.limoges.valadon.gestionfioul.web.proposition;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Formulaire « Proposer un barème » : une date de validité et une LISTE de paliers.
 *
 * <p>Les champs HTML s'appellent {@code paliers[0].volumeMin}, {@code paliers[0].prixLitre},
 * {@code paliers[1].volumeMin}... Spring remplit la liste en suivant ces indices
 * (elle doit exister : {@code new ArrayList<>()}).</p>
 */
public class PropositionForm {

    // format envoyé par <input type="datetime-local"> : 2026-12-31T18:00
    @NotNull(message = "La date de validité est obligatoire")
    @Future(message = "La date de validité doit être dans le futur")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime validiteJusqua;

    // @Valid : valider AUSSI chaque PalierForm de la liste (validation « en cascade »)
    @Valid
    @NotEmpty(message = "Au moins un palier est obligatoire")
    private List<PalierForm> paliers = new ArrayList<>();

    /**
     * Formulaire de départ : un seul palier « de 0 L à l'infini », validité proposée par défaut.
     *
     * @param validiteParDefaut date de validité préremplie
     * @return un formulaire prêt à être affiché
     */
    public static PropositionForm nouveau(LocalDateTime validiteParDefaut) {
        PropositionForm form = new PropositionForm();
        form.validiteJusqua = validiteParDefaut;
        form.paliers.add(new PalierForm(0, null, null));
        return form;
    }

    /**
     * Ajoute une ligne qui commence là où finit la dernière (si sa fin est déjà saisie).
     */
    public void ajouterPalier() {
        Integer debut = paliers.isEmpty() ? Integer.valueOf(0) : paliers.get(paliers.size() - 1).getVolumeMax();
        paliers.add(new PalierForm(debut, null, null));
    }

    /**
     * Retire une ligne. Sans effet sur la dernière ligne restante ou sur un indice invalide.
     *
     * @param index indice de la ligne, à partir de 0
     */
    public void retirerPalier(int index) {
        if (paliers.size() > 1 && index >= 0 && index < paliers.size()) {
            paliers.remove(index);
        }
    }

    /**
     * Convertit la saisie en corps de requête pour l'API.
     *
     * @return la requête de soumission
     */
    public SoumettrePropositionRequest versRequete() {
        return new SoumettrePropositionRequest(validiteJusqua,
                paliers.stream().map(PalierForm::versPalier).toList());
    }

    public LocalDateTime getValiditeJusqua() {
        return validiteJusqua;
    }

    public void setValiditeJusqua(LocalDateTime validiteJusqua) {
        this.validiteJusqua = validiteJusqua;
    }

    public List<PalierForm> getPaliers() {
        return paliers;
    }

    public void setPaliers(List<PalierForm> paliers) {
        this.paliers = paliers;
    }
}
