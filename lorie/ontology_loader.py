#!/usr/bin/env python3
"""
Script pour charger l'ontologie complète dans Fuseki
Dataset: food-kg-v2
"""

import requests
import os
from SPARQLWrapper import SPARQLWrapper
import json

class OntologyLoader:
    def __init__(self, fuseki_server="http://localhost:3030", dataset_name="food-kg-v2"):
        self.fuseki_server = fuseki_server
        self.dataset_name = dataset_name
        self.data_endpoint = f"{fuseki_server}/{dataset_name}/data"
        self.query_endpoint = f"{fuseki_server}/{dataset_name}/sparql"
        
    def check_dataset_exists(self):
        """Vérifier que le dataset existe"""
        try:
            response = requests.get(f"{self.fuseki_server}/$/datasets")
            if response.status_code == 200:
                datasets = response.json()
                dataset_names = [ds.get("ds.name", "").lstrip('/') for ds in datasets.get("datasets", [])]
                
                if self.dataset_name in dataset_names:
                    print(f"✅ Dataset '{self.dataset_name}' trouvé")
                    return True
                else:
                    print(f"❌ Dataset '{self.dataset_name}' non trouvé")
                    print(f"Datasets disponibles: {dataset_names}")
                    return False
            else:
                print(f"❌ Erreur lors de la vérification: {response.status_code}")
                return False
        except Exception as e:
            print(f"❌ Erreur de connexion: {e}")
            return False
    
    def load_ontology_from_file(self, ontology_file):
        """Charger l'ontologie depuis un fichier TTL"""
        print(f"📁 Lecture du fichier: {ontology_file}")
        
        if not os.path.exists(ontology_file):
            print(f"❌ Fichier non trouvé: {ontology_file}")
            return False
        
        try:
            with open(ontology_file, 'r', encoding='utf-8') as f:
                ontology_content = f.read()
            
            print(f"📄 Fichier lu - {len(ontology_content)} caractères")
            return self.load_ontology_content(ontology_content)
            
        except Exception as e:
            print(f"❌ Erreur lecture fichier: {e}")
            return False
    
    def load_ontology_content(self, ontology_content):
        """Charger le contenu de l'ontologie dans Fuseki"""
        print("🔄 Chargement de l'ontologie dans Fuseki...")
        
        headers = {
            'Content-Type': 'text/turtle',
            'Accept': 'application/json'
        }
        
        try:
            response = requests.post(
                self.data_endpoint, 
                data=ontology_content.encode('utf-8'),
                headers=headers,
                timeout=30
            )
            
            if response.status_code in [200, 201, 204]:
                print("✅ Ontologie chargée avec succès!")
                return True
            else:
                print(f"❌ Erreur HTTP: {response.status_code}")
                print(f"Réponse: {response.text[:300]}...")
                return False
                
        except requests.exceptions.Timeout:
            print("❌ Timeout - Le chargement a pris trop de temps")
            return False
        except Exception as e:
            print(f"❌ Erreur lors du chargement: {e}")
            return False
    
    def verify_ontology_loaded(self):
        """Vérifier que l'ontologie a été correctement chargée"""
        print("\n🔍 Vérification du chargement...")
        
        try:
            sparql = SPARQLWrapper(self.query_endpoint)
            sparql.setReturnFormat('json')
            
            # Test 1: Compter les classes
            query1 = """
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            SELECT (COUNT(DISTINCT ?class) as ?classCount) WHERE {
                ?class a owl:Class .
            }
            """
            
            sparql.setQuery(query1)
            result1 = sparql.query().convert()
            class_count = result1['results']['bindings'][0]['classCount']['value']
            print(f"📊 Classes OWL chargées: {class_count}")
            
            # Test 2: Compter les propriétés
            query2 = """
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            SELECT 
                (COUNT(DISTINCT ?objProp) as ?objPropCount)
                (COUNT(DISTINCT ?dataProp) as ?dataPropCount)
            WHERE {
                OPTIONAL { ?objProp a owl:ObjectProperty }
                OPTIONAL { ?dataProp a owl:DatatypeProperty }
            }
            """
            
            sparql.setQuery(query2)
            result2 = sparql.query().convert()
            binding = result2['results']['bindings'][0]
            obj_props = binding.get('objPropCount', {}).get('value', '0')
            data_props = binding.get('dataPropCount', {}).get('value', '0')
            print(f"📊 Propriétés d'objet: {obj_props}")
            print(f"📊 Propriétés de données: {data_props}")
            
            # Test 3: Vérifier quelques classes spécifiques
            query3 = """
            PREFIX : <http://example.org/food-ontology#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            SELECT ?class ?label WHERE {
                VALUES ?class { :Food :Coffee :Tea :Beverage :CookedFood :RawFood }
                ?class rdfs:label ?label .
            }
            ORDER BY ?label
            """
            
            sparql.setQuery(query3)
            result3 = sparql.query().convert()
            
            print(f"\n📋 Classes principales chargées:")
            for binding in result3['results']['bindings']:
                class_name = binding['class']['value'].split('#')[-1]
                label = binding['label']['value']
                print(f"  ✅ {class_name}: {label}")
            
            # Test 4: Vérifier la hiérarchie
            query4 = """
            PREFIX : <http://example.org/food-ontology#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            SELECT ?subclass ?superclass WHERE {
                ?subclass rdfs:subClassOf ?superclass .
                FILTER(STRSTARTS(STR(?subclass), "http://example.org/food-ontology#"))
                FILTER(STRSTARTS(STR(?superclass), "http://example.org/food-ontology#"))
            }
            LIMIT 10
            """
            
            sparql.setQuery(query4)
            result4 = sparql.query().convert()
            
            print(f"\n🌳 Hiérarchie des classes (exemples):")
            for binding in result4['results']['bindings']:
                subclass = binding['subclass']['value'].split('#')[-1]
                superclass = binding['superclass']['value'].split('#')[-1]
                print(f"  {subclass} ⊆ {superclass}")
            
            # Test 5: Propriétés nutritionnelles
            query5 = """
            PREFIX : <http://example.org/food-ontology#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
            SELECT ?prop ?label WHERE {
                VALUES ?prop { :calories :protein :carbohydrates :fat :fiber :sodium :sugar }
                ?prop rdfs:label ?label .
            }
            """
            
            sparql.setQuery(query5)
            result5 = sparql.query().convert()
            
            print(f"\n🥗 Propriétés nutritionnelles:")
            for binding in result5['results']['bindings']:
                prop_name = binding['prop']['value'].split('#')[-1]
                label = binding['label']['value']
                print(f"  ✅ {prop_name}: {label}")
            
            return True
            
        except Exception as e:
            print(f"❌ Erreur lors de la vérification: {e}")
            return False
    
    def get_ontology_summary(self):
        """Afficher un résumé de l'ontologie chargée"""
        print(f"\n📈 RÉSUMÉ DE L'ONTOLOGIE CHARGÉE")
        print("=" * 40)
        
        try:
            sparql = SPARQLWrapper(self.query_endpoint)
            sparql.setReturnFormat('json')
            
            # Compter tous les triplets
            query_count = "SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o }"
            sparql.setQuery(query_count)
            result = sparql.query().convert()
            total_triples = result['results']['bindings'][0]['count']['value']
            
            print(f"📊 Total des triplets RDF: {total_triples}")
            print(f"🌐 Endpoint SPARQL: {self.query_endpoint}")
            print(f"📁 Dataset: {self.dataset_name}")
            
            print(f"\n🧪 Requêtes de test recommandées:")
            print(f"1. Lister toutes les classes:")
            print(f"   SELECT DISTINCT ?class WHERE {{ ?class a owl:Class }}")
            
            print(f"\n2. Hiérarchie des aliments:")
            print(f"   SELECT ?sub ?super WHERE {{ ?sub rdfs:subClassOf ?super }}")
            
            print(f"\n3. Propriétés nutritionnelles:")
            print(f"   SELECT ?prop WHERE {{ ?prop a owl:DatatypeProperty }}")
            
        except Exception as e:
            print(f"❌ Erreur lors du résumé: {e}")

def main():
    print("🎯 CHARGEMENT DE L'ONTOLOGIE ALIMENTAIRE")
    print("=" * 50)
    
    # Initialiser le chargeur
    loader = OntologyLoader()
    
    # Vérifier le dataset
    if not loader.check_dataset_exists():
        print("\n❌ Veuillez d'abord créer le dataset 'food-kg-v2' dans Fuseki")
        print("Instructions:")
        print("1. Allez sur http://localhost:3030")
        print("2. 'manage datasets' → 'add new dataset'")
        print("3. Nom: food-kg-v2, Type: Persistent (TDB2)")
        return False
    
    # Demander le fichier d'ontologie
    ontology_file = input("\nChemin vers paste.txt (Entrée pour './paste.txt'): ").strip()
    if not ontology_file:
        ontology_file = "./paste.txt"
    
    # Charger l'ontologie
    print(f"\n🚀 Chargement de l'ontologie...")
    success = loader.load_ontology_from_file(ontology_file)
    
    if success:
        # Vérifier le chargement
        verification_success = loader.verify_ontology_loaded()
        
        if verification_success:
            # Afficher le résumé
            loader.get_ontology_summary()
            
            print(f"\n SUCCÈS COMPLET!")
            print(f"✅ Ontologie chargée et vérifiée")
            print(f"✅ Prêt pour la population des données")
            
            print(f"\n➡️  PROCHAINE ÉTAPE:")
            print(f"Exécutez maintenant le script de population:")
            print(f"python3 complete_populator_v2.py")
            
            return True
        else:
            print(f"\n⚠️  Ontologie chargée mais vérification échouée")
            return False
    else:
        print(f"\n❌ Échec du chargement de l'ontologie")
        return False

if __name__ == "__main__":
    main()
