package com.foodkg.test_jena;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "food.kg")
public class FoodKGConfig {

    private String fusekiUrl = "http://localhost:3030";
    private String datasetName = "african-middle-eastern-foods";
    private String luceneIndexPath = "./lucene-index";
    private String imagesBasePath = "C:/Users/Swaggie/Desktop/Food_Ontology/Serializer/african_middle_eastern_data/images";

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
