# Pulse Data Visualization using Arcadia
- Visualizing pulse data using Arcadia
- Arcadia is the first visual analytics and BI software that runs natively within modern data platforms such as Apache Hadoop and the cloud
- Read [here](https://www.arcadiadata.com) for more information

## Data visualization using sample arcadia dashboard extract `pulse.json`

### Create new solr connection
- If Solr connection is not existing in Arcadia Data, create one using this link [Creating New Apache Solr Connection](http://documentation.arcadiadata.com/4.3.0.0/#pages/topics/conn-solr.html)


### Create new dataset
- Sign into Arcadia
- Click on `DATA`
![click data](images/click_data.png)

- Go to `solr` connection created in the previous steps and click on `NEW DATASET`
![import visual artifact](images/import-visual-artifact.png)

- Enter your Dataset title, use `solr` as database and finally select the solr collection that you want
![new dataset detail](images/new_dataset_details.png)

- Click `CREATE`
- You should be able to see the new dataset created
![check new dataset](images/check_new_dataset.png)


### Replace `dataset_name` from `pulse.json`
- Use `replace_dataset_name.sh` script to replace dataset_name in pulse.json with new dataset_name created in the last step
- command `./replace_dataset_name.sh pulse.json new_dataset_name`
- `pulse.json` is ready to be imported


### Importing dashboard
- Refer to below screenshots while following the [import dashboard](http://documentation.arcadiadata.com/4.3.0.0/#pages/topics/import-dash.html) document
![import visual artifact](images/import-visual-artifact.png)

- Click on import visual artifacts
![import visual artifact](images/import-vis-artifacts.png)

- Choose `pulse.json` file and click on `import`
![choose pulse](images/choose-pulse.png)

- Click on `Accept and Import`, also check `dataset` at the bottom
![accept and import](images/accept-import.png)

- Make sure success message appeared on window
![import-success](images/import-success.png)

- Look for phData Pulse dashboard in visuals private as shown below.
![image](images/check-import-success.png)

- Lastly examine the dashboard and functionality of filters. The dashboard will look similar to below screenshot.
![visual examine](images/examine-visuals.png)
