package fr.limoges.valadon.gestionfioul.web.proposition;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;

/**
 * Test unitaire du validateur, sans Spring MVC : BeanPropertyBindingResult est l'implémentation
 * de Errors qu'utilise Spring ; on la crée à la main pour lire les erreurs produites.
 */
class BaremeValidatorTest {

    private final BaremeValidator validateur = new BaremeValidator();

    private static PalierForm palier(Integer min, Integer max) {
        return new PalierForm(min, max, new BigDecimal("1.05"));
    }

    private Errors valider(PalierForm... paliers) {
        PropositionForm form = new PropositionForm();
        form.setPaliers(List.of(paliers));
        Errors erreurs = new BeanPropertyBindingResult(form, "proposition");
        validateur.validate(form, erreurs);
        return erreurs;
    }

    @Test
    void bareme_correct_a_trois_paliers() {
        assertThat(valider(palier(0, 5000), palier(5000, 15000), palier(15000, null)).hasErrors()).isFalse();
    }

    @Test
    void un_seul_palier_sans_limite() {
        assertThat(valider(palier(0, null)).hasErrors()).isFalse();
    }

    @Test
    void premier_palier_qui_ne_commence_pas_a_zero() {
        Errors erreurs = valider(palier(100, null));

        assertThat(erreurs.getFieldError("paliers[0].volumeMin").getDefaultMessage())
                .isEqualTo("Le premier palier doit commencer à 0 L");
    }

    @Test
    void trou_ou_chevauchement_entre_deux_paliers() {
        Errors trou = valider(palier(0, 5000), palier(6000, null));
        Errors chevauchement = valider(palier(0, 5000), palier(4000, null));

        assertThat(trou.getFieldError("paliers[1].volumeMin").getDefaultMessage())
                .isEqualTo("Doit valoir 5000 L (fin du palier précédent)");
        assertThat(chevauchement.getFieldError("paliers[1].volumeMin")).isNotNull();
    }

    @Test
    void palier_intermediaire_sans_limite() {
        Errors erreurs = valider(palier(0, null), palier(5000, null));

        assertThat(erreurs.getFieldError("paliers[0].volumeMax").getDefaultMessage())
                .isEqualTo("Seul le dernier palier peut être sans limite");
    }

    @Test
    void dernier_palier_avec_une_limite() {
        Errors erreurs = valider(palier(0, 5000), palier(5000, 15000));

        assertThat(erreurs.getFieldError("paliers[1].volumeMax").getDefaultMessage())
                .isEqualTo("Le dernier palier doit être sans limite (laisser vide)");
    }

    @Test
    void volume_max_inferieur_ou_egal_au_volume_min() {
        Errors erreurs = valider(palier(0, 0), palier(0, null));

        assertThat(erreurs.getFieldError("paliers[0].volumeMax").getDefaultMessage())
                .isEqualTo("Doit être supérieur au volume minimum");
    }

    @Test
    void champs_vides_ignores_bean_validation_s_en_charge() {
        // volumeMin null : @NotNull le signale déjà, le validateur ne doit pas planter (NullPointerException)
        assertThat(valider(palier(null, null)).getErrorCount()).isZero();
    }
}
