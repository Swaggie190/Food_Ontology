
package com.foodkg.test_jena.controler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.foodkg.test_jena.model.Food;
import com.foodkg.test_jena.request.SearchRequest;
import com.foodkg.test_jena.request.SearchResponse;
import com.foodkg.test_jena.service.FoodSearchService;
import com.foodkg.test_jena.service.LuceneService;

import jakarta.validation.Valid;

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
            @RequestParam(required = false) Double minCalories,
            @RequestParam(required = false) Double maxCalories,
            @RequestParam(required = false) Double minProtein,
            @RequestParam(required = false) Double maxProtein,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection) {

        SearchRequest request = new SearchRequest();
        request.setQuery(query);
        request.setFoodClass(foodClass);
        request.setFoodGroup(foodGroup);
        request.setMinCalories(minCalories);
        request.setMaxCalories(maxCalories);
        request.setMinProtein(minProtein);
        request.setMaxProtein(maxProtein);
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
}