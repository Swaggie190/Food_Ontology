#!/usr/bin/env python3
"""
Chargeur d'ontologie pour Windows - Version simplifiée
"""

import requests
import os

def load_ontology_to_fuseki():
    # Configuration
    fuseki_server = "http://localhost:3030"
    dataset_name = "african-middle-eastern-kg"
    data_endpoint = f"{fuseki_server}/{dataset_name}/data"
    
    # Vérifier que le fichier ontologie existe
    ontology_file = "paste.txt"
    if not os.path.exists(ontology_file):
        print(f"❌ Fichier {ontology_file} non trouvé!")
        print("💡 Créez d'abord le fichier african_middle_eastern_ontology.ttl")
        return False
    
    print(f"📁 Lecture de {ontology_file}...")
    
    # Lire l'ontologie
    try:
        with open(ontology_file, 'r', encoding='utf-8') as f:
            ontology_content = f.read()
        
        print(f"📄 Fichier lu - {len(ontology_content)} caractères")
        
        # Headers pour Fuseki
        headers = {
            'Content-Type': 'text/turtle',
            'Accept': 'application/json'
        }
        
        print(f"🔄 Chargement dans Fuseki...")
        
        # Envoyer à Fuseki
        response = requests.post(
            data_endpoint, 
            data=ontology_content.encode('utf-8'),
            headers=headers,
            timeout=30
        )
        
        if response.status_code in [200, 201, 204]:
            print("✅ Ontologie chargée avec succès!")
            
            # Test simple
            test_query = f"""
            PREFIX owl: <http://www.w3.org/2002/07/owl#>
            SELECT (COUNT(?class) as ?count) WHERE {{
                ?class a owl:Class .
            }}
            """
            
            query_endpoint = f"{fuseki_server}/{dataset_name}/sparql"
            test_response = requests.post(
                query_endpoint,
                data={'query': test_query},
                headers={'Accept': 'application/json'}
            )
            
            if test_response.status_code == 200:
                result = test_response.json()
                count = result['results']['bindings'][0]['count']['value']
                print(f"📊 Classes chargées: {count}")
            
            return True
        else:
            print(f"❌ Erreur HTTP: {response.status_code}")
            print(f"Réponse: {response.text}")
            return False
            
    except Exception as e:
        print(f"❌ Erreur: {e}")
        return False

def main():
    print("🎯 CHARGEMENT DE L'ONTOLOGIE AFRICAINE/MOYEN-ORIENTALE")
    print("=" * 60)
    
    print("🔍 Vérification de Fuseki...")
    try:
        response = requests.get("http://localhost:3030")
        if response.status_code == 200:
            print("✅ Fuseki accessible")
        else:
            print("❌ Fuseki non accessible")
            return
    except:
        print("❌ Fuseki non démarré!")
        print("💡 Démarrez Fuseki d'abord: fuseki-server.bat")
        return
    
    success = load_ontology_to_fuseki()
    
    if success:
        print(f"\n🎉 SUCCÈS!")
        print(f"✅ Ontologie chargée dans african-middle-eastern-kg")
        print(f"🌐 Vérifiez sur: http://localhost:3030/#/dataset/african-middle-eastern-kg")
        print(f"\n➡️ PROCHAINE ÉTAPE: Population des données")
    else:
        print(f"\n❌ Échec du chargement")

if __name__ == "__main__":
    main()