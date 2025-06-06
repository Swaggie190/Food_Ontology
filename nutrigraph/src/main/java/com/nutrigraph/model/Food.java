package com.nutrigraph.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Food {
    private String uri;
    private String name;
    private String description;
    private String foodClass;
    private String classLabel;
    private String foodGroup;
    
    // 🎯 NOUVELLES PROPRIÉTÉS SPÉCIALISÉES
    private String region;
    private String cookingMethod;
    private String spiceLevel;
    private String culturalSignificance;
    private String foodType;
    
    // Données nutritionnelles
    private Double calories;
    private Double protein;
    private Double carbohydrates;
    private Double fat;
    private Double fiber;
    private Double sodium;
    private Double sugar;
    
    // Relations
    private List<String> ingredients;
    private List<FoodImage> images;
    
    // Score de recherche (Lucene)
    private Float searchScore;
    
    // Constructeurs
    public Food() {}
    
    public Food(String uri, String name) {
        this.uri = uri;
        this.name = name;
    }
    
    // 🎯 GETTERS ET SETTERS EXISTANTS
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getFoodClass() { return foodClass; }
    public void setFoodClass(String foodClass) { this.foodClass = foodClass; }
    
    public String getClassLabel() { return classLabel; }
    public void setClassLabel(String classLabel) { this.classLabel = classLabel; }
    
    public String getFoodGroup() { return foodGroup; }
    public void setFoodGroup(String foodGroup) { this.foodGroup = foodGroup; }
    
    // 🎯 NOUVEAUX GETTERS ET SETTERS SPÉCIALISÉS
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    
    public String getCookingMethod() { return cookingMethod; }
    public void setCookingMethod(String cookingMethod) { this.cookingMethod = cookingMethod; }
    
    public String getSpiceLevel() { return spiceLevel; }
    public void setSpiceLevel(String spiceLevel) { this.spiceLevel = spiceLevel; }
    
    public String getCulturalSignificance() { return culturalSignificance; }
    public void setCulturalSignificance(String culturalSignificance) { this.culturalSignificance = culturalSignificance; }
    
    public String getFoodType() { return foodType; }
    public void setFoodType(String foodType) { this.foodType = foodType; }
    
    // Getters/setters nutritionnels (inchangés)
    public Double getCalories() { return calories; }
    public void setCalories(Double calories) { this.calories = calories; }
    
    public Double getProtein() { return protein; }
    public void setProtein(Double protein) { this.protein = protein; }
    
    public Double getCarbohydrates() { return carbohydrates; }
    public void setCarbohydrates(Double carbohydrates) { this.carbohydrates = carbohydrates; }
    
    public Double getFat() { return fat; }
    public void setFat(Double fat) { this.fat = fat; }
    
    public Double getFiber() { return fiber; }
    public void setFiber(Double fiber) { this.fiber = fiber; }
    
    public Double getSodium() { return sodium; }
    public void setSodium(Double sodium) { this.sodium = sodium; }
    
    public Double getSugar() { return sugar; }
    public void setSugar(Double sugar) { this.sugar = sugar; }
    
    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
    
    public List<FoodImage> getImages() { return images; }
    public void setImages(List<FoodImage> images) { this.images = images; }
    
    public Float getSearchScore() { return searchScore; }
    public void setSearchScore(Float searchScore) { this.searchScore = searchScore; }
}