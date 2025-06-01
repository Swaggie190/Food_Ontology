package com.nutrigraph;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nutrigraph")
public class NutriGraphConfig {

    private String fusekiUrl = "http://localhost:3030";
    private String datasetName = "african-middle-eastern-kg";
    private String luceneIndexPath = "./lucene-index";
    private String imagesBasePath = "C:/Users/Swaggie/Desktop/Food_Ontology/Serializer/african_middle_eastern_data/images";
    private boolean enableNutritionalAnalysis = true;
    private boolean enableCulturalContext = true;

    // Getters et setters
    public String getFusekiUrl() {
        return fusekiUrl;
    }

    public void setFusekiUrl(String fusekiUrl) {
        this.fusekiUrl = fusekiUrl;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public void setDatasetName(String datasetName) {
        this.datasetName = datasetName;
    }

    public String getLuceneIndexPath() {
        return luceneIndexPath;
    }

    public void setLuceneIndexPath(String luceneIndexPath) {
        this.luceneIndexPath = luceneIndexPath;
    }

    public boolean isEnableNutritionalAnalysis() { return enableNutritionalAnalysis; }
    public void setEnableNutritionalAnalysis(boolean enableNutritionalAnalysis) { 
        this.enableNutritionalAnalysis = enableNutritionalAnalysis; 
    }

    public boolean isEnableCulturalContext() { return enableCulturalContext; }
    public void setEnableCulturalContext(boolean enableCulturalContext) { 
        this.enableCulturalContext = enableCulturalContext; 
    }

    public String getImagesBasePath() {
        return imagesBasePath;
    }

    public void setImagesBasePath(String imagesBasePath) {
        this.imagesBasePath = imagesBasePath;
    }

    public String getSparqlEndpoint() {
        return fusekiUrl + "/" + datasetName + "/sparql";
    }
}
