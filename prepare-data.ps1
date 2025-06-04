# NutriGraph Data Preparation Script for Windows (Fixed)
# Save as prepare-data.ps1 and run with: powershell -ExecutionPolicy Bypass -File prepare-data.ps1

Write-Host "Database Preparing NutriGraph Database for Auto-Loading..." -ForegroundColor Blue

function Write-Status($message) { Write-Host "[INFO] $message" -ForegroundColor Cyan }
function Write-Success($message) { Write-Host "[SUCCESS] $message" -ForegroundColor Green }
function Write-Warning($message) { Write-Host "[WARNING] $message" -ForegroundColor Yellow }

# Create directories
Write-Status "Creating directory structure..."
$directories = @("rdf-data", "scripts", "datasets")
foreach ($dir in $directories) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        Write-Success "Created $dir directory"
    } else {
        Write-Status "$dir directory already exists"
    }
}

# Function to find and organize RDF files
function Organize-RDFFiles {
    Write-Status "Looking for RDF files..."
    
    $rdfExtensions = @("*.ttl", "*.rdf", "*.nt", "*.owl", "*.n3")
    $foundFiles = @()
    
    foreach ($ext in $rdfExtensions) {
        $files = Get-ChildItem -Path . -Filter $ext -Recurse | Where-Object { $_.FullName -notlike "*\rdf-data\*" }
        $foundFiles += $files
    }
    
    if ($foundFiles.Count -gt 0) {
        Write-Status "Found $($foundFiles.Count) RDF files"
        foreach ($file in $foundFiles) {
            Write-Host "  Found: $($file.Name)" -ForegroundColor Yellow
            Copy-Item $file.FullName -Destination "rdf-data\" -Force
            Write-Host "    -> Copied to rdf-data\" -ForegroundColor Green
        }
    } else {
        Write-Warning "No RDF files found in current directory tree"
    }
}

# Function to check for existing TDB databases
function Check-TDBDatabases {
    Write-Status "Checking for existing TDB databases..."
    
    $dbPath = "apache-jena-fuseki-5.4.0\run\databases"
    if (Test-Path $dbPath) {
        Write-Success "Found Fuseki databases directory: $dbPath"
        
        $datasets = Get-ChildItem -Path $dbPath -Directory
        if ($datasets.Count -gt 0) {
            foreach ($dataset in $datasets) {
                Write-Host "  Dataset: $($dataset.Name)" -ForegroundColor Cyan
                
                # Check if it contains TDB files
                $tdbFiles = Get-ChildItem -Path $dataset.FullName -Filter "*.dat"
                $idnFiles = Get-ChildItem -Path $dataset.FullName -Filter "*.idn"
                
                if ($tdbFiles.Count -gt 0 -or $idnFiles.Count -gt 0) {
                    Write-Host "    Contains TDB data files" -ForegroundColor Green
                } else {
                    Write-Host "    Directory exists but no TDB files found" -ForegroundColor Yellow
                }
            }
        } else {
            Write-Warning "Databases directory is empty"
        }
    } else {
        Write-Warning "No existing Fuseki databases directory found"
        Write-Status "Creating empty structure..."
        New-Item -ItemType Directory -Path $dbPath -Force | Out-Null
    }
}

# Function to create sample SPARQL scripts
function Create-SampleScripts {
    Write-Status "Creating sample initialization scripts..."
    
    # Create ontology setup script
    $ontologyScript = @"
# SPARQL Update to set up basic ontology structure
PREFIX : <http://example.org/food-ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>

INSERT DATA {
    # Main food class
    :Food a owl:Class ;
          rdfs:label "Food" ;
          rdfs:comment "Base class for all food items" .
    
    # Food categories
    :Grain a owl:Class ;
           rdfs:subClassOf :Food ;
           rdfs:label "Grain" .
    
    :Bread a owl:Class ;
           rdfs:subClassOf :Food ;
           rdfs:label "Bread" .
    
    :Dip a owl:Class ;
         rdfs:subClassOf :Food ;
         rdfs:label "Dip" .
    
    :Vegetable a owl:Class ;
              rdfs:subClassOf :Food ;
              rdfs:label "Vegetable" .
    
    :Fruit a owl:Class ;
           rdfs:subClassOf :Food ;
           rdfs:label "Fruit" .
    
    :Spice a owl:Class ;
           rdfs:subClassOf :Food ;
           rdfs:label "Spice" .
    
    # Properties
    :name a owl:DatatypeProperty ;
          rdfs:domain :Food ;
          rdfs:range rdfs:Literal ;
          rdfs:label "name" .
    
    :calories a owl:DatatypeProperty ;
             rdfs:domain :Food ;
             rdfs:range xsd:double ;
             rdfs:label "calories per 100g" .
    
    :protein a owl:DatatypeProperty ;
            rdfs:domain :Food ;
            rdfs:range xsd:double ;
            rdfs:label "protein content in grams" .
    
    :carbohydrates a owl:DatatypeProperty ;
                  rdfs:domain :Food ;
                  rdfs:range xsd:double ;
                  rdfs:label "carbohydrate content in grams" .
    
    :region a owl:DatatypeProperty ;
           rdfs:domain :Food ;
           rdfs:range rdfs:Literal ;
           rdfs:label "geographical region of origin" .
    
    :spiceLevel a owl:DatatypeProperty ;
               rdfs:domain :Food ;
               rdfs:range rdfs:Literal ;
               rdfs:label "spice level (mild, medium, hot)" .
    
    :culturalSignificance a owl:DatatypeProperty ;
                         rdfs:domain :Food ;
                         rdfs:range rdfs:Literal ;
                         rdfs:label "cultural and traditional significance" .
}
"@
    
    $ontologyScript | Out-File -FilePath "scripts\00-setup-ontology.sparql" -Encoding UTF8
    
    # Create sample data script
    $sampleDataScript = @"
# Sample SPARQL Update to insert initial data
PREFIX : <http://example.org/food-ontology#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

INSERT DATA {
    :Rice a :Grain ;
          :name "Rice" ;
          :calories 130 ;
          :protein 2.7 ;
          :carbohydrates 28 ;
          :region "International" ;
          rdfs:label "Rice - Basic grain staple" .
    
    :Injera a :Bread ;
           :name "Injera" ;
           :calories 165 ;
           :protein 4.2 ;
           :carbohydrates 33 ;
           :region "East_Africa" ;
           :culturalSignificance "Traditional Ethiopian bread made from teff flour" ;
           rdfs:label "Injera - Ethiopian flatbread" .
    
    :Hummus a :Dip ;
           :name "Hummus" ;
           :calories 166 ;
           :protein 8 ;
           :carbohydrates 14 ;
           :region "Middle_East" ;
           :spiceLevel "mild" ;
           rdfs:label "Hummus - Middle Eastern chickpea dip" .
    
    :Berbere a :Spice ;
            :name "Berbere" ;
            :region "East_Africa" ;
            :spiceLevel "hot" ;
            :culturalSignificance "Traditional Ethiopian spice blend used in many dishes" ;
            rdfs:label "Berbere - Ethiopian spice blend" .
    
    :Pita a :Bread ;
         :name "Pita" ;
         :calories 275 ;
         :protein 9 ;
         :carbohydrates 55 ;
         :region "Middle_East" ;
         rdfs:label "Pita - Middle Eastern flatbread" .
    
    :Falafel a :Dip ;
            :name "Falafel" ;
            :calories 333 ;
            :protein 13 ;
            :carbohydrates 32 ;
            :region "Middle_East" ;
            :spiceLevel "medium" ;
            rdfs:label "Falafel - Middle Eastern chickpea fritters" .
}
"@
    
    $sampleDataScript | Out-File -FilePath "scripts\01-insert-sample-data.sparql" -Encoding UTF8
    
    Write-Success "Created sample SPARQL scripts in scripts\ directory"
}

# Function to create Windows batch loader
function Create-WindowsLoader {
    Write-Status "Creating Windows data loader script..."
    
    $batchScript = @"
@echo off
echo Loading NutriGraph data into Fuseki...

set FUSEKI_URL=http://localhost:3030
set DATASET_NAME=african-middle-eastern-kg

echo Waiting for Fuseki to be ready...
:WAIT_LOOP
curl -f %FUSEKI_URL%/`$/ping >nul 2>&1
if %ERRORLEVEL% == 0 goto FUSEKI_READY
echo    Waiting for Fuseki...
timeout /t 2 /nobreak >nul
goto WAIT_LOOP

:FUSEKI_READY
echo Fuseki is ready

echo Creating dataset %DATASET_NAME%...
curl -X POST %FUSEKI_URL%/`$/datasets -H "Content-Type: application/x-www-form-urlencoded" -d "dbType=tdb2&dbName=%DATASET_NAME%"

echo Loading RDF files...
if exist rdf-data\*.ttl (
    for %%f in (rdf-data\*.ttl) do (
        echo    Loading %%f...
        curl -X POST %FUSEKI_URL%/%DATASET_NAME%/data -H "Content-Type: text/turtle" --data-binary @%%f
    )
)

if exist rdf-data\*.rdf (
    for %%f in (rdf-data\*.rdf) do (
        echo    Loading %%f...
        curl -X POST %FUSEKI_URL%/%DATASET_NAME%/data -H "Content-Type: application/rdf+xml" --data-binary @%%f
    )
)

if exist rdf-data\*.owl (
    for %%f in (rdf-data\*.owl) do (
        echo    Loading %%f...
        curl -X POST %FUSEKI_URL%/%DATASET_NAME%/data -H "Content-Type: application/rdf+xml" --data-binary @%%f
    )
)

echo Executing SPARQL scripts...
if exist scripts\*.sparql (
    for %%f in (scripts\*.sparql) do (
        echo    Executing %%f...
        curl -X POST %FUSEKI_URL%/%DATASET_NAME%/update -H "Content-Type: application/sparql-update" --data-binary @%%f
    )
)

echo Verifying data...
curl -G %FUSEKI_URL%/%DATASET_NAME%/sparql --data-urlencode "query=SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o }" -H "Accept: application/sparql-results+json"

echo Data loading completed!
pause
"@
    
    $batchScript | Out-File -FilePath "scripts\load-data-windows.bat" -Encoding ASCII
    Write-Success "Created Windows batch loader: scripts\load-data-windows.bat"
}

# Function to create PowerShell loader
function Create-PowerShellLoader {
    Write-Status "Creating PowerShell data loader..."
    
    $psScript = @'
# PowerShell NutriGraph Data Loader
param(
    [string]$FusekiUrl = "http://localhost:3030",
    [string]$DatasetName = "african-middle-eastern-kg"
)

Write-Host "Loading NutriGraph data into Fuseki..." -ForegroundColor Blue
Write-Host "   Fuseki URL: $FusekiUrl" -ForegroundColor Cyan
Write-Host "   Dataset: $DatasetName" -ForegroundColor Cyan

# Function to wait for Fuseki
function Wait-ForFuseki {
    Write-Host "Waiting for Fuseki to be ready..." -ForegroundColor Yellow
    
    for ($i = 1; $i -le 30; $i++) {
        try {
            $response = Invoke-WebRequest -Uri "$FusekiUrl/`$/ping" -TimeoutSec 3 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                Write-Host "Fuseki is ready" -ForegroundColor Green
                return $true
            }
        } catch {
            Write-Host "   Attempt $i/30..." -ForegroundColor Gray
            Start-Sleep -Seconds 2
        }
    }
    
    Write-Host "Fuseki not ready after 60 seconds" -ForegroundColor Red
    return $false
}

# Function to create dataset
function Create-Dataset {
    Write-Host "Creating dataset $DatasetName..." -ForegroundColor Yellow
    
    try {
        $body = "dbType=tdb2&dbName=$DatasetName"
        $headers = @{"Content-Type" = "application/x-www-form-urlencoded"}
        $response = Invoke-WebRequest -Uri "$FusekiUrl/`$/datasets" -Method POST -Body $body -Headers $headers -TimeoutSec 10
        
        if ($response.StatusCode -eq 200) {
            Write-Host "Dataset created successfully" -ForegroundColor Green
        }
    } catch {
        if ($_.Exception.Response.StatusCode -eq 409) {
            Write-Host "Dataset already exists" -ForegroundColor Cyan
        } else {
            Write-Host "Dataset creation issue: $($_.Exception.Message)" -ForegroundColor Yellow
        }
    }
}

# Function to load RDF files
function Load-RDFFiles {
    Write-Host "Loading RDF files..." -ForegroundColor Yellow
    
    if (!(Test-Path "rdf-data")) {
        Write-Host "No rdf-data directory found" -ForegroundColor Yellow
        return
    }
    
    $rdfFiles = Get-ChildItem -Path "rdf-data" -Include "*.ttl", "*.rdf", "*.owl", "*.nt", "*.n3" -File
    
    if ($rdfFiles.Count -eq 0) {
        Write-Host "No RDF files found in rdf-data directory" -ForegroundColor Cyan
        return
    }
    
    foreach ($file in $rdfFiles) {
        Write-Host "   Loading $($file.Name)..." -ForegroundColor Gray
        
        # Determine content type
        $contentType = switch ($file.Extension.ToLower()) {
            ".ttl" { "text/turtle" }
            ".turtle" { "text/turtle" }
            ".rdf" { "application/rdf+xml" }
            ".xml" { "application/rdf+xml" }
            ".owl" { "application/rdf+xml" }
            ".nt" { "application/n-triples" }
            ".n3" { "text/n3" }
            default { "text/turtle" }
        }
        
        try {
            $fileContent = Get-Content -Path $file.FullName -Raw -Encoding UTF8
            $headers = @{"Content-Type" = $contentType}
            $response = Invoke-WebRequest -Uri "$FusekiUrl/$DatasetName/data" -Method POST -Body $fileContent -Headers $headers -TimeoutSec 30
            
            if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 204) {
                Write-Host "   $($file.Name) loaded successfully" -ForegroundColor Green
            }
        } catch {
            Write-Host "   Failed to load $($file.Name): $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

# Function to execute SPARQL scripts
function Execute-SPARQLScripts {
    Write-Host "Executing SPARQL scripts..." -ForegroundColor Yellow
    
    if (!(Test-Path "scripts")) {
        Write-Host "No scripts directory found" -ForegroundColor Yellow
        return
    }
    
    $sparqlFiles = Get-ChildItem -Path "scripts" -Filter "*.sparql" -File | Sort-Object Name
    
    if ($sparqlFiles.Count -eq 0) {
        Write-Host "No SPARQL scripts found in scripts directory" -ForegroundColor Cyan
        return
    }
    
    foreach ($script in $sparqlFiles) {
        Write-Host "   Executing $($script.Name)..." -ForegroundColor Gray
        
        try {
            $scriptContent = Get-Content -Path $script.FullName -Raw -Encoding UTF8
            $headers = @{"Content-Type" = "application/sparql-update"}
            $response = Invoke-WebRequest -Uri "$FusekiUrl/$DatasetName/update" -Method POST -Body $scriptContent -Headers $headers -TimeoutSec 30
            
            if ($response.StatusCode -eq 200 -or $response.StatusCode -eq 204) {
                Write-Host "   $($script.Name) executed successfully" -ForegroundColor Green
            }
        } catch {
            Write-Host "   Failed to execute $($script.Name): $($_.Exception.Message)" -ForegroundColor Red
        }
    }
}

# Function to verify data
function Verify-Data {
    Write-Host "Verifying loaded data..." -ForegroundColor Yellow
    
    try {
        # Count triples
        $query = "SELECT (COUNT(*) as ?count) WHERE { ?s ?p ?o }"
        $encodedQuery = [System.Web.HttpUtility]::UrlEncode($query)
        $response = Invoke-WebRequest -Uri "$FusekiUrl/$DatasetName/sparql?query=$encodedQuery" -Headers @{"Accept" = "application/sparql-results+json"} -TimeoutSec 10
        
        if ($response.Content -match '"value":"(\d+)"') {
            $count = $matches[1]
            Write-Host "Database contains $count triples" -ForegroundColor Green
        }
        
        # Test sample query
        $sampleQuery = "SELECT ?food ?name WHERE { ?food <http://example.org/food-ontology#name> ?name } LIMIT 5"
        $encodedSampleQuery = [System.Web.HttpUtility]::UrlEncode($sampleQuery)
        $sampleResponse = Invoke-WebRequest -Uri "$FusekiUrl/$DatasetName/sparql?query=$encodedSampleQuery" -Headers @{"Accept" = "application/sparql-results+json"} -TimeoutSec 10
        
        if ($sampleResponse.Content -match "food") {
            Write-Host "Sample query successful" -ForegroundColor Green
        } else {
            Write-Host "Sample query returned no results" -ForegroundColor Yellow
        }
        
    } catch {
        Write-Host "Could not verify data: $($_.Exception.Message)" -ForegroundColor Yellow
    }
}

# Main execution
function Main {
    Write-Host "Starting NutriGraph data loading process..." -ForegroundColor Blue
    
    # Load required assembly for URL encoding
    Add-Type -AssemblyName System.Web
    
    if (!(Wait-ForFuseki)) {
        exit 1
    }
    
    Create-Dataset
    Load-RDFFiles
    Execute-SPARQLScripts
    Verify-Data
    
    Write-Host "Data loading process completed!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Access points:" -ForegroundColor Cyan
    Write-Host "  Web UI: $FusekiUrl" -ForegroundColor White
    Write-Host "  Dataset: $FusekiUrl/#/dataset/$DatasetName" -ForegroundColor White
    Write-Host "  Query: $FusekiUrl/#/dataset/$DatasetName/query" -ForegroundColor White
}

# Run main function
Main
'@
    
    $psScript | Out-File -FilePath "scripts\Load-NutriGraphData.ps1" -Encoding UTF8
    Write-Success "Created PowerShell loader: scripts\Load-NutriGraphData.ps1"
}

# Main execution
Write-Host "Starting data preparation..." -ForegroundColor Blue

Organize-RDFFiles
Check-TDBDatabases
Create-SampleScripts
Create-WindowsLoader
Create-PowerShellLoader

Write-Host ""
Write-Host "Data preparation completed!" -ForegroundColor Green
Write-Host ""
Write-Host "Directory structure created:" -ForegroundColor Cyan
Write-Host "   rdf-data\     - Place your .ttl, .rdf, .owl files here" -ForegroundColor White
Write-Host "   scripts\      - SPARQL initialization scripts" -ForegroundColor White
Write-Host "   datasets\     - For additional dataset files" -ForegroundColor White
Write-Host ""
Write-Host "Files created:" -ForegroundColor Cyan
Write-Host "   scripts\00-setup-ontology.sparql       - Basic ontology setup" -ForegroundColor White
Write-Host "   scripts\01-insert-sample-data.sparql   - Sample data insertion" -ForegroundColor White
Write-Host "   scripts\load-data-windows.bat          - Windows batch loader" -ForegroundColor White
Write-Host "   scripts\Load-NutriGraphData.ps1        - PowerShell loader" -ForegroundColor White
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "1. Copy your RDF files to the rdf-data\ directory" -ForegroundColor Yellow
Write-Host "2. Modify the SPARQL scripts in scripts\ as needed" -ForegroundColor Yellow
Write-Host "3. Use the updated docker-compose.yml to auto-load data" -ForegroundColor Yellow
Write-Host "4. Run: docker-compose up -d" -ForegroundColor Yellow
Write-Host ""
Write-Host "Manual loading options:" -ForegroundColor Cyan
Write-Host "   Batch: scripts\load-data-windows.bat" -ForegroundColor White
Write-Host "   PowerShell: .\scripts\Load-NutriGraphData.ps1" -ForegroundColor White