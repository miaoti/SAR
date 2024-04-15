package json;

public class RunJsonProcessor {
    public static void main(String[] args) {
        String inputFile = "E:\\Workplace\\sar\\src\\main\\java\\json\\Test.json";
        String outputFile = "E:\\Workplace\\sar\\src\\main\\java\\json\\compare.json";

        JsonProcessor processor = new JsonProcessor(inputFile, outputFile);
        try {
            processor.process();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
