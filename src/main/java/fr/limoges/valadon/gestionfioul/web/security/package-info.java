/**
 * Sécurité du site : connexion par formulaire vérifiée par l'API, session, CSRF et droits d'accès aux pages.
 *
 * <p>Le site n'a pas de base d'utilisateurs : {@link fr.limoges.valadon.gestionfioul.web.security.ApiAuthenticationProvider}
 * soumet l'email et le mot de passe à l'API, puis {@link fr.limoges.valadon.gestionfioul.web.security.UtilisateurConnecte}
 * les garde en session pour les appels suivants. Les droits par rôle sont déclarés dans
 * {@link fr.limoges.valadon.gestionfioul.web.security.SecurityConfig}.</p>
 */
package fr.limoges.valadon.gestionfioul.web.security;
