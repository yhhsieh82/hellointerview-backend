package com.hellointerview.backend.service.feedback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public final class DiagramToTextConverter {

    private DiagramToTextConverter() {
    }

    public static String diagramToText(Map<String, Object> sectionData) {
        if (sectionData == null) {
            return "";
        }
        Object elementsObj = sectionData.get("elements");
        if (!(elementsObj instanceof List<?> rawElements)) {
            return "";
        }
        List<String> description = new ArrayList<>();
        for (Object elemObj : rawElements) {
            if (!(elemObj instanceof Map<?, ?> elem)) {
                continue;
            }
            String type = String.valueOf(elem.get("type"));
            if ("rectangle".equals(type) || "circle".equals(type)) {
                Object text = elem.get("text");
                description.add("- Component: " + (text != null ? text : "Unnamed"));
            } else if ("arrow".equals(type)) {
                Object start = elem.get("startElementId");
                Object end = elem.get("endElementId");
                Object label = elem.get("label");
                description.add("- Connection: " + start + " -> " + end + " (" + (label != null ? label : "") + ")");
            } else if ("text".equals(type)) {
                Object text = elem.get("text");
                if (text != null) {
                    description.add("- Note: " + text);
                }
            }
        }
        return String.join("\n", description);
    }
}
