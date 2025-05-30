package com.foodkg.test_jena.service;

import com.foodkg.test_jena.model.Food;
import com.foodkg.test_jena.request.SearchRequest;
import com.foodkg.test_jena.request.SearchResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FoodSearchService {

    @Autowired
    private SparqlService sparqlService;

    @Autowired
    private LuceneService luceneService;

    public SearchResponse searchFoods(SearchRequest request) {
        try {
            List<Food> foods;

            // Si pas de requête textuelle, utiliser SPARQL directement
            if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
                foods = searchWithSparqlOnly(request);
            } else {
                // Recherche hybride Lucene + SPARQL
                foods = searchWithLuceneAndSparql(request);
            }

            // Enrichir avec les détails complets
            foods = enrichFoodsWithDetails(foods);

            // Pagination
            int totalElements = foods.size();
            int startIndex = request.getPage() * request.getSize();
            int endIndex = Math.min(startIndex + request.getSize(), totalElements);

            List<Food> paginatedFoods = foods.subList(
                    Math.min(startIndex, totalElements),
                    Math.min(endIndex, totalElements));

            return new SearchResponse(paginatedFoods, totalElements, request.getPage(), request.getSize());

        } catch (Exception e) {
            e.printStackTrace();
            return new SearchResponse(List.of(), 0, 0, request.getSize());
        }
    }

    private List<Food> searchWithSparqlOnly(SearchRequest request) {
        // Recherche basée uniquement sur les critères SPARQL
        if (request.getFoodClass() != null && !request.getFoodClass().isEmpty()) {
            return sparqlService.searchByClass(request.getFoodClass());
        }

        if (request.getMinCalories() != null || request.getMaxCalories() != null ||
                request.getMinProtein() != null || request.getMaxProtein() != null) {
            return sparqlService.searchByNutritionalRange(
                    request.getMinCalories(), request.getMaxCalories(),
                    request.getMinProtein(), request.getMaxProtein());
        }

        return sparqlService.getAllFoods();
    }

    private List<Food> searchWithLuceneAndSparql(SearchRequest request) throws Exception {
        // Recherche textuelle avec Lucene
        List<Food> luceneResults = luceneService.searchFoodsAdvanced(
                request.getQuery(),
                request.getFoodClass(),
                request.getFoodGroup(),
                request.getMinCalories(),
                request.getMaxCalories(),
                100 // Récupérer plus de résultats pour filtrage ultérieur
        );

        // Filtrer avec des critères SPARQL additionnels si nécessaire
        if (request.getMinProtein() != null || request.getMaxProtein() != null) {
            luceneResults = luceneResults.stream()
                    .filter(food -> matchesProteinRange(food, request.getMinProtein(), request.getMaxProtein()))
                    .collect(Collectors.toList());
        }

        return luceneResults;
    }

    private boolean matchesProteinRange(Food food, Double minProtein, Double maxProtein) {
        if (food.getProtein() == null)
            return false;

        if (minProtein != null && food.getProtein() < minProtein)
            return false;
        if (maxProtein != null && food.getProtein() > maxProtein)
            return false;

        return true;
    }

    private List<Food> enrichFoodsWithDetails(List<Food> foods) {
        return foods.stream()
                .map(food -> {
                    try {
                        Food detailedFood = sparqlService.getFoodDetails(food.getUri());
                        if (detailedFood != null) {
                            // Conserver le score de recherche Lucene
                            detailedFood.setSearchScore(food.getSearchScore());
                            return detailedFood;
                        }
                        return food;
                    } catch (Exception e) {
                        return food; // Retourner l'aliment de base en cas d'erreur
                    }
                })
                .collect(Collectors.toList());
    }

    public List<String> getAutocompleteSuggestions(String prefix) {
        try {
            return luceneService.getAutocompleteSuggestions(prefix, 10);
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<String> getFoodClasses() {
        return sparqlService.getFoodClasses();
    }

    public List<String> getFoodGroups() {
        return sparqlService.getFoodGroups();
    }

    public Food getFoodDetails(String foodUri) {
        return sparqlService.getFoodDetails(foodUri);
    }

    public void reindexLucene() {
        try {
            luceneService.indexFoodsFromKnowledgeGraph();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}