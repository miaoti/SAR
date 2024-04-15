package json;

import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class RatioCalculator {

    public static void main(String[] args) {
        try {
            String goldenPath = "E:\\Workplace\\sar\\src\\main\\java\\json\\Output.json";
            String comparisonPath = "E:\\Workplace\\sar\\src\\main\\java\\json\\compare.json";

            String goldenContent = new String(Files.readAllBytes(Paths.get(goldenPath)));
            JSONObject goldenStandard = new JSONObject(goldenContent);
            JSONArray goldenOperations = goldenStandard.getJSONArray("operationNames");

            String comparisonContent = new String(Files.readAllBytes(Paths.get(comparisonPath)));
            JSONObject comparisonInput = new JSONObject(comparisonContent);
            JSONObject comparisonOperation = comparisonInput.getJSONArray("operationNames").getJSONObject(0);

            String operationName = comparisonOperation.getString("operationName");
            Set<String> comparisonServices = getServiceNames(comparisonOperation.getJSONArray("child"));

            double bestSAR = -1;
            double bestPSAR = -1;
            boolean unusualPath = true;

            for (int i = 0; i < goldenOperations.length(); i++) {
                JSONObject goldenOperation = goldenOperations.getJSONObject(i);
                if (goldenOperation.getString("operationName").equals(operationName)) {
                    Set<String> goldenServices = getServiceNames(goldenOperation.getJSONArray("child"));

                    if (!goldenServices.containsAll(comparisonServices)) {
                        System.out.println("Unusual Path Happens");
                        continue;
                    }

                    unusualPath = false;
                    int goldenNumSpans = goldenOperation.getInt("numSpans");
                    int comparisonNumSpans = comparisonOperation.getInt("numSpans");
                    int comparisonNumSpansWith400 = comparisonOperation.getInt("numSpansWith400");

                    double SAR = (double) comparisonNumSpans / goldenNumSpans;
                    double PSAR = (double) (comparisonNumSpans - comparisonNumSpansWith400) / goldenNumSpans;

                    if (bestSAR < 0 || SAR > bestSAR) {
                        bestSAR = SAR;
                    }
                    if (bestPSAR < 0 || PSAR > bestPSAR) {
                        bestPSAR = PSAR;
                    }
                }
            }

            if (!unusualPath) {
                System.out.println("Best SAR: " + bestSAR);
                System.out.println("Best PSAR: " + bestPSAR);
            } else {
                System.out.println("Unusual Path Happens in all matches.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Set<String> getServiceNames(JSONArray children) {
        Set<String> services = new HashSet<>();
        for (int i = 0; i < children.length(); i++) {
            JSONObject child = children.getJSONObject(i);
            services.add(child.getString("serviceName"));
            if (child.getJSONArray("child").length() > 0) {
                services.addAll(getServiceNames(child.getJSONArray("child")));
            }
        }
        return services;
    }
}
