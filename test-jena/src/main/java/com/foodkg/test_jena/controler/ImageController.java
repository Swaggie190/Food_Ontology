package com.foodkg.test_jena.controler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.foodkg.test_jena.FoodKGConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "*")
public class ImageController {

    @Autowired
    private FoodKGConfig config;

    /**
     * Servir une image spécifique
     * URL: /api/images/{category}/{filename}
     */
    @GetMapping("/{category}/{filename:.+}")
    public ResponseEntity<Resource> getImage(
            @PathVariable String category,
            @PathVariable String filename) {

        try {
            // Vos logs existants...
            System.out.println("=== Image Request ===");
            System.out.println("Category: " + category);
            System.out.println("Filename: " + filename);
            System.out.println("Base path: " + config.getImagesBasePath());

            Path imagePath = Paths.get(config.getImagesBasePath(), category, filename);
            System.out.println("Full path: " + imagePath.toAbsolutePath());

            Resource resource = new UrlResource(imagePath.toUri());
            System.out.println("Resource exists: " + resource.exists());
            System.out.println("Resource readable: " + resource.isReadable());

            if (resource.exists() && resource.isReadable()) {
                String contentType = getContentType(filename);
                System.out.println("Content type: " + contentType);

                // NOUVEAUX LOGS - Fixed for cross-platform compatibility
                System.out.println("File size: " + Files.size(imagePath) + " bytes");
                
                // Cross-platform file permissions check
                if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
                    try {
                        Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(imagePath);
                        System.out.println("File permissions: " + permissions);
                    } catch (UnsupportedOperationException e) {
                        System.out.println("File permissions: POSIX permissions not supported on this system");
                    }
                } else {
                    // For Windows, just show basic file attributes
                    System.out.println("File permissions: Windows system - readable: " + 
                                     Files.isReadable(imagePath) + ", writable: " + Files.isWritable(imagePath));
                }

                ResponseEntity<Resource> response = ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                        .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);

                System.out.println("✅ Response sent successfully for: " + filename);
                return response;

            } else {
                System.out.println("❌ Image not found or not readable");
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            System.err.println("❌ Error serving image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Placeholder image
     */
    @GetMapping("/placeholder/{width}x{height}")
    public ResponseEntity<String> getPlaceholder(
            @PathVariable int width,
            @PathVariable int height) {

        String placeholderUrl = String.format(
                "https://via.placeholder.com/%dx%d/667eea/ffffff?text=Food+Image",
                width, height);

        return ResponseEntity.ok(placeholderUrl);
    }

    /**
     * Test de l'endpoint
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        try {
            Path basePath = Paths.get(config.getImagesBasePath());
            String message = String.format(
                    "ImageController Test\n" +
                            "Base Path: %s\n" +
                            "Exists: %s\n" +
                            "Absolute: %s\n" +
                            "Timestamp: %s",
                    config.getImagesBasePath(),
                    Files.exists(basePath),
                    basePath.toAbsolutePath(),
                    System.currentTimeMillis());
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.ok("Error: " + e.getMessage());
        }
    }

    /**
     * Lister les images d'une catégorie
     */
    @GetMapping("/list/{category}")
    public ResponseEntity<String[]> listImages(@PathVariable String category) {
        try {
            Path categoryPath = Paths.get(config.getImagesBasePath(), category);

            if (!Files.exists(categoryPath) || !Files.isDirectory(categoryPath)) {
                return ResponseEntity.ok(new String[0]);
            }

            String[] images = Files.list(categoryPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase();
                        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
                                name.endsWith(".png") || name.endsWith(".gif") ||
                                name.endsWith(".webp");
                    })
                    .map(path -> path.getFileName().toString())
                    .toArray(String[]::new);

            return ResponseEntity.ok(images);

        } catch (Exception e) {
            return ResponseEntity.ok(new String[0]);
        }
    }

    /**
     * Déterminer le type MIME
     */
    private String getContentType(String filename) {
        try {
            String contentType = Files.probeContentType(Paths.get(filename));
            if (contentType != null) {
                return contentType;
            }
        } catch (Exception e) {
            // Utiliser le fallback
        }

        // Fallback basé sur l'extension
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "bmp" -> "image/bmp";
            default -> "image/jpeg"; // Default pour les images
        };
    }
}