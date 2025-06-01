// ===== ImageSimilarityService.java - VERSION ACCÈS DIRECT =====
package com.nutrigraph.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.nutrigraph.NutriGraphConfig;
import com.nutrigraph.model.Food;
import com.nutrigraph.model.FoodRecognitionResult;
import com.nutrigraph.model.ImageRecognitionResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ImageSimilarityService {

    @Autowired
    private SparqlService sparqlService;

    @Autowired
    private NutriGraphConfig config;

    /**
     * Reconnaissance par accès DIRECT au système de fichiers - VERSION OPTIMISÉE
     */
    public ImageRecognitionResponse recognizeFood(MultipartFile uploadedImage,
            Double confidenceThreshold,
            Integer maxResults) throws IOException {

        System.out.println(" === RECONNAISSANCE OPTIMISÉE ===");
        System.out.println("Seuil: " + confidenceThreshold + ", Max: " + maxResults);

        // 1. Charger l'image uploadée
        BufferedImage queryImage = ImageIO.read(uploadedImage.getInputStream());
        if (queryImage == null) {
            throw new IOException("Format d'image non supporté");
        }
        System.out.println("Image uploadée: " + queryImage.getWidth() + "x" + queryImage.getHeight());

        // 2. Calculer l'histogramme de l'image uploadée
        ImageHistogram queryHistogram = calculateHistogram(queryImage);

        // 3. Scanner DIRECTEMENT les dossiers d'images
        Path imagesBasePath = Paths.get(config.getImagesBasePath()).toAbsolutePath();
        System.out.println(" Scan optimisé du dossier: " + imagesBasePath);

        if (!Files.exists(imagesBasePath)) {
            throw new IOException("Dossier d'images introuvable: " + imagesBasePath);
        }

        // 4. Structures pour stocker seulement le MEILLEUR par catégorie
        Map<String, CategoryBestMatch> bestMatchPerCategory = new HashMap<>();
        int totalImages = 0;
        int imagesLues = 0;

        try (DirectoryStream<Path> categories = Files.newDirectoryStream(imagesBasePath, Files::isDirectory)) {

            for (Path categoryPath : categories) {
                String categoryName = categoryPath.getFileName().toString();
                System.out.println(" Analyse catégorie: " + categoryName);

                double bestSimilarityForCategory = 0.0;
                String bestFileForCategory = null;
                int categoryImageCount = 0;

                // Scanner max 20 images par catégorie pour accélérer
                try (DirectoryStream<Path> imageFiles = Files.newDirectoryStream(categoryPath,
                        "*.{jpg,jpeg,png,gif,bmp}")) {

                    for (Path imagePath : imageFiles) {
                        totalImages++;
                        categoryImageCount++;
                        String fileName = imagePath.getFileName().toString();

                        // Limite pour accélérer (optionnel)
                        if (categoryImageCount > 70)
                            break;

                        try {
                            // Lire l'image directement
                            BufferedImage storedImage = ImageIO.read(imagePath.toFile());
                            if (storedImage != null) {
                                imagesLues++;

                                // Calculer la similarité
                                double similarity = calculateSimilarity(queryHistogram, storedImage);

                                // Garder seulement le meilleur pour cette catégorie
                                if (similarity > bestSimilarityForCategory) {
                                    bestSimilarityForCategory = similarity;
                                    bestFileForCategory = fileName;
                                }

                            }

                        } catch (Exception e) {
                            // Ignorer les erreurs d'images individuelles
                        }
                    }
                }

                // Stocker le meilleur de cette catégorie
                if (bestFileForCategory != null && bestSimilarityForCategory > 0.1) {
                    bestMatchPerCategory.put(categoryName, new CategoryBestMatch(
                            categoryName, bestFileForCategory, bestSimilarityForCategory, categoryImageCount));

                    System.out.println("   ⭐ Meilleur: " + bestFileForCategory +
                            " = " + String.format("%.4f", bestSimilarityForCategory));
                }
            }
        }

        System.out.println(" === RÉSUMÉ OPTIMISÉ ===");
        System.out.println("   Images scannées: " + totalImages);
        System.out.println("   Images lues: " + imagesLues);
        System.out.println("   Catégories avec matches: " + bestMatchPerCategory.size());

        // 5. Trier les catégories par meilleure similarité
        List<CategoryBestMatch> sortedMatches = bestMatchPerCategory.values().stream()
                .sorted((a, b) -> Double.compare(b.getBestSimilarity(), a.getBestSimilarity()))
                .limit(maxResults)
                .collect(Collectors.toList());

        System.out.println(" === TOP CATÉGORIES ===");
        for (int i = 0; i < Math.min(5, sortedMatches.size()); i++) {
            CategoryBestMatch match = sortedMatches.get(i);
            System.out.println("   " + (i + 1) + ". " + match.getCategoryName() +
                    " = " + String.format("%.6f", match.getBestSimilarity()) +
                    " (" + match.getBestFileName() + ")");
        }

        // 6. Filtrer par seuil (mais garder au moins le meilleur)
        List<CategoryBestMatch> filteredMatches = sortedMatches.stream()
                .filter(match -> match.getBestSimilarity() >= confidenceThreshold)
                .collect(Collectors.toList());

        // Si aucun résultat après filtrage, garder au moins le meilleur
        if (filteredMatches.isEmpty() && !sortedMatches.isEmpty()) {
            filteredMatches.add(sortedMatches.get(0));
            System.out.println("⚠️ Aucun résultat au-dessus du seuil, garde le meilleur: " +
                    sortedMatches.get(0).getCategoryName());
        }

        System.out.println("✂️ === RÉSULTATS FINAUX ===");
        System.out.println("   Résultats gardés: " + filteredMatches.size());

        // 7. Enrichir avec les données de l'ontologie
        List<FoodRecognitionResult> results = new ArrayList<>();

        for (CategoryBestMatch categoryMatch : filteredMatches) {
            // Chercher l'aliment correspondant dans l'ontologie
            Food matchingFood = findFoodByCategory(categoryMatch.getCategoryName());

            FoodRecognitionResult result = new FoodRecognitionResult();
            result.setPredictedName(categoryMatch.getCategoryName().replace("_", " "));
            result.setConfidence(categoryMatch.getBestSimilarity());
            result.setFoodDetails(matchingFood);

            // Ingrédients de l'ontologie ou génériques
            if (matchingFood != null && matchingFood.getIngredients() != null) {
                result.setIngredients(matchingFood.getIngredients());
            } else {
                result.setIngredients(Arrays.asList(
                        categoryMatch.getCategoryName().replace("_", " "),
                        "Composants naturels"));
            }

            results.add(result);

            System.out.println("   ✅ " + categoryMatch.getCategoryName() +
                    " → " + (matchingFood != null ? "Ontologie trouvée" : "Pas dans ontologie") +
                    " (confiance: " + String.format("%.4f", categoryMatch.getBestSimilarity()) + ")");
        }

        System.out.println(" === RÉSULTAT FINAL ===");
        System.out.println("   Résultats retournés: " + results.size());
        if (!results.isEmpty()) {
            System.out.println("   🥇 MEILLEUR: " + results.get(0).getPredictedName() +
                    " avec " + String.format("%.2f%%", results.get(0).getConfidence() * 100));
        }

        return new ImageRecognitionResponse(
                results,
                true,
                sortedMatches.size(),
                filteredMatches.size());
    }

    /**
     * Cherche un aliment dans l'ontologie par nom de catégorie
     */
    private Food findFoodByCategory(String categoryName) {
        try {
            List<Food> allFoods = sparqlService.getAllFoods();

            // Recherche exacte
            for (Food food : allFoods) {
                if (food.getName().equals(categoryName)) {
                    return food;
                }
            }

            // Recherche approximative (en cas de différences de nommage)
            String cleanCategoryName = categoryName.replace("_", " ").toLowerCase();
            for (Food food : allFoods) {
                if (food.getName().toLowerCase().contains(cleanCategoryName) ||
                        cleanCategoryName.contains(food.getName().toLowerCase())) {
                    return food;
                }
            }

            return null;

        } catch (Exception e) {
            System.err.println("⚠️ Erreur recherche ontologie pour " + categoryName + ": " + e.getMessage());
            return null;
        }
    }

    // ===== MÉTHODES DE CALCUL INCHANGÉES =====

    private ImageHistogram calculateHistogram(BufferedImage image) {
        BufferedImage resized = resizeImage(image, 64, 64);
        int[] redHist = new int[16];
        int[] greenHist = new int[16];
        int[] blueHist = new int[16];

        int width = resized.getWidth();
        int height = resized.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = resized.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;

                redHist[red / 16]++;
                greenHist[green / 16]++;
                blueHist[blue / 16]++;
            }
        }

        return new ImageHistogram(redHist, greenHist, blueHist);
    }

    private double calculateSimilarity(ImageHistogram queryHist, BufferedImage storedImage) {
        ImageHistogram storedHist = calculateHistogram(storedImage);
        double distance = 0.0;

        for (int i = 0; i < 16; i++) {
            distance += Math.pow(queryHist.getRed()[i] - storedHist.getRed()[i], 2);
            distance += Math.pow(queryHist.getGreen()[i] - storedHist.getGreen()[i], 2);
            distance += Math.pow(queryHist.getBlue()[i] - storedHist.getBlue()[i], 2);
        }

        distance = Math.sqrt(distance);
        double maxDistance = Math.sqrt(3 * 16 * Math.pow(4096, 2));
        double similarity = 1.0 - (distance / maxDistance);

        return Math.max(0.0, Math.min(1.0, similarity));
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        return resized;
    }

    // ===== CLASSES INTERNES =====

    private static class ImageHistogram {
        private int[] red, green, blue;

        public ImageHistogram(int[] red, int[] green, int[] blue) {
            this.red = red;
            this.green = green;
            this.blue = blue;
        }

        public int[] getRed() {
            return red;
        }

        public int[] getGreen() {
            return green;
        }

        public int[] getBlue() {
            return blue;
        }
    }

    private static class ImageMatch {
        private String categoryName;
        private String fileName;
        private String fullPath;
        private double similarity;

        public ImageMatch(String categoryName, String fileName, String fullPath, double similarity) {
            this.categoryName = categoryName;
            this.fileName = fileName;
            this.fullPath = fullPath;
            this.similarity = similarity;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFullPath() {
            return fullPath;
        }

        public double getSimilarity() {
            return similarity;
        }
    }

    private static class CategoryBestMatch {
        private String categoryName;
        private String bestFileName;
        private double bestSimilarity;
        private int totalImages;

        public CategoryBestMatch(String categoryName, String bestFileName, double bestSimilarity, int totalImages) {
            this.categoryName = categoryName;
            this.bestFileName = bestFileName;
            this.bestSimilarity = bestSimilarity;
            this.totalImages = totalImages;
        }

        public String getCategoryName() {
            return categoryName;
        }

        public String getBestFileName() {
            return bestFileName;
        }

        public double getBestSimilarity() {
            return bestSimilarity;
        }

        public int getTotalImages() {
            return totalImages;
        }
    }
}