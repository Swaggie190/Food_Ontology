@echo off
echo Loading NutriGraph data into Fuseki...

set FUSEKI_URL=http://localhost:3030
set DATASET_NAME=african-middle-eastern-kg

echo Waiting for Fuseki to be ready...
:WAIT_LOOP
curl -f %FUSEKI_URL%/$/ping >nul 2>&1
if %ERRORLEVEL% == 0 goto FUSEKI_READY
echo    Waiting for Fuseki...
timeout /t 2 /nobreak >nul
goto WAIT_LOOP

:FUSEKI_READY
echo Fuseki is ready

echo Creating dataset %DATASET_NAME%...
curl -X POST %FUSEKI_URL%/$/datasets -H "Content-Type: application/x-www-form-urlencoded" -d "dbType=tdb2&dbName=%DATASET_NAME%"

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
