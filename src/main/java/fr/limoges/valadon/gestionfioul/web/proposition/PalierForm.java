package fr.limoges.valadon.gestionfioul.web.proposition;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Une ligne de palier dans le formulaire de proposition.
 *
 * <p>Champs en types objets ({@code Integer}, {@code BigDecimal}) : une case vide donne {@code null},
 * ce qui est justement le sens de « volume max vide = sans limite ».</p>
 */
public class PalierForm {

    @NotNull(message = "Obligatoire")
    @PositiveOrZero(message = "Doit être positif ou nul")
    private Integer volumeMin;

    @Positive(message = "Doit être positif")
    private Integer volumeMax;

    @NotNull(message = "Obligatoire")
    @Positive(message = "Doit être strictement positif")
    @Digits(integer = 2, fraction = 3, message = "3 décimales au maximum")
    private BigDecimal prixLitre;

    /**
     * Ligne vide : constructeur utilisé par Spring pour remplir la liste à partir des champs du formulaire.
     */
    public PalierForm() {
    }

    /**
     * Ligne préremplie.
     *
     * @param volumeMin début de la tranche, en litres
     * @param volumeMax fin de la tranche, en litres, ou {@code null} pour « sans limite »
     * @param prixLitre prix au litre, ou {@code null} s'il reste à saisir
     */
    public PalierForm(Integer volumeMin, Integer volumeMax, BigDecimal prixLitre) {
        this.volumeMin = volumeMin;
        this.volumeMax = volumeMax;
        this.prixLitre = prixLitre;
    }

    /**
     * Convertit la ligne saisie en palier pour l'API.
     *
     * @return le palier correspondant
     */
    public Palier versPalier() {
        return new Palier(volumeMin, volumeMax, prixLitre);
    }

    public Integer getVolumeMin() {
        return volumeMin;
    }

    public void setVolumeMin(Integer volumeMin) {
        this.volumeMin = volumeMin;
    }

    public Integer getVolumeMax() {
        return volumeMax;
    }

    public void setVolumeMax(Integer volumeMax) {
        this.volumeMax = volumeMax;
    }

    public BigDecimal getPrixLitre() {
        return prixLitre;
    }

    public void setPrixLitre(BigDecimal prixLitre) {
        this.prixLitre = prixLitre;
    }
}
