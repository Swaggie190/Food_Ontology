#!/bin/sh

set -e

FUSEKI_URL=${FUSEKI_SERVER:-"http://fuseki:3030"}
DATASET=${DATASET_NAME:-"african-middle-eastern-kg"}
DATA_DIR="/data"

echo "🚀 Starting automatic data loading..."
echo "📍 Fuseki URL: $FUSEKI_URL"
echo "📊 Dataset: $DATASET"

# Wait for Fuseki to be ready
echo "⏳ Waiting for Fuseki..."
for i in $(seq 1 60); do
    if curl -f "$FUSEKI_URL/\$/ping" >/dev/null 2>&1; then
        echo "✅ Fuseki is ready!"
        break
    fi
    echo "   Attempt $i/60..."
    sleep 2
done

# Check if dataset exists and has data
echo "🔍 Checking dataset status..."
DATASETS=$(curl -s "$FUSEKI_URL/\$/datasets" 2>/dev/null || echo "{}")

if echo "$DATASETS" | grep -q "\"$DATASET\""; then
    echo "✅ Dataset '$DATASET' exists"
    
    # Check if it has data
    QUERY="SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o }"
    ENCODED_QUERY=$(echo "$QUERY" | sed 's/ /%20/g; s/?/%3F/g; s/{/%7B/g; s/}/%7D/g; s/(/%28/g; s/)/%29/g; s/\*/%2A/g')
    RESULT=$(curl -s "$FUSEKI_URL/$DATASET/sparql?query=$ENCODED_QUERY" 2>/dev/null || echo "")
    
    if echo "$RESULT" | grep -q '"value"[[:space:]]*:[[:space:]]*"0"'; then
        echo "⚠️  Dataset is empty, loading data..."
        LOAD_DATA=true
    else
        echo "✅ Dataset has data, skipping load"
        LOAD_DATA=false
    fi
else
    echo "📝 Creating dataset '$DATASET'..."
    curl -X POST "$FUSEKI_URL/\$/datasets" \
         -H "Content-Type: application/x-www-form-urlencoded" \
         -d "dbName=$DATASET&dbType=tdb2" \
         --silent --show-error
    
    if [ $? -eq 0 ]; then
        echo "✅ Dataset created successfully!"
        LOAD_DATA=true
    else
        echo "❌ Failed to create dataset"
        exit 1
    fi
fi

# Load data if needed
if [ "$LOAD_DATA" = "true" ]; then
    echo "📂 Scanning for RDF files in $DATA_DIR..."
    
    # Find RDF files
    RDF_FILES=""
    if [ -d "$DATA_DIR" ]; then
        for ext in ttl rdf owl nt n3; do
            files=$(find "$DATA_DIR" -name "*.$ext" -type f 2>/dev/null || true)
            RDF_FILES="$RDF_FILES $files"
        done
    fi
    
    # Remove leading/trailing spaces
    RDF_FILES=$(echo "$RDF_FILES" | xargs)
    
    if [ -n "$RDF_FILES" ]; then
        echo "📊 Found RDF files:"
        for file in $RDF_FILES; do
            echo "   - $(basename "$file")"
        done
        
        # Load each file
        LOADED_COUNT=0
        for file in $RDF_FILES; do
            if [ -f "$file" ]; then
                filename=$(basename "$file")
                echo "📥 Loading $filename..."
                
                # Determine content type
                case "${file##*.}" in
                    ttl) CONTENT_TYPE="text/turtle" ;;
                    rdf|owl) CONTENT_TYPE="application/rdf+xml" ;;
                    nt) CONTENT_TYPE="application/n-triples" ;;
                    n3) CONTENT_TYPE="text/n3" ;;
                    *) CONTENT_TYPE="text/turtle" ;;
                esac
                
                # Upload file
                if curl -X POST "$FUSEKI_URL/$DATASET/data" \
                        -H "Content-Type: $CONTENT_TYPE" \
                        --data-binary "@$file" \
                        --silent --show-error; then
                    echo "✅ Loaded $filename"
                    LOADED_COUNT=$((LOADED_COUNT + 1))
                else
                    echo "❌ Failed to load $filename"
                fi
            fi
        done
        
        echo "📊 Loaded $LOADED_COUNT files successfully"
        
        # Verify final count
        echo "🔍 Verifying data..."
        FINAL_RESULT=$(curl -s "$FUSEKI_URL/$DATASET/sparql?query=$ENCODED_QUERY" 2>/dev/null || echo "")
        if [ -n "$FINAL_RESULT" ]; then
            COUNT=$(echo "$FINAL_RESULT" | grep -o '"value"[[:space:]]*:[[:space:]]*"[0-9]*"' | grep -o '[0-9]*' | head -1)
            echo "📊 Total triples in dataset: ${COUNT:-0}"
        fi
        
    else
        echo "⚠️  No RDF files found in $DATA_DIR"
        echo "📁 Directory contents:"
        ls -la "$DATA_DIR" 2>/dev/null || echo "Cannot access directory"
        
        # Create empty dataset anyway
        echo "✅ Empty dataset created - ready for manual data loading"
    fi
else
    echo "⏭️  Data loading skipped"
fi

echo "🎉 Data loading completed!"
echo "🌐 Fuseki admin: $FUSEKI_URL"
echo "📊 Dataset: $DATASET"
echo "🔗 SPARQL endpoint: $FUSEKI_URL/$DATASET/sparql"