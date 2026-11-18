/**
 * Site web gestionfioul : interface Thymeleaf de l'API REST de commandes groupées de fioul.
 *
 * <p>Le site ne possède aucune base de données : chaque page appelle l'API (port 9999)
 * au nom de l'utilisateur connecté. Les sous-packages suivent les fonctionnalités :</p>
 * <ul>
 *   <li>{@link fr.limoges.valadon.gestionfioul.web.api} : le {@code RestClient} partagé et la gestion des erreurs de l'API ;</li>
 *   <li>{@link fr.limoges.valadon.gestionfioul.web.security} : connexion et droits d'accès aux pages ;</li>
 *   <li>{@link fr.limoges.valadon.gestionfioul.web.common} : pages d'erreur communes ;</li>
 *   <li>{@link fr.limoges.valadon.gestionfioul.web.accueil}, {@link fr.limoges.valadon.gestionfioul.web.user},
 *       {@link fr.limoges.valadon.gestionfioul.web.campagne}, {@link fr.limoges.valadon.gestionfioul.web.participation},
 *       {@link fr.limoges.valadon.gestionfioul.web.proposition}, {@link fr.limoges.valadon.gestionfioul.web.admin} :
 *       une fonctionnalité chacun.</li>
 * </ul>
 */
package fr.limoges.valadon.gestionfioul.web;
