#!/usr/bin/env python3
"""
Population pour Fuseki 5.4.0 avec endpoints corrects
"""

import json
import csv
import os
from pathlib import Path
import requests
import time

class AfricanMiddleEasternPopulatorFixed:
    def __init__(self):
        self.fuseki_server = "http://localhost:3030"
        self.dataset_name = "african-middle-eastern-kg"
        
        # ENDPOINTS CORRECTS POUR FUSEKI 5.4.0
        self.update_endpoint = f"{self.fuseki_server}/{self.dataset_name}/update"
        self.query_endpoint = f"{self.fuseki_server}/{self.dataset_name}/query"  # CHANGÉ
        self.data_endpoint = f"{self.fuseki_server}/{self.dataset_name}/data"
        
        self.food_ns = "http://example.org/food-ontology#"
        self.foods_added = 0
        self.images_added = 0
        self.errors = []
    
    def test_endpoints(self):
        """Test des endpoints Fuseki 5.4.0"""
        print("🔍 Test des endpoints Fuseki 5.4.0...")
        
        endpoints = {
            'query': self.query_endpoint,
            'update': self.update_endpoint,
            'data': self.data_endpoint
        }
        
        for name, url in endpoints.items():
            try:
                if name == 'update':
                    # Test avec une requête vide pour l'update
                    response = requests.post(url, 
                        data="", 
                        headers={'Content-Type': 'application/sparql-update'},
                        timeout=5)
                else:
                    response = requests.get(url, timeout=5)
                
                print(f"   {name}: {response.status_code} ({'✅' if response.status_code in [200, 400] else '❌'})")
                
            except Exception as e:
                print(f"   {name}: ❌ {e}")
        
        # Test requête simple
        return self.test_simple_query()
    
    def test_simple_query(self):
        """Test d'une requête SPARQL simple"""
        query = "SELECT * WHERE { ?s ?p ?o } LIMIT 1"
        
        try:
            response = requests.post(
                self.query_endpoint,  # Utilise /query au lieu de /sparql
                data={'query': query},
                headers={'Accept': 'application/sparql-results+json'},
                timeout=10
            )
            
            print(f"🧪 Test requête simple: {response.status_code}")
            
            if response.status_code == 200:
                try:
                    result = response.json()
                    bindings = result.get('results', {}).get('bindings', [])
                    print(f"   ✅ Requête OK: {len(bindings)} résultats")
                    return True
                except:
                    print(f"   ✅ Requête OK (pas de données)")
                    return True
            else:
                print(f"   ❌ Erreur: {response.text[:100]}")
                return False
                
        except Exception as e:
            print(f"   ❌ Exception: {e}")
            return False
    
    def safe_string(self, value):
        """Échapper pour SPARQL"""
        if not value:
            return ""
        return str(value).replace('"', '\\"').replace('\n', ' ').replace('\r', ' ')
    
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
        """Exécuter une mise à jour SPARQL avec Fuseki 5.4.0"""
        try:
            headers = {
                'Content-Type': 'application/sparql-update',
                'Accept': 'text/plain'
            }
            
            response = requests.post(
                self.update_endpoint,
                data=query.encode('utf-8'),
                headers=headers,
                timeout=30
            )
            
            # Fuseki 5.4.0 codes de succès
            if response.status_code in [200, 201, 204]:
                return True
            else:
                error_msg = f"HTTP {response.status_code}: {response.text[:200]}"
                self.errors.append(error_msg)
                return False
                
        except Exception as e:
            error_msg = f"Erreur SPARQL: {str(e)}"
            self.errors.append(error_msg)
            return False
    
    def add_food_with_specialization(self, food_data, images_list):
        """Ajouter un plat avec ses spécificités"""
        food_name = food_data.get('food_name', '').strip()
        if not food_name:
            return False
        
        # Informations spécialisées
        region = food_data.get('region', 'Unknown')
        cooking_method = food_data.get('cooking_method', 'cooked')
        owl_class = food_data.get('owl_class', 'Food')
        spice_level = food_data.get('spice_level', 'medium')
        
        print(f"🍽️ {food_name}")
        print(f"    → Région: {region}")
        print(f"    → Classe: {owl_class}")
        print(f"    → Méthode: {cooking_method}")
        print(f"    → Niveau d'épices: {spice_level}")
        
        # URIs
        food_uri = f"<{self.create_uri(food_name)}>"
        class_uri = f"<{self.food_ns}{owl_class}>"
        
        # Requête SPARQL pour Fuseki 5.4.0
        query = f"""
        PREFIX : <{self.food_ns}>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        
        INSERT DATA {{
            {food_uri} a {class_uri} ;
                       :name "{self.safe_string(food_name)}" ;
                       :description "{self.safe_string(food_data.get('description', f'Plat traditionnel {region}'))}" ;
                       :region "{self.safe_string(region)}" ;
                       :cookingMethod "{self.safe_string(cooking_method)}" ;
                       :spiceLevel "{self.safe_string(spice_level)}" ;
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
        
        # Signification culturelle
        cultural_significance = food_data.get('cultural_significance', '')
        if cultural_significance:
            query += f'                       :culturalSignificance "{self.safe_string(cultural_significance)}" ;\n'
        
        # Retirer le dernier point-virgule
        query = query.rstrip(' ;\n') + ' .\n'
        
        # Ajouter les ingrédients
        ingredients_str = food_data.get('ingredients', '')
        if ingredients_str:
            ingredients = [ing.strip() for ing in ingredients_str.split(',')]
            for ingredient in ingredients[:5]:  # Max 5 ingrédients
                if ingredient and len(ingredient) > 1:
                    ingredient_uri = f"<{self.create_uri(ingredient, 'ingredient_')}>"
                    query += f"""
            {ingredient_uri} a :Ingredient ;
                            :name "{self.safe_string(ingredient)}" .
            {food_uri} :contains {ingredient_uri} .
            """
        
        # Ajouter les images
        category_name = food_name.replace(' ', '_').lower()
        food_images = [img for img in images_list if img.get('category_name') == category_name]
        
        for i, img in enumerate(food_images[:3]):  # Max 3 images par plat
            image_id = img.get('image_id', f"{category_name}_{i}")
            image_uri = f"<{self.create_uri(image_id, 'image_')}>"
            
            query += f"""
            {image_uri} a :FoodImage ;
                        :imagePath "{self.safe_string(img.get('relative_path', ''))}" ;
                        :filename "{self.safe_string(img.get('filename', ''))}" .
            {food_uri} :hasImage {image_uri} .
            """
            
            self.images_added += 1
        
        query += "}"
        
        # Exécuter avec Fuseki 5.4.0
        success = self.execute_sparql_update(query)
        if success:
            self.foods_added += 1
            print(f"     ✅ Ajouté avec {len(food_images[:3])} images")
        else:
            print(f"     ❌ Échec")
        
        return success
    
    def populate_knowledge_graph(self, data_dir="african_middle_eastern_data"):
        """Population complète avec Fuseki 5.4.0"""
        print("🌍 POPULATION AVEC FUSEKI 5.4.0")
        print("=" * 50)
        
        # Test des endpoints
        if not self.test_endpoints():
            print("❌ Endpoints non fonctionnels")
            return False
        
        # Vérifier les données
        data_path = Path(data_dir)
        if not data_path.exists():
            print(f"❌ Dossier {data_dir} non trouvé!")
            return False
        
        # Charger CSV
        nutrition_file = data_path / "nutritional" / "african_middle_eastern_nutritional.csv"
        if not nutrition_file.exists():
            print(f"❌ CSV non trouvé: {nutrition_file}")
            return False
        
        nutritional_data = []
        with open(nutrition_file, 'r', encoding='utf-8') as f:
            reader = csv.DictReader(f)
            for row in reader:
                if row.get('food_name') and row['food_name'].strip():
                    nutritional_data.append(row)
        
        print(f"🍽️ {len(nutritional_data)} plats à traiter")
        
        # Charger images
        images_file = data_path / "metadata" / "african_middle_eastern_food_index.json"
        all_images = []
        if images_file.exists():
            with open(images_file, 'r', encoding='utf-8') as f:
                images_data = json.load(f)
            
            for cat_data in images_data.get('categories', {}).values():
                all_images.extend(cat_data.get('images', []))
            
            print(f"🖼️ {len(all_images)} images disponibles")
        
        # Population
        print(f"\n📝 Population en cours...")
        print("-" * 40)
        
        start_time = time.time()
        
        for i, food_data in enumerate(nutritional_data, 1):
            print(f"\n[{i}/{len(nutritional_data)}]")
            success = self.add_food_with_specialization(food_data, all_images)
            
            # Pause pour Fuseki 5.4.0
            if i % 3 == 0:
                time.sleep(0.5)
        
        end_time = time.time()
        duration = end_time - start_time
        
        # Résumé
        print(f"\n" + "=" * 50)
        print(f"🌍 POPULATION TERMINÉE (Fuseki 5.4.0)")
        print(f"⏱️ Durée: {duration:.1f} secondes")
        print(f"🍽️ Plats ajoutés: {self.foods_added}/{len(nutritional_data)}")
        print(f"🖼️ Images liées: {self.images_added}")
        
        if self.errors:
            print(f"❌ Erreurs: {len(self.errors)}")
            for error in self.errors[:3]:
                print(f"   - {error}")
        
        return True
    
    def verify_knowledge_graph(self):
        """Vérification avec endpoints Fuseki 5.4.0"""
        print(f"\n🔍 VÉRIFICATION (Fuseki 5.4.0)")
        print("=" * 40)
        
        try:
            # Test 1: Compter les plats
            query1 = f"""
            PREFIX : <{self.food_ns}>
            SELECT (COUNT(?food) as ?count) WHERE {{
                ?food :name ?name .
            }}
            """
            
            response1 = requests.post(
                self.query_endpoint,  # Utilise /query
                data={'query': query1},
                headers={'Accept': 'application/sparql-results+json'}
            )
            
            if response1.status_code == 200:
                result1 = response1.json()
                count = result1['results']['bindings'][0]['count']['value']
                print(f"📊 Plats dans le graphe: {count}")
            
            # Test 2: Par région
            query2 = f"""
            PREFIX : <{self.food_ns}>
            SELECT ?region (COUNT(?food) as ?count) WHERE {{
                ?food :region ?region .
            }}
            GROUP BY ?region
            ORDER BY DESC(?count)
            """
            
            response2 = requests.post(
                self.query_endpoint,
                data={'query': query2},
                headers={'Accept': 'application/sparql-results+json'}
            )
            
            if response2.status_code == 200:
                result2 = response2.json()
                print(f"\n🗺️ Répartition par région:")
                for binding in result2['results']['bindings']:
                    region = binding['region']['value']
                    count = binding['count']['value']
                    print(f"   - {region}: {count} plats")
            
        except Exception as e:
            print(f"❌ Erreur vérification: {e}")

def main():
    print("🚀 POPULATION FUSEKI 5.4.0 - VERSION CORRIGÉE")
    print("=" * 60)
    
    # Vérifier dossier
    data_dir = input("📁 Dossier data (Entrée pour 'african_middle_eastern_data'): ").strip()
    if not data_dir:
        data_dir = "african_middle_eastern_data"
    
    if not os.path.exists(data_dir):
        print(f"❌ Dossier {data_dir} non trouvé!")
        return
    
    # Exécuter
    populator = AfricanMiddleEasternPopulatorFixed()
    
    success = populator.populate_knowledge_graph(data_dir)
    
    if success:
        populator.verify_knowledge_graph()
        
        print(f"\n🎉 SUCCÈS AVEC FUSEKI 5.4.0!")
        print(f"✅ Graphe culinaire spécialisé créé")
        print(f"🌐 Interface: http://localhost:3030")
        print(f"📊 Dataset: african-middle-eastern-kg")
        
    else:
        print(f"\n❌ Échec de la population")

if __name__ == "__main__":
    main()