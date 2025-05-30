#!/usr/bin/env python3
"""
Population du graphe de connaissances avec l'ontologie complète
Dataset: food-kg-v2 (avec ontologie déjà chargée)
"""

import json
import csv
import os
from pathlib import Path
from SPARQLWrapper import SPARQLWrapper, POST
import requests
import uuid
import time

class RichOntologyPopulator:
    def __init__(self, fuseki_server="http://localhost:3030", dataset_name="food-kg-v2"):
        self.fuseki_server = fuseki_server
        self.dataset_name = dataset_name
        self.update_endpoint = f"{fuseki_server}/{dataset_name}/update"
        self.query_endpoint = f"{fuseki_server}/{dataset_name}/sparql"
        
        self.sparql = SPARQLWrapper(self.update_endpoint)
        self.sparql.setMethod(POST)
        
        self.food_ns = "http://example.org/food-ontology#"
        
        # Mapping intelligent vers les classes de l'ontologie
        self.category_to_owl_class = {
            # Boissons - Café (toutes variantes)
            "coffee_with_milk_and_with_sugar": "Coffee",
            "coffee_with_milk_without_sugar": "Coffee",
            "coffee_without_milk_and_with_sugar": "Coffee",
            
            # Boissons - Thé (toutes variantes)
            "cup_of_tea": "Tea",
            "sweet_tea": "Tea", 
            "tea_without_sugar_and_with_milk": "Tea",
            
            # Bouillons = CookedFood (plats cuisinés liquides)
            "beef_broth_with_oil": "CookedFood",
            "beef_broth_without_oil": "CookedFood",
            "cabbage_broth": "CookedFood",
            
            # Aliments crus
            "cuttlefish_raw": "RawFood",
            "groundnut": "RawFood",           # Arachides brutes
            "dry_peant_unsalted": "RawFood",  # Arachides non salées = brutes
            
            # Fruits
            "date_fruits": "Fruit",
            
            # Légumes (si vous en avez)
            # "tomato_fresh": "Vegetables",
            
            # Aliments manufacturés/transformés
            "groundnut_clusteri": "ManufacturedFood",          # Arachides en clusters
            "dried_hazelnut": "ManufacturedFood",              # Noisettes séchées
            "dried_octopus_food": "ManufacturedFood",          # Poulpe séché
            "dried_sesame_and_seed_food": "ManufacturedFood",  # Graines transformées
            "dried_walnuts_food": "ManufacturedFood",          # Noix séchées
            "tomato_ketchup": "ManufacturedFood",              # Sauce industrielle
            
            # Aliments cuisinés (grillés, rôtis)
            "dry_roasted_mixed_nuts_food": "CookedFood",       # Noix grillées
            "dry_peant_roasted_and_salted": "CookedFood",      # Arachides grillées
            "halwa": "CookedFood",                             # Dessert cuisiné
            
            # Céréales brutes
            "dry_rice_and_polished_rice_food": "RawFood",      # Riz brut
            
            # Jus naturels
            "tomato_juice": "NaturalBeverage",
            
            # Classe par défaut
            "default": "Food"
        }
        
        # Groupes alimentaires nutritionnels
        self.category_to_food_group = {
            "coffee_": "Beverages",
            "tea": "Beverages",
            "broth": "Soups_Broths", 
            "groundnut": "Legumes_Nuts",
            "peant": "Legumes_Nuts",
            "nuts": "Legumes_Nuts",
            "hazelnut": "Legumes_Nuts",
            "walnuts": "Legumes_Nuts",
            "sesame": "Seeds_Grains",
            "rice": "Cereals_Grains",
            "fruit": "Fruits",
            "date": "Fruits",
            "juice": "Beverages",
            "ketchup": "Condiments_Sauces",
            "halwa": "Desserts_Sweets",
            "octopus": "Seafood",
            "cuttlefish": "Seafood"
        }
        
        # Ingrédients principaux détectés automatiquement
        self.category_to_main_ingredients = {
            "coffee_": ["coffee", "water","sugar or not","milk or not"],
            "tea": ["tea", "water","melon","glasses"],
            "beef_broth": ["beef", "water","oil or not","carote","tomato","epices","salt","cloves","ginger","onion","pepper"],
            "cabbage_broth": ["cabbage", "water","carote","tomato"],
            "groundnut": ["peanuts"],
            "peant": ["peanuts"],
            "dried_hazelnut": ["hazelnuts"],
            "dried_walnuts": ["walnuts"],
            "sesame": ["sesame seeds"],
            "rice": ["rice"],
            "tomato_juice": ["tomatoes","water","sugar"],
            "tomato_ketchup": ["tomatoes", "sugar", "vinegar","salt"],
            "cuttlefish": ["cuttlefish","lemond","salt"],
            "octopus": ["octopus","salt","salad"],
            "date": ["dates"]
        }
        
        # Compteurs
        self.foods_added = 0
        self.images_added = 0
        self.food_groups_created = 0
        self.errors = []
    
    def get_owl_class(self, food_name):
        """Déterminer la classe OWL appropriée"""
        name_lower = food_name.lower().replace(" ", "_")
        
        # Recherche exacte
        if name_lower in self.category_to_owl_class:
            return self.category_to_owl_class[name_lower]
        
        # Recherche par patterns
        for pattern, owl_class in self.category_to_owl_class.items():
            if pattern in name_lower or any(word in name_lower for word in pattern.split("_")):
                return owl_class
        
        return "Food"  # Classe racine par défaut
    
    def get_food_group(self, food_name):
        """Déterminer le groupe alimentaire"""
        name_lower = food_name.lower()
        
        for pattern, group in self.category_to_food_group.items():
            if pattern in name_lower:
                return group
        
        return "Other_Foods"
    
    def get_main_ingredients(self, food_name):
        """Détecter les ingrédients principaux"""
        name_lower = food_name.lower()
        
        for pattern, ingredients in self.category_to_main_ingredients.items():
            if pattern in name_lower:
                return ingredients
        
        # Extraction basique du nom
        clean_name = food_name.replace("_", " ").replace("with", "").replace("without", "")
        words = [w for w in clean_name.split() if len(w) > 3]
        return words[:2] if words else [food_name]
    
    def safe_string(self, value):
        """Échapper pour SPARQL"""
        if not value:
            return ""
        return str(value).replace('"', '\\"').replace('\n', ' ').replace('\r', ' ').replace('\\', '\\\\')
    
    def is_number(self, value):
        """Vérifier si numérique"""
        try:
            float(value)
            return True
        except (ValueError, TypeError):
            return False
    
    def create_uri(self, name, prefix=""):
        """Créer un URI propre"""
        clean_name = "".join(c if c.isalnum() else "_" for c in str(name))
        clean_name = clean_name.strip("_")
        return f"{self.food_ns}{prefix}{clean_name}"
    
    def execute_sparql_update(self, query):
        """Exécuter requête SPARQL UPDATE"""
        try:
            self.sparql.setQuery(query)
            self.sparql.query()
            return True
        except Exception as e:
            error_msg = f"Erreur SPARQL: {str(e)[:100]}..."
            self.errors.append(error_msg)
            return False
    
    def create_food_group_if_needed(self, group_name):
        """Créer un groupe alimentaire s'il n'existe pas"""
        group_uri = f"<{self.create_uri(group_name, 'group_')}>"
        
        query = f"""
        PREFIX : <{self.food_ns}>
        
        INSERT DATA {{
            {group_uri} a :FoodGroup ;
                        :name "{self.safe_string(group_name.replace('_', ' '))}" ;
                        :description "Groupe alimentaire: {self.safe_string(group_name.replace('_', ' '))}" .
        }}
        """
        
        success = self.execute_sparql_update(query)
        if success:
            self.food_groups_created += 1
        return success
    
    def add_food_with_complete_ontology(self, food_data, images_list):
        """Ajouter un aliment avec l'ontologie complète"""
        food_name = food_data.get('food_name', '').strip()
        if not food_name:
            return False
        
        # Déterminer la classification
        owl_class = self.get_owl_class(food_name)
        food_group = self.get_food_group(food_name)
        main_ingredients = self.get_main_ingredients(food_name)
        
        print(f"  {food_name}")
        print(f"    → Classe: {owl_class}")
        print(f"    → Groupe: {food_group}")
        print(f"    → Ingrédients: {', '.join(main_ingredients)}")
        
        # URIs
        food_uri = f"<{self.create_uri(food_name)}>"
        class_uri = f"<{self.food_ns}{owl_class}>"
        group_uri = f"<{self.create_uri(food_group, 'group_')}>"
        
        # Créer le groupe alimentaire
        self.create_food_group_if_needed(food_group)
        
        # Construire la requête principale
        query = f"""
        PREFIX : <{self.food_ns}>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        
        INSERT DATA {{
            # Aliment principal avec sa classe OWL
            {food_uri} a {class_uri} ;
                       :name "{self.safe_string(food_name)}" ;
                       :description "{self.safe_string(food_data.get('description', f'Aliment de type {owl_class}'))}" ;
                       :belongsTo {group_uri} ;
        """
        
        # Propriétés nutritionnelles
        nutritional_mapping = {
            'calories_per_100g': 'calories',
            'proteins': 'protein',
            'carbohydrates': 'carbohydrates',
            'fats': 'fat',
            'fiber': 'fiber',
            'sodium': 'sodium',
            'sugar': 'sugar'
        }
        
        for csv_prop, onto_prop in nutritional_mapping.items():
            value = food_data.get(csv_prop, '')
            if value and self.is_number(value) and float(value) >= 0:
                query += f'                       :{onto_prop} {float(value)} ;\n'
        
        # Retirer le dernier point-virgule
        query = query.rstrip(' ;\n') + ' .\n'
        
        # Ajouter les ingrédients
        for ingredient in main_ingredients:
            if ingredient and len(ingredient) > 2:
                ingredient_uri = f"<{self.create_uri(ingredient, 'ingredient_')}>"
                query += f"""
            # Ingrédient: {ingredient}
            {ingredient_uri} a :Ingredient ;
                            :name "{self.safe_string(ingredient)}" .
            {food_uri} :contains {ingredient_uri} .
            """
        
        # Ajouter les images
        category_name = food_name.replace(' ', '_').lower()
        food_images = [img for img in images_list if img.get('category_name') == category_name]
        
        for i, img in enumerate(food_images[:8]):  # Max 8 images par aliment
            image_id = img.get('image_id', f"{category_name}_{i}")
            image_uri = f"<{self.create_uri(image_id, 'image_')}>"
            
            query += f"""
            # Image {i+1}
            {image_uri} a :FoodImage ;
                        :imagePath "{self.safe_string(img.get('relative_path', ''))}" ;
                        :filename "{self.safe_string(img.get('filename', ''))}" .
            {food_uri} :hasImage {image_uri} .
            """
            
            # Dimensions si disponibles
            if img.get('width') and self.is_number(img['width']):
                query += f"            {image_uri} :width {int(img['width'])} .\n"
            if img.get('height') and self.is_number(img['height']):
                query += f"            {image_uri} :height {int(img['height'])} .\n"
            
            self.images_added += 1
        
        query += "}"
        
        # Exécuter
        success = self.execute_sparql_update(query)
        if success:
            self.foods_added += 1
            print(f"     Ajouté avec {len(food_images[:8])} images")
        else:
            print(f"    ❌ Échec")
        
        return success
    
    def populate_complete_knowledge_graph(self, data_dir="./data"):
        """Population complète du graphe de connaissances"""
        print("🚀 POPULATION DU GRAPHE DE CONNAISSANCES ENRICHI")
        print("=" * 60)
        
        # Charger données nutritionnelles
        nutrition_file = Path(data_dir) / "nutritional" / "food_categories_nutritional.csv"
        if not nutrition_file.exists():
            print(f"❌ CSV nutritionnel non trouvé: {nutrition_file}")
            return False
        
        nutritional_data = []
        with open(nutrition_file, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                if row.get('food_name') and row['food_name'].strip():
                    nutritional_data.append(row)
        
        print(f"📊 {len(nutritional_data)} aliments à traiter")
        
        # Charger images
        images_file = Path(data_dir) / "metadata" / "food_categories_index.json"
        all_images = []
        if images_file.exists():
            with open(images_file, 'r', encoding='utf-8') as f:
                images_data = json.load(f)
            
            for cat_data in images_data.get('categories', {}).values():
                all_images.extend(cat_data.get('images', []))
            
            print(f"🖼️  {len(all_images)} images disponibles")
        
        # Population
        print(f"\n📝 Population en cours...")
        print("-" * 40)
        
        start_time = time.time()
        
        for i, food_data in enumerate(nutritional_data, 1):
            print(f"\n[{i}/{len(nutritional_data)}]")
            success = self.add_food_with_complete_ontology(food_data, all_images)
            
            # Pause légère pour éviter surcharge
            if i % 5 == 0:
                time.sleep(0.5)
        
        end_time = time.time()
        duration = end_time - start_time
        
        # Résumé final
        print(f"\n" + "=" * 60)
        print(f" POPULATION TERMINÉE")
        print(f" Durée: {duration:.1f} secondes")
        print(f" Aliments ajoutés: {self.foods_added}/{len(nutritional_data)}")
        print(f"  Images liées: {self.images_added}")
        print(f"  Groupes alimentaires créés: {self.food_groups_created}")
        
        if self.errors:
            print(f"❌ Erreurs: {len(self.errors)}")
            print("Exemples d'erreurs:")
            for error in self.errors[:3]:
                print(f"  - {error}")
        
        return True
    
    def verify_complete_knowledge_graph(self):
        """Vérification complète du graphe enrichi"""
        print(f"\n🔍 VÉRIFICATION DU GRAPHE DE CONNAISSANCES")
        print("=" * 50)
        
        try:
            sparql_query = SPARQLWrapper(self.query_endpoint)
            sparql_query.setReturnFormat('json')
            
            # 1. Répartition par classes OWL
            query1 = f"""
            PREFIX : <{self.food_ns}>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            SELECT ?class (COUNT(?food) as ?count) WHERE {{
                ?food a ?class .
                ?class rdfs:subClassOf* :Food .
                FILTER(?class != :Food)
            }}
            GROUP BY ?class
            ORDER BY DESC(?count)
            """
            
            sparql_query.setQuery(query1)
            results1 = sparql_query.query().convert()
            
            print("📊 Répartition par classe OWL:")
            for binding in results1['results']['bindings']:
                class_name = binding['class']['value'].split('#')[-1]
                count = binding['count']['value']
                print(f"  - {class_name}: {count} aliments")
            
            # 2. Groupes alimentaires
            query2 = f"""
            PREFIX : <{self.food_ns}>
            SELECT ?groupName (COUNT(?food) as ?count) WHERE {{
                ?food :belongsTo ?group .
                ?group :name ?groupName .
            }}
            GROUP BY ?groupName
            ORDER BY DESC(?count)
            """
            
            sparql_query.setQuery(query2)
            results2 = sparql_query.query().convert()
            
            print(f"\n🗂️  Groupes alimentaires:")
            for binding in results2['results']['bindings']:
                group_name = binding['groupName']['value']
                count = binding['count']['value']
                print(f"  - {group_name}: {count} aliments")
            
            # 3. Ingrédients les plus utilisés
            query3 = f"""
            PREFIX : <{self.food_ns}>
            SELECT ?ingredientName (COUNT(?food) as ?usage) WHERE {{
                ?food :contains ?ingredient .
                ?ingredient :name ?ingredientName .
            }}
            GROUP BY ?ingredientName
            ORDER BY DESC(?usage)
            LIMIT 8
            """
            
            sparql_query.setQuery(query3)
            results3 = sparql_query.query().convert()
            
            print(f"\n🥄 Ingrédients les plus utilisés:")
            for binding in results3['results']['bindings']:
                ingredient = binding['ingredientName']['value']
                usage = binding['usage']['value']
                print(f"  - {ingredient}: {usage} aliments")
            
            # 4. Images par aliment
            query4 = f"""
            PREFIX : <{self.food_ns}>
            SELECT (COUNT(DISTINCT ?image) as ?totalImages) (COUNT(DISTINCT ?food) as ?foodsWithImages) WHERE {{
                ?food :hasImage ?image .
            }}
            """
            
            sparql_query.setQuery(query4)
            results4 = sparql_query.query().convert()
            
            binding = results4['results']['bindings'][0]
            total_images = binding['totalImages']['value']
            foods_with_images = binding['foodsWithImages']['value']
            
            print(f"\n🖼️  Images:")
            print(f"  - Total images: {total_images}")
            print(f"  - Aliments avec images: {foods_with_images}")
            
            # 5. Quelques exemples d'aliments complets
            query5 = f"""
            PREFIX : <{self.food_ns}>
            SELECT ?name ?className ?groupName ?calories WHERE {{
                ?food :name ?name ;
                      a ?class ;
                      :belongsTo ?group ;
                      :calories ?calories .
                ?group :name ?groupName .
                FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
                BIND(STRAFTER(STR(?class), "#") AS ?className)
            }}
            ORDER BY DESC(?calories)
            LIMIT 5
            """
            
            sparql_query.setQuery(query5)
            results5 = sparql_query.query().convert()
            
            print(f"\n📋 Exemples d'aliments enrichis:")
            for binding in results5['results']['bindings']:
                name = binding['name']['value']
                class_name = binding['className']['value']
                group = binding['groupName']['value']
                calories = binding['calories']['value']
                print(f"  - {name} ({class_name}, {group}): {calories} cal")
            
        except Exception as e:
            print(f"❌ Erreur lors de la vérification: {e}")

def main():
    print("🎯 POPULATION AVEC ONTOLOGIE COMPLÈTE")
    print("Dataset: food-kg-v2")
    print("=" * 50)
    
    # Vérifier les données
    data_dir = input("Dossier data (Entrée pour './data'): ").strip()
    if not data_dir:
        data_dir = "./data"
    
    if not os.path.exists(data_dir):
        print(f"❌ Dossier data non trouvé: {data_dir}")
        return
    
    # Initialiser et exécuter
    populator = RichOntologyPopulator()
    
    success = populator.populate_complete_knowledge_graph(data_dir)
    
    if success:
        populator.verify_complete_knowledge_graph()
        
        print(f"\n GRAPHE DE CONNAISSANCES COMPLET!")
        print(f"✅ Ontologie riche + Données réelles")
        print(f"✅ Classification OWL précise")
        print(f"✅ Groupes alimentaires")
        print(f"✅ Ingrédients liés")
        print(f"✅ Images multimédia")
        
        print(f"\n🌐 Interface: http://localhost:3030/#{populator.dataset_name}")
        
        print(f"\n🧪 Requêtes avancées possibles:")
        print(f"- Tous les cafés: ?x a :Coffee")
        print(f"- Aliments d'un groupe: ?x :belongsTo :group_Beverages")
        print(f"- Hiérarchie: ?x rdfs:subClassOf* :Food")
        print(f"- Ingrédients: ?x :contains ?ingredient")
        
    else:
        print(f"\n❌ Échec de la population")

if __name__ == "__main__":
    main()
