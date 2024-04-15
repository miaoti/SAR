package json;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;

public class JsonProcessor {

    private Map<String, JSONObject> spanDetails = new HashMap<>();
    private Map<String, List<String>> childrenMap = new HashMap<>();
    private int numSpans = 0;
    private int numSpansWith400 = 0;
    private String inputFile;
    private String outputFile;

    public JsonProcessor(String inputFile, String outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    public void process() throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(inputFile)));
        JSONObject inputObj = new JSONObject(content);
        JSONArray data = inputObj.getJSONArray("data");

        JSONArray operationNames = new JSONArray();

        for (int dataIndex = 0; dataIndex < data.length(); dataIndex++) {
            JSONObject trace = data.getJSONObject(dataIndex);
            JSONArray spans = trace.getJSONArray("spans");

            spanDetails.clear();
            childrenMap.clear();
            numSpans = 0;
            numSpansWith400 = 0;

            processSpans(spans);

            String rootSpanID = findRootSpanID(spans);
            JSONObject output = buildOutputJson(rootSpanID);
            operationNames.put(output);
        }

        JSONObject finalOutput = new JSONObject().put("operationNames", operationNames);
        Files.write(Paths.get(outputFile), finalOutput.toString(2).getBytes());
        System.out.println("Output written to " + outputFile);
    }

    private void processSpans(JSONArray spans) {
        for (int i = 0; i < spans.length(); i++) {
            JSONObject span = spans.getJSONObject(i);
            String spanID = span.getString("spanID");
            String returnCode = span.getJSONArray("tags").getJSONObject(0).getString("value");

            if (returnCode.startsWith("5")) {
                continue;
            }

            if (returnCode.startsWith("4")) {
                numSpansWith400++;
            }

            String operationName = span.getString("operationName");
            JSONObject detail = new JSONObject();
            detail.put("serviceName", operationName.split("\\.")[0]);
            detail.put("operationName", operationName);
            detail.put("returnCode", returnCode);
            detail.put("child", new JSONArray());

            spanDetails.put(spanID, detail);
            childrenMap.putIfAbsent(spanID, new ArrayList<>());

            JSONArray references = span.getJSONArray("references");
            for (int j = 0; j < references.length(); j++) {
                JSONObject reference = references.getJSONObject(j);
                childrenMap.get(reference.getString("spanID")).add(spanID);
            }

            numSpans++;
        }
    }

    private String findRootSpanID(JSONArray spans) {
        for (int i = 0; i < spans.length(); i++) {
            JSONObject span = spans.getJSONObject(i);
            if (span.getJSONArray("references").length() == 0) {
                return span.getString("spanID");
            }
        }
        return spans.getJSONObject(0).getString("spanID");
    }

    private JSONObject buildOutputJson(String rootSpanID) {
        JSONObject rootSpan = spanDetails.get(rootSpanID);
        buildTree(rootSpanID, rootSpan);
        return new JSONObject()
                .put("numSpans", numSpans)
                .put("numSpansWith400", numSpansWith400)
                .put("operationName", rootSpan.getString("operationName"))
                .put("child", rootSpan.getJSONArray("child"));
    }

    private void buildTree(String spanID, JSONObject parentDetail) {
        List<String> childrenIDs = childrenMap.getOrDefault(spanID, new ArrayList<>());
        for (String childID : childrenIDs) {
            JSONObject childDetail = spanDetails.get(childID);
            buildTree(childID, childDetail);
            JSONObject childOutput = new JSONObject();
            childOutput.put("serviceName", childDetail.getString("serviceName"));
            childOutput.put("returnCode", childDetail.getString("returnCode"));
            childOutput.put("child", childDetail.getJSONArray("child"));
            parentDetail.getJSONArray("child").put(childOutput);
        }
    }
}
