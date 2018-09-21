# Arcadia Data

## Importing Dashboard
Steps to Import Arcadia data dashboard.


1. If Solr connection is not existing in Arcadia Data, create one using this link [Creating New Apache Solr Connection](http://documentation.arcadiadata.com/4.3.0.0/#pages/topics/conn-solr.html).
2. Follow this Arcadia data document to [Import dashboard](http://documentation.arcadiadata.com/4.3.0.0/#pages/topics/import-dash.html). But instead of using `Documentation` connection mentioned in this document use the connection we created in previous step.\
\
Refer to below screenshots while following the `import dashboard` document.\
![import visual artifact](images/import-visual-artifact.png)
\
Click on import visual artifacts.
\
![import visual artifact](images/import-vis-artifacts.png)
\
Choose `pulse.json` file and click on `import`.
\
![choose pulse](images/choose-pulse.png)
\
Click on `Accept and Import`.
\
![accept and import](images/accept-import.png)
\
Make sure success message appeared on window.
\
![import-success](images/import-success.png)
\
Look for phData Pulse dashboard in visuals private as shown below.
\
![image](images/check-import-success.png)

3. After importing dashboard, confirm that the Dataset and Visuals imported successfully. Click Data in the top navigation menu and you must be able see `solr_test_dataset` got created. Dataset connections, Solr collection table names, Visuals are all customizable.\
\
![dataset check](images/import-visual-artifact.png)
4. Click on `solr_test_dataset` and explore the options.\
\
![explore dataset](images/explore-dataset.png)\
\
Use edit button to change Dataset name, Solr collection table name to desired. Change the table name to solr collection `solr.<collection name>` where you are storing the logs captured by **Pulse** log-collector.\
\
![data model](images/data_model.png)\
\
Change the Solr collection name from `logging-pulse-test` to desired collection name in you solr database.
\
![table browser](images/table_browser.png)
5. Lastly examine the dashboard and functionality of filters. The dashboard will look similar to below screenshot.
\
![visual examine](images/examine-visuals.png)