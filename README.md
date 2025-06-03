# NutriGraph: Advanced Food Intelligence Platform

NutriGraph is a comprehensive food intelligence platform that combines semantic knowledge graphs, AI-powered image recognition, and advanced search capabilities to provide users with detailed information about foods, their nutritional profiles, cultural contexts, and more.

## 🌟 Features

### 1. Multi-Modal Food Search
- **Text Search**: Find foods by name, ingredients, or properties
- **Image Recognition**: Upload food photos for AI-powered identification
- **Cultural Explorer**: Discover regional and traditional cuisines
- **Nutrition Focus**: Filter foods by nutritional requirements

### 2. AI-Powered Image Recognition
- Upload images of food for automatic identification
- Receive confidence scores and detailed food information
- View similar foods and their nutritional profiles

### 3. Comprehensive Food Data
- Detailed nutritional information (calories, protein, carbs, fat, etc.)
- Cultural context and regional origins
- Cooking methods and traditional preparations
- Ingredient compositions

### 4. Interactive User Interface
- Modern, responsive design with glass-card UI elements
- Real-time search with autocomplete suggestions
- Dynamic filtering and sorting options
- Detailed food cards with visual representations

### 5. Knowledge Graph Integration
- Powered by Apache Jena Fuseki SPARQL endpoint
- Semantic relationships between foods, ingredients, and properties
- Ontology-based food classification

## 🛠️ Technology Stack

- **Backend**: Java Spring Boot
- **Frontend**: React (embedded in HTML with Babel)
- **Knowledge Graph**: Apache Jena Fuseki (RDF/SPARQL)
- **Search Engine**: Apache Lucene
- **Image Recognition**: AI-based food recognition system
- **Containerization**: Docker

## 🚀 Getting Started with Docker

### Prerequisites
- Docker and Docker Compose installed on your system
- Git (to clone the repository)

### Setup and Running

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Food_Ontology
   ```

2. **Start the Docker environment**
   ```bash
   docker-compose up -d
   ```
   This will:
   - Start the Apache Jena Fuseki server on port 3030
   - Build and start the NutriGraph application on port 8080
   - Mount the necessary volumes for data persistence

3. **Access the applications**
   - NutriGraph Web Interface: http://localhost:8080
   - Fuseki Admin Interface: http://localhost:3030 

4. **Stop the environment**
   ```bash
   docker-compose down
   ```

## 📊 Project Structure

```
Food_Ontology/
├── nutrigraph/                      # Main application
│   ├── src/                         # Source code
│   │   ├── main/java/com/nutrigraph/
│   │   │   ├── controller/          # REST and web controllers
│   │   │   ├── model/               # Data models
│   │   │   ├── service/             # Business logic services
│   │   │   └── ...                  # Configuration and utilities
│   │   └── main/resources/
│   │       ├── static/              # Frontend resources
│   │       │   └── nutrigraph-frontend.html  # Main React application
│   │       └── application.properties # Configuration
│   └── lucene-index-african/        # Lucene search index
├── apache-jena-fuseki-5.4.0/        # Fuseki SPARQL server
│   └── run/databases/african-middle-eastern-kg/  # Knowledge graph data
├── Serializer/                      # Data processing utilities
│   └── african_middle_eastern_data/ # Food data and images
├── Dockerfile                       # Docker image definition
└── docker-compose.yml               # Docker environment configuration
```

## 🔍 Using NutriGraph

### Text Search
1. Select the "Text Search" tab
2. Enter a food name, ingredient, or description
3. Use autocomplete suggestions or press Enter to search
4. Browse results with sorting and pagination options

### Image Recognition
1. Select the "Image Recognition" tab
2. Upload a food image by dragging and dropping or clicking to browse
3. Click "Identify Food" to start the AI recognition process
4. View recognition results with confidence scores
5. Click "View Details" for more information about identified foods

### Cultural Explorer
1. Select the "Cultural Explorer" tab
2. Filter foods by region, cooking method, and spice level
3. Click "Explore Cultures" to search based on filters
4. Browse results to discover foods from different cultures

### Nutrition Focus
1. Select the "Nutrition Focus" tab
2. Set filters for calories, protein, carbohydrates, and fat content
3. Click "Find Nutrition Match" to search based on nutritional requirements
4. Browse results sorted by nutritional relevance

## 🧩 API Endpoints

The NutriGraph platform provides a comprehensive REST API:

- `GET /api/foods/search` - Search foods with text and filters
- `GET /api/foods/autocomplete` - Get autocomplete suggestions
- `GET /api/foods/{id}` - Get detailed information about a specific food
- `POST /api/foods/recognize` - Identify food from an uploaded image
- `GET /api/metadata` - Get metadata for filtering options

## 🔄 Data Flow

1. **Knowledge Graph**: Food data is stored in RDF format in the Apache Jena Fuseki server
2. **Search Index**: Apache Lucene provides fast text search capabilities
3. **Spring Backend**: Processes requests, queries the knowledge graph, and serves data
4. **React Frontend**: Provides an interactive UI for searching and displaying results
5. **Image Recognition**: Processes uploaded images and identifies foods

## 🛡️ Security and Performance

- CORS configuration for secure API access
- Optimized search with Apache Lucene
- Efficient SPARQL queries for knowledge graph access
- Responsive design for various device sizes
- Image processing optimizations

## 🤝 Contributing

Contributions to NutriGraph are welcome! Please feel free to submit a Pull Request.

## 📜 License

This project is licensed under the [MIT License](LICENSE).
