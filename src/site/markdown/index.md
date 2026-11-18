# gestionweb : documentation technique

**gestionweb** est l'interface web (Spring Boot, Thymeleaf, Tailwind CSS) de l'API REST **gestionfioul**,
qui gère les commandes groupées de fioul : des clients s'engagent sur un volume dans une campagne,
des fournisseurs proposent un barème de prix par paliers, et le moins-disant est retenu.

Le site **n'accède jamais à la base de données** : chaque page appelle l'API (port 9999) avec les
identifiants de l'utilisateur connecté.

## Contenu de cette documentation

| Rapport | Ce qu'il montre | Produit par |
|---|---|---|
| [Javadoc](apidocs/index.html) | Le rôle de chaque package, classe et méthode publique | `maven-javadoc-plugin`, à partir des commentaires `/** ... */` |
| [Résultats des tests](surefire.html) | Chaque test exécuté, réussi ou en échec, et sa durée | `maven-surefire-report-plugin`, à partir de `target/surefire-reports/` |
| [Couverture de code](jacoco/index.html) | Les lignes et les branches exécutées par les tests | `jacoco-maven-plugin`, à partir de `target/jacoco.exec` |
| [Dépendances](dependencies.html) | Les bibliothèques utilisées et leurs versions | `maven-project-info-reports-plugin` |

## Architecture en un coup d'œil

| Package | Rôle |
|---|---|
| `web.api` | Configuration du `RestClient` partagé : adresse de l'API, identifiants, traduction des erreurs |
| `web.security` | Connexion par formulaire, vérifiée par l'API ; droits d'accès aux pages |
| `web.common` | Pages d'erreur communes à tous les contrôleurs |
| `web.accueil` | Page d'accueil et état de l'API |
| `web.user` | Inscription et profils utilisateurs |
| `web.campagne` | Campagnes : liste des campagnes ouvertes, statuts et actions du cycle de vie |
| `web.participation` | Espace client : participer, consulter, annuler |
| `web.proposition` | Espace fournisseur : proposer un barème, consulter ses propositions |
| `web.admin` | Espace administrateur : créer et piloter les campagnes |

Chaque fonctionnalité suit le même découpage :

- un **contrôleur** (`@Controller`) reçoit la requête du navigateur et choisit la vue Thymeleaf ;
- un **client** (`@Component`) appelle l'API avec le `RestClient` ;
- des **records** transportent le JSON échangé avec l'API, des **formulaires** (`...Form`) portent la saisie et sa validation.

## Régénérer cette documentation

```
./mvnw clean verify site
```

puis ouvrir `target/site/index.html`.

- `clean` efface `target/` : sans lui, la Javadoc peut ne pas être régénérée après une simple modification
  de commentaire (le plugin ne détecte pas toujours le changement).
- `verify` exécute les tests et contrôle les seuils de couverture.
- `site` met en forme les résultats. Lancé seul, il donnerait des rapports de tests et de couverture
  vides ou périmés.

La génération échoue si un commentaire Javadoc est incorrect (HTML mal formé, `@param` qui ne correspond
à aucun paramètre, lien `{@link}` vers une classe inconnue...).
