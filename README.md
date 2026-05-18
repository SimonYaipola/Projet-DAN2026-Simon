# CraftBoard - Execution de l'application

## Lancer l'application avec l'executable

L'executable Windows se trouve ici:

```text
dist\CraftBoard\CraftBoard.exe
```

Pour lancer l'application:

1. Ouvrir le dossier `dist\CraftBoard`.
2. Double-cliquer sur `CraftBoard.exe`.
3. Attendre quelques secondes pendant que le serveur demarre.
4. L'application desktop s'ouvre automatiquement.

Quand `CraftBoard.exe` est lance, il demarre le serveur local sur:

```text
http://localhost:8080
```

Ensuite, l'application desktop se connecte automatiquement a ce serveur local.

## Important pour remettre ou deplacer l'application

Il ne faut pas deplacer seulement `CraftBoard.exe`.

L'executable depend aussi des dossiers qui sont avec lui, surtout:

```text
dist\CraftBoard\app
dist\CraftBoard\runtime
```

Donc, pour remettre l'application a quelqu'un, il faut donner le dossier complet:

```text
dist\CraftBoard
```

ou un fichier `.zip` qui contient tout ce dossier.

## Attention: ne marchera pas au cegep

L'application ne marchera pas sur le reseau du cegep.

Le serveur a besoin de se connecter a une base de donnees MariaDB externe configuree dans le projet. Sur le reseau du cegep, cette connexion risque d'etre bloquee par les restrictions reseau ou le pare-feu.

Donc, meme si `CraftBoard.exe` s'ouvre, l'application ne pourra pas fonctionner correctement au cegep si elle n'arrive pas a rejoindre la base de donnees.

Pour que ca marche, il faut l'executer sur un reseau qui permet la connexion a la base de donnees externe.

## Regenerer l'executable

Depuis la racine du projet, executer:

```powershell
powershell.exe -ExecutionPolicy Bypass -File .\package-windows.ps1
```

Le nouvel executable sera regenere dans:

```text
dist\CraftBoard\CraftBoard.exe
```

## Verification rapide

Apres avoir lance `CraftBoard.exe`, ouvrir cette adresse dans un navigateur:

```text
http://localhost:8080/api/health
```

Si le serveur fonctionne, la reponse sera:

```text
Le serveur tourne !

le compte admin est Admin mdp : Admin, sinon tous les comptes ayant 
un mot de passe configuré est : test. Chaque compte peut être configurer comme première connexion.
```
