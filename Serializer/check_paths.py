#!/usr/bin/env python3
"""
Script pour vérifier et tester les chemins vers vos dossiers
"""

import os
from pathlib import Path

def check_path(path_str):
    """Vérifier un chemin et afficher des informations"""
    print(f"\n=== Vérification du chemin ===")
    print(f"Chemin saisi: {path_str}")
    
    # Résoudre le tilde si présent
    expanded_path = os.path.expanduser(path_str)
    print(f"Chemin étendu: {expanded_path}")
    
    # Convertir en Path object
    path_obj = Path(expanded_path)
    print(f"Chemin absolu: {path_obj.absolute()}")
    
    # Vérifier l'existence
    if path_obj.exists():
        print("✅ Le chemin existe!")
        
        if path_obj.is_dir():
            print("✅ C'est bien un dossier")
            
            # Lister le contenu
            items = list(path_obj.iterdir())
            print(f"📁 Contient {len(items)} éléments:")
            
            # Afficher les premiers éléments
            for i, item in enumerate(items[:10]):
                if item.is_dir():
                    # Compter les images dans le dossier
                    image_count = len([f for f in item.iterdir() 
                                     if f.suffix.lower() in {'.jpg', '.jpeg', '.png', '.bmp', '.gif', '.tiff', '.webp'}])
                    print(f"  📁 {item.name} ({image_count} images)")
                else:
                    print(f"  📄 {item.name}")
            
            if len(items) > 10:
                print(f"  ... et {len(items) - 10} autres éléments")
                
        else:
            print("❌ Ce n'est pas un dossier")
    else:
        print("❌ Le chemin n'existe pas!")
        
        # Suggérer des chemins alternatifs
        print("\n🔍 Chemins suggérés à vérifier:")
        parent = path_obj.parent
        while not parent.exists() and len(parent.parts) > 1:
            parent = parent.parent
        
        if parent.exists():
            print(f"✅ Ce chemin existe: {parent}")
            print("📁 Contenu:")
            for item in parent.iterdir():
                if item.is_dir():
                    print(f"  📁 {item.name}")

def main():
    print("=== Vérificateur de chemins pour Collection-d'images ===")
    
    # Chemins à tester
    test_paths = [
        " C:\\Users\\Swaggie\\Desktop\\Food_Ontology\\Serializer\\images"
    ]
    
    print("Voici les chemins typiques à tester:")
    for i, path in enumerate(test_paths, 1):
        print(f"{i}. {path}")
    
    while True:
        print("\n" + "="*50)
        choice = input("Entrez un numéro (1-4) ou un chemin personnalisé (q pour quitter): ").strip()
        
        if choice.lower() == 'q':
            break
        
        if choice.isdigit() and 1 <= int(choice) <= 4:
            path_to_check = test_paths[int(choice) - 1]
        else:
            path_to_check = choice
        
        check_path(path_to_check)

if __name__ == "__main__":
    main()
