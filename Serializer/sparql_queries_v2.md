# Requêtes SPARQL pour l'ontologie enrichie african-middle-eastern-kg

## Instructions d'utilisation
1. Allez sur **http://localhost:3030**
2. Sélectionnez le dataset **african-middle-eastern-kg**
3. Cliquez sur l'onglet **query**
4. Copiez-collez chaque requête ci-dessous
5. Cliquez sur **Run Query**

---

## 1. Exploration des classes OWL

### 1.1 Lister toutes les classes de l'ontologie
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?class ?label ?comment WHERE {
    ?class a owl:Class ;
           rdfs:label ?label .
    OPTIONAL { ?class rdfs:comment ?comment }
    FILTER(STRSTARTS(STR(?class), "http://example.org/food-ontology#"))
}
ORDER BY ?label
```

### 1.2 Hiérarchie complète des classes
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?subclass ?subclassLabel ?superclass ?superclassLabel WHERE {
    ?subclass rdfs:subClassOf ?superclass ;
              rdfs:label ?subclassLabel .
    ?superclass rdfs:label ?superclassLabel .
    FILTER(STRSTARTS(STR(?subclass), "http://example.org/food-ontology#"))
    FILTER(STRSTARTS(STR(?superclass), "http://example.org/food-ontology#"))
}
ORDER BY ?superclassLabel ?subclassLabel
```

### 1.3 Classes racines (niveau 1)
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?class ?label WHERE {
    ?class a owl:Class ;
           rdfs:label ?label .
    FILTER(STRSTARTS(STR(?class), "http://example.org/food-ontology#"))
    FILTER NOT EXISTS {
        ?class rdfs:subClassOf ?parent .
        FILTER(STRSTARTS(STR(?parent), "http://example.org/food-ontology#"))
    }
}
ORDER BY ?label
```

### 1.4 Compter les classes par type
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?superclass ?superclassLabel (COUNT(?subclass) as ?subclassCount) WHERE {
    ?subclass rdfs:subClassOf ?superclass .
    ?superclass rdfs:label ?superclassLabel .
    FILTER(STRSTARTS(STR(?superclass), "http://example.org/food-ontology#"))
}
GROUP BY ?superclass ?superclassLabel
ORDER BY DESC(?subclassCount)
```

---

## 2. Exploration des propriétés

### 2.1 Toutes les propriétés d'objet
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?property ?label ?domain ?range ?comment WHERE {
    ?property a owl:ObjectProperty ;
              rdfs:label ?label .
    OPTIONAL { ?property rdfs:domain ?domain }
    OPTIONAL { ?property rdfs:range ?range }
    OPTIONAL { ?property rdfs:comment ?comment }
    FILTER(STRSTARTS(STR(?property), "http://example.org/food-ontology#"))
}
ORDER BY ?label
```

### 2.2 Toutes les propriétés de données
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?property ?label ?domain ?range ?comment WHERE {
    ?property a owl:DatatypeProperty ;
              rdfs:label ?label .
    OPTIONAL { ?property rdfs:domain ?domain }
    OPTIONAL { ?property rdfs:range ?range }
    OPTIONAL { ?property rdfs:comment ?comment }
    FILTER(STRSTARTS(STR(?property), "http://example.org/food-ontology#"))
}
ORDER BY ?label
```

### 2.3 Propriétés nutritionnelles uniquement
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?property ?label ?comment WHERE {
    ?property a owl:DatatypeProperty ;
              rdfs:label ?label ;
              rdfs:domain :Food .
    OPTIONAL { ?property rdfs:comment ?comment }
    VALUES ?property { :calories :protein :carbohydrates :fat :fiber :sodium :sugar }
}
ORDER BY ?label
```

---

## 3. Exploration des instances (si peuplées)

### 3.1 Tous les aliments avec leur classe
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?food ?name ?class ?classLabel WHERE {
    ?food :name ?name ;
          a ?class .
    ?class rdfs:label ?classLabel .
    FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
    FILTER(STRSTARTS(STR(?class), "http://example.org/food-ontology#"))
}
ORDER BY ?classLabel ?name
```

### 3.2 Répartition des aliments par classe OWL
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?class ?classLabel (COUNT(?food) as ?foodCount) WHERE {
    ?food a ?class ;
          :name ?name .
    ?class rdfs:label ?classLabel .
    FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
    FILTER(STRSTARTS(STR(?class), "http://example.org/food-ontology#"))
}
GROUP BY ?class ?classLabel
ORDER BY DESC(?foodCount)
```

### 3.3 Tous les cafés (exemple classe spécialisée)
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?coffee ?name ?calories WHERE {
    ?coffee a :Coffee ;
            :name ?name .
    OPTIONAL { ?coffee :calories ?calories }
}
ORDER BY ?name
```

### 3.4 Tous les thés
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?tea ?name ?calories WHERE {
    ?tea a :Tea ;
         :name ?name .
    OPTIONAL { ?tea :calories ?calories }
}
ORDER BY ?name
```

### 3.5 Tous les aliments cuisinés
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?food ?name ?calories WHERE {
    ?food a :CookedFood ;
          :name ?name .
    OPTIONAL { ?food :calories ?calories }
}
ORDER BY ?name
```

---

## 4. Requêtes nutritionnelles avancées

### 4.1 Aliments avec profil nutritionnel complet
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?name ?calories ?protein ?carbohydrates ?fat ?fiber WHERE {
    ?food :name ?name ;
          :calories ?calories ;
          :protein ?protein ;
          :carbohydrates ?carbohydrates ;
          :fat ?fat ;
          :fiber ?fiber .
}
ORDER BY DESC(?calories)
```

### 4.2 Aliments riches en protéines (>15g/100g)
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?name ?protein ?class WHERE {
    ?food :name ?name ;
          :protein ?protein ;
          a ?class .
    FILTER(?protein > 15)
    FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
}
ORDER BY DESC(?protein)
```

### 4.3 Aliments faibles en calories (<100 kcal/100g)
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?name ?calories ?class WHERE {
    ?food :name ?name ;
          :calories ?calories ;
          a ?class .
    FILTER(?calories < 100)
    FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
}
ORDER BY ?calories
```

### 4.4 Moyennes nutritionnelles par classe
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?classLabel 
       (AVG(?calories) as ?avgCalories)
       (AVG(?protein) as ?avgProtein)
       (COUNT(?food) as ?foodCount) WHERE {
    ?food a ?class ;
          :name ?name ;
          :calories ?calories ;
          :protein ?protein .
    ?class rdfs:label ?classLabel .
    FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
}
GROUP BY ?class ?classLabel
ORDER BY DESC(?avgCalories)
```

---

## 5. Requêtes sur les groupes alimentaires

### 5.1 Tous les groupes alimentaires
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?group ?groupName (COUNT(?food) as ?foodCount) WHERE {
    ?food :belongsTo ?group .
    ?group :name ?groupName .
}
GROUP BY ?group ?groupName
ORDER BY DESC(?foodCount)
```

### 5.2 Aliments d'un groupe spécifique (ex: Beverages)
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?food ?name ?calories WHERE {
    ?food :name ?name ;
          :belongsTo ?group .
    ?group :name "Beverages" .
    OPTIONAL { ?food :calories ?calories }
}
ORDER BY ?name
```

### 5.3 Aliments avec leur groupe et classe
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?name ?classLabel ?groupName WHERE {
    ?food :name ?name ;
          a ?class ;
          :belongsTo ?group .
    ?class rdfs:label ?classLabel .
    ?group :name ?groupName .
    FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
}
ORDER BY ?groupName ?classLabel ?name
```

---

## 6. Requêtes sur les ingrédients

### 6.1 Tous les ingrédients utilisés
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?ingredient ?ingredientName (COUNT(?food) as ?usageCount) WHERE {
    ?food :contains ?ingredient .
    ?ingredient :name ?ingredientName .
}
GROUP BY ?ingredient ?ingredientName
ORDER BY DESC(?usageCount)
```

### 6.2 Aliments contenant un ingrédient spécifique
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?food ?foodName ?ingredientName WHERE {
    ?food :name ?foodName ;
          :contains ?ingredient .
    ?ingredient :name ?ingredientName .
    FILTER(CONTAINS(LCASE(?ingredientName), "coffee"))
}
ORDER BY ?foodName
```

### 6.3 Composition complète des aliments
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?foodName (GROUP_CONCAT(?ingredientName; separator=", ") as ?ingredients) WHERE {
    ?food :name ?foodName ;
          :contains ?ingredient .
    ?ingredient :name ?ingredientName .
}
GROUP BY ?food ?foodName
ORDER BY ?foodName
```

---

## 7. Requêtes sur les images

### 7.1 Aliments avec leurs images
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?food ?name ?image ?imagePath WHERE {
    ?food :name ?name ;
          :hasImage ?image .
    ?image :imagePath ?imagePath .
}
ORDER BY ?name
```

### 7.2 Nombre d'images par aliment
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?name (COUNT(?image) as ?imageCount) WHERE {
    ?food :name ?name ;
          :hasImage ?image .
}
GROUP BY ?food ?name
ORDER BY DESC(?imageCount)
```

### 7.3 Images avec leurs métadonnées
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?foodName ?imagePath ?filename ?width ?height WHERE {
    ?food :name ?foodName ;
          :hasImage ?image .
    ?image :imagePath ?imagePath .
    OPTIONAL { ?image :filename ?filename }
    OPTIONAL { ?image :width ?width }
    OPTIONAL { ?image :height ?height }
}
ORDER BY ?foodName
LIMIT 20
```

---

## 8. Requêtes hiérarchiques avancées

### 8.1 Toutes les boissons (hiérarchie complète)
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?beverage ?name ?specificClass WHERE {
    ?beverage :name ?name ;
              a ?specificClass .
    ?specificClass rdfs:subClassOf* :Beverage .
    FILTER(?specificClass != <http://www.w3.org/2002/07/owl#NamedIndividual>)
}
ORDER BY ?specificClass ?name
```

### 8.2 Tous les aliments crus (RawFood et sous-classes)
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?food ?name ?specificClass WHERE {
    ?food :name ?name ;
          a ?specificClass .
    ?specificClass rdfs:subClassOf* :RawFood .
    FILTER(?specificClass != <http://www.w3.org/2002/07/owl#NamedIndividual>)
}
ORDER BY ?specificClass ?name
```

### 8.3 Chemin hiérarchique complet d'un aliment
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?food ?name ?class ?parent ?grandParent WHERE {
    ?food :name ?name ;
          a ?class .
    OPTIONAL { ?class rdfs:subClassOf ?parent }
    OPTIONAL { ?parent rdfs:subClassOf ?grandParent }
    FILTER(CONTAINS(?name, "coffee"))
    FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
}
```

---

## 9. Requêtes de validation et statistiques

### 9.1 Statistiques générales du graphe
```sparql
PREFIX : <http://example.org/food-ontology#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>

SELECT 
    (COUNT(DISTINCT ?class) as ?totalClasses)
    (COUNT(DISTINCT ?food) as ?totalFoods)
    (COUNT(DISTINCT ?image) as ?totalImages)
    (COUNT(DISTINCT ?ingredient) as ?totalIngredients)
    (COUNT(DISTINCT ?group) as ?totalGroups)
WHERE {
    OPTIONAL { ?class a owl:Class . FILTER(STRSTARTS(STR(?class), "http://example.org/food-ontology#")) }
    OPTIONAL { ?food :name ?foodName }
    OPTIONAL { ?image a :FoodImage }
    OPTIONAL { ?ingredient a :Ingredient }
    OPTIONAL { ?group a :FoodGroup }
}
```

### 9.2 Aliments sans données nutritionnelles
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?name WHERE {
    ?food :name ?name .
    FILTER NOT EXISTS { ?food :calories ?cal }
}
ORDER BY ?name
```

### 9.3 Aliments sans images
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?name WHERE {
    ?food :name ?name .
    FILTER NOT EXISTS { ?food :hasImage ?image }
}
ORDER BY ?name
```

### 9.4 Cohérence des relations belongsTo
```sparql
PREFIX : <http://example.org/food-ontology#>

SELECT ?food ?name ?group WHERE 