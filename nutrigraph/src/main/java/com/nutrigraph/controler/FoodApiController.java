package com.nutrigraph.controler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.nutrigraph.model.Food;
import com.nutrigraph.request.SearchRequest;
import com.nutrigraph.request.SearchResponse;
import com.nutrigraph.service.FoodSearchService;
import com.nutrigraph.service.LuceneService;
import com.nutrigraph.service.SparqlService;

import jakarta.validation.Valid;

import java.util.ArrayList;  // ← AJOUTÉ
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/foods")
@CrossOrigin(origins = "*")
public class FoodApiController {

    @Autowired
    private FoodSearchService searchService;

    @Autowired
    private LuceneService luceneService;

    // 🎯 AJOUTÉ : Injection de SparqlService
    @Autowired
    private SparqlService sparqlService;

    @PostMapping("/search")
    public ResponseEntity<SearchResponse> searchFoods(@Valid @RequestBody SearchRequest request) {
        SearchResponse response = searchService.searchFoods(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<SearchResponse> searchFoodsGet(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String foodClass,
            @RequestParam(required = false) String foodGroup,
            @RequestParam(required = false) String minCalories,
            @RequestParam(required = false) String maxCalories,
            @RequestParam(required = false) String minProtein,
            @RequestParam(required = false) String maxProtein,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        request.setFoodClass(foodClass);
        request.setFoodGroup(foodGroup);
        
        // 🔧 CORRECTION : Conversion String vers Double
        if (minCalories != null && !minCalories.isEmpty()) {
            try {
                request.setMinCalories(Double.parseDouble(minCalories));
            } catch (NumberFormatException e) {
                // Ignorer si non numérique
            }
        }
        
        if (maxCalories != null && !maxCalories.isEmpty()) {
            try {
                request.setMaxCalories(Double.parseDouble(maxCalories));
            } catch (NumberFormatException e) {
                // Ignorer si non numérique
            }
        }
        
        if (minProtein != null && !minProtein.isEmpty()) {
            try {
                request.setMinProtein(Double.parseDouble(minProtein));
            } catch (NumberFormatException e) {
                // Ignorer si non numérique
            }
        }
        
        if (maxProtein != null && !maxProtein.isEmpty()) {
            try {
                request.setMaxProtein(Double.parseDouble(maxProtein));
            } catch (NumberFormatException e) {
                // Ignorer si non numérique
            }
        }
        
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);

        SearchResponse response = searchService.searchFoods(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{foodUri}")
    public ResponseEntity<Food> getFoodDetails(@PathVariable String foodUri) {
        // Décoder l'URI
        String decodedUri = java.net.URLDecoder.decode(foodUri, java.nio.charset.StandardCharsets.UTF_8);

        Food food = searchService.getFoodDetails(decodedUri);
        if (food != null) {
            return ResponseEntity.ok(food);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> getAutocompleteSuggestions(@RequestParam String query) {
        List<String> suggestions = searchService.getAutocompleteSuggestions(query);
        return ResponseEntity.ok(suggestions);
    }

    @GetMapping("/classes")
    public ResponseEntity<List<String>> getFoodClasses() {
        List<String> classes = searchService.getFoodClasses();
        return ResponseEntity.ok(classes);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<String>> getFoodGroups() {
        List<String> groups = searchService.getFoodGroups();
        return ResponseEntity.ok(groups);
    }

    @PostMapping("/reindex")
    public ResponseEntity<String> reindexLucene() {
        try {
            searchService.reindexLucene();
            return ResponseEntity.ok("Réindexation terminée avec succès");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur lors de la réindexation: " + e.getMessage());
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getIndexStatistics() {
        try {
            LuceneService.IndexStats stats = luceneService.getIndexStats();

            Map<String, Object> response = new HashMap<>();
            response.put("totalDocuments", stats.getTotalDocuments());
            response.put("maxDocuments", stats.getMaxDocuments());
            response.put("deletedDocuments", stats.getDeletedDocuments());
            response.put("indexValid", luceneService.isIndexValid());
            response.put("timestamp", System.currentTimeMillis());

            System.out.println("📊 Stats requested: " + stats.toString());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Error getting index stats: " + e.getMessage());
            e.printStackTrace();

            // Retourner des statistiques par défaut en cas d'erreur
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("totalDocuments", 0);
            errorResponse.put("maxDocuments", 0);
            errorResponse.put("deletedDocuments", 0);
            errorResponse.put("indexValid", false);
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.ok(errorResponse);
        }
    }

    // 🎯 NOUVEAUX ENDPOINTS SPÉCIALISÉS

    @GetMapping("/regions")
    public ResponseEntity<List<String>> getRegions() {
        try {
            List<String> regions = sparqlService.getRegions();
            return ResponseEntity.ok(regions);
        } catch (Exception e) {
            System.err.println("❌ Error getting regions: " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/search/region/{region}")
    public ResponseEntity<List<Food>> getFoodsByRegion(@PathVariable String region) {
        try {
            List<Food> foods = sparqlService.searchByRegion(region);
            return ResponseEntity.ok(foods);
        } catch (Exception e) {
            System.err.println("❌ Error searching by region: " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/search/spice/{level}")
    public ResponseEntity<SearchResponse> getFoodsBySpiceLevel(
            @PathVariable String level,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // 🔧 CORRECTION : Implémentation basique
        SearchResponse response = new SearchResponse();
        response.setFoods(new ArrayList<>());
        response.setTotalElements(0);
        response.setCurrentPage(page);
        response.setSize(size);
        response.setTotalPages(0);
        response.setHasNext(false);
        response.setHasPrevious(false);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/cultural")
    public ResponseEntity<Map<String, Object>> getCulturalStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            List<String> regions = sparqlService.getRegions();
            stats.put("regions", regions);
            stats.put("totalRegions", regions.size());
            
            Map<String, Integer> regionCounts = new HashMap<>();
            for (String region : regions) {
                List<Food> foods = sparqlService.searchByRegion(region);
                regionCounts.put(region, foods.size());
            }
            stats.put("foodsByRegion", regionCounts);
            
        } catch (Exception e) {
            System.err.println("❌ Error getting cultural stats: " + e.getMessage());
            stats.put("error", e.getMessage());
            stats.put("regions", new ArrayList<>());
            stats.put("totalRegions", 0);
            stats.put("foodsByRegion", new HashMap<>());
        }
        
        return ResponseEntity.ok(stats);
    }

    // 🎯 NOUVEAUX ENDPOINTS POUR VOTRE INTERFACE

    @GetMapping("/cooking-methods")
    public ResponseEntity<List<String>> getCookingMethods() {
        try {
            List<String> methods = sparqlService.getCookingMethods();
            return ResponseEntity.ok(methods);
        } catch (Exception e) {
            System.err.println("❌ Error getting cooking methods: " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/spice-levels")
    public ResponseEntity<List<String>> getSpiceLevels() {
        try {
            List<String> levels = sparqlService.getSpiceLevels();
            return ResponseEntity.ok(levels);
        } catch (Exception e) {
            System.err.println("❌ Error getting spice levels: " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    @GetMapping("/search/cooking-method/{method}")
    public ResponseEntity<List<Food>> getFoodsByCookingMethod(@PathVariable String method) {
        try {
            List<Food> foods = sparqlService.searchByCookingMethod(method);
            return ResponseEntity.ok(foods);
        } catch (Exception e) {
            System.err.println("❌ Error searching by cooking method: " + e.getMessage());
            return ResponseEntity.ok(new ArrayList<>());
        }
    }
}