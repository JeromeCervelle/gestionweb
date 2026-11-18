package fr.limoges.valadon.gestionfioul.web.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import fr.limoges.valadon.gestionfioul.web.user.Role;

/**
 * Connecte une requête MockMvc avec NOTRE principal (UtilisateurConnecte), comme après un vrai formulaire de
 * connexion. Le gabarit lit #authentication.principal.nomAffiche() : un utilisateur de test générique
 * (user("alice")) ne conviendrait pas.
 */
public final class UtilisateursDeTest {

    private UtilisateursDeTest() {
    }

    public static UtilisateurConnecte unClient() {
        return new UtilisateurConnecte("client-1", "alice@test.fr", "motdepasse1", "Durand", "Alice", Role.CLIENT);
    }

    public static UtilisateurConnecte unFournisseur() {
        return new UtilisateurConnecte("fournisseur-1", "fioul@test.fr", "motdepasse1", "Leroy", "Paul", Role.FOURNISSEUR);
    }

    public static UtilisateurConnecte unAdmin() {
        return new UtilisateurConnecte("admin-1", "admin@gestionfioul.local", "12345678", "Administrateur", null, Role.ADMIN);
    }

    public static RequestPostProcessor client() {
        return connecte(unClient());
    }

    public static RequestPostProcessor fournisseur() {
        return connecte(unFournisseur());
    }

    public static RequestPostProcessor admin() {
        return connecte(unAdmin());
    }

    public static RequestPostProcessor connecte(UtilisateurConnecte utilisateur) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                UsernamePasswordAuthenticationToken.authenticated(utilisateur, null, utilisateur.autorites()));
    }
}
