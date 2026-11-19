# Infrastructure

Ce dossier contient les fichiers nécessaires à la création et à la gestion de l'infrastructure virtuelle du projet.

## Technologies utilisées

- **VirtualBox :** plateforme de virtualisation.
- **Vagrant :** automatisation de la création et de la gestion des machines virtuelles.
- **Terraform :** préparation et gestion d'une future infrastructure Cloud.

## Organisation

```text
infrastructure/
├── README.md
├── vagrant/
│   ├── Vagrantfile
│   └── scripts/
└── terraform/
```

- **Vagrantfile :** fichier de configuration définissant les machines virtuelles, leurs ressources (CPU, RAM) et leurs paramètres réseau.
- **Terraform :** fichiers de configuration destinés à une éventuelle migration vers AWS ou Azure.

## Objectifs

- Créer les machines virtuelles nécessaires au projet.
- Configurer les ressources et les réseaux virtuels.
- Automatiser le déploiement de l'environnement local.
- Préparer une éventuelle migration vers le Cloud.