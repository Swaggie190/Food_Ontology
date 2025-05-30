// ===== ImageRecognitionController.java CORRIGÉ =====
package com.foodkg.test_jena.controler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.foodkg.test_jena.model.ImageRecognitionResponse;
import com.foodkg.test_jena.service.ImageSimilarityService;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/foods")
@CrossOrigin(origins = "*")
public class ImageRecognitionController {

    @Autowired
    private ImageSimilarityService similarityService;

    @PostMapping("/recognize")
    public ResponseEntity<ImageRecognitionResponse> recognizeFood(
            @RequestParam("image") MultipartFile imageFile,
            @RequestParam(defaultValue = "0.01") Double confidenceThreshold, // ← TRÈS BAS pour debug
            @RequestParam(defaultValue = "6") Integer maxResults) {
        try {
            System.out.println("📸 Nouvelle image reçue: " + imageFile.getOriginalFilename());
            System.out.println("📏 Taille: " + imageFile.getSize() + " bytes");
            System.out.println("📄 Content-Type: " + imageFile.getContentType());

            if (imageFile.isEmpty()) {
                throw new IllegalArgumentException("Fichier image requis");
            }

            // ===== VALIDATION CORRIGÉE DU TYPE DE FICHIER =====
            String contentType = imageFile.getContentType();
            String filename = imageFile.getOriginalFilename();

            boolean isValidImage = false;

            // Vérifier par Content-Type
            if (contentType != null && contentType.startsWith("image/")) {
                isValidImage = true;
            }

            // Vérifier par extension de fichier (fallback)
            if (!isValidImage && filename != null) {
                String lowerFilename = filename.toLowerCase();
                if (lowerFilename.endsWith(".jpg") ||
                        lowerFilename.endsWith(".jpeg") ||
                        lowerFilename.endsWith(".png") ||
                        lowerFilename.endsWith(".gif") ||
                        lowerFilename.endsWith(".bmp") ||
                        lowerFilename.endsWith(".webp")) {
                    isValidImage = true;
                    System.out.println("✅ Image validée par extension: " + filename);
                }
            }

            if (!isValidImage) {
                throw new IllegalArgumentException(
                        "Le fichier doit être une image. Reçu: contentType=" + contentType +
                                ", filename=" + filename);
            }

            System.out.println("✅ Image validée, début de la reconnaissance...");

            ImageRecognitionResponse response = similarityService.recognizeFood(
                    imageFile, confidenceThreshold, maxResults);

            System.out.println("✅ Reconnaissance réussie: " + response.getFilteredPredictions() + " résultats");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur reconnaissance: " + e.getMessage());
            e.printStackTrace();

            // Réponse d'erreur détaillée
            ImageRecognitionResponse errorResponse = new ImageRecognitionResponse();
            errorResponse.setImageProcessed(false);
            errorResponse.setResults(new ArrayList<>());
            errorResponse.setTotalPredictions(0);
            errorResponse.setFilteredPredictions(0);
            errorResponse.setError("Erreur lors de la reconnaissance: " + e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ===== ENDPOINT DE TEST =====
    @GetMapping("/test-recognition")
    public ResponseEntity<String> testRecognition() {
        return ResponseEntity.ok("✅ Endpoint de reconnaissance d'images disponible");
    }
}
