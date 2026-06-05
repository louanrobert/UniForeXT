package be.ccb_uliege.incd.uniforext_viewer.services;

import java.util.List;

import be.ccb_uliege.incd.uniforext_viewer.services.records.TimelineItem;
import be.ccb_uliege.incd.uniforext_viewer.services.records.UndatedIndividual;

/**
 * Facade that exposes the knowledge-graph operations used by the UI.
 */
public class KGService {

    private final KGServiceContext context;
    private final KGTimelineService timelineService;
    private final KGNeighborhoodService neighborhoodService;
    private final KGDetailService detailService;
    private final KGSparqlService sparqlService;

    public KGService(String ttlFilePath) {
        this.context = new KGServiceContext(ttlFilePath);
        this.timelineService = new KGTimelineService(context);
        this.neighborhoodService = new KGNeighborhoodService(context);
        this.detailService = new KGDetailService(context);
        this.sparqlService = new KGSparqlService(context);
    }

    /**
     * Returns a local name for a URI resource (after # or last /).
     */
    public static String localName(String uri) {
        return KGServiceContext.localName(uri);
    }

    public List<TimelineItem> getTimelineItems() {
        return timelineService.getTimelineItems();
    }

    public String getTimelineItemsJson() {
        return timelineService.getTimelineItemsJson();
    }

    public String getTimelineGroupsJson() {
        return timelineService.getTimelineGroupsJson();
    }

    public List<UndatedIndividual> getUndatedIndividuals() {
        return timelineService.getUndatedIndividuals();
    }

    public String getUndatedIndividualsJson() {
        return timelineService.getUndatedIndividualsJson();
    }

    public String getNeighborsJson(String individualUri) {
        return neighborhoodService.getNeighborsJson(individualUri);
    }

    public String getNeighborSummaryJson(String individualUri) {
        return neighborhoodService.getNeighborSummaryJson(individualUri);
    }

    public String getFilteredNeighborsJson(String individualUri, String allowedTypesJson,
            int maxPerType, boolean includeLiterals) {
        return neighborhoodService.getFilteredNeighborsJson(individualUri, allowedTypesJson, maxPerType, includeLiterals);
    }

    public boolean isExplored(String uri) {
        return neighborhoodService.isExplored(uri);
    }

    public String getIndividualDetailsJson(String individualUri) {
        return detailService.getIndividualDetailsJson(individualUri);
    }

    public String executeSparqlJson(String sparql) {
        return sparqlService.executeSparqlJson(sparql);
    }
}