package json;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RatioCalculator {

    public static String FILE_PATH = "src/main/resources/";

    public static void main(String[] args) {
        try {
            String goldenPath = FILE_PATH + "Output.json";
            String comparisonPath = FILE_PATH + "compare.json";

            JSONObject goldenStandard = new JSONObject(new String(Files.readAllBytes(Paths.get(goldenPath))));
            JSONObject comparisonInput = new JSONObject(new String(Files.readAllBytes(Paths.get(comparisonPath))));

            JSONObject comparisonOperation = comparisonInput.getJSONArray("operationNames").getJSONObject(0);
            String operationName = comparisonOperation.getString("operationName");
            JSONArray goldenOperations = goldenStandard.getJSONArray("operationNames");

            evaluateComparison(goldenOperations, comparisonOperation, operationName);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void evaluateComparison(JSONArray goldenOperations, JSONObject comparisonOperation, String operationName) {
        boolean unusualPath = true;
        double bestSAR = -1;
        double bestPSAR = -1;
        double bestThreshold = -1;

        for (int i = 0; i < goldenOperations.length(); i++) {
            JSONObject goldenOperation = goldenOperations.getJSONObject(i);
            if (goldenOperation.getString("operationName").equals(operationName)) {
                JSONArray goldenChildren = goldenOperation.getJSONArray("child");
                JSONArray comparisonChildren = comparisonOperation.getJSONArray("child");

                if (!compareServiceStructure(goldenChildren, comparisonChildren)) {
                    if (allResponsesAre200(comparisonChildren)) {
                        System.out.println("Non-standard but Successful Path Detected:");
                        reportUnusualSuccessfulPath(comparisonChildren, goldenChildren, 0);  // Start with indentation level 0
                    } else {
                        System.out.println("Problematic Unusual Path Detected");
                    }
                    continue;
                }

                unusualPath = false;
                int goldenNumSpans = goldenOperation.getInt("numSpans");
                int comparisonNumSpans = comparisonOperation.getInt("numSpans");
                int comparisonNumSpansWith400 = comparisonOperation.getInt("numSpansWith400");

                double SAR = (double) comparisonNumSpans / goldenNumSpans;
                double PSAR = (double) (comparisonNumSpans - comparisonNumSpansWith400) / goldenNumSpans;
                double threshold = calculateDynamicThreshold(goldenOperation);

                if (SAR > bestSAR) {
                    bestSAR = SAR;
                    bestPSAR = PSAR;
                    bestThreshold = threshold;
                }
            }
        }

        if (!unusualPath) {
            System.out.println("Best SAR: " + bestSAR + " (Threshold: " + bestThreshold + ")");
            System.out.println("Best PSAR: " + bestPSAR);
        } else {
            System.out.println("Unusual Path Happens in all matches.");
        }
    }

    private static double calculateDynamicThreshold(JSONObject goldenOperation) {
        int depth = calculateDepth(goldenOperation.getJSONArray("child"));
        int numServicesInOperation = extractServiceNames(goldenOperation.getJSONArray("child")).size();
        int totalServices = extractServiceNames(goldenOperation.getJSONArray("child")).size();  // This needs refinement to consider all services in all operations
        System.out.println(totalServices);
        double baseThreshold = 0.95;
        double serviceRatio = (double) numServicesInOperation / totalServices;
        double adjustmentFactor = Math.exp(-serviceRatio);

        double adjustedThreshold = baseThreshold * adjustmentFactor;
        adjustedThreshold = Math.max(adjustedThreshold - 0.01 * depth, 0.5);  // Ensure threshold does not go below 0.5
        return adjustedThreshold;
    }

    private static int calculateDepth(JSONArray children) {
        int maxDepth = 0;
        for (int i = 0; i < children.length(); i++) {
            maxDepth = Math.max(maxDepth, calculateDepth(children.getJSONObject(i).getJSONArray("child")) + 1);
        }
        return maxDepth;
    }

    private static Set<String> extractServiceNames(JSONArray children) {
        Set<String> services = new HashSet<>();
        for (int i = 0; i < children.length(); i++) {
            JSONObject child = children.getJSONObject(i);
            services.add(child.getString("serviceName"));
            if (child.getJSONArray("child").length() > 0) {
                services.addAll(extractServiceNames(child.getJSONArray("child")));
            }
        }
        return services;
    }

    private static boolean compareServiceStructure(JSONArray goldenChildren, JSONArray comparisonChildren) {
        Set<String> goldenServices = new HashSet<>();
        Set<String> comparisonServices = new HashSet<>();
        extractServiceStructure(goldenChildren, goldenServices, "");
        extractServiceStructure(comparisonChildren, comparisonServices, "");

        return goldenServices.equals(comparisonServices);
    }

    private static void extractServiceStructure(JSONArray children, Set<String> services, String path) {
        for (int i = 0; i < children.length(); i++) {
            JSONObject child = children.getJSONObject(i);
            String newPath = path + "/" + child.getString("serviceName");
            services.add(newPath);
            if (child.getJSONArray("child").length() > 0) {
                extractServiceStructure(child.getJSONArray("child"), services, newPath);
            }
        }
    }

    private static boolean allResponsesAre200(JSONArray services) {
        for (int i = 0; i < services.length(); i++) {
            JSONObject service = services.getJSONObject(i);
            if (!service.getString("returnCode").equals("200")) {
                return false;
            }
            if (service.getJSONArray("child").length() > 0 && !allResponsesAre200(service.getJSONArray("child"))) {
                return false;
            }
        }
        return true;
    }

    private static void reportUnusualSuccessfulPath(JSONArray services, JSONArray goldenChildren, int level) {
        String indent = "->".repeat(level);
        Set<String> goldenServices = extractServiceNames(goldenChildren);

        for (int i = 0; i < services.length(); i++) {
            JSONObject service = services.getJSONObject(i);
            String serviceName = service.getString("serviceName");
            String status = service.getString("returnCode");
            String additionalInfo = goldenServices.contains(serviceName) ? "" : " [NON-STANDARD]";

            System.out.println(indent + "Service: " + serviceName + additionalInfo + ", Status: " + status);
            if (service.getJSONArray("child").length() > 0) {
                reportUnusualSuccessfulPath(service.getJSONArray("child"), findChildGoldenChildren(serviceName, goldenChildren), level + 1);  // Increase the level for nested children
            }
        }
    }

    private static JSONArray findChildGoldenChildren(String serviceName, JSONArray goldenChildren) {
        for (int i = 0; i < goldenChildren.length(); i++) {
            JSONObject child = goldenChildren.getJSONObject(i);
            if (child.getString("serviceName").equals(serviceName)) {
                return child.getJSONArray("child");
            }
        }
        return new JSONArray();
    }
}
