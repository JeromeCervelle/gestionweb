package fr.limoges.valadon.gestionfioul.web.proposition;

import java.math.BigDecimal;

/**
 * Tranche de volume et son prix au litre, tels que l'API les échange en JSON.
 *
 * <p>{@link BigDecimal} pour un prix : jamais de {@code double} pour de l'argent
 * (1.10 n'est pas représentable exactement en {@code double}).</p>
 *
 * @param volumeMin début de la tranche, en litres (inclus)
 * @param volumeMax fin de la tranche, en litres (exclue) ; {@code null} = pas de limite haute
 * @param prixLitre prix au litre appliqué à toute la commande si son volume tombe dans cette tranche
 */
public record Palier(int volumeMin, Integer volumeMax, BigDecimal prixLitre) {

    /**
     * Indique si un volume tombe dans cette tranche.
     *
     * @param volume volume en litres
     * @return {@code true} si {@code volumeMin <= volume < volumeMax}
     */
    public boolean contient(int volume) {
        return volume >= volumeMin && (volumeMax == null || volume < volumeMax);
    }
}
