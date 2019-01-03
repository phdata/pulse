# Visualization

Pulse uses Solr, which can use several Visualization tools, including Hue and Arcadia Data.

## Pulse Data Visualization using Arcadia

Arcadia is visual analytics and BI software that runs natively within modern data platforms such as
Apache Hadoop and the cloud. Read [here](https://www.arcadiadata.com) for more information.

### Data visualization using sample arcadia dashboard extract `pulse.json`

#### Create new solr connection

- If Solr connection is not existing in Arcadia Data, create one using this link
[Creating New Apache Solr Connection](http://documentation.arcadiadata.com/4.3.0.0/#pages/topics/conn-solr.html)


### Create new dataset

- Sign into Arcadia
- Click on `DATA`
![click data](images/arcadia/click_data.png)

- Go to `solr` connection created in the previous steps and click on `NEW DATASET`
![import visual artifact](images/arcadia/import-visual-artifact.png)

- Enter your Dataset title, use `solr` as database and finally select the solr collection that you want
![new dataset detail](images/arcadia/new_dataset_details.png)

- Click `CREATE`
- You should be able to see the new dataset created
![check new dataset](images/arcadia/check_new_dataset.png)


### Replace dataset_name and dataset_detail from `pulse.json`

The `replace-dataset-name.sh` script can be found in the `visualizations/arcadia` folder.

- Use `replace-dataset-name.sh` script to replace dataset_name and dataset_detail with the dataset name and Solr index name of your own dataset
- command `./replace-dataset-name.sh my_dataset_name Solr.my_application_all`
- `dashboard.json` is ready to be imported


### Importing dashboard

- Refer to below screenshots while following the [import dashboard](http://documentation.arcadiadata.com/4.3.0.0/#pages/topics/import-dash.html) document
![import visual artifact](images/arcadia/import-visual-artifact.png)

- Click on import visual artifacts
![import visual artifact](images/arcadia/import-vis-artifacts.png)

- Choose `pulse.json` file and click on `import`
![choose pulse](images/arcadia/choose-pulse.png)

- Click on `Accept and Import`, also check `dataset` at the bottom
![accept and import](images/arcadia/accept-import.png)

- Make sure success message appeared on window
![import-success](images/arcadia/import-success.png)

- Look for phData Pulse dashboard in visuals private as shown below.
![image](images/arcadia/check-import-success.png)

- Lastly examine the dashboard and functionality of filters. The dashboard will look similar to below screenshot.
![visual examine](images/arcadia/examine-visuals.png)
