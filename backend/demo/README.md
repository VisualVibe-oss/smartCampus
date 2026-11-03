# SmartCampus — backend REST

API Spring Boot du projet SmartCampus : gestion des équipements, des salles et des réservations. Les contrôleurs échangent des DTO, les services portent les règles métier et les transactions, les repositories assurent la persistance.

Cette version pédagogique n'a pas encore d'authentification : les endpoints sont accessibles directement. Les mots de passe sont hachés avec BCrypt et ne sont jamais retournés, même sous forme de hash.

Le modèle de données est conforme au **Rapport technique SmartCampus v1.0** : identifiants **UUID**, réservations d'une **durée fixe de 7 jours**, statuts `CONFIRMED`/`CANCELLED`/`REVOKED` et révocation administrative.

## Prérequis et commandes

- Un **JDK 21+** (le `pom.xml` cible Java 21 ; la compilation a été validée avec un JDK récent).
- Une connexion Internet au premier lancement pour télécharger Maven et les dépendances.
- Le Maven Wrapper fourni ; aucune installation globale de Maven n'est nécessaire.

Toutes les commandes suivantes s'exécutent depuis `smartCampus/backend/demo`.

Linux :

```bash
chmod +x mvnw
./mvnw -version
./mvnw clean verify
./mvnw spring-boot:run
```

Après `clean verify`, le JAR peut aussi être lancé :

```bash
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

Le serveur écoute sur **http://localhost:8080**. Le contrôle de santé est disponible sur [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health). Seul l'endpoint Actuator `health` est exposé.

## Structure

```text
src/main/java/com/example/demo/
├── DemoApplication.java
├── entity/       # Equipment, Room, User, Reservation, Role, ReservationStatus
├── repository/   # Quatre interfaces JpaRepository (identifiants UUID)
├── service/      # Quatre services : transactions, règles métier, mappings DTO
├── controller/   # Quatre contrôleurs REST
├── dto/          # Entrées validées et réponses plates
└── exception/    # Exceptions métier et @RestControllerAdvice
src/main/resources/application.properties
src/test/java/com/example/demo/DemoApplicationTests.java
docs/schema.sql  # schéma PostgreSQL de référence
```

## Base de données

Cinq tables :

| Table | Rôle |
| --- | --- |
| `users` | Comptes et rôles (`STUDENT`, `STAFF`, `ADMIN`) |
| `rooms` | Salles (`code`, `name`, `building`, `capacity`, `is_active`) |
| `equipments` | Référentiel des équipements |
| `room_equipments` | Association salle ↔ équipement |
| `reservations` | Réservations et historique (annulation, révocation) |

Par défaut, H2 utilise le fichier `./data/smartcampus`, relatif au répertoire de lancement. Hibernate utilise `spring.jpa.hibernate.ddl-auto=update` ; `spring.jpa.open-in-view=false` oblige les services à lire les relations dans leurs transactions.

Les tests activent le profil `test`, utilisent une **base H2 en mémoire** (`jdbc:h2:mem:smartcampus-test`) et recréent leur schéma avec `create-drop`.

Spring Boot accepte les variables standard pour pointer vers PostgreSQL :

| Variable | Exemple |
| --- | --- |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/smartcampus` |
| `SPRING_DATASOURCE_USERNAME` | `smartcampus` |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe de la base |

Le schéma PostgreSQL complet, avec la contrainte d'exclusion `no_overlapping_reservations` (anti double-réservation), se trouve dans `docs/schema.sql`.

## Docker et Docker Compose

Depuis la racine du dépôt, après avoir produit le JAR (`cd backend/demo && ../mvnw package -DskipTests`) :

```bash
cp .env.example .env   # puis renseigner POSTGRES_PASSWORD
docker compose build
docker compose up -d
docker compose logs -f api
```

`compose.yaml` démarre **PostgreSQL 17** (initialisé avec `backend/demo/docs/schema.sql`) et l'**API** exposée sur `http://localhost:8080`. Le service `api` attend que PostgreSQL soit `healthy`.

## Endpoints

Les collections retournent des tableaux JSON sans pagination.

| Ressource | Liste — GET | Détail — GET | Création — POST | Modification — PUT | Suppression — DELETE |
| --- | --- | --- | --- | --- | --- |
| Équipements | `/api/equipment` | `/api/equipment/{id}` | `/api/equipment` | `/api/equipment/{id}` | `/api/equipment/{id}` |
| Salles | `/api/rooms` | `/api/rooms/{id}` | `/api/rooms` | `/api/rooms/{id}` | `/api/rooms/{id}` |
| Utilisateurs | `/api/users` | `/api/users/{id}` | `/api/users` | `/api/users/{id}` | `/api/users/{id}` |
| Réservations | `/api/reservations` | `/api/reservations/{id}` | `/api/reservations` | `/api/reservations/{id}` | `/api/reservations/{id}` |

`{id}` est un **UUID**.

| Statut HTTP | Signification |
| --- | --- |
| `200` | Lecture ou modification réussie |
| `201` | Création réussie ; l'en-tête `Location` indique la ressource |
| `204` | Suppression réussie, sans corps |
| `400` | Entrée invalide, JSON illisible, début dans le passé ou salle/compte désactivé |
| `404` | Ressource ou identifiant de relation inexistant |
| `409` | Unicité en conflit, chevauchement de réservation, ou suppression empêchée par une relation |

## Règles métier

- Une réservation dure **exactement 7 jours** : `endAt` est calculé à partir de `startAt` (+168 h).
- La date de début ne peut pas être dans le passé.
- Seules une **salle active** et un **compte actif** peuvent être réservés.
- Une salle ne peut pas avoir deux réservations `CONFIRMED` qui se chevauchent (réponse `409`).
- Une annulation passe le statut à `CANCELLED` et renseigne `cancelledAt`.
- Une révocation passe le statut à `REVOKED`, exige `revokedBy` et renseigne `revokedAt` (+ `revocationReason` facultatif).

## Contrat JSON

Les identifiants et `createdAt` sont gérés par le serveur. Les entrées ne contiennent pas de graphe d'entités imbriquées. Les champs inconnus (notamment `id`, `createdAt`, relations imbriquées) sont refusés avec `400`.

Champs obligatoires :

- Équipement : `name`, non vide et unique.
- Salle : `code`, `name`, `building`, `capacity` strictement positive et `equipmentIds` (tableau, éventuellement vide). Le `code` est unique par bâtiment.
- Utilisateur : `email` valide et unique (insensible à la casse), `fullName`, `role` et, à la création, `password` (≥ 8 caractères).
- Réservation : `roomId`, `userId` et `startAt`. `endAt` est calculé par le serveur.

Valeurs de `role` : `STUDENT`, `STAFF`, `ADMIN`. Valeurs de `status` : `CONFIRMED`, `CANCELLED`, `REVOKED`.

Les dates utilisent `Instant` : chaînes **ISO 8601 avec fuseau horaire**, par exemple `2026-12-01T09:00:00Z`.

Exemple de création d'une réservation :

```json
{
  "roomId": "a07d627e-b812-4d24-868a-974d8ee6dbbf",
  "userId": "784638ce-9c91-4d1a-8302-c05ddde89e53",
  "startAt": "2026-12-01T09:00:00Z"
}
```

La réponse ajoute `id`, `endAt` (= `startAt` + 7 jours), `status`, `createdAt` et les champs de révocation/annulation.

## Parcours de démonstration

Avec l'API démarrée (localement ou via Docker) :

```bash
B=http://localhost:8080/api
EQ=$(curl -s -X POST $B/equipment -H 'Content-Type: application/json' -d '{"name":"Projecteur"}' | jq -r .id)
US=$(curl -s -X POST $B/users -H 'Content-Type: application/json' \
  -d '{"email":"amina@ensa.ma","password":"secret123","fullName":"Amina","role":"STUDENT"}' | jq -r .id)
RM=$(curl -s -X POST $B/rooms -H 'Content-Type: application/json' \
  -d "{\"code\":\"A101\",\"name\":\"A101\",\"building\":\"B1\",\"capacity\":30,\"equipmentIds\":[\"$EQ\"]}" | jq -r .id)
curl -s -X POST $B/reservations -H 'Content-Type: application/json' \
  -d "{\"roomId\":\"$RM\",\"userId\":\"$US\",\"startAt\":\"2026-12-01T09:00:00Z\"}" | jq
```

## Vérification automatisée

`./mvnw clean verify` compile, exécute les tests et produit le JAR exécutable. La suite (10 tests) couvre le CRUD complet, les réponses plates, les valeurs par défaut, les références inexistantes, les validations, la durée de 7 jours, le refus des chevauchements, l'annulation, la révocation, le hash BCrypt et `/actuator/health`.
