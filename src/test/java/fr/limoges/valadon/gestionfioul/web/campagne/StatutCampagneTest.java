package fr.limoges.valadon.gestionfioul.web.campagne;

import static fr.limoges.valadon.gestionfioul.web.campagne.ActionCampagne.ANNULER;
import static fr.limoges.valadon.gestionfioul.web.campagne.ActionCampagne.ATTRIBUER;
import static fr.limoges.valadon.gestionfioul.web.campagne.ActionCampagne.CLOTURER;
import static fr.limoges.valadon.gestionfioul.web.campagne.ActionCampagne.LIVRER;
import static fr.limoges.valadon.gestionfioul.web.campagne.ActionCampagne.OUVRIR;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Le cycle de vie, tel que le README de l'API le décrit (diagramme d'états).
 */
class StatutCampagneTest {

    static Stream<Arguments> cycleDeVie() {
        return Stream.of(
                Arguments.of(StatutCampagne.BROUILLON, List.of(OUVRIR, ANNULER)),
                Arguments.of(StatutCampagne.OUVERTE, List.of(CLOTURER, ANNULER)),
                Arguments.of(StatutCampagne.CLOTUREE, List.of(ATTRIBUER, ANNULER)),
                Arguments.of(StatutCampagne.ATTRIBUEE, List.of(LIVRER, ANNULER)),
                Arguments.of(StatutCampagne.LIVREE, List.of()),
                Arguments.of(StatutCampagne.ANNULEE, List.of()));
    }

    @ParameterizedTest
    @MethodSource("cycleDeVie")
    void actions_possibles_selon_le_statut(StatutCampagne statut, List<ActionCampagne> attendues) {
        assertThat(statut.actionsPossibles()).containsExactlyElementsOf(attendues);
    }

    @ParameterizedTest
    @EnumSource(value = StatutCampagne.class, names = {"LIVREE", "ANNULEE"}, mode = EnumSource.Mode.EXCLUDE)
    void on_peut_annuler_tant_que_la_campagne_n_est_ni_livree_ni_annulee(StatutCampagne statut) {
        assertThat(statut.actionsPossibles()).contains(ANNULER);
    }
}
