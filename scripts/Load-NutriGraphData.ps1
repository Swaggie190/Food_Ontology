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
