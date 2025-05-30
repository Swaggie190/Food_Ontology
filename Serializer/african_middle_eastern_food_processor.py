#!/usr/bin/env python3
"""
Processeur spécialisé pour les plats africains et du Moyen-Orient
Adapté pour Windows - Version simplifiée pour test
"""

import os
import json
import csv
from pathlib import Path
from PIL import Image
from datetime import datetime
import uuid

class AfricanMiddleEasternFoodProcessor:
    def __init__(self, base_data_dir="./african_middle_eastern_data"):
        self.base_dir = Path(base_data_dir)
        self.images_dir = self.base_dir / "images"
        self.metadata_dir = self.base_dir / "metadata"
        self.nutritional_dir = self.base_dir / "nutritional"
        
        # Créer les dossiers s'ils n'existent pas
        for directory in [self.images_dir, self.metadata_dir, self.nutritional_dir]:
            directory.mkdir(parents=True, exist_ok=True)
            print(f"📁 Dossier créé: {directory}")
        
        # Mapping pour vos plats spécifiques
        self.food_category_mapping = {
            "brown_chapati": {
                "class": "CookedFood", 
                "type": "pain_plat", 
                "main_ingredient": "whole_wheat_flour,water,salt",
                "region": "East_Africa",
                "cooking_method": "grilled"
            },
            "white_chapati": {
                "class": "CookedFood", 
                "type": "pain_plat", 
                "main_ingredient": "wheat_flour,water,salt",
                "region": "East_Africa", 
                "cooking_method": "grilled"
            },
            "busara_whole_maize_and_finger_millet_porridge": {
                "class": "CookedFood",
                "type": "porridge_cereales",
                "main_ingredient": "maize,finger_millet,water,milk",
                "region": "East_Africa",
                "cooking_method": "boiled"
            },
            "drop_scones": {
                "class": "CookedFood",
                "type": "pancakes",
                "main_ingredient": "flour,milk,eggs,sugar,baking_powder",
                "region": "British",
                "cooking_method": "grilled"
            },
            "fried_egg_mayai_ya_kukaangwa": {
                "class": "CookedFood",
                "type": "plat_oeufs",
                "main_ingredient": "eggs,oil,salt",
                "region": "East_Africa",
                "cooking_method": "fried"
            },
            "meat_samosa_sambusa_ya_nyama": {
                "class": "CookedFood",
                "type": "samosa",
                "main_ingredient": "wheat_flour,beef,onions,spices,oil",
                "region": "East_Africa_Middle_East",
                "cooking_method": "deep_fried"
            },
            "vegetable_samosa_sambusa_ya_mboga": {
                "class": "CookedFood",
                "type": "samosa",
                "main_ingredient": "wheat_flour,vegetables,onions,spices,oil",
                "region": "East_Africa_Middle_East",
                "cooking_method": "deep_fried"
            },
            "omelette": {
                "class": "CookedFood",
                "type": "plat_oeufs", 
                "main_ingredient": "eggs,milk,salt,oil",
                "region": "International",
                "cooking_method": "fried"
            },
            "spanish_omelette": {
                "class": "CookedFood",
                "type": "plat_oeufs",
                "main_ingredient": "eggs,potatoes,onions,oil,salt",
                "region": "European",
                "cooking_method": "fried"
            },
            "pancakes_chapati_za_maji": {
                "class": "CookedFood",
                "type": "pancakes",
                "main_ingredient": "flour,milk,eggs,sugar,baking_powder",
                "region": "International",
                "cooking_method": "grilled"
            },
            "stir_fried_cabbage": {
                "class": "CookedFood",
                "type": "legumes_sautes",
                "main_ingredient": "cabbage,onions,tomatoes,oil",
                "region": "East_Africa",
                "cooking_method": "stir_fried"
            },
            "sukumawiki_stir_fried_kales": {
                "class": "CookedFood",
                "type": "legumes_sautes",
                "main_ingredient": "kale,onions,tomatoes,oil",
                "region": "East_Africa",
                "cooking_method": "stir_fried"
            },
            "terere_stir_fried_amaranth_leaves": {
                "class": "CookedFood",
                "type": "legumes_sautes",
                "main_ingredient": "amaranth_leaves,onions,tomatoes,oil",
                "region": "East_Africa",
                "cooking_method": "stir_fried"
            },
            "tosti_mayai_egg_toast": {
                "class": "CookedFood",
                "type": "plat_oeufs",
                "main_ingredient": "bread,eggs,oil,salt",
                "region": "East_Africa",
                "cooking_method": "fried"
            },
            "mrenda_and_seveve_jute_mallow_and_pumpkin": {
                "class": "CookedFood",
                "type": "legumes_sautes",
                "main_ingredient": "jute_mallow,pumpkin_leaves,onions,oil",
                "region": "East_Africa",
                "cooking_method": "stir_fried"
            },
            "roumy_cheese": {
                "class": "ManufacturedFood",
                "type": "fromage",
                "main_ingredient": "milk,salt,rennet,cultures",
                "region": "Middle_East",
                "cooking_method": "fermented"
            },
            "kebda": {
                "class": "CookedFood",
                "type": "abats",
                "main_ingredient": "liver,onions,spices,oil",
                "region": "Middle_East",
                "cooking_method": "fried"
            },
            "bamia_the_egyptian_okra": {
                "class": "CookedFood",
                "type": "ragout",
                "main_ingredient": "okra,tomatoes,onions,garlic,spices",
                "region": "Middle_East",
                "cooking_method": "stewed"
            },
            "egyptian_rice_with_noodles": {
                "class": "CookedFood",
                "type": "riz_compose",
                "main_ingredient": "rice,noodles,oil,salt",
                "region": "Middle_East",
                "cooking_method": "boiled"
            },
            "malfuf_mahshi": {
                "class": "CookedFood",
                "type": "legumes_farcis",
                "main_ingredient": "cabbage,rice,meat,spices,tomatoes",
                "region": "Middle_East",
                "cooking_method": "stuffed_boiled"
            },
            "molokhia": {
                "class": "CookedFood",
                "type": "soupe_epaisse",
                "main_ingredient": "molokhia_leaves,broth,garlic,coriander",
                "region": "Middle_East",
                "cooking_method": "boiled"
            },
            "egyptian_lamb_kofta": {
                "class": "CookedFood",
                "type": "viande_hachee",
                "main_ingredient": "lamb,onions,parsley,spices,oil",
                "region": "Middle_East",
                "cooking_method": "grilled"
            },
            "stuffed_grape_leaves": {
                "class": "CookedFood",
                "type": "legumes_farcis",
                "main_ingredient": "grape_leaves,rice,herbs,oil,lemon",
                "region": "Middle_East",
                "cooking_method": "stuffed_boiled"
            },
            "kibbe_quipe": {
                "class": "CookedFood",
                "type": "boulette",
                "main_ingredient": "bulgur,meat,onions,spices,oil",
                "region": "Middle_East",
                "cooking_method": "fried"
            },
            "tahini_arabic": {
                "class": "ManufacturedFood",
                "type": "pate_graines",
                "main_ingredient": "sesame_seeds",
                "region": "Middle_East",
                "cooking_method": "ground"
            },
            # Par défaut
            "default": {
                "class": "Food", 
                "type": "aliment", 
                "main_ingredient": "unknown",
                "region": "Unknown",
                "cooking_method": "unknown"
            }
        }
    
    def clean_folder_name(self, folder_name):
        """Nettoyer et normaliser le nom de dossier"""
        cleaned = folder_name.lower()
        cleaned = cleaned.replace(" ", "_").replace("-", "_")
        cleaned = cleaned.replace("(", "").replace(")", "")
        cleaned = cleaned.replace("__", "_").replace("___", "_")
        return cleaned.strip("_")
    
    def get_food_category_info(self, category_name):
        """Obtenir les informations sur une catégorie d'aliment"""
        category_key = category_name.lower()
        
        if category_key in self.food_category_mapping:
            return self.food_category_mapping[category_key]
        
        return self.food_category_mapping["default"]
    
    def is_image_file(self, file_path):
        """Vérifier si le fichier est une image"""
        image_extensions = {'.jpg', '.jpeg', '.png', '.bmp', '.gif', '.tiff', '.webp'}
        return file_path.suffix.lower() in image_extensions
    
    def extract_image_metadata(self, image_path):
        """Extraire les métadonnées d'une image"""
        metadata = {
            'filename': image_path.name,
            'file_size': image_path.stat().st_size,
            'creation_date': datetime.fromtimestamp(image_path.stat().st_ctime).isoformat(),
            'image_id': str(uuid.uuid4())
        }
        
        try:
            with Image.open(image_path) as img:
                metadata.update({
                    'width': img.width,
                    'height': img.height,
                    'format': img.format,
                    'mode': img.mode,
                    'aspect_ratio': round(img.width / img.height, 2) if img.height > 0 else 1.0
                })
        except Exception as e:
            print(f"⚠️ Erreur métadonnées pour {image_path}: {e}")
        
        return metadata
    
    def process_images(self, source_dir):
        """Traiter les images"""
        source_path = Path(source_dir)
        organized_data = {}
        
        print(f"🌍 Traitement depuis: {source_path}")
        
        if not source_path.exists():
            print(f"❌ Le dossier {source_path} n'existe pas!")
            return {}
        
        # Lister les dossiers
        folders = [f for f in source_path.iterdir() if f.is_dir()]
        print(f"📁 {len(folders)} dossiers trouvés:")
        for folder in folders:
            print(f"   - {folder.name}")
        
        for category_folder in folders:
            original_name = category_folder.name
            cleaned_name = self.clean_folder_name(original_name)
            
            print(f"\n🍽️ Traitement: {original_name} → {cleaned_name}")
            
            # Créer le dossier de destination
            category_path = self.images_dir / cleaned_name
            category_path.mkdir(exist_ok=True)
            
            images_info = []
            image_count = 0
            
            # Traiter les images
            image_files = [f for f in category_folder.iterdir() if self.is_image_file(f)]
            print(f"   📸 {len(image_files)} images trouvées")
            
            for image_file in image_files:
                image_count += 1
                
                # Nouveau nom
                new_image_name = f"{cleaned_name}_{image_count:03d}{image_file.suffix.lower()}"
                new_image_path = category_path / new_image_name
                
                # Copier le fichier
                try:
                    import shutil
                    shutil.copy2(image_file, new_image_path)
                    
                    # Métadonnées
                    image_metadata = self.extract_image_metadata(new_image_path)
                    image_metadata.update({
                        'category_name': cleaned_name,
                        'original_category_name': original_name,
                        'original_filename': image_file.name,
                        'processed_filename': new_image_name,
                        'relative_path': f"images/{cleaned_name}/{new_image_name}",
                        'image_number': image_count
                    })
                    
                    # Info catégorie
                    category_info = self.get_food_category_info(cleaned_name)
                    image_metadata.update(category_info)
                    
                    images_info.append(image_metadata)
                    
                except Exception as e:
                    print(f"   ❌ Erreur copie {image_file.name}: {e}")
            
            organized_data[cleaned_name] = {
                'images': images_info,
                'total_images': len(images_info),
                'category_info': self.get_food_category_info(cleaned_name),
                'original_folder_name': original_name
            }
            
            print(f"   ✅ {len(images_info)} images copiées")
        
        # Sauvegarder les résultats
        self.save_results(organized_data)
        return organized_data
    
    def save_results(self, organized_data):
        """Sauvegarder les résultats"""
        # Index JSON
        index_file = self.metadata_dir / "african_middle_eastern_food_index.json"
        summary = {
            'total_categories': len(organized_data),
            'total_images': sum(cat['total_images'] for cat in organized_data.values()),
            'processing_date': datetime.now().isoformat(),
            'categories': organized_data
        }
        
        with open(index_file, 'w', encoding='utf-8') as f:
            json.dump(summary, f, indent=2, ensure_ascii=False)
        print(f"📄 Index sauvé: {index_file}")
        
        # CSV nutritionnel
        self.create_nutritional_csv(organized_data)
    
    def create_nutritional_csv(self, organized_data):
        """Créer le CSV nutritionnel"""
        csv_file = self.nutritional_dir / "african_middle_eastern_nutritional.csv"
        
        headers = [
            'food_name', 'category', 'owl_class', 'food_type', 'region', 'cooking_method',
            'main_ingredient', 'calories_per_100g', 'proteins', 'carbohydrates', 'fats', 
            'fiber', 'sodium', 'sugar', 'ingredients', 'allergens', 'description',
            'spice_level', 'cultural_significance'
        ]
        
        with open(csv_file, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            writer.writerow(headers)
            
            for category_name, category_data in organized_data.items():
                category_info = category_data['category_info']
                
                row = [
                    category_name,  # food_name
                    category_info['type'],  # category
                    category_info['class'],  # owl_class
                    category_info['type'],   # food_type
                    category_info['region'], # region
                    category_info['cooking_method'], # cooking_method
                    category_info['main_ingredient'],  # main_ingredient
                    250,  # calories_per_100g - VALEUR PAR DÉFAUT
                    10,   # proteins - VALEUR PAR DÉFAUT
                    30,   # carbohydrates - VALEUR PAR DÉFAUT
                    5,    # fats - VALEUR PAR DÉFAUT
                    3,    # fiber - VALEUR PAR DÉFAUT
                    200,  # sodium - VALEUR PAR DÉFAUT
                    2,    # sugar - VALEUR PAR DÉFAUT
                    category_info['main_ingredient'],  # ingredients
                    "",   # allergens - À REMPLIR
                    f"Plat traditionnel: {category_name.replace('_', ' ')}", # description
                    "medium", # spice_level par défaut
                    ""    # cultural_significance - À REMPLIR
                ]
                writer.writerow(row)
        
        print(f"📊 CSV nutritionnel créé: {csv_file}")

def main():
    processor = AfricanMiddleEasternFoodProcessor()
    
    print("🌍 === TRAITEMENT DES PLATS AFRICAINS ET DU MOYEN-ORIENT ===")
    print("🍽️ Version simplifiée pour test Windows")
    
    # Demander le chemin
    print("\n📁 Entrez le chemin vers votre dossier d'images:")
    print("   Exemple: C:\\Users\\Swaggie\\Desktop\\mes_images")
    print("   Ou tapez 'test' pour utiliser un dossier de test")
    
    source_directory = input("Chemin: ").strip()
    
    if source_directory.lower() == 'test':
        source_directory = "./test_images"
        print(f"🧪 Mode test avec: {source_directory}")
    
    if not source_directory:
        print("❌ Veuillez spécifier un chemin!")
        return
    
    if not os.path.exists(source_directory):
        print(f"❌ Le dossier '{source_directory}' n'existe pas!")
        print("💡 Vérifiez le chemin et réessayez")
        return
    
    print(f"\n🔄 Traitement en cours...")
    organized_data = processor.process_images(source_directory)
    
    if organized_data:
        print(f"\n🎉 === TERMINÉ ===")
        print(f"✅ Catégories traitées: {len(organized_data)}")
        total_images = sum(cat['total_images'] for cat in organized_data.values())
        print(f"✅ Images totales: {total_images}")
        
        print(f"\n📁 Fichiers créés:")
        print(f"  - Images: ./african_middle_eastern_data/images/")
        print(f"  - Index: ./african_middle_eastern_data/metadata/")
        print(f"  - CSV: ./african_middle_eastern_data/nutritional/")
        
        print(f"\n🎯 PROCHAINE ÉTAPE:")
        print(f"Vérifiez les fichiers créés, puis nous passerons à Fuseki!")
        
    else:
        print("❌ Aucune image traitée!")
        print("💡 Vérifiez que vos dossiers contiennent des images (.jpg, .png, etc.)")

if __name__ == "__main__":
    main()