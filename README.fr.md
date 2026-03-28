<img align="right" src="https://github.com/michaelmassee/Petanque-Turnier-Manager/raw/master/doku/images/petanqueturniermanager-logo-256px.png" alt="Logo" height="120">

# Gestionnaire de tournois de Pétanque

[![License: EUPL v1.2](https://img.shields.io/badge/License-EUPL_1.2-blue.svg)](https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12)
[![Wiki](https://img.shields.io/badge/Doku-Projekt_Wiki-green.svg)](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki)
[![Spenden mit PayPal](https://img.shields.io/badge/Spenden-PayPal-blue.svg)](https://www.paypal.me/@michaelmassee1)

*Read this in other languages: [🇩🇪 DE](README.md) | [🇬🇧 EN](README.en.md) | [🇫🇷 FR](README.fr.md) | [🇪🇸 ES](README.es.md) | [🇳🇱 NL](README.nl.md)*


---

## 🎯 Introduction
Le **Gestionnaire de tournois de Pétanque** (Pétanque-Turnier-Manager) est un logiciel open-source performant pour la gestion de tournois, qui s'intègre parfaitement comme extension dans **LibreOffice Calc**.

Ce logiciel a été spécialement conçu pour rendre l'organisation et le déroulement des tournois de pétanque et de boules aussi simples et efficaces que possible. Comme il fonctionne de manière totalement hors ligne et directement dans le tableur, il consomme très peu de ressources. Cela en fait la solution idéale pour une utilisation sur le terrain de boules – même sur d'anciens ordinateurs portables fonctionnant, par exemple, sous un système Linux léger.

**Les avantages en un coup d'œil :**
* **Indépendant du système d'exploitation :** Fonctionne de manière fiable sous Linux, macOS et Windows.
* **Multilingue :** L'interface utilisateur prend en charge plusieurs langues, dont l'allemand (DE), l'anglais (EN), le français (FR), l'espagnol (ES) et le néerlandais (NL).
* **Gratuit & Open Source :** Pas de frais de licence, pas de publicité.
* **Économe en ressources :** Parfaitement adapté au matériel plus ancien.
* **Tout en un seul endroit :** Aucune base de données externe n'est nécessaire ; tout est calculé et enregistré directement dans LibreOffice Calc.

Available in Languages: 🇩🇪 DE | 🇬🇧 EN | 🇫🇷 FR | 🇪🇸 ES | 🇳🇱 NL

---

## 🛠️ Personnalisation sans limites : Faites-en *votre* tournoi !

Le plus grand atout de ce gestionnaire de tournoi est sans doute sa base : **LibreOffice Calc**. Comme toutes les données, tableaux et classements sont écrits directement dans des feuilles de calcul Calc ordinaires, vous n'êtes pas enfermé dans une structure de programme rigide.

> **💡 Contrôle total avec les outils intégrés de Calc :** Vous pouvez étendre **n'importe quel** système de tournoi généré de manière totalement libre et selon vos propres souhaits !

* **Tournois internationaux :** Grâce au support multilingue intégré, vous êtes parfaitement équipé pour les tournois avec des invités internationaux ou pour les ligues transfrontalières.
* **Évaluations personnalisées :** Utilisez RECHERCHEV, des tableaux croisés dynamiques ou vos propres formules pour extraire des statistiques internes aux équipes, des moyennes de points ou des classements spéciaux à partir des données du tournoi.
* **Personnalisation visuelle :** Créez vos propres tableaux de bord, insérez les logos du club, modifiez la mise en page pour l'impression ou concevez des vues de présentation pour un vidéoprojecteur (par ex. pour l'affichage en direct du classement actuel).
* **Macros personnalisées & API publique :** Si vous avez besoin de fonctions avancées, vous pouvez écrire vos propres macros LibreOffice (Basic, Python, etc.) qui interagissent avec les tableaux générés. L'extension fournit une **API publique** pour cela. → [Macros & Formules – documentation complète de l'API (en allemand)](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki/Makros-und-Formeln)

L'extension s'occupe de la logique complexe (tirage au sort, calcul du classement selon le système Buchholz, etc.) – ce que vous faites des données à la fin dépend entièrement de vous !

---

## 🏆 Systèmes de tournoi pris en charge
L'extension propose déjà un large choix de modes de tournoi éprouvés, adaptés aux petits championnats de clubs comme aux grands tournois :

* **Supermêlée / Mêlée :** Idéal pour les tournois décontractés. Les joueurs sont tirés au sort dans de nouvelles équipes et contre de nouveaux adversaires à chaque tour (ou pour tout le tournoi).
* **Ligue (Championnat) :** Pour l'organisation d'une ligue de club ou régionale avec des équipes fixes. Le calendrier des matchs est généré à l'avance. Prend en charge l'exportation HTML.
* **Système Suisse (Formule Suisse) :** Le système le plus équitable pour les tournois avec de nombreux participants et un temps limité. Comprend le calcul automatique des victoires, de la différence de boules et des points Buchholz (et Buchholz médian) pour un classement exact.
* **Toutes rondes (Round Robin) :** Le système de ligue classique où chaque équipe affronte toutes les autres équipes au cours du tournoi.
* **Élimination directe (Système à élimination directe) :** Pour les phases finales classiques. Une victoire signifie la qualification, une défaite l'élimination immédiate. Les appariements suivent le système croisé (1er vs Dernier, 2e vs Avant-dernier...). Une phase de cadrage est automatiquement calculée si nécessaire.
* **Système de Maastricht :** Combine le Système Suisse avec des phases finales à élimination directe. Les équipes sont appariées selon l'algorithme suisse sur plusieurs tours préliminaires (2–5). Ensuite, les équipes sont réparties en groupes de niveau (A, B, C, D) selon leur nombre de victoires – chaque groupe dispute sa propre finale en élimination directe. Résultat : quatre vainqueurs de tournoi, une répartition équitable et des finales palpitantes.

(En cours de développement)
* **Système par Poules :** Mode classique avec phase de groupes (Poules) et répartition ultérieure dans des tableaux à élimination directe (Concours A, B, C, D).

---

## 💻 Prérequis système

* **LibreOffice :** à partir de la version 25.x (ou plus récente)
* **Java (JRE/JDK) :** à partir de la version 25
* **Système d'exploitation :** Linux, macOS ou Windows

---

## ⚙️ Installation & Configuration

### Étape 1 : Installer Java
* **Temurin Adoptium JDK (LTS) :** [Téléchargement gratuit ici](https://adoptium.net/)
* **Oracle Java :** [Téléchargement officiel ici](https://www.oracle.com/java/technologies/downloads/)

### Étape 2 : Activer Java dans LibreOffice
1. Ouvrez **LibreOffice**. Allez dans `Outils` ▸ `Options` ▸ `LibreOffice` ▸ `Avancé`. *(Sur macOS : `LibreOffice` ▸ `Préférences...`)*
2. Cochez la case **"Utiliser un environnement d'exécution Java"**.
3. Sélectionnez le JRE installé dans la liste. Validez avec `OK` et redémarrez LibreOffice.

### Étape 3 : Installer l'extension
1. Téléchargez la dernière version de l'extension (`PetanqueTurnierManager-vx.xx.oxt`) dans la section [Releases](https://github.com/michaelmassee/Petanque-Turnier-Manager/releases).
2. Double-cliquez sur le fichier `.oxt`. LibreOffice ouvrira le **Gestionnaire des extensions**.
3. Confirmez l'installation.

---

## 🚀 Premiers pas & Utilisation

1. Démarrez **LibreOffice Calc** et ouvrez un nouveau classeur vide.
2. Dans la barre de menu supérieure, vous trouverez un nouveau menu appelé **"PétTurnMngr"**.
3. Cliquez dessus. Le centre de contrôle s'ouvre : vous pouvez ajouter des joueurs, choisir le mode et générer les tours.
4. Saisissez simplement les résultats des parties dans les champs générés ; les tableaux et les tours suivants se calculent automatiquement.

---

## 📖 Documentation & Aide
La documentation complète se trouve sur notre Wiki officiel (actuellement en allemand) :
👉 **[Accéder au Wiki du projet ici](https://github.com/michaelmassee/Petanque-Turnier-Manager/wiki)**

**Licence :** Ce projet est sous [Licence EUPL-1.2](LICENSE).