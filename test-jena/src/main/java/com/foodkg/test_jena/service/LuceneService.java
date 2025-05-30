package com.foodkg.test_jena.service;

// Imports Spring Boot
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

// Imports Lucene Core
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;

// Imports Lucene Index
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;

// Imports Lucene Search
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

// Imports Lucene Query Parser
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.ParseException;

// Imports Lucene Store
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

// Imports Java Standard
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// Imports du projet
import com.foodkg.test_jena.FoodKGConfig;
import com.foodkg.test_jena.model.Food;

@Service
public class LuceneService {

    @Autowired
    private FoodKGConfig config;

    @Autowired
    private SparqlService sparqlService;

    private StandardAnalyzer analyzer;
    private Directory indexDirectory;

    @PostConstruct
    public void init() throws IOException {
        analyzer = new StandardAnalyzer();
        indexDirectory = FSDirectory.open(Paths.get(config.getLuceneIndexPath()));

        // Indexer les données au démarrage si l'index n'existe pas
        if (!DirectoryReader.indexExists(indexDirectory)) {
            indexFoodsFromKnowledgeGraph();
        }
    }

    public void indexFoodsFromKnowledgeGraph() {
        try {
            List<Food> foods = sparqlService.getAllFoods();
            indexFoods(foods);
            System.out.println("Indexation Lucene terminée: " + foods.size() + " aliments indexés");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void indexFoods(List<Food> foods) throws IOException {
        IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        try (IndexWriter writer = new IndexWriter(indexDirectory, writerConfig)) {
            for (Food food : foods) {
                Document doc = createDocument(food);
                writer.addDocument(doc);
            }
            writer.commit();
        }
    }

    private Document createDocument(Food food) {
        Document doc = new Document();

        // Champs exacts (non analysés)
        if (food.getUri() != null) {
            doc.add(new StringField("uri", food.getUri(), Field.Store.YES));
        }

        if (food.getName() != null) {
            doc.add(new StringField("name", food.getName(), Field.Store.YES));
            // Champ textuel pour la recherche
            doc.add(new TextField("nameText", food.getName(), Field.Store.NO));
        }

        if (food.getFoodClass() != null) {
            doc.add(new StringField("foodClass", food.getFoodClass(), Field.Store.YES));
        }

        if (food.getFoodGroup() != null) {
            doc.add(new StringField("foodGroup", food.getFoodGroup(), Field.Store.YES));
        }

        // Champs textuels (analysés pour la recherche)
        if (food.getDescription() != null) {
            doc.add(new TextField("description", food.getDescription(), Field.Store.YES));
            doc.add(new TextField("descriptionText", food.getDescription(), Field.Store.NO));
        }

        if (food.getClassLabel() != null) {
            doc.add(new TextField("classLabelText", food.getClassLabel(), Field.Store.NO));
        }

        // Champs numériques (stockés comme String pour simplicité)
        if (food.getCalories() != null) {
            doc.add(new StringField("calories", food.getCalories().toString(), Field.Store.YES));
        }

        if (food.getProtein() != null) {
            doc.add(new StringField("protein", food.getProtein().toString(), Field.Store.YES));
        }

        if (food.getCarbohydrates() != null) {
            doc.add(new StringField("carbohydrates", food.getCarbohydrates().toString(), Field.Store.YES));
        }

        if (food.getFat() != null) {
            doc.add(new StringField("fat", food.getFat().toString(), Field.Store.YES));
        }

        if (food.getFiber() != null) {
            doc.add(new StringField("fiber", food.getFiber().toString(), Field.Store.YES));
        }

        if (food.getSodium() != null) {
            doc.add(new StringField("sodium", food.getSodium().toString(), Field.Store.YES));
        }

        if (food.getSugar() != null) {
            doc.add(new StringField("sugar", food.getSugar().toString(), Field.Store.YES));
        }

        // Ingrédients (si disponibles)
        if (food.getIngredients() != null && !food.getIngredients().isEmpty()) {
            String ingredientsText = String.join(" ", food.getIngredients());
            doc.add(new TextField("ingredients", ingredientsText, Field.Store.YES));
            doc.add(new TextField("ingredientsText", ingredientsText, Field.Store.NO));
        }

        return doc;
    }

    public List<Food> searchFoods(String queryText, int maxResults) throws Exception {
        if (queryText == null || queryText.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            // Échapper les caractères spéciaux pour éviter les erreurs de parsing
            String escapedQuery = QueryParser.escape(queryText);

            // Créer une requête qui cherche dans plusieurs champs
            String multiFieldQuery = String.format(
                    "nameText:%s^3 OR descriptionText:%s^2 OR classLabelText:%s OR ingredientsText:%s",
                    escapedQuery, escapedQuery, escapedQuery, escapedQuery);

            QueryParser parser = new QueryParser("nameText", analyzer);
            Query query = parser.parse(multiFieldQuery);

            TopDocs topDocs = searcher.search(query, maxResults);

            List<Food> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                Food food = documentToFood(doc);
                food.setSearchScore(scoreDoc.score);
                results.add(food);
            }

            return results;
        }
    }

    public List<Food> searchFoodsAdvanced(String queryText, String foodClass, String foodGroup,
            Double minCalories, Double maxCalories, int maxResults) throws Exception {

        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            StringBuilder queryBuilder = new StringBuilder();

            // Recherche textuelle
            if (queryText != null && !queryText.trim().isEmpty()) {
                String escapedQuery = QueryParser.escape(queryText);
                queryBuilder.append(String.format(
                        "(nameText:%s^3 OR descriptionText:%s^2 OR classLabelText:%s OR ingredientsText:%s)",
                        escapedQuery, escapedQuery, escapedQuery, escapedQuery));
            } else {
                queryBuilder.append("*:*");
            }

            // Filtres par classe
            if (foodClass != null && !foodClass.trim().isEmpty()) {
                queryBuilder.append(" AND foodClass:").append(QueryParser.escape(foodClass));
            }

            // Filtres par groupe
            if (foodGroup != null && !foodGroup.trim().isEmpty()) {
                queryBuilder.append(" AND foodGroup:\"").append(QueryParser.escape(foodGroup)).append("\"");
            }

            QueryParser parser = new QueryParser("nameText", analyzer);
            Query query = parser.parse(queryBuilder.toString());

            TopDocs topDocs = searcher.search(query, maxResults * 2); // Plus de résultats pour filtrer

            List<Food> results = new ArrayList<>();
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                Food food = documentToFood(doc);

                // Filtres nutritionnels (post-processing)
                if (matchesNutritionalFilters(food, minCalories, maxCalories)) {
                    food.setSearchScore(scoreDoc.score);
                    results.add(food);

                    if (results.size() >= maxResults) {
                        break;
                    }
                }
            }

            return results;
        }
    }

    private boolean matchesNutritionalFilters(Food food, Double minCalories, Double maxCalories) {
        if (minCalories != null && (food.getCalories() == null || food.getCalories() < minCalories)) {
            return false;
        }
        if (maxCalories != null && (food.getCalories() == null || food.getCalories() > maxCalories)) {
            return false;
        }
        return true;
    }

    private Food documentToFood(Document doc) {
        Food food = new Food();

        food.setUri(doc.get("uri"));
        food.setName(doc.get("name"));
        food.setFoodClass(doc.get("foodClass"));
        food.setFoodGroup(doc.get("foodGroup"));
        food.setDescription(doc.get("description"));

        // Conversion des valeurs numériques avec gestion d'erreurs
        setDoubleField(doc, food, "calories", food::setCalories);
        setDoubleField(doc, food, "protein", food::setProtein);
        setDoubleField(doc, food, "carbohydrates", food::setCarbohydrates);
        setDoubleField(doc, food, "fat", food::setFat);
        setDoubleField(doc, food, "fiber", food::setFiber);
        setDoubleField(doc, food, "sodium", food::setSodium);
        setDoubleField(doc, food, "sugar", food::setSugar);

        return food;
    }

    private void setDoubleField(Document doc, Food food, String fieldName, java.util.function.Consumer<Double> setter) {
        try {
            String value = doc.get(fieldName);
            if (value != null && !value.trim().isEmpty()) {
                setter.accept(Double.parseDouble(value));
            }
        } catch (NumberFormatException e) {
            // Ignorer les erreurs de conversion
            System.err.println("Erreur de conversion pour le champ " + fieldName + ": " + doc.get(fieldName));
        }
    }

    /**
     * Autocomplétion sans erreur ParseException
     */
    public List<String> getAutocompleteSuggestions(String prefix, int maxSuggestions) throws Exception {
        List<String> suggestions = new ArrayList<>();

        // Validation d'entrée renforcée
        if (prefix == null || prefix.trim().length() < 2) {
            return suggestions;
        }

        // Nettoyer le préfixe
        String cleanPrefix = sanitizeQuery(prefix);
        if (cleanPrefix.isEmpty()) {
            return suggestions;
        }

        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            // Construction sécurisée de la requête d'autocomplétion
            String queryString = "nameText:" + cleanPrefix + "*";

            System.out.println(" Autocomplete query: " + queryString);

            QueryParser parser = new QueryParser("nameText", analyzer);
            Query query = parser.parse(queryString);

            TopDocs topDocs = searcher.search(query, maxSuggestions * 2);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String name = doc.get("name");
                if (name != null && !suggestions.contains(name) && suggestions.size() < maxSuggestions) {
                    suggestions.add(name);
                }
            }

            System.out.println("✅ Found " + suggestions.size() + " suggestions");

        } catch (ParseException e) {
            System.err.println(" ParseException in autocomplete: " + e.getMessage());
            System.err.println("Original prefix: '" + prefix + "'");
            System.err.println("Clean prefix: '" + cleanPrefix + "'");

            // Fallback: recherche exacte sans wildcard
            try {
                return getFallbackSuggestions(cleanPrefix, maxSuggestions);
            } catch (Exception fallbackException) {
                System.err.println(" Fallback autocomplete also failed: " + fallbackException.getMessage());
                return new ArrayList<>();
            }

        } catch (Exception e) {
            System.err.println(" Unexpected error in autocomplete: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }

        return suggestions;
    }

    /**
     * Nettoyage sécurisé des requêtes
     */
    private String sanitizeQuery(String query) {
        if (query == null) {
            return "";
        }

        String clean = query.trim().toLowerCase();

        // Supprimer les wildcards en début (cause de l'erreur)
        while (clean.startsWith("*") || clean.startsWith("?")) {
            clean = clean.substring(1).trim();
        }

        // Supprimer les espaces multiples
        clean = clean.replaceAll("\\s+", " ");

        // Échapper les caractères spéciaux Lucene
        clean = QueryParser.escape(clean);

        return clean;
    }

    /**
     * Autocomplétion de secours
     */
    private List<String> getFallbackSuggestions(String prefix, int maxSuggestions) throws Exception {
        List<String> suggestions = new ArrayList<>();

        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            // Recherche exacte sans wildcard
            String exactQuery = "nameText:" + prefix;

            QueryParser parser = new QueryParser("nameText", analyzer);
            Query query = parser.parse(exactQuery);

            TopDocs topDocs = searcher.search(query, maxSuggestions);

            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                String name = doc.get("name");
                if (name != null && !suggestions.contains(name)) {
                    suggestions.add(name);
                }
            }

            System.out.println(" Fallback found " + suggestions.size() + " suggestions");
        }

        return suggestions;
    }

    /**
     * Réindexer tous les aliments depuis le graphe de connaissances
     */
    public void reindexAll() throws Exception {
        System.out.println("Début de la réindexation Lucene...");

        // Supprimer l'index existant
        IndexWriterConfig writerConfig = new IndexWriterConfig(analyzer);
        writerConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        try (IndexWriter writer = new IndexWriter(indexDirectory, writerConfig)) {
            writer.deleteAll();
            writer.commit();
        }

        // Réindexer
        indexFoodsFromKnowledgeGraph();
    }

    /**
     * Obtenir des statistiques sur l'index Lucene
     */
    public IndexStats getIndexStats() throws IOException {
        try (IndexReader reader = DirectoryReader.open(indexDirectory)) {
            IndexStats stats = new IndexStats();
            stats.setTotalDocuments(reader.numDocs());
            stats.setMaxDocuments(reader.maxDoc());
            stats.setDeletedDocuments(reader.numDeletedDocs());
            return stats;
        }
    }

    /**
     * Vérifier si l'index existe et est valide
     */
    public boolean isIndexValid() {
        try {
            return DirectoryReader.indexExists(indexDirectory);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Fermer proprement les ressources Lucene
     */
    public void close() throws IOException {
        if (indexDirectory != null) {
            indexDirectory.close();
        }
    }

    // Classe interne pour les statistiques
    public static class IndexStats {
        private int totalDocuments;
        private int maxDocuments;
        private int deletedDocuments;

        // Getters et setters
        public int getTotalDocuments() {
            return totalDocuments;
        }

        public void setTotalDocuments(int totalDocuments) {
            this.totalDocuments = totalDocuments;
        }

        public int getMaxDocuments() {
            return maxDocuments;
        }

        public void setMaxDocuments(int maxDocuments) {
            this.maxDocuments = maxDocuments;
        }

        public int getDeletedDocuments() {
            return deletedDocuments;
        }

        public void setDeletedDocuments(int deletedDocuments) {
            this.deletedDocuments = deletedDocuments;
        }

        @Override
        public String toString() {
            return String.format("IndexStats{total=%d, max=%d, deleted=%d}",
                    totalDocuments, maxDocuments, deletedDocuments);
        }
    }
}