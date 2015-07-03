/**
 * Created by Madison on 2/07/2015.
 */
public class LogEntry {
    private String dataset;
    private String testopt;
    private String resultopt;
    private String classifier;
    private String parameters;
    private String datetime;

    public LogEntry(String dataset, String testopt, String resultopt, String classifier, String parameters, String datetime) {
        this.dataset = dataset;
        this.testopt = testopt;
        this.resultopt = resultopt;
        this.classifier = classifier;
        this.parameters = parameters;
        this.datetime = datetime;
    }

    public String getDataset() {
        return dataset;
    }

    public void setDataset(String dataset) {
        this.dataset = dataset;
    }

    public String getTestopt() {
        return testopt;
    }

    public void setTestopt(String testopt) {
        this.testopt = testopt;
    }

    public String getResultopt() {
        return resultopt;
    }

    public void setResultopt(String resultopt) {
        this.resultopt = resultopt;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }
}
