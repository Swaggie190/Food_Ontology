1- rassemblement des images dans un dossier 
2- organisation des images par le code cameroon_food_processor.py( il va creer une hierarchie entre images, metada et agenda nutritionel dans un dossier data)
3-implementation de l'ontologie concu dans food-ontology.ttl et exporter vers paste.txt
4- conception de l'ontologie par ontology_loader.py dans jena fuseki..modifier le nom de mon dataset ( creer ton propre dataset dans jena de type TDB, mettre ce nom dans les code )
5- peuplement de l'ontologie par rich_ontology_populator( tester la connexion fuseki, la polulation utilisera un le dossier data qui a toute ton organisation et chargera les images en fonction de types de repas)

6- ce sont tes repas que tu nettras dans les codes ainsi que les images...

8- teste ton peuplement avecc sparql_query.