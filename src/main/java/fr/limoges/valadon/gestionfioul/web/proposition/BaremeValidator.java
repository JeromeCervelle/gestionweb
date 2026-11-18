package fr.limoges.valadon.gestionfioul.web.proposition;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

/**
 * Vérifie l'ENCHAÎNEMENT des paliers d'un barème, impossible à exprimer avec une annotation sur un champ.
 *
 * <ul>
 *   <li>le barème part de 0 L ;</li>
 *   <li>chaque palier commence où finit le précédent ;</li>
 *   <li>seul le dernier palier est sans limite ;</li>
 *   <li>dans un palier, le volume maximum dépasse le volume minimum.</li>
 * </ul>
 *
 * <p>{@link Validator} de Spring : il écrit ses erreurs dans le même objet {@link Errors} ({@code BindingResult})
 * que Bean Validation, chacune rattachée au champ précis ({@code paliers[1].volumeMin}...) pour s'afficher
 * sur la bonne ligne.</p>
 *
 * <p>Ce sont les mêmes règles que dans l'API : ici pour guider l'utilisateur ligne par ligne,
 * là-bas pour garantir les données. L'API reste juge en dernier ressort.</p>
 */
@Component
public class BaremeValidator implements Validator {

    /**
     * Ce validateur ne sait vérifier que les formulaires de proposition.
     *
     * @param classe classe de l'objet à valider
     * @return {@code true} pour {@link PropositionForm}
     */
    @Override
    public boolean supports(Class<?> classe) {
        return PropositionForm.class.isAssignableFrom(classe);
    }

    /**
     * Ajoute une erreur sur chaque champ de palier qui ne respecte pas l'enchaînement.
     *
     * @param cible   le {@link PropositionForm} à vérifier
     * @param erreurs erreurs de validation, complétées par ce validateur
     */
    @Override
    public void validate(Object cible, Errors erreurs) {
        List<PalierForm> paliers = ((PropositionForm) cible).getPaliers();
        int dernier = paliers.size() - 1;

        for (int i = 0; i <= dernier; i++) {
            PalierForm palier = paliers.get(i);
            String champ = "paliers[" + i + "]";

            if (i == 0 && palier.getVolumeMin() != null && palier.getVolumeMin() != 0) {
                erreurs.rejectValue(champ + ".volumeMin", "bareme.debut", "Le premier palier doit commencer à 0 L");
            }
            if (i > 0) {
                Integer finPrecedent = paliers.get(i - 1).getVolumeMax();
                if (finPrecedent != null && palier.getVolumeMin() != null && !finPrecedent.equals(palier.getVolumeMin())) {
                    erreurs.rejectValue(champ + ".volumeMin", "bareme.continuite",
                            "Doit valoir " + finPrecedent + " L (fin du palier précédent)");
                }
            }

            if (i < dernier && palier.getVolumeMax() == null) {
                erreurs.rejectValue(champ + ".volumeMax", "bareme.limite", "Seul le dernier palier peut être sans limite");
            }
            if (i == dernier && palier.getVolumeMax() != null) {
                erreurs.rejectValue(champ + ".volumeMax", "bareme.fin", "Le dernier palier doit être sans limite (laisser vide)");
            }
            if (palier.getVolumeMin() != null && palier.getVolumeMax() != null
                    && palier.getVolumeMax() <= palier.getVolumeMin()) {
                erreurs.rejectValue(champ + ".volumeMax", "bareme.ordre", "Doit être supérieur au volume minimum");
            }
        }
    }
}
