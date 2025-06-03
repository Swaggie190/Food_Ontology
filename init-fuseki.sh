#!/bin/bash

set -e

# Configuration
FUSEKI_URL=${FUSEKI_SERVER:-"http://fuseki:3030"}
DATASET=${DATASET_NAME:-"african-middle-eastern-kg"}
DATA_DIR="/data"

echo "🚀 Starting Fuseki database initialization..."
echo "📍 Fuseki URL: $FUSEKI_URL"
echo "📊 Dataset: $DATASET"
echo "📁 Data directory: $DATA_DIR"

# Wait for Fuseki to be ready
echo "⏳ Waiting for Fuseki server to be ready..."
timeout=60
counter=0
while ! curl -f "$FUSEKI_URL/$/ping" >/dev/null 2>&1; do
    counter=$((counter + 1))
    if [ $counter -gt $timeout ]; then
        echo "❌ Timeout waiting for Fuseki server"
        exit 1
    fi
    echo "   Waiting... ($counter/$timeout)"
    sleep 1
done

echo "✅ Fuseki server is ready!"

# Check if dataset already exists
echo "🔍 Checking if dataset '$DATASET' exists..."
if curl -f "$FUSEKI_URL/$/datasets" 2>/dev/null | grep -q "\"ds.name\" : \"/$DATASET\""; then
    echo "✅ Dataset '$DATASET' already exists, checking if it has data..."
    
    # Check if dataset has data by running a simple COUNT query
    QUERY="SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o }"
    ENCODED_QUERY=$(echo "$QUERY" | sed 's/ /%20/g' | sed 's/?/%3F/g' | sed 's/{/%7B/g' | sed 's/}/%7D/g' | sed 's/(/%28/g' | sed 's/)/%29/g')
    
    if curl -f "$FUSEKI_URL/$DATASET/sparql?query=$ENCODED_QUERY" 2>/dev/null | grep -q '"value" : "0"'; then
        echo "⚠️  Dataset exists but is empty, proceeding with data loading..."
        LOAD_DATA=true
    else
        echo "✅ Dataset has data, skipping initialization."
        LOAD_DATA=false
    fi
else
    echo "📝 Creating dataset '$DATASET'..."
    
    # Create the dataset
    curl -X POST "$FUSEKI_URL/$/datasets" \
         -H "Content-Type: application/x-www-form-urlencoded" \
         -d "dbName=$DATASET&dbType=tdb2" \
         --fail --silent --show-error
    
    if [ $? -eq 0 ]; then
        echo "✅ Dataset '$DATASET' created successfully!"
        LOAD_DATA=true
    else
        echo "❌ Failed to create dataset '$DATASET'"
        exit 1
    fi
fi

# Load data if needed
if [ "$LOAD_DATA" = true ]; then
    echo "📂 Looking for data files in $DATA_DIR..."
    
    # Common RDF file extensions
    RDF_FILES=$(find "$DATA_DIR" -name "*.ttl" -o -name "*.rdf" -o -name "*.nt" -o -name "*.n3" -o -name "*.owl" 2>/dev/null || true)
    
    if [ -z "$RDF_FILES" ]; then
        echo "⚠️  No RDF files found in $DATA_DIR"
        echo "📁 Contents of $DATA_DIR:"
        ls -la "$DATA_DIR" 2>/dev/null || echo "Directory not accessible"
        
        # Look for alternative file structures
        if [ -d "$DATA_DIR/ontologies" ]; then
            echo "🔍 Found ontologies directory, checking for RDF files..."
            RDF_FILES=$(find "$DATA_DIR/ontologies" -name "*.ttl" -o -name "*.rdf" -o -name "*.nt" -o -name "*.n3" -o -name "*.owl" 2>/dev/null || true)
        fi
        
        if [ -d "$DATA_DIR/rdf" ]; then
            echo "🔍 Found rdf directory, checking for RDF files..."
            RDF_FILES=$(find "$DATA_DIR/rdf" -name "*.ttl" -o -name "*.rdf" -o -name "*.nt" -o -name "*.n3" -o -name "*.owl" 2>/dev/null || true)
        fi
    fi
    
    if [ -n "$RDF_FILES" ]; then
        echo "📊 Found RDF files to load:"
        echo "$RDF_FILES"
        
        # Load each RDF file
        for file in $RDF_FILES; do
            echo "📥 Loading file: $(basename "$file")"
            
            # Determine content type based on file extension
            case "${file##*.}" in
                ttl) CONTENT_TYPE="text/turtle" ;;
                rdf|owl) CONTENT_TYPE="application/rdf+xml" ;;
                nt) CONTENT_TYPE="application/n-triples" ;;
                n3) CONTENT_TYPE="text/n3" ;;
                *) CONTENT_TYPE="text/turtle" ;;
            esac
            
            # Upload the file
            if curl -X POST "$FUSEKI_URL/$DATASET/data" \
                    -H "Content-Type: $CONTENT_TYPE" \
                    --data-binary "@$file" \
                    --fail --silent --show-error; then
                echo "✅ Successfully loaded $(basename "$file")"
            else
                echo "❌ Failed to load $(basename "$file")"
                # Don't exit, try to load other files
            fi
        done
        
        # Verify data was loaded
        echo "🔍 Verifying data was loaded..."
        QUERY="SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o }"
        ENCODED_QUERY=$(echo "$QUERY" | sed 's/ /%20/g' | sed 's/?/%3F/g' | sed 's/{/%7B/g' | sed 's/}/%7D/g' | sed 's/(/%28/g' | sed 's/)/%29/g')
        
        RESULT=$(curl -f "$FUSEKI_URL/$DATASET/sparql?query=$ENCODED_QUERY" 2>/dev/null)
        COUNT=$(echo "$RESULT" | grep -o '"value" : "[0-9]*"' | grep -o '[0-9]*' || echo "0")
        
        echo "📊 Total triples loaded: $COUNT"
        
        if [ "$COUNT" -gt "0" ]; then
            echo "✅ Data successfully loaded into dataset '$DATASET'"
        else
            echo "⚠️  No data found in dataset after loading"
        fi
    else
        echo "⚠️  No RDF files found to load"
        echo "💡 You may need to manually add your ontology files to the data directory"
    fi
else
    echo "⏭️  Skipping data loading as dataset already contains data"
fi

echo "🎉 Fuseki initialization completed!"
echo "🌐 Access Fuseki admin interface at: $FUSEKI_URL"
echo "📊 Dataset '$DATASET' is ready for use"