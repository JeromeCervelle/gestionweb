package fr.limoges.valadon.gestionfioul.web.participation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import fr.limoges.valadon.gestionfioul.web.campagne.Campagne;
import fr.limoges.valadon.gestionfioul.web.campagne.StatutCampagne;

/**
 * Test unitaire pur : aucune dépendance à Spring.
 */
class LigneParticipationTest {

    private static Participation participation(StatutParticipation statut) {
        return new Participation("p1", "c1", "client-1", 3000, "1 rue A", statut, LocalDateTime.now().minusDays(1));
    }

    private static Campagne campagne(StatutCampagne statut, LocalDateTime dateCloture) {
        return new Campagne("c1", "Hiver", dateCloture.minusMonths(1), dateCloture, statut, 0, "87", null);
    }

    @Test
    void annulable_si_confirmee_et_campagne_ouverte_non_echue() {
        LigneParticipation ligne = new LigneParticipation(participation(StatutParticipation.CONFIRMEE),
                campagne(StatutCampagne.OUVERTE, LocalDateTime.now().plusDays(10)));

        assertThat(ligne.annulable()).isTrue();
    }

    @Test
    void pas_annulable_si_deja_annulee() {
        LigneParticipation ligne = new LigneParticipation(participation(StatutParticipation.ANNULEE),
                campagne(StatutCampagne.OUVERTE, LocalDateTime.now().plusDays(10)));

        assertThat(ligne.annulable()).isFalse();
    }

    @Test
    void pas_annulable_si_la_date_limite_est_passee_meme_campagne_encore_ouverte() {
        // la clôture automatique n'est peut-être pas encore passée : la campagne est toujours OUVERTE
        LigneParticipation ligne = new LigneParticipation(participation(StatutParticipation.CONFIRMEE),
                campagne(StatutCampagne.OUVERTE, LocalDateTime.now().minusMinutes(1)));

        assertThat(ligne.annulable()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = StatutCampagne.class, names = "OUVERTE", mode = EnumSource.Mode.EXCLUDE)
    void pas_annulable_si_la_campagne_n_est_plus_ouverte(StatutCampagne statut) {
        LigneParticipation ligne = new LigneParticipation(participation(StatutParticipation.CONFIRMEE),
                campagne(statut, LocalDateTime.now().plusDays(10)));

        assertThat(ligne.annulable()).isFalse();
    }
}
