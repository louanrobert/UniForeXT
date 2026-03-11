package be.ccb_uliege.incd.ontology_viewer;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

/**
 * Service class that wraps Apache Jena to parse a Turtle (.ttl) file,
 * extract individuals with date/time properties, and resolve neighborhoods.
 */
public class OntologyService {

    private final Model model;
    private final ObjectMapper mapper = new ObjectMapper();

    /** Cache: individual URI -> JSON neighborhood string */
    private final Map<String, String> neighborhoodCache = new ConcurrentHashMap<>();

    /** Cache: individual URI -> JSON neighbor summary string */
    private final Map<String, String> neighborSummaryCache = new ConcurrentHashMap<>();

    /** Cache: detected date properties (computed once) */
    private volatile Set<Property> datePropertiesCache;

    /** Cache: timeline items (computed once, invalidated never since model is read-only) */
    private volatile List<TimelineItem> timelineItemsCache;

    /** Cache: resource URI -> label */
    private final Map<String, String> labelCache = new ConcurrentHashMap<>();

    /** Cache: resource URI -> type */
    private final Map<String, String> typeCache = new ConcurrentHashMap<>();

    /** Set of property URIs considered as date/time properties */
    private static final Set<String> DATE_PROPERTIES = Set.of(
        "http://www.semanticweb.org/robert_louan/ontologies/2026/1/unified-forensics-results#hasTimestamp",
        "http://www.w3.org/2001/XMLSchema#date",
        "http://www.w3.org/2001/XMLSchema#dateTime",
        "http://schema.org/startDate",
        "http://schema.org/endDate",
        "http://schema.org/dateCreated",
        "http://schema.org/dateModified",
        "http://purl.org/dc/terms/created",
        "http://purl.org/dc/terms/modified",
        "http://purl.org/dc/terms/date"
    );

    /** Formatters for parsing timestamps */
    private static final DateTimeFormatter DD_MM_YYYY_HH_MM =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter ISO_OFFSET =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public OntologyService(String ttlFilePath) {
        model = ModelFactory.createDefaultModel();
        model.read(ttlFilePath);
        // Pre-resolve frequently-used property references
        propHasName = model.getProperty(
            "http://www.semanticweb.org/robert_louan/ontologies/2026/1/unified-forensics-results#hasName");
        propRdfsLabel = model.getProperty("http://www.w3.org/2000/01/rdf-schema#label");
    }

    /**
     * Returns a local name for a URI resource (after # or last /).
     */
    public static String localName(String uri) {
        if (uri == null) return "";
        int hash = uri.lastIndexOf('#');
        if (hash >= 0 && hash < uri.length() - 1) return uri.substring(hash + 1);
        int slash = uri.lastIndexOf('/');
        if (slash >= 0 && slash < uri.length() - 1) return uri.substring(slash + 1);
        return uri;
    }

    /**
     * Detect whether a property is a date/time property, either by URI match
     * or by checking if literal values look like dates.
     */
    @SuppressWarnings("unused")
    private boolean isDateProperty(Property prop) {
        return DATE_PROPERTIES.contains(prop.getURI());
    }

    /**
     * Find all properties in the model that carry date-like values.
     * Result is cached after the first call.
     */
    private Set<Property> detectDateProperties() {
        if (datePropertiesCache != null) return datePropertiesCache;
        Set<Property> result = new HashSet<>();
        // Check known date property URIs
        for (String uri : DATE_PROPERTIES) {
            Property p = model.getProperty(uri);
            if (model.contains(null, p, (RDFNode) null)) {
                result.add(p);
            }
        }
        // Track which predicate URIs we've already confirmed as non-date
        Set<String> checkedNonDateProps = new HashSet<>();
        // Also scan for any literal property whose value looks like a date
        StmtIterator iter = model.listStatements();
        while (iter.hasNext()) {
            Statement stmt = iter.next();
            if (stmt.getObject().isLiteral()) {
                String propUri = stmt.getPredicate().getURI();
                if (!DATE_PROPERTIES.contains(propUri) && !result.contains(stmt.getPredicate())
                        && !checkedNonDateProps.contains(propUri)) {
                    String val = stmt.getObject().asLiteral().getString();
                    if (tryParseDate(val) != null) {
                        result.add(stmt.getPredicate());
                    } else {
                        checkedNonDateProps.add(propUri);
                    }
                }
            }
        }
        datePropertiesCache = Collections.unmodifiableSet(result);
        return datePropertiesCache;
    }

    /**
     * Quick regex pre-check to avoid expensive parse attempts on clearly non-date strings.
     * Matches strings that start with 2–4 digits followed by a common date separator
     * ('/' or '-') or 'T', which is typical of the date formats we support (e.g.
     * "2024-01-31", "31/01/2024", "2024-01-31T10:15:30"). This is intentionally a
     * loose filter to skip obviously non-date values before running full parsers.
     */
    private static final java.util.regex.Pattern DATE_LIKE_PATTERN =
        java.util.regex.Pattern.compile("^\\d{2,4}[/\\-T]");

    /**
     * Try to parse a string as a date/time. Returns ISO string or null.
     * Uses a regex pre-check to skip obviously non-date strings early.
     */
    private String tryParseDate(String value) {
        if (value == null || value.isBlank()) return null;
        value = value.trim();
        // Quick reject: if it doesn't start with digits followed by a separator, skip
        if (!DATE_LIKE_PATTERN.matcher(value).find()) return null;
        // Try ISO 8601 with offset (e.g. 2025-01-02T14:42:40.031980+00:00)
        try {
            OffsetDateTime odt = OffsetDateTime.parse(value, ISO_OFFSET);
            return odt.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {}
        // Try dd/MM/yyyy HH:mm
        try {
            LocalDateTime ldt = LocalDateTime.parse(value, DD_MM_YYYY_HH_MM);
            return ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {}
        // Try ISO local date-time
        try {
            LocalDateTime ldt = LocalDateTime.parse(value);
            return ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {}
        return null;
    }

    /**
     * Data class for a timeline item.
     */
    public record TimelineItem(String id, String uri, String label, String type,
                               String start, String content) {}

    /**
     * Data class for an undated individual.
     */
    public record UndatedIndividual(String uri, String label, String type) {}

    /**
     * Extract all individuals that have a date property, grouped for the timeline.
     * Result is cached after the first call.
     */
    public List<TimelineItem> getTimelineItems() {
        if (timelineItemsCache != null) return timelineItemsCache;

        Set<Property> dateProps = detectDateProperties();
        List<TimelineItem> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Property dateProp : dateProps) {
            StmtIterator iter = model.listStatements(null, dateProp, (RDFNode) null);
            while (iter.hasNext()) {
                Statement stmt = iter.next();
                Resource subject = stmt.getSubject();
                if (!subject.isURIResource()) continue;
                String uri = subject.getURI();
                if (seen.contains(uri)) continue;

                String dateStr = stmt.getObject().asLiteral().getString();
                String isoDate = tryParseDate(dateStr);
                if (isoDate == null) continue;
                seen.add(uri);

                String label = getLabel(subject);
                String type = getType(subject);
                String tooltip = buildTooltip(subject);

                items.add(new TimelineItem(
                    uri, uri, label, type, isoDate, tooltip
                ));
            }
        }
        // Sort by date
        items.sort(Comparator.comparing(TimelineItem::start));
        timelineItemsCache = Collections.unmodifiableList(items);
        return timelineItemsCache;
    }

    /**
     * Get all individuals that have NO date property.
     */
    public List<UndatedIndividual> getUndatedIndividuals() {
        Set<Property> dateProps = detectDateProperties();
        Set<String> datedUris = new HashSet<>();

        for (Property dateProp : dateProps) {
            StmtIterator iter = model.listStatements(null, dateProp, (RDFNode) null);
            while (iter.hasNext()) {
                Resource s = iter.next().getSubject();
                if (s.isURIResource()) datedUris.add(s.getURI());
            }
        }

        List<UndatedIndividual> undated = new ArrayList<>();
        // Find all typed resources
        StmtIterator typeIter = model.listStatements(null, RDF.type, (RDFNode) null);
        Set<String> seen = new HashSet<>();
        while (typeIter.hasNext()) {
            Statement stmt = typeIter.next();
            Resource subject = stmt.getSubject();
            if (!subject.isURIResource()) continue;
            String uri = subject.getURI();
            if (seen.contains(uri) || datedUris.contains(uri)) continue;
            // Skip owl:Ontology
            String typeUri = stmt.getObject().isURIResource() ? stmt.getObject().asResource().getURI() : "";
            if (typeUri.equals("http://www.w3.org/2002/07/owl#Ontology")) continue;
            seen.add(uri);
            undated.add(new UndatedIndividual(uri, getLabel(subject), localName(typeUri)));
        }
        return undated;
    }

    /**
     * Returns a JSON string of timeline items compatible with Vis.js Timeline.
     */
    public String getTimelineItemsJson() {
        List<TimelineItem> items = getTimelineItems();
        ArrayNode array = mapper.createArrayNode();

        // Group by type for coloring
        Map<String, String> typeColors = new HashMap<>();
        String[] colors = {"#4e79a7", "#f28e2b", "#e15759", "#76b7b2", "#59a14f",
                           "#edc948", "#b07aa1", "#ff9da7", "#9c755f", "#bab0ac"};
        int colorIdx = 0;

        for (TimelineItem item : items) {
            ObjectNode node = mapper.createObjectNode();
            node.put("id", item.uri());
            node.put("content", escapeHtml(item.label()));
            node.put("start", item.start());
            node.put("title", item.content()); // tooltip on hover

            String group = item.type();
            if (!typeColors.containsKey(group)) {
                typeColors.put(group, colors[colorIdx % colors.length]);
                colorIdx++;
            }
            node.put("group", group);
            node.put("style", "background-color: " + typeColors.get(group) + "; color: white; border-radius: 4px; padding: 2px 6px;");
            array.add(node);
        }
        return array.toString();
    }

    /**
     * Returns a JSON string of timeline groups (one per type).
     */
    public String getTimelineGroupsJson() {
        List<TimelineItem> items = getTimelineItems();
        Set<String> types = items.stream().map(TimelineItem::type).collect(Collectors.toCollection(LinkedHashSet::new));
        ArrayNode array = mapper.createArrayNode();
        for (String type : types) {
            ObjectNode group = mapper.createObjectNode();
            group.put("id", type);
            group.put("content", type);
            array.add(group);
        }
        return array.toString();
    }

    /**
     * Returns a JSON string of undated individuals for the sidebar.
     */
    public String getUndatedIndividualsJson() {
        List<UndatedIndividual> undated = getUndatedIndividuals();
        ArrayNode array = mapper.createArrayNode();
        for (UndatedIndividual ind : undated) {
            ObjectNode node = mapper.createObjectNode();
            node.put("uri", ind.uri());
            node.put("label", ind.label());
            node.put("type", ind.type());
            array.add(node);
        }
        return array.toString();
    }

    /**
     * Get the direct 1-hop neighborhood of an individual as JSON.
     * Returns { "nodes": [...], "edges": [...] }
     */
    public String getNeighborsJson(String individualUri) {
        if (neighborhoodCache.containsKey(individualUri)) {
            return neighborhoodCache.get(individualUri);
        }

        Resource resource = model.getResource(individualUri);
        ObjectNode result = mapper.createObjectNode();
        ArrayNode nodes = mapper.createArrayNode();
        ArrayNode edges = mapper.createArrayNode();

        Set<String> nodeUris = new HashSet<>();
        int edgeId = 0;

        // Add the center node
        String centerLabel = getLabel(resource);
        String centerType = getType(resource);
        addNode(nodes, individualUri, centerLabel, centerType, true, nodeUris);

        // Outgoing edges: subject -> predicate -> object
        StmtIterator outgoing = resource.listProperties();
        while (outgoing.hasNext()) {
            Statement stmt = outgoing.next();
            String predUri = stmt.getPredicate().getURI();
            // Skip rdf:type for edges (we use it for labels)
            if (predUri.equals(RDF.type.getURI())) continue;

            if (stmt.getObject().isURIResource()) {
                Resource obj = stmt.getObject().asResource();
                String objUri = obj.getURI();
                String objLabel = getLabel(obj);
                String objType = getType(obj);
                addNode(nodes, objUri, objLabel, objType, false, nodeUris);

                ObjectNode edge = mapper.createObjectNode();
                edge.put("id", "e" + (edgeId++));
                edge.put("from", individualUri);
                edge.put("to", objUri);
                edge.put("label", localName(predUri));
                edge.put("arrows", "to");
                edges.add(edge);
            } else if (stmt.getObject().isLiteral()) {
                // Represent literals as special nodes
                String litVal = stmt.getObject().asLiteral().getString();
                String litNodeId = individualUri + "_lit_" + edgeId;
                String shortVal = litVal.length() > 60 ? litVal.substring(0, 57) + "..." : litVal;

                ObjectNode litNode = mapper.createObjectNode();
                litNode.put("id", litNodeId);
                litNode.put("label", shortVal);
                litNode.put("title", litVal); // full value as tooltip
                litNode.put("shape", "box");
                litNode.put("color", "#f0f0f0");
                ObjectNode font = mapper.createObjectNode();
                font.put("size", 11);
                font.put("color", "#333");
                litNode.set("font", font);
                nodes.add(litNode);

                ObjectNode edge = mapper.createObjectNode();
                edge.put("id", "e" + (edgeId++));
                edge.put("from", individualUri);
                edge.put("to", litNodeId);
                edge.put("label", localName(predUri));
                edge.put("arrows", "to");
                ObjectNode edgeColor = mapper.createObjectNode();
                edgeColor.put("color", "#ccc");
                edge.set("color", edgeColor);
                edge.put("dashes", true);
                edges.add(edge);
            }
        }

        // Incoming edges: other -> predicate -> this resource
        StmtIterator incoming = model.listStatements(null, null, resource);
        while (incoming.hasNext()) {
            Statement stmt = incoming.next();
            String predUri = stmt.getPredicate().getURI();
            if (predUri.equals(RDF.type.getURI())) continue;

            Resource subj = stmt.getSubject();
            if (!subj.isURIResource()) continue;
            String subjUri = subj.getURI();
            String subjLabel = getLabel(subj);
            String subjType = getType(subj);
            addNode(nodes, subjUri, subjLabel, subjType, false, nodeUris);

            ObjectNode edge = mapper.createObjectNode();
            edge.put("id", "e" + (edgeId++));
            edge.put("from", subjUri);
            edge.put("to", individualUri);
            edge.put("label", localName(predUri));
            edge.put("arrows", "to");
            edges.add(edge);
        }

        result.set("nodes", nodes);
        result.set("edges", edges);

        String json = result.toString();
        neighborhoodCache.put(individualUri, json);
        return json;
    }

    /**
     * Returns a lightweight summary of neighbor types and counts, without loading
     * the full neighborhood. Used to decide whether to show a filter dialog.
     * Returns JSON: { "totalCount": N, "literalCount": N, "types": [{ "type":"...", "count":N, "color":"..." }, ...] }
     */
    public String getNeighborSummaryJson(String individualUri) {
        String cached = neighborSummaryCache.get(individualUri);
        if (cached != null) return cached;

        Resource resource = model.getResource(individualUri);
        Map<String, Set<String>> typeUris = new LinkedHashMap<>();
        int literalCount = 0;

        // Outgoing edges
        StmtIterator outgoing = resource.listProperties();
        while (outgoing.hasNext()) {
            Statement stmt = outgoing.next();
            if (stmt.getPredicate().getURI().equals(RDF.type.getURI())) continue;
            if (stmt.getObject().isURIResource()) {
                Resource obj = stmt.getObject().asResource();
                String type = getType(obj);
                typeUris.computeIfAbsent(type, k -> new HashSet<>()).add(obj.getURI());
            } else if (stmt.getObject().isLiteral()) {
                literalCount++;
            }
        }

        // Incoming edges
        StmtIterator incoming = model.listStatements(null, null, resource);
        while (incoming.hasNext()) {
            Statement stmt = incoming.next();
            if (stmt.getPredicate().getURI().equals(RDF.type.getURI())) continue;
            Resource subj = stmt.getSubject();
            if (!subj.isURIResource()) continue;
            String type = getType(subj);
            typeUris.computeIfAbsent(type, k -> new HashSet<>()).add(subj.getURI());
        }

        ObjectNode summaryResult = mapper.createObjectNode();
        int totalNodes = typeUris.values().stream().mapToInt(Set::size).sum();
        summaryResult.put("totalCount", totalNodes + literalCount);
        summaryResult.put("literalCount", literalCount);

        ArrayNode types = mapper.createArrayNode();
        for (Map.Entry<String, Set<String>> entry : typeUris.entrySet()) {
            ObjectNode t = mapper.createObjectNode();
            t.put("type", entry.getKey());
            t.put("count", entry.getValue().size());
            t.put("color", getColorForType(entry.getKey()));
            types.add(t);
        }
        summaryResult.set("types", types);

        String json = summaryResult.toString();
        neighborSummaryCache.put(individualUri, json);
        return json;
    }

    /**
     * Returns a filtered, optionally limited set of neighbors.
     * @param individualUri    the center individual
     * @param allowedTypesJson JSON array of type names to include (e.g. ["Sigma","LogFile"])
     * @param maxPerType       max unique neighbor nodes per type (0 = no limit)
     * @param includeLiterals  whether to include literal (data-property) nodes
     */
    public String getFilteredNeighborsJson(String individualUri, String allowedTypesJson,
                                           int maxPerType, boolean includeLiterals) {
        // Parse allowed types
        Set<String> allowedTypes = new HashSet<>();
        boolean filterByType = false;
        try {
            ArrayNode arr = (ArrayNode) mapper.readTree(allowedTypesJson);
            for (int i = 0; i < arr.size(); i++) {
                allowedTypes.add(arr.get(i).asText());
            }
            filterByType = !allowedTypes.isEmpty();
        } catch (Exception e) {
            filterByType = false;
        }

        Resource resource = model.getResource(individualUri);
        ObjectNode filteredResult = mapper.createObjectNode();
        ArrayNode nodes = mapper.createArrayNode();
        ArrayNode edges = mapper.createArrayNode();

        Set<String> nodeUris = new HashSet<>();
        Map<String, Set<String>> addedPerType = new HashMap<>();
        int edgeId = 0;

        // Center node
        String centerLabel = getLabel(resource);
        String centerType = getType(resource);
        addNode(nodes, individualUri, centerLabel, centerType, true, nodeUris);

        // Outgoing edges
        StmtIterator outgoing = resource.listProperties();
        while (outgoing.hasNext()) {
            Statement stmt = outgoing.next();
            String predUri = stmt.getPredicate().getURI();
            if (predUri.equals(RDF.type.getURI())) continue;

            if (stmt.getObject().isURIResource()) {
                Resource obj = stmt.getObject().asResource();
                String objUri = obj.getURI();
                String objType = getType(obj);

                if (filterByType && !allowedTypes.contains(objType)) continue;

                Set<String> added = addedPerType.computeIfAbsent(objType, k -> new HashSet<>());
                if (!added.contains(objUri) && maxPerType > 0 && added.size() >= maxPerType) continue;
                added.add(objUri);

                addNode(nodes, objUri, getLabel(obj), objType, false, nodeUris);

                ObjectNode edge = mapper.createObjectNode();
                edge.put("id", "ef" + (edgeId++));
                edge.put("from", individualUri);
                edge.put("to", objUri);
                edge.put("label", localName(predUri));
                edge.put("arrows", "to");
                edges.add(edge);
            } else if (stmt.getObject().isLiteral() && includeLiterals) {
                String litVal = stmt.getObject().asLiteral().getString();
                String litNodeId = individualUri + "_lit_" + edgeId;
                String shortVal = litVal.length() > 60 ? litVal.substring(0, 57) + "..." : litVal;

                ObjectNode litNode = mapper.createObjectNode();
                litNode.put("id", litNodeId);
                litNode.put("label", shortVal);
                litNode.put("title", litVal);
                litNode.put("shape", "box");
                litNode.put("color", "#f0f0f0");
                ObjectNode font = mapper.createObjectNode();
                font.put("size", 11);
                font.put("color", "#333");
                litNode.set("font", font);
                nodes.add(litNode);

                ObjectNode edge = mapper.createObjectNode();
                edge.put("id", "ef" + (edgeId++));
                edge.put("from", individualUri);
                edge.put("to", litNodeId);
                edge.put("label", localName(predUri));
                edge.put("arrows", "to");
                ObjectNode edgeColor = mapper.createObjectNode();
                edgeColor.put("color", "#ccc");
                edge.set("color", edgeColor);
                edge.put("dashes", true);
                edges.add(edge);
            }
        }

        // Incoming edges
        StmtIterator incoming = model.listStatements(null, null, resource);
        while (incoming.hasNext()) {
            Statement stmt = incoming.next();
            String predUri = stmt.getPredicate().getURI();
            if (predUri.equals(RDF.type.getURI())) continue;

            Resource subj = stmt.getSubject();
            if (!subj.isURIResource()) continue;
            String subjUri = subj.getURI();
            String subjType = getType(subj);

            if (filterByType && !allowedTypes.contains(subjType)) continue;

            Set<String> added = addedPerType.computeIfAbsent(subjType, k -> new HashSet<>());
            if (!added.contains(subjUri) && maxPerType > 0 && added.size() >= maxPerType) continue;
            added.add(subjUri);

            addNode(nodes, subjUri, getLabel(subj), subjType, false, nodeUris);

            ObjectNode edge = mapper.createObjectNode();
            edge.put("id", "ef" + (edgeId++));
            edge.put("from", subjUri);
            edge.put("to", individualUri);
            edge.put("label", localName(predUri));
            edge.put("arrows", "to");
            edges.add(edge);
        }

        filteredResult.set("nodes", nodes);
        filteredResult.set("edges", edges);

        return filteredResult.toString();
    }

    /**
     * Add a node to the nodes array if not already included.
     */
    private void addNode(ArrayNode nodes, String uri, String label, String type,
                         boolean isCenter, Set<String> added) {
        if (added.contains(uri)) return;
        added.add(uri);

        ObjectNode node = mapper.createObjectNode();
        node.put("id", uri);
        node.put("label", label);
        node.put("title", type + "\n" + uri); // tooltip

        // Color coding by type
        String color = getColorForType(type);
        if (isCenter) {
            ObjectNode colorObj = mapper.createObjectNode();
            colorObj.put("background", color);
            colorObj.put("border", "#333");
            ObjectNode highlight = mapper.createObjectNode();
            highlight.put("background", color);
            highlight.put("border", "#000");
            colorObj.set("highlight", highlight);
            node.set("color", colorObj);
            node.put("shape", "dot");
            node.put("size", 25);
            ObjectNode font = mapper.createObjectNode();
            font.put("size", 14);
            font.put("bold", true);
            node.set("font", font);
        } else {
            ObjectNode colorObj = mapper.createObjectNode();
            colorObj.put("background", color);
            colorObj.put("border", lightenColor(color));
            node.set("color", colorObj);
            node.put("shape", "dot");
            node.put("size", 18);
        }
        nodes.add(node);
    }

    /** Pre-resolved property references (avoids repeated model.getProperty lookups) */
    private final Property propHasName;
    private final Property propRdfsLabel;

    private String getLabel(Resource resource) {
        String uri = resource.getURI();
        if (uri != null) {
            String cached = labelCache.get(uri);
            if (cached != null) return cached;
        }
        String label;
        // Try ufr:hasName first
        Statement nameStmt = resource.getProperty(propHasName);
        if (nameStmt != null && nameStmt.getObject().isLiteral()) {
            label = nameStmt.getObject().asLiteral().getString();
        } else {
            // Try rdfs:label
            Statement labelStmt = resource.getProperty(propRdfsLabel);
            if (labelStmt != null && labelStmt.getObject().isLiteral()) {
                label = labelStmt.getObject().asLiteral().getString();
            } else {
                // Fall back to local name
                label = localName(uri);
            }
        }
        if (uri != null) labelCache.put(uri, label);
        return label;
    }

    private String getType(Resource resource) {
        String uri = resource.getURI();
        if (uri != null) {
            String cached = typeCache.get(uri);
            if (cached != null) return cached;
        }
        String type;
        Statement typeStmt = resource.getProperty(RDF.type);
        if (typeStmt != null && typeStmt.getObject().isURIResource()) {
            type = localName(typeStmt.getObject().asResource().getURI());
        } else {
            type = "Unknown";
        }
        if (uri != null) typeCache.put(uri, type);
        return type;
    }

    private String buildTooltip(Resource resource) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(escapeHtml(getLabel(resource))).append("</b><br>");
        sb.append("<i>Type: ").append(escapeHtml(getType(resource))).append("</i><br>");
        sb.append("<small>").append(escapeHtml(resource.getURI())).append("</small><br><br>");

        StmtIterator props = resource.listProperties();
        while (props.hasNext()) {
            Statement stmt = props.next();
            String predName = localName(stmt.getPredicate().getURI());
            if (predName.equals("type")) continue;
            if (stmt.getObject().isLiteral()) {
                String val = stmt.getObject().asLiteral().getString();
                String shortVal = val.length() > 100 ? val.substring(0, 97) + "..." : val;
                sb.append("<b>").append(escapeHtml(predName)).append(":</b> ")
                  .append(escapeHtml(shortVal)).append("<br>");
            } else if (stmt.getObject().isURIResource()) {
                sb.append("<b>").append(escapeHtml(predName)).append(":</b> ")
                  .append(escapeHtml(localName(stmt.getObject().asResource().getURI()))).append("<br>");
            }
        }
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
    /**
     * Assign a color based on type name (thread-safe).
     *
     * Note: this uses a ConcurrentHashMap for concurrent access and an AtomicInteger
     * to step through the palette. The specific color assigned to a given type
     * depends on the order in which types are first encountered, which may vary
     * between runs and across threads. As a result, color assignments are not
     * guaranteed to be stable or reproducible across executions.
     */
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    /** Assign a color based on type name (thread-safe) */
    private static final Map<String, String> TYPE_COLORS = new ConcurrentHashMap<>();
    private static final String[] PALETTE = {
        "#4e79a7", "#f28e2b", "#e15759", "#76b7b2", "#59a14f",
        "#edc948", "#b07aa1", "#ff9da7", "#9c755f", "#bab0ac"
    };
    private static final java.util.concurrent.atomic.AtomicInteger paletteIdx =
        new java.util.concurrent.atomic.AtomicInteger(0);

    private static String getColorForType(String type) {
        return TYPE_COLORS.computeIfAbsent(type,
            t -> PALETTE[paletteIdx.getAndIncrement() % PALETTE.length]);
    }

    private static String lightenColor(String hex) {
        // Simple lighten: blend with white
        try {
            int r = Integer.parseInt(hex.substring(1, 3), 16);
            int g = Integer.parseInt(hex.substring(3, 5), 16);
            int b = Integer.parseInt(hex.substring(5, 7), 16);
            r = Math.min(255, r + (255 - r) / 3);
            g = Math.min(255, g + (255 - g) / 3);
            b = Math.min(255, b + (255 - b) / 3);
            return String.format("#%02x%02x%02x", r, g, b);
        } catch (Exception e) {
            return "#cccccc";
        }
    }

    /**
     * Check if the neighborhood of an individual has already been cached/explored.
     */
    public boolean isExplored(String uri) {
        return neighborhoodCache.containsKey(uri);
    }
}
