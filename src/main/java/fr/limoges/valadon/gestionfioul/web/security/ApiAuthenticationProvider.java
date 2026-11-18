package fr.limoges.valadon.gestionfioul.web.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;
import fr.limoges.valadon.gestionfioul.web.user.Utilisateur;
import fr.limoges.valadon.gestionfioul.web.user.UtilisateurClient;

/**
 * Vérifie l'email et le mot de passe saisis dans le formulaire de connexion... en les soumettant à l'API.
 *
 * <p>Le site n'a pas de base d'utilisateurs : c'est l'API qui sait si les identifiants sont bons.
 * Spring Security détecte automatiquement ce bean (seul {@link AuthenticationProvider} de l'application)
 * et l'utilise à la place de son mécanisme par défaut ({@code UserDetailsService} + {@code PasswordEncoder}).</p>
 */
@Component
public class ApiAuthenticationProvider implements AuthenticationProvider {

    private final UtilisateurClient utilisateurClient;

    /**
     * Crée le fournisseur d'authentification.
     *
     * @param utilisateurClient client de l'API utilisé pour vérifier les identifiants
     */
    public ApiAuthenticationProvider(UtilisateurClient utilisateurClient) {
        this.utilisateurClient = utilisateurClient;
    }

    /**
     * Interroge {@code GET /api/users/moi} avec les identifiants saisis.
     *
     * @param authentication email et mot de passe saisis (jeton non authentifié)
     * @return un jeton authentifié dont le principal est un {@link UtilisateurConnecte}
     * @throws AuthenticationException {@link BadCredentialsException} si l'API répond 401,
     *         {@link AuthenticationServiceException} si l'API est injoignable ou répond une autre erreur
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String email = authentication.getName();
        String motDePasse = String.valueOf(authentication.getCredentials());

        try {
            Utilisateur utilisateur = utilisateurClient.moi(email, motDePasse);
            UtilisateurConnecte principal = new UtilisateurConnecte(
                    utilisateur.id(), utilisateur.email(), motDePasse,
                    utilisateur.nom(), utilisateur.prenom(), utilisateur.role());
            // jeton "authentifié" : il sera rangé dans le SecurityContext, puis dans la session
            return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.autorites());

        } catch (ApiException e) {
            if (e.getStatut().value() == HttpStatus.UNAUTHORIZED.value()) {
                throw new BadCredentialsException("Email ou mot de passe incorrect.");
            }
            throw new AuthenticationServiceException("Connexion impossible : " + e.getMessage(), e);
        } catch (ResourceAccessException e) {
            throw new AuthenticationServiceException(
                    "Connexion impossible : le service est momentanément indisponible.", e);
        }
    }

    /**
     * Ce fournisseur traite les connexions par identifiant + mot de passe (celles du formulaire).
     *
     * @param authentication type de jeton à vérifier
     * @return {@code true} pour un {@link UsernamePasswordAuthenticationToken}
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
