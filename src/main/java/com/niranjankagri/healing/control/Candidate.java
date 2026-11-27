package com.niranjankagri.healing.control;

import com.epam.healenium.treecomparing.Node;
import com.epam.healenium.treecomparing.Scored;

import java.util.stream.Collectors;

/**
 * One element Healenium considered for a broken locator, with its similarity score. Kept as text, so the heal
 * journal and the review report can show why the winner won (or why it was too close to call).
 *
 * @param score   Healenium's similarity to the saved reference element, 0..1
 * @param element short description: tag, id, classes, position among siblings, start of the text
 */
public record Candidate(double score, String element) {

    private static final int TEXT_LIMIT = 30;

    /** Describes one of Healenium's scored nodes, e.g. {@code button.btn-add [3] "Add to basket"}. */
    public static Candidate of(Scored<Node> scored) {
        return new Candidate(scored.getScore(), describe(scored.getValue()));
    }

    static String describe(Node node) {
        StringBuilder text = new StringBuilder();
        // The parent and its position tell look-alike elements apart: article [1] > button vs article [3] > button
        Node parent = node.getParent();
        if (parent != null && parent.getTag() != null) {
            text.append(parent.getTag());
            if (parent.getIndex() != null) {
                text.append(" [").append(parent.getIndex() + 1).append(']');
            }
            text.append(" > ");
        }
        text.append(node.getTag());
        if (node.getId() != null && !node.getId().isBlank()) {
            text.append('#').append(node.getId());
        }
        if (node.getClasses() != null && !node.getClasses().isEmpty()) {
            text.append(node.getClasses().stream().sorted().map(c -> "." + c).collect(Collectors.joining()));
        }
        // Index among the parent's children: tells look-alike siblings apart in the report
        if (node.getIndex() != null) {
            text.append(" [").append(node.getIndex() + 1).append(']');
        }
        String inner = node.getInnerText() == null ? "" : node.getInnerText().strip().replaceAll("\\s+", " ");
        if (!inner.isEmpty()) {
            String shortText = inner.length() > TEXT_LIMIT ? inner.substring(0, TEXT_LIMIT) + "..." : inner;
            text.append(" \"").append(shortText).append('"');
        }
        return text.toString();
    }
}
