import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.nio.file.Files;

/**
 * Created by Madison on 2/07/2015.
 */

public class LogFile {
    String resultsFolder;
    String xmlFileName;
    ArrayList<LogEntry> entries;

    public LogFile() {
        resultsFolder = "";
        xmlFileName = "";
        entries = new ArrayList<>();
    }

    public LogFile(String resultsFolder, String xmlFileName) {
        this.resultsFolder = resultsFolder;
        this.xmlFileName = xmlFileName;
        entries = new ArrayList<>();
    }

    public String getResultsFolder() {
        return resultsFolder;
    }

    public void setResultsFolder(String resultsFolder) {
        this.resultsFolder = resultsFolder;
    }

    public String getXmlFileName() {
        return xmlFileName;
    }

    public void setXmlFileName(String xmlFileName) {
        this.xmlFileName = xmlFileName;
    }

    public ArrayList<LogEntry> getEntries() {
        return entries;
    }

    public void processFile(File file) {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath())) {
            String line = null;
            xmlFileName = line = reader.readLine();
            resultsFolder = line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] splitted = line.split(", ");
                if (splitted.length == 5) {
                    entries.add(new LogEntry(splitted[0], splitted[1], splitted[2].replaceAll("\"", ""), splitted[3], splitted[4]));
                }
            }
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }
}
