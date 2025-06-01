package com.nutrigraph.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class SearchRequest {
    private String query;
    private String foodClass;
    private String foodGroup;
    
    @Min(0) @Max(1000)
    private Double minCalories;
    
    @Min(0) @Max(1000) 
    private Double maxCalories;
    
    @Min(0) @Max(100)
    private Double minProtein;
    
    @Min(0) @Max(100)
    private Double maxProtein;
    
    private int page = 0;
    private int size = 20;
    private String sortBy = "name";
    private String sortDirection = "asc";
    
    // Constructeurs
    public SearchRequest() {}
    
    // Getters et setters
    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }
    
    public String getFoodClass() { return foodClass; }
    public void setFoodClass(String foodClass) { this.foodClass = foodClass; }
    
    public String getFoodGroup() { return foodGroup; }
    public void setFoodGroup(String foodGroup) { this.foodGroup = foodGroup; }
    
    public Double getMinCalories() { return minCalories; }
    public void setMinCalories(Double minCalories) { this.minCalories = minCalories; }
    
    public Double getMaxCalories() { return maxCalories; }
    public void setMaxCalories(Double maxCalories) { this.maxCalories = maxCalories; }
    
    public Double getMinProtein() { return minProtein; }
    public void setMinProtein(Double minProtein) { this.minProtein = minProtein; }
    
    public Double getMaxProtein() { return maxProtein; }
    public void setMaxProtein(Double maxProtein) { this.maxProtein = maxProtein; }
    
    public int getPage() { return page; }
    public void setPage(int page) { this.page = page; }
    
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }

}
