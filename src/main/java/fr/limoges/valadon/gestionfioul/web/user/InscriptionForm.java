package fr.limoges.valadon.gestionfioul.web.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Données saisies dans le formulaire d'inscription (« form backing bean »).
 *
 * <p>Une CLASSE avec getters / setters, et non un record :</p>
 * <ul>
 *   <li>Spring MVC remplit l'objet champ par champ (setters) à partir des paramètres du formulaire ;</li>
 *   <li>Thymeleaf ({@code th:field}) relit les valeurs par les getters pour réafficher le formulaire en cas d'erreur.</li>
 * </ul>
 *
 * <p>Les annotations Bean Validation décrivent les règles simples, vérifiées par {@code @Valid} dans le contrôleur.
 * Les règles qui portent sur plusieurs champs sont vérifiées dans le contrôleur.</p>
 */
public class InscriptionForm {

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'email n'est pas valide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String motDePasse;

    private String confirmation;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String prenom;

    private String telephone;

    @NotNull(message = "Choisissez un type de compte")
    private Role role = Role.CLIENT;

    private String adresse;

    private String raisonSociale;

    /**
     * Construit le corps de la requête pour l'API. La confirmation du mot de passe n'est utile qu'au site.
     *
     * @return la requête d'inscription ; la raison sociale n'est transmise que pour un fournisseur
     */
    public InscriptionRequest versRequete() {
        return new InscriptionRequest(email, motDePasse, nom, prenom, telephone, role, adresse,
                role == Role.FOURNISSEUR ? raisonSociale : null);
    }

    /**
     * Vide les deux champs de mot de passe avant de réafficher le formulaire : on ne renvoie jamais
     * un mot de passe dans une page HTML, même celui que l'utilisateur vient de taper.
     */
    public void effacerMotsDePasse() {
        motDePasse = null;
        confirmation = null;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getConfirmation() {
        return confirmation;
    }

    public void setConfirmation(String confirmation) {
        this.confirmation = confirmation;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getRaisonSociale() {
        return raisonSociale;
    }

    public void setRaisonSociale(String raisonSociale) {
        this.raisonSociale = raisonSociale;
    }
}
