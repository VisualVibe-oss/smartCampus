Voici une proposition de **Cahier des Charges Fonctionnel Minimal** pour le projet **SmartCampus**, rédigé de façon claire et structurée pour être transmis aux leads / responsables du projet.

---

# Cahier des Charges Fonctionnel Minimal (STP / MVP)

**Nom du Produit :** SmartCampus

**Sujet :** Plateforme de Gestion et de Réservation des Salles du Campus

**Public Cible :** Étudiants, Professeurs et Personnel de l'ENSA Khouribga

---

## 1. Contexte et Objectif du Projet

Le projet **SmartCampus** vise à fournir une solution numérique centralisée pour rationaliser l'accès aux infrastructures du campus. La première version (MVP) se concentre sur **l'authentification des utilisateurs**, la **consultation des salles disponibles** et la **gestion des réservations de salles**.

---

## 2. Périmètre Fonctionnel (Besoins Utilisateurs)

### Module 1 : Authentification & Sécurité (`AUTH`)



* **Connexion sécurisée :** Permettre aux étudiants et au personnel de se connecter via leur adresse e-mail institutionnelle et un mot de passe.


* **Gestion des échecs :** Afficher des messages d'erreur explicites et tracer les tentatives de connexion échouées dans les logs de sécurité.


* **Déconnexion :** Invalider la session de l'utilisateur lors de sa déconnexion pour garantir la sécurité sur les ordinateurs partagés.

### Module 2 : Gestion des Salles (`ROOM`)



* **Consultation du catalogue :** Visualiser la liste des salles du campus avec leurs caractéristiques (capacité, bâtiment, équipements : projecteur, climatisation, etc.).
* **Recherche et filtrage :** Rechercher des salles par critères multiples (nom, capacité minimale, équipements disponibles).
* **Indicateur de disponibilité :** Afficher en temps réel le statut d'une salle (Libre / Occupée).

### Module 3 : Réservation de Salles (`BOOK`)



* **Création de réservation :** Permettre à un utilisateur connecté de réserver une salle disponible sur un créneau horaire précis.
* **Contrôle des conflits :** Empêcher la sur-réservation d'une même salle sur des créneaux chevauchants.
* **Annulation :** Permettre à l'auteur d'une réservation de l'annuler, remettant ainsi la salle à disposition du campus.

---

## 3. Exigences Non Fonctionnelles Minimales

* **Architecture & Déploiement :** Structuration modulaire facilitant l'intégration continue et le déploiement sur des conteneurs (Docker / Kubernetes).


* **Persistance des données :** Séparation claire entre la couche applicative et le stockage des données.
* **Traçabilité DevOps :** Chaque fonctionnalité, tâche ou correctif doit être rattaché à un ticket Jira (`SC-xx`) pour assurer le suivi dans les commits et branches Git.



