package fr.limoges.valadon.gestionfioul.web.participation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Saisie du formulaire « Participer à une campagne ».
 *
 * <p>{@code Integer} (et non {@code int}) pour le volume : un champ laissé vide donne {@code null},
 * que {@code @NotNull} sait signaler. Avec un {@code int}, la valeur vide ne pourrait pas être convertie
 * et produirait une erreur de conversion.</p>
 */
public class ParticipationForm {

    @NotNull(message = "Le volume est obligatoire")
    @Positive(message = "Le volume doit être strictement positif")
    @Max(value = 100_000, message = "Le volume ne peut pas dépasser 100 000 litres")
    private Integer volumeSouhaite;

    // vide = l'API reprend l'adresse du profil du client
    private String adresseLivraison;

    /**
     * Convertit la saisie en corps de requête pour l'API.
     *
     * @return la requête de participation
     */
    public ParticiperRequest versRequete() {
        return new ParticiperRequest(volumeSouhaite, adresseLivraison);
    }

    public Integer getVolumeSouhaite() {
        return volumeSouhaite;
    }

    public void setVolumeSouhaite(Integer volumeSouhaite) {
        this.volumeSouhaite = volumeSouhaite;
    }

    public String getAdresseLivraison() {
        return adresseLivraison;
    }

    public void setAdresseLivraison(String adresseLivraison) {
        this.adresseLivraison = adresseLivraison;
    }
}
