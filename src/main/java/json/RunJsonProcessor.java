package json;

import static json.RatioCalculator.FILE_PATH;

public class RunJsonProcessor {
    public static void main(String[] args) {
        String inputFile = FILE_PATH + "order_trace_diff_path.json";
        String outputFile = FILE_PATH + "order_trace_diff_compare.json";

        JsonProcessor processor = new JsonProcessor(inputFile, outputFile);
        try {
            processor.process();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
