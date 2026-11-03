# SmartCampus — backend REST pédagogique

Ce module Maven fournit les CRUD des équipements, salles, utilisateurs et réservations, ainsi que la connexion des utilisateurs. Il conserve les entités et tables existantes. Les contrôleurs reçoivent et retournent des DTO ; les services réalisent les mappings manuels, les règles métier et les transactions.

La connexion est disponible sur `POST /api/auth/login`. Elle vérifie l'e-mail et le mot de passe, refuse les comptes désactivés et retourne un jeton opaque à envoyer dans l'en-tête `Authorization: Bearer <jeton>`. Les mots de passe sont hachés avec BCrypt et ne sont jamais retournés, même sous forme de hash. Pour l'instant, les autres endpoints CRUD restent accessibles sans jeton : cette protection pourra être ajoutée dans une étape ultérieure.

## Prérequis et commandes

- Un **JDK 25**, avec `JAVA_HOME` pointant vers ce JDK et son dossier `bin` dans `PATH`.
- Une connexion Internet au premier lancement pour télécharger Maven et les dépendances.
- Le Maven Wrapper fourni ; aucune installation globale de Maven n'est nécessaire.

Le projet conserve Java 25, Spring Boot 4.1.1 et les coordonnées `com.example:demo:0.0.1-SNAPSHOT`. Toutes les commandes suivantes s'exécutent depuis `smartCampus/backend/demo`.

Le Wrapper et sa version sont conservés. Une vérification de valeur nulle dans `mvnw.cmd` a été corrigée pour permettre son exécution avec Windows PowerShell lorsque le dossier `.m2` n'est pas un lien.

Sous Windows PowerShell :

```powershell
java -version
.\mvnw.cmd -version
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
```

Sous Linux :

```bash
java -version
chmod +x mvnw
./mvnw -version
./mvnw clean verify
./mvnw spring-boot:run
```

Après `clean verify`, le JAR peut aussi être lancé sous Windows ou Linux :

```text
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

Le serveur écoute sur **http://localhost:8080**. Arrêter avec `Ctrl+C`. Le contrôle de santé est disponible sur [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health), avec une réponse contenant `"status":"UP"` lorsque l'application et la base sont disponibles. Seul l'endpoint Actuator `health` est exposé.

## Structure

```text
src/main/java/com/example/demo/
├── DemoApplication.java
├── SecurityConfig.java
├── entity/       # Equipment, Room, User, Reservation, AuthSession, Role, ReservationStatus
├── repository/   # Cinq interfaces JpaRepository avec identifiants Long
├── service/      # Cinq classes concrètes : transactions, règles, mappings DTO
├── controller/   # Quatre contrôleurs CRUD et AuthController
├── dto/          # Entrées validées et réponses plates, LoginRequest/LoginResponse
└── exception/    # Exceptions métier et @RestControllerAdvice
src/main/resources/application.properties
src/test/java/com/example/demo/DemoApplicationTests.java
src/test/java/com/example/demo/AuthControllerTests.java
src/test/resources/application-test.properties
```

Il n'y a pas de frontend, de pagination, de moteur de disponibilité ni de détection des chevauchements.

## Base de données et configuration

Par défaut, H2 utilise le fichier `./data/smartcampus`, relatif au répertoire de lancement. En lançant depuis ce module, les données sont donc enregistrées dans `backend/demo/data/smartcampus.mv.db` et conservées entre deux démarrages. Les fichiers H2 locaux sont exclus de Git.

Hibernate utilise `spring.jpa.hibernate.ddl-auto=update` pour créer ou mettre à jour les tables de ce projet pédagogique. `spring.jpa.open-in-view=false` oblige les services à lire les relations dans leurs transactions avant de construire les DTO. Les tables restent `equipment`, `rooms`, `users`, `reservations`, `room_equipment` et la nouvelle table `auth_sessions`.

Les tests activent le profil `test`, utilisent une **base H2 distincte en mémoire** (`jdbc:h2:mem:smartcampus-test`) et recréent leur schéma avec `create-drop`. L'annotation `@SpringBootTest` impose cette datasource, même si des variables `SPRING_DATASOURCE_*` sont définies dans le terminal. Ils ne nécessitent ni PostgreSQL, ni Docker, ni Jenkins et n'utilisent pas les données locales.

Le pilote PostgreSQL est déjà disponible pour une future étape. Spring Boot accepte les variables standard, sans driver ni dialecte H2 imposé :

| Variable | Exemple PostgreSQL |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/smartcampus` |
| `SPRING_DATASOURCE_USERNAME` | `smartcampus` |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe de la base |

Exemple PowerShell, avec une base PostgreSQL préalablement créée :

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/smartcampus'
$env:SPRING_DATASOURCE_USERNAME = 'smartcampus'
$env:SPRING_DATASOURCE_PASSWORD = Read-Host 'Mot de passe PostgreSQL'
.\mvnw.cmd spring-boot:run
```

Exemple Linux :

```bash
export SPRING_DATASOURCE_URL='jdbc:postgresql://localhost:5432/smartcampus'
export SPRING_DATASOURCE_USERNAME='smartcampus'
read -r -s -p 'Mot de passe PostgreSQL : ' SPRING_DATASOURCE_PASSWORD
export SPRING_DATASOURCE_PASSWORD
./mvnw spring-boot:run
```

Pour revenir à H2 dans la session PowerShell :

```powershell
Remove-Item Env:SPRING_DATASOURCE_URL, Env:SPRING_DATASOURCE_USERNAME, Env:SPRING_DATASOURCE_PASSWORD -ErrorAction SilentlyContinue
```

Ne pas committer de mots de passe ni de fichiers de configuration privés.

## Endpoints

Les collections retournent des tableaux JSON sans pagination.

| Ressource | Liste — GET | Détail — GET | Création — POST | Modification — PUT | Suppression — DELETE |
| --- | --- | --- | --- | --- | --- |
| Équipements | `/api/equipment` | `/api/equipment/{id}` | `/api/equipment` | `/api/equipment/{id}` | `/api/equipment/{id}` |
| Salles | `/api/rooms` | `/api/rooms/{id}` | `/api/rooms` | `/api/rooms/{id}` | `/api/rooms/{id}` |
| Utilisateurs | `/api/users` | `/api/users/{id}` | `/api/users` | `/api/users/{id}` | `/api/users/{id}` |
| Réservations | `/api/reservations` | `/api/reservations/{id}` | `/api/reservations` | `/api/reservations/{id}` | `/api/reservations/{id}` |

| Authentification | Méthode | Description |
| --- | --- | --- |
| `/api/auth/login` | `POST` | Connexion avec `email` et `password`, retourne un jeton et l'utilisateur |
| `/api/auth/me` | `GET` | Retourne l'utilisateur du jeton `Authorization: Bearer <jeton>` |
| `/api/auth/logout` | `POST` | Invalide le jeton courant, réponse `204` |

| Statut HTTP | Signification |
| --- | --- |
| `200` | Lecture, connexion ou modification réussie |
| `201` | Création réussie ; l'en-tête `Location` indique la ressource |
| `204` | Suppression ou déconnexion réussie, sans corps |
| `400` | Entrée invalide, champ manquant, JSON illisible ou dates incohérentes |
| `401` | Identifiants incorrects, jeton absent, invalide ou expiré |
| `403` | Compte désactivé |
| `404` | Ressource ou identifiant de relation inexistant |
| `409` | Nom d'équipement/email déjà utilisé, ou suppression empêchée par une relation |

Une suppression de salle ou d'utilisateur est refusée s'il existe encore une réservation qui le référence, y compris une réservation annulée. Une suppression d'équipement est refusée tant qu'une salle lui est associée. Supprimer d'abord les réservations puis détacher les équipements ou supprimer les salles concernées. Aucune cascade ne supprime automatiquement les ressources associées.

## Contrat JSON

Les identifiants et `createdAt` sont gérés par le serveur. Les entrées ne contiennent pas de graphe d'entités imbriquées : utiliser `equipmentIds` pour une salle et `roomId`/`userId` pour une réservation. Les réponses utilisent ces mêmes identifiants de relation, sans récursion JPA.

Les champs JSON inconnus, notamment `id`, `createdAt` et les relations imbriquées, sont refusés avec `400`. Les erreurs utilisent les champs `timestamp`, `status`, `error`, `message`, `path` et `details` ; les détails des validations associent chaque champ invalide à un message. Aucun hash, contenu de requête sensible ni trace technique n'est inclus dans ces erreurs.

Les champs obligatoires sont :

- Équipement : `name`, non vide et unique.
- Salle : `name`, `building`, `capacity` strictement positive et `equipmentIds` (tableau, éventuellement vide).
- Utilisateur : `email` valide et unique, `fullName`, `role` et, à la création, `password` d'au moins huit caractères.
- Réservation : `title`, `roomId`, `userId`, `startsAt` et `endsAt` ; le début doit être strictement antérieur à la fin.

Les valeurs de `role` sont `STUDENT`, `PROFESSOR`, `STAFF`, `ADMIN`. Celles de `status` sont `CONFIRMED`, `CANCELLED`.

À la création, l'omission de `active`, `enabled` et `status` conserve respectivement `true`, `true` et `CONFIRMED`. À la modification, leur omission conserve la valeur existante. Pour un `PUT`, fournir tous les champs obligatoires. Un `password` absent ou `null` conserve le hash existant ; un nouveau mot de passe valide le remplace. La limite technique BCrypt est de 72 octets UTF-8.

Une salle avec `"equipmentIds": []` ne conserve aucun équipement. `description` est facultatif et peut être `null`.

Les dates utilisent `Instant` : fournir des chaînes **ISO 8601 avec fuseau horaire**, par exemple `2026-11-02T09:00:00Z` ou `2026-11-02T10:00:00+01:00`. Le serveur peut les restituer en UTC avec `Z`. Une date locale telle que `2026-11-02T09:00:00` n'est pas acceptée.

Exemple de création d'une réservation :

```json
{
  "roomId": 1,
  "userId": 1,
  "title": "TP Java",
  "startsAt": "2026-11-02T09:00:00Z",
  "endsAt": "2026-11-02T10:00:00Z"
}
```

La réponse ajoute `id`, `status` et `createdAt` ; elle conserve `roomId` et `userId`. Une réponse utilisateur contient `id`, `email`, `fullName`, `role`, `enabled` et `createdAt`, sans champ `password`.

## Authentification

La connexion se fait avec `POST /api/auth/login`. Le corps ne contient que `email` et `password` ; tout champ supplémentaire est refusé avec `400`.

```json
{
  "email": "amina.demo@example.test",
  "password": "secretDemo123"
}
```

Avec un compte actif et des identifiants corrects, la réponse `200` contient un jeton opaque, sa date d'expiration et l'utilisateur, sans mot de passe :

```json
{
  "token": "h2P0...",
  "tokenType": "Bearer",
  "expiresAt": "2026-11-03T20:00:00Z",
  "user": {
    "id": 1,
    "email": "amina.demo@example.test",
    "fullName": "Amina Benali",
    "role": "STUDENT",
    "enabled": true,
    "createdAt": "2026-11-03T09:00:00Z"
  }
}
```

Le jeton s'utilise ensuite dans l'en-tête `Authorization: Bearer <jeton>` :

```powershell
$base = 'http://localhost:8080'
$loginBody = @{ email = 'amina.demo@example.test'; password = 'secretDemo123' } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType 'application/json; charset=utf-8' -Body $loginBody
$login.user

$headers = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod "$base/api/auth/me" -Headers $headers

Invoke-RestMethod -Method Post -Uri "$base/api/auth/logout" -Headers $headers
```

Cas d'erreur :

- Un e-mail ou un mot de passe absent ou vide renvoie `400`. Le champ `details` indique le ou les champs à compléter (`email`, `password`).
- Un e-mail inconnu ou un mot de passe incorrect renvoie `401` avec le message `Adresse e-mail ou mot de passe incorrect.` Le même message est volontairement utilisé pour ne pas révéler l'existence d'un compte.
- Un compte existant mais désactivé renvoie `403` avec `Ce compte est désactivé. Contactez l'administration.`
- Un jeton absent, mal formé, inconnu ou expiré renvoie `401`.
- `POST /api/auth/logout` supprime la session ; le jeton ne peut plus être réutilisé.

Un jeton contient 32 octets aléatoires encodés en Base64URL. Seul son condensat SHA-256 est stocké dans la table `auth_sessions`. La durée de validité est de huit heures par défaut et se règle avec `smartcampus.auth.session-ttl` au format ISO 8601 (`PT8H`, `PT30M`). Les sessions expirées sont purgées lors d'une connexion. Les e-mails sont comparés sans tenir compte de la casse et des espaces autour de la valeur. La suppression d'un utilisateur supprime aussi ses sessions.

Pour restreindre la connexion à des domaines institutionnels, renseigner une liste séparée par des virgules ; vide, la restriction est désactivée :

```properties
smartcampus.auth.institutional-domains=univ-exemple.dz,etu.univ-exemple.dz
```

Les endpoints CRUD restent ouverts sans jeton dans cette version. Les endpoints `/api/auth/me` et `/api/auth/logout` valident le jeton fourni, mais la protection globale des autres ressources reste à réaliser.

## Parcours de démonstration PowerShell

Démarrer l'application dans un premier terminal, puis exécuter les commandes suivantes dans un second. Les identifiants sont repris des réponses : il n'est pas nécessaire de supposer leur valeur. Utiliser des noms/emails encore disponibles ou supprimer les objets après la démonstration.

```powershell
$base = 'http://localhost:8080'
$jsonType = 'application/json; charset=utf-8'

# 1. Équipement
$equipmentBody = @{ name = 'Projecteur demo' } | ConvertTo-Json
$equipment = Invoke-RestMethod -Method Post -Uri "$base/api/equipment" -ContentType $jsonType -Body $equipmentBody
$equipment

# 2. Utilisateur
$userBody = @{
    email = 'amina.demo@example.test'
    password = 'secretDemo123'
    fullName = 'Amina Benali'
    role = 'STUDENT'
} | ConvertTo-Json
$user = Invoke-RestMethod -Method Post -Uri "$base/api/users" -ContentType $jsonType -Body $userBody
$user  # Aucun mot de passe dans cette réponse

# 2b. Connexion et accès au profil avec le jeton
$loginBody = @{ email = $user.email; password = 'secretDemo123' } | ConvertTo-Json
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" -ContentType $jsonType -Body $loginBody
$login.user
$authHeaders = @{ Authorization = "Bearer $($login.token)" }
Invoke-RestMethod "$base/api/auth/me" -Headers $authHeaders

# 3. Salle
$roomBody = @{
    name = 'A101 demo'
    building = 'Sciences'
    capacity = 30
    description = 'Salle de travaux pratiques'
    equipmentIds = @($equipment.id)
} | ConvertTo-Json
$room = Invoke-RestMethod -Method Post -Uri "$base/api/rooms" -ContentType $jsonType -Body $roomBody
$room

# 4. Réservation : dates ISO 8601 avec Z (UTC)
$reservationBody = @{
    roomId = $room.id
    userId = $user.id
    title = 'TP Java'
    startsAt = '2026-11-02T09:00:00Z'
    endsAt = '2026-11-02T10:00:00Z'
} | ConvertTo-Json
$reservation = Invoke-RestMethod -Method Post -Uri "$base/api/reservations" -ContentType $jsonType -Body $reservationBody
$reservation

# Lire la liste et le détail
Invoke-RestMethod "$base/api/equipment"
Invoke-RestMethod "$base/api/users/$($user.id)"
Invoke-RestMethod "$base/api/rooms"
Invoke-RestMethod "$base/api/reservations/$($reservation.id)"
Invoke-RestMethod "$base/actuator/health"

# Modifier la salle et retirer tous ses équipements
$roomUpdate = @{
    name = $room.name
    building = $room.building
    capacity = 40
    description = $room.description
    active = $true
    equipmentIds = @()
} | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "$base/api/rooms/$($room.id)" -ContentType $jsonType -Body $roomUpdate

# Modifier l'utilisateur sans changer son mot de passe
$userUpdate = @{
    email = $user.email
    fullName = 'Amina Benali - professeur'
    role = 'PROFESSOR'
} | ConvertTo-Json
Invoke-RestMethod -Method Put -Uri "$base/api/users/$($user.id)" -ContentType $jsonType -Body $userUpdate

# Nettoyer les objets, dans un ordre qui respecte les références
Invoke-RestMethod -Method Post -Uri "$base/api/auth/logout" -Headers $authHeaders
Invoke-RestMethod -Method Delete -Uri "$base/api/reservations/$($reservation.id)"
Invoke-RestMethod -Method Delete -Uri "$base/api/rooms/$($room.id)"
Invoke-RestMethod -Method Delete -Uri "$base/api/users/$($user.id)"
Invoke-RestMethod -Method Delete -Uri "$base/api/equipment/$($equipment.id)"
```

Pour observer un `409`, tenter de supprimer la salle avant sa réservation. Pour un `400`, envoyer une réservation dont la date de fin est égale ou antérieure au début. Pour un `404`, lire un identifiant inexistant. PowerShell signale ces réponses HTTP comme erreurs ; leur corps JSON décrit l'erreur.

## Vérification automatisée

`clean verify` compile, exécute les tests et produit le JAR exécutable. La suite teste le parcours CRUD, les réponses plates, les valeurs par défaut, les références inexistantes, les validations et dates, les conflits d'unicité et de suppression, le hash BCrypt et sa conservation/remplacement, l'absence du mot de passe dans les réponses et `/actuator/health`. Les tests d'authentification couvrent la connexion réussie, la casse et les espaces de l'e-mail, le mot de passe incorrect, l'e-mail inconnu, les champs manquants, le format d'e-mail, le compte désactivé, l'accès avec jeton, le rejet d'un jeton invalide et l'invalidation après déconnexion.

Les résultats de la dernière exécution sont consultables dans `target/surefire-reports/`. Pour une vérification HTTP réelle après démarrage, utiliser le parcours PowerShell ci-dessus.

Vérifications effectuées sous Windows avec Temurin Java 25 et Spring Boot 4.1.1 :

- 3 novembre 2026 — `mvnw.cmd clean verify` : succès, **15 tests** (9 authentification, 6 CRUD), aucun échec ni erreur ; le JAR exécutable est produit. Les tests d'authentification utilisent une base H2 en mémoire distincte et vérifient aussi que le jeton en clair n'est pas stocké et que la suppression d'un utilisateur supprime ses sessions.
- 26 octobre 2026 — `mvnw.cmd clean verify` : succès, 6 tests, aucun échec ni erreur. L'exécution finale a aussi confirmé l'isolation H2 avec des variables PostgreSQL factices dans l'environnement.
- Démarrage du JAR sur le port 8080 avec la configuration H2 fichier par défaut.
- 33 requêtes HTTP réelles vérifiées lors de la vérification du 26 octobre : quatre CRUD, statuts `200`/`201`/`204`/`400`/`404`/`409`, réponses utilisateur sans mot de passe et `/actuator/health` avec statut `UP`. Le parcours d'authentification peut être vérifié avec les commandes PowerShell de la section précédente.
- Données conservées après arrêt et redémarrage, puis objets de démonstration supprimés dans l'ordre des références.

Un JDK 25 portable a été utilisé dans `.tools/` pour cette vérification ; ce dossier est exclu de Git. Le JDK utilisé globalement par la machine n'a pas été modifié. PostgreSQL, Linux, Docker et Jenkins n'ont pas été utilisés pour ces vérifications.
