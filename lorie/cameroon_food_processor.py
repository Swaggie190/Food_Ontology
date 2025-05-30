#!/usr/bin/env python3
"""
Script adapté pour traiter les catégories d'aliments spécifiques
Structure: Collection-d'images/nom_categorie/images...
"""

import os
import json
import csv
import hashlib
from pathlib import Path
from PIL import Image, ExifTags
import requests
from datetime import datetime
import uuid

class FoodCategoriesProcessor:
    def __init__(self, base_data_dir="./data"):
        self.base_dir = Path(base_data_dir)
        self.images_dir = self.base_dir / "images"
        self.metadata_dir = self.base_dir / "metadata"
        self.nutritional_dir = self.base_dir / "nutritional"
        
        # Créer les dossiers s'ils n'existent pas
        for directory in [self.images_dir, self.metadata_dir, self.nutritional_dir]:
            directory.mkdir(parents=True, exist_ok=True)
        
        # Mapping des catégories vers les classes OWL basé sur vos dossiers
        self.category_mapping = {
            # Bouillons et soupes
            "beef_broth_with_oil": {"class": "CookedFood", "type": "bouillon", "main_ingredient": "beef,oil,water,carot,tomato"},
            "beef_broth_without_oil": {"class": "CookedFood", "type": "bouillon", "main_ingredient": "beef,water,carot,tomato"},
            "cabbage_broth": {"class": "CookedFood", "type": "bouillon", "main_ingredient": "cabbage"},
            
            # Café et thé
            "coffee_with_milk_and_with_sugar": {"class": "Coffee", "type": "boisson_chaude", "main_ingredient": "coffee,milk,sugar,water"},
            "coffee_with_milk_without_sugar": {"class": "Coffee", "type": "boisson_chaude", "main_ingredient": "coffee,milk,water"},
            "coffee_without_milk_and_with_sugar": {"class": "Coffee", "type": "boisson_chaude", "main_ingredient": "coffee,coffee,milk,sugar,water"},
            "cup_of_tea": {"class": "Tea", "type": "boisson_chaude", "main_ingredient": "tea,citron"},
            "sweet_tea": {"class": "Tea", "type": "boisson_chaude", "main_ingredient": "tea,citron"},
            "tea_without_sugar_and_with_milk": {"class": "Tea", "type": "boisson_chaude", "main_ingredient": "tea,milk"},
            
            # Fruits
            "date_fruits": {"class": "Fruit", "type": "fruit_sec", "main_ingredient": "dates"},
            
            # Poissons et fruits de mer
            "cuttlefish_raw": {"class": "RawFood", "type": "fruit_de_mer", "main_ingredient": "cuttlefish,citron"},
            
            # Noix et graines
            "dried_hazelnut": {"class": "RawFood", "type": "noix", "main_ingredient": "hazelnut"},
            "dried_octopus_food": {"class": "ManufacturedFood", "type": "fruit_de_mer_sec", "main_ingredient": "octopus,epices,citron"},
            "dried_sesame_and_seed_food": {"class": "RawFood", "type": "graine", "main_ingredient": "sesame "},
            "dried_walnuts_food": {"class": "RawFood", "type": "noix", "main_ingredient": "walnuts"},
            "dry_roasted_mixed_nuts_food": {"class": "CookedFood", "type": "noix_grillees", "main_ingredient": "mixed_nuts"},
            "groundnut": {"class": "RawFood", "type": "legumineuse", "main_ingredient": "peanut"},
            "groundnut_clusteri": {"class": "ManufacturedFood", "type": "collation", "main_ingredient": "peanut,chocolate"},
            
            # Légumineuses préparées
            "dry_peant_roasted_and_salted": {"class": "CookedFood", "type": "legumineuse_preparee", "main_ingredient": "peanut"},
            "dry_peant_unsalted": {"class": "CookedFood", "type": "legumineuse_preparee", "main_ingredient": "peanut"},
            
            # Riz
            "dry_rice_and_polished_rice_food": {"class": "RawFood", "type": "cereale", "main_ingredient": "rice"},
            
            # Légumes et autres
            "halwa": {"class": "CookedFood", "type": "dessert", "main_ingredient": "mixed"},
            
            # Jus et sauces
            "tomato_juice": {"class": "NaturalBeverage", "type": "jus", "main_ingredient": "tomato,sugar,water"},
            "tomato_ketchup": {"class": "ManufacturedFood", "type": "sauce", "main_ingredient": "tomato,epices."},
            
            # Par défaut
            "default": {"class": "Food", "type": "aliment", "main_ingredient": "unknown"}
        }
        
        # Détection des doublons potentiels
        self.potential_duplicates = [
            ("dry peant ,roasted and salted", "dry_peant_,roasted_and_salted"),
            # Ajoutez d'autres doublons détectés ici
        ]
    
    def clean_folder_name(self, folder_name):
        """Nettoyer et normaliser le nom de dossier"""
        # Remplacer les caractères spéciaux et espaces
        cleaned = folder_name.lower()
        cleaned = cleaned.replace(" ", "_").replace(",", "").replace("'", "")
        cleaned = cleaned.replace("__", "_").replace("___", "_")
        return cleaned.strip("_")
    
    def organize_food_categories(self, source_dir):
        """
        Organise les images par catégorie d'aliment
        Structure source: source_dir/nom_categorie/image_files
        """
        source_path = Path(source_dir)
        organized_data = {}
        duplicates_detected = []
        
        print(f"Traitement du dossier source: {source_path}")
        
        for category_folder in source_path.iterdir():
            if category_folder.is_dir():
                original_name = category_folder.name
                cleaned_name = self.clean_folder_name(original_name)
                
                # Vérifier les doublons potentiels
                if self.is_potential_duplicate(cleaned_name, organized_data.keys()):
                    duplicates_detected.append((original_name, cleaned_name))
                    print(f"⚠️  DOUBLON DÉTECTÉ: {original_name} -> {cleaned_name}")
                    continue
                
                # Créer le dossier de destination
                category_path = self.images_dir / cleaned_name
                category_path.mkdir(exist_ok=True)
                
                images_info = []
                image_count = 0
                
                # Traiter toutes les images de la catégorie
                for image_file in category_folder.iterdir():
                    if self.is_image_file(image_file):
                        image_count += 1
                        
                        # Générer un nom unique pour l'image
                        new_image_name = f"{cleaned_name}_{image_count:03d}{image_file.suffix.lower()}"
                        new_image_path = category_path / new_image_name
                        
                        # Copier le fichier
                        import shutil
                        shutil.copy2(image_file, new_image_path)
                        
                        # Extraire les métadonnées
                        image_metadata = self.extract_image_metadata(new_image_path)
                        image_metadata.update({
                            'category_name': cleaned_name,
                            'original_category_name': original_name,
                            'original_filename': image_file.name,
                            'processed_filename': new_image_name,
                            'relative_path': f"images/{cleaned_name}/{new_image_name}",
                            'image_number': image_count
                        })
                        
                        # Ajouter les informations de la catégorie
                        category_info = self.get_category_info(cleaned_name)
                        image_metadata.update(category_info)
                        
                        images_info.append(image_metadata)
                
                organized_data[cleaned_name] = {
                    'images': images_info,
                    'total_images': len(images_info),
                    'category_info': self.get_category_info(cleaned_name),
                    'original_folder_name': original_name
                }
                
                print(f"✓ {cleaned_name}: {len(images_info)} images traitées")
        
        # Rapport sur les doublons
        if duplicates_detected:
            print(f"\n⚠️  DOUBLONS DÉTECTÉS ET IGNORÉS:")
            for original, cleaned in duplicates_detected:
                print(f"   - {original}")
        
        # Sauvegarder l'index général
        self.save_categories_index(organized_data, duplicates_detected)
        return organized_data
    
    def is_potential_duplicate(self, cleaned_name, existing_names):
        """Détecter si une catégorie est un doublon potentiel"""
        for existing in existing_names:
            # Vérifier la similarité (distance de Levenshtein simple)
            if self.similar_strings(cleaned_name, existing):
                return True
        return False
    
    def similar_strings(self, s1, s2, threshold=0.8):
        """Vérifier si deux chaînes sont similaires"""
        if len(s1) == 0 or len(s2) == 0:
            return False
        
        # Calcul de similarité simple
        set1 = set(s1.split('_'))
        set2 = set(s2.split('_'))
        
        intersection = len(set1.intersection(set2))
        union = len(set1.union(set2))
        
        similarity = intersection / union if union > 0 else 0
        return similarity >= threshold
    
    def get_category_info(self, category_name):
        """Obtenir les informations sur une catégorie d'aliment"""
        category_key = category_name.lower()
        
        # Chercher une correspondance exacte
        if category_key in self.category_mapping:
            return self.category_mapping[category_key]
        
        # Chercher une correspondance partielle
        for key, info in self.category_mapping.items():
            if key in category_key or any(word in category_key for word in key.split('_')):
                return info
        
        # Par défaut
        return self.category_mapping["default"]
    
    def is_image_file(self, file_path):
        """Vérifier si le fichier est une image supportée"""
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
                    'mode': img.mode
                })
                
                # Calculer le ratio d'aspect
                if img.height > 0:
                    metadata['aspect_ratio'] = round(img.width / img.height, 2)
        
        except Exception as e:
            print(f"Erreur lors de l'extraction des métadonnées pour {image_path}: {e}")
        
        return metadata
    
    def save_categories_index(self, organized_data, duplicates_detected):
        """Sauvegarder l'index des catégories organisées"""
        index_file = self.metadata_dir / "food_categories_index.json"
        
        # Créer un résumé pour l'index
        summary = {
            'total_categories': len(organized_data),
            'total_images': sum(cat['total_images'] for cat in organized_data.values()),
            'processing_date': datetime.now().isoformat(),
            'duplicates_detected': duplicates_detected,
            'categories': organized_data
        }
        
        with open(index_file, 'w', encoding='utf-8') as f:
            json.dump(summary, f, indent=2, ensure_ascii=False)
        
        print(f"Index des catégories sauvegardé dans {index_file}")
        
        # Créer un résumé lisible
        summary_file = self.metadata_dir / "categories_summary.txt"
        with open(summary_file, 'w', encoding='utf-8') as f:
            f.write("RÉSUMÉ DES CATÉGORIES D'ALIMENTS TRAITÉES\n")
            f.write("=" * 50 + "\n\n")
            f.write(f"Nombre total de catégories: {len(organized_data)}\n")
            f.write(f"Nombre total d'images: {sum(cat['total_images'] for cat in organized_data.values())}\n\n")
            
            if duplicates_detected:
                f.write("DOUBLONS DÉTECTÉS ET IGNORÉS:\n")
                for original, cleaned in duplicates_detected:
                    f.write(f"- {original}\n")
                f.write("\n")
            
            f.write("CATÉGORIES TRAITÉES:\n")
            for category_name, category_data in organized_data.items():
                f.write(f"\n{category_name.upper()}\n")
                f.write(f"  - Images: {category_data['total_images']}\n")
                f.write(f"  - Classe OWL: {category_data['category_info']['class']}\n")
                f.write(f"  - Type: {category_data['category_info']['type']}\n")
                f.write(f"  - Ingrédient principal: {category_data['category_info']['main_ingredient']}\n")
                f.write(f"  - Dossier original: {category_data['original_folder_name']}\n")
        
        print(f"Résumé lisible sauvegardé dans {summary_file}")
    
    def create_nutritional_templates_for_categories(self, organized_data):
        """Créer des templates nutritionnels pour les catégories d'aliments"""
        
        # Headers pour le fichier CSV
        headers = [
            'food_name', 'category', 'owl_class', 'food_type', 'main_ingredient',
            'calories_per_100g', 'proteins', 'carbohydrates', 'fats', 'fiber',
            'sodium', 'potassium', 'calcium', 'iron', 'vitamin_c', 'vitamin_a',
            'sugar', 'saturated_fat', 'cholesterol', 'water_content',
            'ingredients', 'allergens', 'origin', 'preparation_method',
            'storage_tips', 'serving_size', 'description'
        ]
        
        # Créer le fichier CSV principal
        csv_file = self.nutritional_dir / "food_categories_nutritional.csv"
        with open(csv_file, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            writer.writerow(headers)
            
            # Ajouter une ligne pour chaque catégorie traitée
            for category_name, category_data in organized_data.items():
                category_info = category_data['category_info']
                
                # Valeurs par défaut basées sur le type d'aliment
                default_values = self.get_default_nutritional_values(category_info)
                
                example_row = [
                    category_name,  # food_name
                    category_info['type'],  # category
                    category_info['class'],  # owl_class
                    category_info['type'],   # food_type
                    category_info['main_ingredient'],  # main_ingredient
                    default_values['calories'],  # calories_per_100g
                    default_values['proteins'],  # proteins
                    default_values['carbohydrates'],  # carbohydrates
                    default_values['fats'],    # fats
                    default_values['fiber'],   # fiber
                    default_values['sodium'],  # sodium
                    default_values['potassium'],  # potassium
                    default_values['calcium'],   # calcium
                    default_values['iron'],     # iron
                    default_values['vitamin_c'], # vitamin_c
                    default_values['vitamin_a'], # vitamin_a
                    default_values['sugar'],    # sugar
                    default_values['saturated_fat'], # saturated_fat
                    default_values['cholesterol'],   # cholesterol
                    default_values['water_content'], # water_content
                    category_info['main_ingredient'],  # ingredients
                    "",   # allergens (à remplir)
                    "Various",  # origin
                    self.get_preparation_method(category_info),  # preparation_method
                    self.get_storage_tips(category_info),        # storage_tips
                    "100g",      # serving_size
                    f"Catégorie d'aliment: {category_name.replace('_', ' ')}"  # description
                ]
                writer.writerow(example_row)
        
        print(f"Template nutritionnel créé: {csv_file}")
        print(f"  - {len(organized_data)} catégories à renseigner")
        
        # Créer un guide de remplissage spécialisé
        guide_file = self.nutritional_dir / "guide_remplissage_categories.md"
        with open(guide_file, 'w', encoding='utf-8') as f:
            f.write("# Guide de remplissage des données nutritionnelles\n\n")
            f.write("## Catégories d'aliments à renseigner:\n\n")
            
            # Grouper par type
            types_groups = {}
            for category_name, category_data in organized_data.items():
                food_type = category_data['category_info']['type']
                if food_type not in types_groups:
                    types_groups[food_type] = []
                types_groups[food_type].append(category_name)
            
            for food_type, categories in types_groups.items():
                f.write(f"### {food_type.upper()}\n")
                for category in categories:
                    f.write(f"- [ ] {category}\n")
                f.write("\n")
            
            f.write("## Instructions spéciales par type:\n\n")
            f.write("### Bouillons et soupes\n")
            f.write("- Attention au sodium (souvent élevé)\n")
            f.write("- Protéines variables selon la base\n\n")
            
            f.write("### Café et thé\n")
            f.write("- Calories très faibles sauf avec sucre/lait\n")
            f.write("- Noter la caféine dans les commentaires\n\n")
            
            f.write("### Noix et graines\n")
            f.write("- Lipides élevés (bons gras)\n")
            f.write("- Protéines importantes\n")
            f.write("- Attention aux allergènes\n\n")
            
            f.write("## Sources recommandées:\n")
            f.write("- USDA FoodData Central: https://fdc.nal.usda.gov/\n")
            f.write("- Table Ciqual (France): https://ciqual.anses.fr/\n")
            f.write("- Bases de données nutritionnelles spécialisées\n")
        
        print(f"Guide de remplissage créé: {guide_file}")
    
    def get_default_nutritional_values(self, category_info):
        """Obtenir des valeurs nutritionnelles par défaut selon le type"""
        food_type = category_info['type']
        
        defaults = {
            'bouillon': {'calories': 10, 'proteins': 1, 'carbohydrates': 1, 'fats': 0.5, 'fiber': 0, 'sodium': 800, 'potassium': 50, 'calcium': 5, 'iron': 0.1, 'vitamin_c': 0, 'vitamin_a': 0, 'sugar': 0, 'saturated_fat': 0.1, 'cholesterol': 0, 'water_content': 95},
            'boisson_chaude': {'calories': 5, 'proteins': 0.1, 'carbohydrates': 1, 'fats': 0, 'fiber': 0, 'sodium': 5, 'potassium': 30, 'calcium': 5, 'iron': 0, 'vitamin_c': 0, 'vitamin_a': 0, 'sugar': 0, 'saturated_fat': 0, 'cholesterol': 0, 'water_content': 98},
            'noix': {'calories': 600, 'proteins': 15, 'carbohydrates': 10, 'fats': 50, 'fiber': 8, 'sodium': 5, 'potassium': 400, 'calcium': 50, 'iron': 2, 'vitamin_c': 0, 'vitamin_a': 0, 'sugar': 3, 'saturated_fat': 8, 'cholesterol': 0, 'water_content': 5},
            'fruit_sec': {'calories': 280, 'proteins': 2, 'carbohydrates': 75, 'fats': 0.4, 'fiber': 7, 'sodium': 5, 'potassium': 650, 'calcium': 40, 'iron': 1, 'vitamin_c': 2, 'vitamin_a': 10, 'sugar': 60, 'saturated_fat': 0.1, 'cholesterol': 0, 'water_content': 20},
            'cereale': {'calories': 350, 'proteins': 7, 'carbohydrates': 80, 'fats': 1, 'fiber': 3, 'sodium': 5, 'potassium': 150, 'calcium': 10, 'iron': 1.5, 'vitamin_c': 0, 'vitamin_a': 0, 'sugar': 1, 'saturated_fat': 0.2, 'cholesterol': 0, 'water_content': 12},
            'jus': {'calories': 40, 'proteins': 1, 'carbohydrates': 9, 'fats': 0.2, 'fiber': 0.5, 'sodium': 10, 'potassium': 200, 'calcium': 10, 'iron': 0.5, 'vitamin_c': 20, 'vitamin_a': 50, 'sugar': 8, 'saturated_fat': 0, 'cholesterol': 0, 'water_content': 90}
        }
        
        return defaults.get(food_type, defaults['cereale'])  # Valeur par défaut: céréale
    
    def get_preparation_method(self, category_info):
        """Obtenir la méthode de préparation selon le type"""
        methods = {
            'bouillon': 'Cuisson lente en bouillon',
            'boisson_chaude': 'Infusion à chaud',
            'noix': 'Séchage naturel ou grillage',
            'fruit_sec': 'Séchage naturel',
            'cereale': 'Séchage et polissage',
            'jus': 'Extraction et pasteurisation'
        }
        return methods.get(category_info['type'], 'Préparation variable')
    
    def get_storage_tips(self, category_info):
        """Obtenir les conseils de conservation selon le type"""
        tips = {
            'bouillon': 'Réfrigérer et consommer rapidement',
            'boisson_chaude': 'Consommer immédiatement',
            'noix': 'Conserver dans un endroit sec et frais',
            'fruit_sec': 'Conserver dans un endroit sec',
            'cereale': 'Conserver dans un endroit sec, à l\'abri des insectes',
            'jus': 'Réfrigérer après ouverture'
        }
        return tips.get(category_info['type'], 'Suivre les instructions du fabricant')

def main():
    # Initialiser le processeur
    processor = FoodCategoriesProcessor()
    
    print("=== Processeur de catégories d'aliments ===")
    
    # Demander le chemin vers les images
    source_directory = input("Chemin vers votre dossier Collection-d'images (ex: ./Collection-d'images): ").strip()
    
    if not source_directory:
        source_directory = "./Collection-d'images"
    
    if os.path.exists(source_directory):
        print(f"1. Traitement des catégories depuis {source_directory}...")
        organized_data = processor.organize_food_categories(source_directory)
        
        print("2. Création des templates nutritionnels...")
        processor.create_nutritional_templates_for_categories(organized_data)
        
        print(f"\n=== RÉSUMÉ ===")
        print(f"Catégories traitées: {len(organized_data)}")
        total_images = sum(cat['total_images'] for cat in organized_data.values())
        print(f"Images totales: {total_images}")
        
        # Statistiques par type
        print(f"\nCatégories par type:")
        type_counts = {}
        for cat_data in organized_data.values():
            cat_type = cat_data['category_info']['type']
            type_counts[cat_type] = type_counts.get(cat_type, 0) + 1
        
        for cat_type, count in type_counts.items():
            print(f"- {cat_type}: {count} catégories")
        
        print(f"\nFichiers créés:")
        print(f"- Index: ./data/metadata/food_categories_index.json")
        print(f"- Résumé: ./data/metadata/categories_summary.txt") 
        print(f"- Template nutritionnel: ./data/nutritional/food_categories_nutritional.csv")
        print(f"- Guide: ./data/nutritional/guide_remplissage_categories.md")
        
        print(f"\nPROCHAINE ÉTAPE:")
        print(f"1. Vérifiez le fichier de résumé pour les doublons détectés")
        print(f"2. Ouvrez le fichier food_categories_nutritional.csv")
        print(f"3. Complétez les données nutritionnelles pour vos catégories")
        print(f"4. Exécutez ensuite le script de population de l'ontologie")
        
    else:
        print(f" Dossier '{source_directory}' non trouvé.")
        print("Vérifiez le chemin vers votre dossier Collection-d'images.")

if __name__ == "__main__":
    main()