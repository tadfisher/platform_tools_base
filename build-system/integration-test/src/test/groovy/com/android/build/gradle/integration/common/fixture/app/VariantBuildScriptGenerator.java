package com.android.build.gradle.integration.common.fixture.app;

import java.util.Map;

/**
 * Generator to create build.gradle with arbitrary number of variants.
 */
public class VariantBuildScriptGenerator {

    public static final Integer LARGE_NUMBER = 15;

    public static final Integer MEDIUM_NUMBER = 5;

    public static final Integer SMALL_NUMBER = 2;

    private final String template;

    private final Map<String, Integer> variantCounts;

    public VariantBuildScriptGenerator(Map<String, Integer> variantCounts, String template) {
        this.template = template;
        this.variantCounts = variantCounts;
    }

    public String createBuildScript() {
        String buildScript = template;
        System.out.println(template);
        for (Map.Entry<String, Integer> variantCount : variantCounts.entrySet()) {
            String variantName = variantCount.getKey();
            StringBuilder variants = new StringBuilder();
            for (int i = 0; i < variantCount.getValue(); i++) {
                variants.append(variantName);
                variants.append(i);
                variants.append("\n");
            }
            System.out.println(variants.toString());

            buildScript = buildScript.replace("${" + variantName + "}", variants.toString());
        }

        return buildScript;
    }
}
