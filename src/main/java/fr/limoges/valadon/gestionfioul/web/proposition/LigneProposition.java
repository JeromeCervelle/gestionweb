package fr.limoges.valadon.gestionfioul.web.proposition;

import java.math.BigDecimal;

import fr.limoges.valadon.gestionfioul.web.campagne.Campagne;

/**
 * Une proposition avec sa campagne (objet de présentation), utilisée dans « Mes propositions »
 * et dans le classement des offres de l'administrateur.
 *
 * @param proposition le barème proposé
 * @param campagne    la campagne concernée, dont le volume total détermine le palier applicable
 */
public record LigneProposition(Proposition proposition, Campagne campagne) {

    /**
     * Prix au litre de CE barème pour le volume total de la campagne, appliqué à tout le volume
     * (même règle que l'API).
     *
     * @return le prix du palier qui contient le volume total, ou {@code null} tant que la campagne
     *         n'est pas clôturée ({@code volumeTotal} vaut alors 0)
     */
    public BigDecimal prixAuVolumeTotal() {
        if (campagne.volumeTotal() <= 0) {
            return null;
        }
        return proposition.paliers().stream()
                .filter(p -> p.contient(campagne.volumeTotal()))
                .map(Palier::prixLitre)
                .findFirst()
                .orElse(null);
    }

    /**
     * Montant total de la commande avec ce barème : volume total × prix au litre.
     *
     * @return le montant en euros, ou {@code null} si le prix n'est pas encore connu
     */
    public BigDecimal montantTotal() {
        BigDecimal prix = prixAuVolumeTotal();
        return prix == null ? null : prix.multiply(BigDecimal.valueOf(campagne.volumeTotal()));
    }
}
