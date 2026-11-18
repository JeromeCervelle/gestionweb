package fr.limoges.valadon.gestionfioul.web.user;

/**
 * Profil d'un utilisateur tel que l'API le renvoie ({@code UserResponse} côté API : jamais de mot de passe).
 *
 * @param id            identifiant de l'utilisateur
 * @param email         email de connexion
 * @param nom           nom de famille
 * @param prenom        prénom, éventuellement vide
 * @param telephone     téléphone, éventuellement vide
 * @param role          rôle de l'utilisateur
 * @param adresse       adresse postale
 * @param raisonSociale nom de l'entreprise (fournisseur)
 */
public record Utilisateur(
        String id,
        String email,
        String nom,
        String prenom,
        String telephone,
        Role role,
        String adresse,
        String raisonSociale) {

    /**
     * Nom à afficher dans les tableaux de l'administrateur.
     *
     * @return la raison sociale pour un fournisseur, « Prénom Nom » sinon
     */
    public String designation() {
        if (role == Role.FOURNISSEUR && raisonSociale != null && !raisonSociale.isBlank()) {
            return raisonSociale;
        }
        return prenom == null || prenom.isBlank() ? nom : prenom + " " + nom;
    }
}
