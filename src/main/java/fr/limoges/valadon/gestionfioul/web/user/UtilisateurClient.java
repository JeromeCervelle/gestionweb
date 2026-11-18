package fr.limoges.valadon.gestionfioul.web.user;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import fr.limoges.valadon.gestionfioul.web.api.ApiException;

/**
 * Appels à l'API concernant les utilisateurs.
 *
 * <p>Toute erreur de l'API est levée sous forme d'{@link ApiException}.</p>
 */
@Component
public class UtilisateurClient {

    private final RestClient restClient;

    public UtilisateurClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * {@code POST /api/users} : création d'un compte (route publique de l'API, aucun identifiant n'est envoyé).
     *
     * @param requete données saisies dans le formulaire d'inscription
     * @return le profil créé
     * @throws ApiException 400 si l'API refuse une donnée (email déjà utilisé...), avec son message
     */
    public Utilisateur inscrire(InscriptionRequest requete) {
        return restClient.post()
                .uri("/api/users")
                // l'objet Java est converti en JSON par Jackson (Content-Type: application/json)
                .body(requete)
                .retrieve()
                .body(Utilisateur.class);
    }

    /**
     * {@code GET /api/users/{id}} : l'administrateur peut lire n'importe quel profil.
     *
     * @param id identifiant de l'utilisateur
     * @return son profil (nom d'un client, raison sociale d'un fournisseur)
     */
    public Utilisateur trouver(String id) {
        return restClient.get()
                .uri("/api/users/{id}", id)
                .retrieve()
                .body(Utilisateur.class);
    }

    /**
     * {@code GET /api/users/moi} avec les identifiants fournis.
     *
     * <p>Sert à la connexion : si l'API répond 200, les identifiants sont bons et on récupère l'id et le rôle.
     * Les identifiants sont passés explicitement : au moment de la connexion, personne n'est encore connecté.</p>
     *
     * @param email      email saisi
     * @param motDePasse mot de passe saisi
     * @return le profil de l'utilisateur
     * @throws ApiException 401 si les identifiants sont faux
     */
    public Utilisateur moi(String email, String motDePasse) {
        return restClient.get()
                .uri("/api/users/moi")
                // UTF-8 : sans ce paramètre, un mot de passe accentué serait mal encodé et refusé par l'API
                .headers(entetes -> entetes.setBasicAuth(email, motDePasse, StandardCharsets.UTF_8))
                .retrieve()
                .body(Utilisateur.class);
    }
}
