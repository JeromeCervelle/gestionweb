package fr.limoges.valadon.gestionfioul.web.user;

/**
 * Corps JSON envoyé à {@code POST /api/users} (même structure que l'{@code InscriptionRequest} de l'API).
 *
 * @param email         email, qui servira d'identifiant de connexion
 * @param motDePasse    mot de passe en clair : c'est l'API qui le hache avant de l'enregistrer
 * @param nom           nom de famille
 * @param prenom        prénom, facultatif
 * @param telephone     téléphone, facultatif
 * @param role          {@code CLIENT} ou {@code FOURNISSEUR}
 * @param adresse       adresse postale, adresse de livraison par défaut d'un client
 * @param raisonSociale nom de l'entreprise, pour un fournisseur uniquement
 */
public record InscriptionRequest(
        String email,
        String motDePasse,
        String nom,
        String prenom,
        String telephone,
        Role role,
        String adresse,
        String raisonSociale) {
}
