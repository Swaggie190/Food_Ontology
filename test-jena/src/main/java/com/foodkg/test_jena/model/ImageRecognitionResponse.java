package com.foodkg.test_jena.model;

import java.util.List;

public class ImageRecognitionResponse {
    private List<FoodRecognitionResult> results;
    private boolean imageProcessed;
    private int totalPredictions;
    private int filteredPredictions;
    private String error;

    // Constructeurs
    public ImageRecognitionResponse() {
    }

    public ImageRecognitionResponse(List<FoodRecognitionResult> results,
            boolean imageProcessed,
            int totalPredictions,
            int filteredPredictions) {
        this.results = results;
        this.imageProcessed = imageProcessed;
        this.totalPredictions = totalPredictions;
        this.filteredPredictions = filteredPredictions;
    }

    // Getters et setters
    public List<FoodRecognitionResult> getResults() {
        return results;
    }

    public void setResults(List<FoodRecognitionResult> results) {
        this.results = results;
    }

    public boolean isImageProcessed() {
        return imageProcessed;
    }

    public void setImageProcessed(boolean imageProcessed) {
        this.imageProcessed = imageProcessed;
    }

    public int getTotalPredictions() {
        return totalPredictions;
    }

    public void setTotalPredictions(int totalPredictions) {
        this.totalPredictions = totalPredictions;
    }

    public int getFilteredPredictions() {
        return filteredPredictions;
    }

    public void setFilteredPredictions(int filteredPredictions) {
        this.filteredPredictions = filteredPredictions;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
