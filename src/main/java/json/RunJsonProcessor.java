package json;

import static json.RatioCalculator.FILE_PATH;

public class RunJsonProcessor {
    public static void main(String[] args) {
        String inputFile = FILE_PATH + "Test2.json";
        String outputFile = FILE_PATH + "alltraces_parsed.json";

        JsonProcessor processor = new JsonProcessor(inputFile, outputFile);
        try {
            processor.process();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
