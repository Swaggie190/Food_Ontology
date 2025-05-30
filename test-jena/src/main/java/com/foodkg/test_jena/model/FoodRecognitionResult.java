package com.foodkg.test_jena.model;


import java.util.List;

public class FoodRecognitionResult {
    private String predictedName;
    private double confidence;
    private Food foodDetails;
    private List<String> ingredients;

    // Constructeurs
    public FoodRecognitionResult() {}

    public FoodRecognitionResult(String predictedName, double confidence) {
        this.predictedName = predictedName;
        this.confidence = confidence;
    }

    // Getters et setters
    public String getPredictedName() { return predictedName; }
    public void setPredictedName(String predictedName) { this.predictedName = predictedName; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public Food getFoodDetails() { return foodDetails; }
    public void setFoodDetails(Food foodDetails) { this.foodDetails = foodDetails; }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
}