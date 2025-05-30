package com.foodkg.test_jena.service;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.foodkg.test_jena.FoodKGConfig;
import com.foodkg.test_jena.model.Food;
import com.foodkg.test_jena.model.FoodImage;

import java.util.ArrayList;
import java.util.List;

@Service
public class SparqlService {

    @Autowired
    private FoodKGConfig config;

    // private static final String FOOD_NS = "http://example.org/food-ontology#";

    public List<Food> getAllFoods() {
        String queryString = """
            PREFIX : <http://example.org/food-ontology#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

            SELECT ?food ?name ?class ?classLabel ?region ?cookingMethod ?spiceLevel ?culturalSignificance
                ?calories ?protein ?carbohydrates ?fat ?fiber ?sodium ?sugar ?description WHERE {
                ?food :name ?name ;
                    a ?class .
                ?class rdfs:label ?classLabel .

                OPTIONAL { ?food :region ?region }
                OPTIONAL { ?food :cookingMethod ?cookingMethod }
                OPTIONAL { ?food :spiceLevel ?spiceLevel }
                OPTIONAL { ?food :culturalSignificance ?culturalSignificance }
                OPTIONAL { ?food :calories ?calories }
                OPTIONAL { ?food :protein ?protein }
                OPTIONAL { ?food :carbohydrates ?carbohydrates }
                OPTIONAL { ?food :fat ?fat }
                OPTIONAL { ?food :fiber ?fiber }
                OPTIONAL { ?food :sodium ?sodium }
                OPTIONAL { ?food :sugar ?sugar }
                OPTIONAL { ?food :description ?description }

                FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
                FILTER(STRSTARTS(STR(?class), "http://example.org/food-ontology#"))
            }
            ORDER BY ?name
            """;

        return executeQuery(queryString);
    }

    public List<Food> searchByName(String namePattern) {
        String queryString = String.format("""
                PREFIX : <http://example.org/food-ontology#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                SELECT ?food ?name ?class ?classLabel ?calories ?protein ?description WHERE {
                    ?food :name ?name ;
                          a ?class .
                    ?class rdfs:label ?classLabel .

                    OPTIONAL { ?food :calories ?calories }
                    OPTIONAL { ?food :protein ?protein }
                    OPTIONAL { ?food :description ?description }

                    FILTER(CONTAINS(LCASE(?name), LCASE("%s")))
                    FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
                }
                ORDER BY ?name
                LIMIT 50
                """, namePattern);

        return executeQuery(queryString);
    }

    public List<Food> searchByClass(String foodClass) {
        String queryString = String.format("""
                PREFIX : <http://example.org/food-ontology#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                SELECT ?food ?name ?class ?classLabel ?calories ?protein WHERE {
                    ?food :name ?name ;
                          a :%s .
                    :%s rdfs:label ?classLabel .

                    OPTIONAL { ?food :calories ?calories }
                    OPTIONAL { ?food :protein ?protein }

                    BIND(:%s as ?class)
                }
                ORDER BY ?name
                """, foodClass, foodClass, foodClass);

        return executeQuery(queryString);
    }

    public List<Food> searchByNutritionalRange(Double minCalories, Double maxCalories,
            Double minProtein, Double maxProtein) {
        StringBuilder query = new StringBuilder("""
                PREFIX : <http://example.org/food-ontology#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                SELECT ?food ?name ?class ?classLabel ?calories ?protein ?carbohydrates ?fat WHERE {
                    ?food :name ?name ;
                          a ?class ;
                          :calories ?calories ;
                          :protein ?protein .
                    ?class rdfs:label ?classLabel .

                    OPTIONAL { ?food :carbohydrates ?carbohydrates }
                    OPTIONAL { ?food :fat ?fat }

                    FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
                """);

        // Ajouter les filtres nutritionnels
        if (minCalories != null) {
            query.append(String.format("    FILTER(?calories >= %f)\n", minCalories));
        }
        if (maxCalories != null) {
            query.append(String.format("    FILTER(?calories <= %f)\n", maxCalories));
        }
        if (minProtein != null) {
            query.append(String.format("    FILTER(?protein >= %f)\n", minProtein));
        }
        if (maxProtein != null) {
            query.append(String.format("    FILTER(?protein <= %f)\n", maxProtein));
        }

        query.append("}\nORDER BY ?calories\nLIMIT 100");

        return executeQuery(query.toString());
    }

    public List<String> getFoodClasses() {
        String queryString = """
                PREFIX : <http://example.org/food-ontology#>
                PREFIX owl: <http://www.w3.org/2002/07/owl#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                SELECT DISTINCT ?className ?classLabel WHERE {
                    ?class a owl:Class ;
                           rdfs:label ?classLabel ;
                           rdfs:subClassOf* :Food .

                    FILTER(?class != :Food)
                    FILTER(STRSTARTS(STR(?class), "http://example.org/food-ontology#"))

                    BIND(STRAFTER(STR(?class), "#") AS ?className)
                }
                ORDER BY ?classLabel
                """;

        List<String> classes = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(config.getSparqlEndpoint(), queryString)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                classes.add(solution.getLiteral("className").getString());
            }
        }
        return classes;
    }

    public List<String> getFoodGroups() {
        String queryString = """
                PREFIX : <http://example.org/food-ontology#>

                SELECT DISTINCT ?groupName WHERE {
                    ?group a :FoodGroup ;
                           :name ?groupName .
                }
                ORDER BY ?groupName
                """;

        List<String> groups = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(config.getSparqlEndpoint(), queryString)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                groups.add(solution.getLiteral("groupName").getString());
            }
        }
        return groups;
    }

    public Food getFoodDetails(String foodUri) {
        String queryString = String.format("""
                PREFIX : <http://example.org/food-ontology#>
                PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

                SELECT ?food ?name ?class ?classLabel ?group ?groupName ?calories ?protein ?carbohydrates
                       ?fat ?fiber ?sodium ?sugar ?description WHERE {
                    <%s> :name ?name ;
                         a ?class .
                    ?class rdfs:label ?classLabel .

                    OPTIONAL { <%s> :belongsTo ?group . ?group :name ?groupName }
                    OPTIONAL { <%s> :calories ?calories }
                    OPTIONAL { <%s> :protein ?protein }
                    OPTIONAL { <%s> :carbohydrates ?carbohydrates }
                    OPTIONAL { <%s> :fat ?fat }
                    OPTIONAL { <%s> :fiber ?fiber }
                    OPTIONAL { <%s> :sodium ?sodium }
                    OPTIONAL { <%s> :sugar ?sugar }
                    OPTIONAL { <%s> :description ?description }

                    BIND(<%s> as ?food)
                }
                """, foodUri, foodUri, foodUri, foodUri, foodUri, foodUri, foodUri, foodUri, foodUri, foodUri, foodUri);

        List<Food> foods = executeQuery(queryString);
        if (!foods.isEmpty()) {
            Food food = foods.get(0);

            // Ajouter les images
            food.setImages(getFoodImages(foodUri));

            // Ajouter les ingrédients
            food.setIngredients(getFoodIngredients(foodUri));

            return food;
        }
        return null;
    }

    public List<FoodImage> getFoodImages(String foodUri) {
        String queryString = String.format("""
                PREFIX : <http://example.org/food-ontology#>

                SELECT ?image ?imagePath ?filename ?width ?height WHERE {
                    <%s> :hasImage ?image .
                    ?image :imagePath ?imagePath .

                    OPTIONAL { ?image :filename ?filename }
                    OPTIONAL { ?image :width ?width }
                    OPTIONAL { ?image :height ?height }
                }
                """, foodUri);

        List<FoodImage> images = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(config.getSparqlEndpoint(), queryString)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();

                FoodImage image = new FoodImage();
                image.setUri(solution.getResource("image").getURI());
                image.setImagePath(solution.getLiteral("imagePath").getString());

                if (solution.contains("filename")) {
                    image.setFilename(solution.getLiteral("filename").getString());
                }
                if (solution.contains("width")) {
                    image.setWidth(solution.getLiteral("width").getInt());
                }
                if (solution.contains("height")) {
                    image.setHeight(solution.getLiteral("height").getInt());
                }

                images.add(image);
            }
        }
        return images;
    }

    public List<String> getFoodIngredients(String foodUri) {
        String queryString = String.format("""
                PREFIX : <http://example.org/food-ontology#>

                SELECT ?ingredientName WHERE {
                    <%s> :contains ?ingredient .
                    ?ingredient :name ?ingredientName .
                }
                ORDER BY ?ingredientName
                """, foodUri);

        List<String> ingredients = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(config.getSparqlEndpoint(), queryString)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                ingredients.add(solution.getLiteral("ingredientName").getString());
            }
        }
        return ingredients;
    }

    private List<Food> executeQuery(String queryString) {
        List<Food> foods = new ArrayList<>();

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(config.getSparqlEndpoint(), queryString)) {
            ResultSet results = qexec.execSelect();

            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();

                Food food = new Food();
                food.setUri(solution.getResource("food").getURI());
                food.setName(solution.getLiteral("name").getString());

                if (solution.contains("class")) {
                    String classUri = solution.getResource("class").getURI();
                    food.setFoodClass(classUri.substring(classUri.lastIndexOf("#") + 1));
                }

                if (solution.contains("classLabel")) {
                    food.setClassLabel(solution.getLiteral("classLabel").getString());
                }

                // 🎯 NOUVELLES PROPRIÉTÉS SPÉCIALISÉES
                if (solution.contains("region")) {
                    food.setRegion(solution.getLiteral("region").getString());
                }
                
                if (solution.contains("cookingMethod")) {
                    food.setCookingMethod(solution.getLiteral("cookingMethod").getString());
                }
                
                if (solution.contains("spiceLevel")) {
                    food.setSpiceLevel(solution.getLiteral("spiceLevel").getString());
                }
                
                if (solution.contains("culturalSignificance")) {
                    food.setCulturalSignificance(solution.getLiteral("culturalSignificance").getString());
                }

                // Propriétés nutritionnelles (inchangées)
                setNutritionalValue(solution, food, "calories", food::setCalories);
                setNutritionalValue(solution, food, "protein", food::setProtein);
                setNutritionalValue(solution, food, "carbohydrates", food::setCarbohydrates);
                setNutritionalValue(solution, food, "fat", food::setFat);
                setNutritionalValue(solution, food, "fiber", food::setFiber);
                setNutritionalValue(solution, food, "sodium", food::setSodium);
                setNutritionalValue(solution, food, "sugar", food::setSugar);

                if (solution.contains("description")) {
                    food.setDescription(solution.getLiteral("description").getString());
                }

                foods.add(food);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return foods;
    }

    private void setNutritionalValue(QuerySolution solution, Food food, String varName,
            java.util.function.Consumer<Double> setter) {
        if (solution.contains(varName)) {
            RDFNode node = solution.get(varName);
            if (node.isLiteral()) {
                try {
                    setter.accept(((Literal) node).getDouble());
                } catch (Exception e) {
                    // Ignorer les erreurs de conversion
                }
            }
        }
    }

    public List<Food> searchByRegion(String region) {
        String queryString = String.format("""
            PREFIX : <http://example.org/food-ontology#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

            SELECT ?food ?name ?class ?classLabel ?region ?spiceLevel ?cookingMethod ?calories WHERE {
                ?food :name ?name ;
                    a ?class ;
                    :region "%s" .
                ?class rdfs:label ?classLabel .

                OPTIONAL { ?food :spiceLevel ?spiceLevel }
                OPTIONAL { ?food :cookingMethod ?cookingMethod }
                OPTIONAL { ?food :calories ?calories }

                FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
            }
            ORDER BY ?name
            """, region);

        return executeQuery(queryString);
    }

    public List<String> getRegions() {
        String queryString = """
            PREFIX : <http://example.org/food-ontology#>
            
            SELECT DISTINCT ?region WHERE {
                ?food :region ?region .
            }
            ORDER BY ?region
            """;
        
        List<String> regions = new ArrayList<>();
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(config.getSparqlEndpoint(), queryString)) {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution solution = results.nextSolution();
                regions.add(solution.getLiteral("region").getString());
            }
        }
        return regions;
    }

public List<String> getCookingMethods() {
    String queryString = """
        PREFIX : <http://example.org/food-ontology#>
        
        SELECT DISTINCT ?method WHERE {
            ?food :cookingMethod ?method .
        }
        ORDER BY ?method
        """;
    
    List<String> methods = new ArrayList<>();
    try (QueryExecution qexec = QueryExecutionFactory.sparqlService(config.getSparqlEndpoint(), queryString)) {
        ResultSet results = qexec.execSelect();
        while (results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            methods.add(solution.getLiteral("method").getString());
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return methods;
}

public List<String> getSpiceLevels() {
    String queryString = """
        PREFIX : <http://example.org/food-ontology#>
        
        SELECT DISTINCT ?level WHERE {
            ?food :spiceLevel ?level .
        }
        ORDER BY ?level
        """;
    
    List<String> levels = new ArrayList<>();
    try (QueryExecution qexec = QueryExecutionFactory.sparqlService(config.getSparqlEndpoint(), queryString)) {
        ResultSet results = qexec.execSelect();
        while (results.hasNext()) {
            QuerySolution solution = results.nextSolution();
            levels.add(solution.getLiteral("level").getString());
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return levels;
}

public List<Food> searchByCookingMethod(String method) {
    String queryString = String.format("""
        PREFIX : <http://example.org/food-ontology#>
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

        SELECT ?food ?name ?class ?classLabel ?region ?cookingMethod ?calories WHERE {
            ?food :name ?name ;
                  a ?class ;
                  :cookingMethod "%s" .
            ?class rdfs:label ?classLabel .

            OPTIONAL { ?food :region ?region }
            OPTIONAL { ?food :calories ?calories }

            FILTER(?class != <http://www.w3.org/2002/07/owl#NamedIndividual>)
        }
        ORDER BY ?name
        """, method);

    return executeQuery(queryString);
}
}
