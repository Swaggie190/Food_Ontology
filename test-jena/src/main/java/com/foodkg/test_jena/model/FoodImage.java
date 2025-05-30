package com.foodkg.test_jena.model;

public class FoodImage {
    private String uri;
    private String imagePath;
    private String filename;
    private Integer width;
    private Integer height;
    
    // Constructeurs
    public FoodImage() {}
    
    public FoodImage(String imagePath, String filename) {
        this.imagePath = imagePath;
        this.filename = filename;
    }
    
    // Getters et setters
    public String getUri() { return uri; }
    public void setUri(String uri) { this.uri = uri; }
    
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    
    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }
    
    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

}
