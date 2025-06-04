#!/bin/sh

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
while ! curl -f "$FUSEKI_URL/\$/ping" >/dev/null 2>&1; do
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
DATASETS_RESPONSE=$(curl -s "$FUSEKI_URL/\$/datasets" 2>/dev/null || echo "{}")

if echo "$DATASETS_RESPONSE" | grep -q "\"$DATASET\""; then
    echo "✅ Dataset '$DATASET' already exists"
    echo "ℹ️  Skipping initialization - dataset found"
else
    echo "📝 Creating dataset '$DATASET'..."
    
    # Create the dataset using the correct endpoint
    CREATE_RESPONSE=$(curl -s -X POST "$FUSEKI_URL/\$/datasets" \
         -H "Content-Type: application/x-www-form-urlencoded" \
         -d "dbName=$DATASET&dbType=tdb2" 2>/dev/null || echo "error")
    
    if [ "$CREATE_RESPONSE" != "error" ]; then
        echo "✅ Dataset '$DATASET' created successfully!"
        
        # Look for RDF files to load
        echo "📂 Looking for data files in $DATA_DIR..."
        
        # Find RDF files
        RDF_FILES=""
        if [ -d "$DATA_DIR" ]; then
            # Use simple find that works in minimal containers
            for ext in ttl rdf owl nt n3; do
                files=$(find "$DATA_DIR" -name "*.$ext" 2>/dev/null || true)
                if [ -n "$files" ]; then
                    RDF_FILES="$RDF_FILES $files"
                fi
            done
        fi
        
        if [ -n "$RDF_FILES" ]; then
            echo "📊 Found RDF files to load:"
            echo "$RDF_FILES"
            
            # Load each RDF file
            for file in $RDF_FILES; do
                if [ -f "$file" ]; then
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
                    if curl -s -X POST "$FUSEKI_URL/$DATASET/data" \
                            -H "Content-Type: $CONTENT_TYPE" \
                            --data-binary "@$file" >/dev/null 2>&1; then
                        echo "✅ Successfully loaded $(basename "$file")"
                    else
                        echo "❌ Failed to load $(basename "$file")"
                    fi
                fi
            done
            
            echo "🔍 Verifying data was loaded..."
            # Simple verification without complex query parsing
            QUERY="SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o }"
            ENCODED_QUERY=$(echo "$QUERY" | sed 's/ /%20/g; s/?/%3F/g; s/{/%7B/g; s/}/%7D/g; s/(/%28/g; s/)/%29/g; s/\*/%2A/g')
            
            COUNT_RESULT=$(curl -s "$FUSEKI_URL/$DATASET/sparql?query=$ENCODED_QUERY" 2>/dev/null || echo "")
            
            if [ -n "$COUNT_RESULT" ]; then
                echo "📊 Data loading completed"
            else
                echo "⚠️  Could not verify data loading"
            fi
            
        else
            echo "⚠️  No RDF files found in $DATA_DIR"
            echo "💡 Dataset created but no data loaded"
            echo "📁 Contents of $DATA_DIR:"
            ls -la "$DATA_DIR" 2>/dev/null || echo "Directory not accessible"
        fi
        
    else
        echo "❌ Failed to create dataset '$DATASET'"
        exit 1
    fi
fi

echo "🎉 Fuseki initialization completed!"
echo "🌐 Access Fuseki admin interface at: $FUSEKI_URL"
echo "📊 Dataset '$DATASET' is ready for use"