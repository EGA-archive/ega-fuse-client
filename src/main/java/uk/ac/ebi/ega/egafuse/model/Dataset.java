package uk.ac.ebi.ega.egafuse.model;

public class Dataset {
    private String datasetId;
    private String description;
    private String dacStableId;

    public Dataset() {}

    public Dataset(String datasetId, String description, String dacStableId) {
        this.datasetId = datasetId;
        this.description = description;
        this.dacStableId = dacStableId;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public String getDescription() {
        return datasetId;
    }

    public String getDacStableId() {
        return dacStableId;
    }

    public void setDatasetId(String datasetId) {
        this.datasetId = datasetId;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setDacStableId(String dacStableId) {
        this.dacStableId = dacStableId;
    }

}
