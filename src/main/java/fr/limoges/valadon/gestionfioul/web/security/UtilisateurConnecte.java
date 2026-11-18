package fr.limoges.valadon.gestionfioul.web.security;

import java.io.Serializable;
import java.util.List;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import fr.limoges.valadon.gestionfioul.web.user.Role;

/**
 * L'utilisateur connecté au site, conservé par Spring Security dans la session HTTP.
 *
 * <p>Il garde le mot de passe : l'API est sans session (HTTP Basic), il faut donc renvoyer
 * email + mot de passe à CHAQUE appel. Le mot de passe reste côté serveur (en mémoire, dans la session),
 * il n'est jamais envoyé au navigateur, qui ne reçoit qu'un cookie de session {@code JSESSIONID}.</p>
 *
 * <ul>
 *   <li>{@link AuthenticatedPrincipal} : fournit {@link #getName()}, utilisé par Spring Security
 *       (et par {@code #authentication.name} dans les vues).</li>
 *   <li>{@link Serializable} : un objet stocké en session doit pouvoir être sérialisé.</li>
 * </ul>
 *
 * @param id         identifiant de l'utilisateur dans l'API
 * @param email      email, qui sert d'identifiant de connexion
 * @param motDePasse mot de passe saisi à la connexion, renvoyé à l'API à chaque appel
 * @param nom        nom de famille
 * @param prenom     prénom, éventuellement vide
 * @param role       rôle, qui détermine les pages accessibles
 */
public record UtilisateurConnecte(
        String id,
        String email,
        String motDePasse,
        String nom,
        String prenom,
        Role role) implements AuthenticatedPrincipal, Serializable {

    /**
     * Nom de l'utilisateur pour Spring Security : son email.
     *
     * @return l'email
     */
    @Override
    public String getName() {
        return email;
    }

    /**
     * Nom affiché dans l'en-tête des pages.
     *
     * @return « Alice Durand », ou seulement le nom si le prénom est vide
     */
    public String nomAffiche() {
        return prenom == null || prenom.isBlank() ? nom : prenom + " " + nom;
    }

    /**
     * Autorités Spring Security de l'utilisateur.
     *
     * <p>La convention du préfixe {@code ROLE_} permet d'écrire {@code hasRole('CLIENT')}.</p>
     *
     * @return une seule autorité, {@code ROLE_CLIENT}, {@code ROLE_FOURNISSEUR} ou {@code ROLE_ADMIN}
     */
    public List<GrantedAuthority> autorites() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    /**
     * Description SANS le mot de passe : le {@code toString()} généré d'un record l'afficherait en clair
     * (dans les logs par exemple).
     *
     * @return l'identifiant, l'email et le rôle
     */
    @Override
    public String toString() {
        return "UtilisateurConnecte[id=" + id + ", email=" + email + ", role=" + role + "]";
    }
}
