package fr.limoges.valadon.gestionfioul.web.proposition;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import fr.limoges.valadon.gestionfioul.web.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.web.campagne.StatutCampagne;

class LignePropositionTest {

    // barème du README de l'API : 1,10 € jusqu'à 5 000 L, 1,05 € jusqu'à 15 000 L, 1,00 € au-delà
    private static final Proposition BAREME = new Proposition("pr1", "c1", "f1",
            LocalDateTime.now(), LocalDateTime.now().plusMonths(2), StatutProposition.SOUMISE, List.of(
            new Palier(0, 5000, new BigDecimal("1.10")),
            new Palier(5000, 15000, new BigDecimal("1.05")),
            new Palier(15000, null, new BigDecimal("1.00"))));

    private static LigneProposition ligne(int volumeTotal) {
        Campagne campagne = new Campagne("c1", "Hiver", LocalDateTime.now().minusMonths(1), LocalDateTime.now(),
                StatutCampagne.CLOTUREE, volumeTotal, "87", null);
        return new LigneProposition(BAREME, campagne);
    }

    // bornes : minimum inclus, maximum exclu (5 000 L tombe dans le palier 2)
    @ParameterizedTest
    @CsvSource({
            "1,     1.10",
            "4999,  1.10",
            "5000,  1.05",
            "8000,  1.05",
            "14999, 1.05",
            "15000, 1.00",
            "90000, 1.00"})
    void prix_du_palier_qui_contient_le_volume_total(int volumeTotal, BigDecimal prixAttendu) {
        assertThat(ligne(volumeTotal).prixAuVolumeTotal()).isEqualByComparingTo(prixAttendu);
    }

    @ParameterizedTest
    @CsvSource({"8000, 8400.00", "15000, 15000.00"})
    void montant_total_le_prix_s_applique_a_tout_le_volume(int volumeTotal, BigDecimal montantAttendu) {
        // 8 000 L à 1,05 € = 8 400 € (et non 5 000 × 1,10 + 3 000 × 1,05)
        assertThat(ligne(volumeTotal).montantTotal()).isEqualByComparingTo(montantAttendu);
    }

    @Test
    void campagne_pas_encore_cloturee_pas_de_prix() {
        // tant que la campagne est ouverte, l'API laisse volumeTotal à 0
        assertThat(ligne(0).prixAuVolumeTotal()).isNull();
        assertThat(ligne(0).montantTotal()).isNull();
    }
}
