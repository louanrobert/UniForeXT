package be.ccb_uliege.incd.ontology_viewer.services.records;

/**
 * Data class for a timeline item.
 */
public record TimelineItem(String id, String uri, String label, String type,
        String start, long timestamp, String content) {
}