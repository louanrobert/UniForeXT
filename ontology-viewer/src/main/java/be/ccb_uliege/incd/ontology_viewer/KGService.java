package be.ccb_uliege.incd.ontology_viewer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.io.StringWriter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;

/**
 * Service class that wraps Apache Jena to parse a Turtle (.ttl) file,
 * extract individuals with date/time properties, and resolve neighborhoods.
 */
public class KGService {

    private static final Logger LOG = Logger.getLogger(KGService.class.getName());

    private final Model model;
    private final ObjectMapper mapper = new ObjectMapper();

    /** Cache: individual URI -> JSON neighborhood string */
    private final Map<String, String> neighborhoodCache = new ConcurrentHashMap<>();

    /** Cache: individual URI -> JSON neighbor summary string */
    private final Map<String, String> neighborSummaryCache = new ConcurrentHashMap<>();

    /** Cache: detected date properties (computed once) */
    private volatile Set<Property> datePropertiesCache;

    /**
     * Cache: timeline items (computed once, invalidated never since model is
     * read-only)
     */
    private volatile List<TimelineItem> timelineItemsCache;

    /** Cache: serialized timeline items JSON */
    private volatile String timelineItemsJsonCache;

    /** Cache: serialized timeline groups JSON */
    private volatile String timelineGroupsJsonCache;

    /** Cache: serialized undated individuals JSON */
    private volatile String undatedIndividualsJsonCache;

    /** Cache: resource URI -> label */
    private final Map<String, String> labelCache = new ConcurrentHashMap<>();

    /** Cache: resource URI -> type */
    private final Map<String, String> typeCache = new ConcurrentHashMap<>();

    /** Delegate for color management */
    private final ColorService colorService = new ColorService();

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
            "http://purl.org/dc/terms/date");

    /** Formatters for parsing timestamps */
    private static final DateTimeFormatter DD_MM_YYYY_HH_MM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * Initializes the knowledge graph service with a Turtle file.
     * Validates file syntax and provides meaningful error messages for malformed files.
     *
     * @param ttlFilePath the path to the Turtle (.ttl) file
     * @throws IllegalArgumentException if the file cannot be read or parsed
     */
    public KGService(String ttlFilePath) {
        model = ModelFactory.createDefaultModel();
        try {
            model.read(ttlFilePath);
            LOG.info("Successfully loaded ontology from: " + ttlFilePath);
        } catch (Exception e) {
            String msg = "Failed to parse Turtle file: " + ttlFilePath;
            LOG.log(Level.SEVERE, msg, e);
            throw new IllegalArgumentException(msg + "\n\nDetails: " + e.getMessage(), e);
        }
        // Pre-resolve frequently-used property references
        propHasName = model.getProperty(
                "http://www.semanticweb.org/robert_louan/ontologies/2026/1/unified-forensics-results#hasName");
        propRdfsLabel = model.getProperty("http://www.w3.org/2000/01/rdf-schema#label");
    }

    /**
     * Returns a local name for a URI resource (after # or last /).
     */
    public static String localName(String uri) {
        if (uri == null)
            return "";
        int hash = uri.lastIndexOf('#');
        if (hash >= 0 && hash < uri.length() - 1)
            return uri.substring(hash + 1);
        int slash = uri.lastIndexOf('/');
        if (slash >= 0 && slash < uri.length() - 1)
            return uri.substring(slash + 1);
        return uri;
    }

    /**
     * Find all properties in the model that carry date-like values.
     * Result is cached after the first call (double-checked locking).
     */
    private Set<Property> detectDateProperties() {
        if (datePropertiesCache != null)
            return datePropertiesCache;

        synchronized (this) {
            if (datePropertiesCache != null)
                return datePropertiesCache;
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
                        if (parseDate(val) != null) {
                            result.add(stmt.getPredicate());
                        } else {
                            checkedNonDateProps.add(propUri);
                        }
                    }
                }
            }
            datePropertiesCache = Collections.unmodifiableSet(result);
            return datePropertiesCache;
        } // end synchronized
    }

    /**
     * Quick regex pre-check to avoid expensive parse attempts on clearly non-date
     * strings.
     * Matches strings that start with 2–4 digits followed by a common date
     * separator
     * ('/' or '-') or 'T', which is typical of the date formats we support (e.g.
     * "2024-01-31", "31/01/2024", "2024-01-31T10:15:30"). This is intentionally a
     * loose filter to skip obviously non-date values before running full parsers.
     */
    private static final Pattern DATE_LIKE_PATTERN = Pattern.compile("^\\d{2,4}[/\\-T]");

    private record ParsedDate(String iso, long epochMillis) {
    }

    /**
     * Try to parse a string as a date/time. Returns ISO string plus epoch millis,
     * or null.
     * Uses a regex pre-check to skip obviously non-date strings early.
     */
    private ParsedDate parseDate(String value) {
        if (value == null || value.isBlank())
            return null;
        value = value.trim();
        if (!DATE_LIKE_PATTERN.matcher(value).find())
            return null;
        try {
            OffsetDateTime odt = OffsetDateTime.parse(value, ISO_OFFSET);
            LocalDateTime ldt = odt.toLocalDateTime();
            return new ParsedDate(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), odt.toInstant().toEpochMilli());
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(value, DD_MM_YYYY_HH_MM);
            return new ParsedDate(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDate ld = LocalDate.parse(value, DD_MM_YYYY);
            LocalDateTime ldt = ld.atStartOfDay();
            return new ParsedDate(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(value);
            return new ParsedDate(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    /**
     * Data class for a timeline item.
     */
    public record TimelineItem(String id, String uri, String label, String type,
            String start, long timestamp, String content) {
    }

    /**
     * Data class for an undated individual.
     */
    public record UndatedIndividual(String uri, String label, String type) {
    }

    /**
     * Extract all individuals that have a date property, grouped for the timeline.
     * Result is cached after the first call.
     */
    public List<TimelineItem> getTimelineItems() {
        if (timelineItemsCache != null)
            return timelineItemsCache;
        synchronized (this) {
            if (timelineItemsCache != null)
                return timelineItemsCache;

            Set<Property> dateProps = detectDateProperties();
            List<TimelineItem> items = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (Property dateProp : dateProps) {
                StmtIterator iter = model.listStatements(null, dateProp, (RDFNode) null);
                while (iter.hasNext()) {
                    Statement stmt = iter.next();
                    Resource subject = stmt.getSubject();
                    if (!subject.isURIResource())
                        continue;
                    String uri = subject.getURI();
                    if (seen.contains(uri))
                        continue;

                    String dateStr = stmt.getObject().asLiteral().getString();
                    ParsedDate parsedDate = parseDate(dateStr);
                    if (parsedDate == null)
                        continue;
                    seen.add(uri);

                    String label = getLabel(subject);
                    String type = getType(subject);
                    String tooltip = buildTimelineTooltip(subject, label, type);

                    items.add(new TimelineItem(
                            uri, uri, label, type, parsedDate.iso(), parsedDate.epochMillis(), tooltip));
                }
            }
            // Sort by date
            items.sort(Comparator.comparing(TimelineItem::start));
            timelineItemsCache = Collections.unmodifiableList(items);
            return timelineItemsCache;
        } // end synchronized
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
                if (s.isURIResource())
                    datedUris.add(s.getURI());
            }
        }

        // URIs of ontology/schema items to skip (not instances)
        Set<String> ontologyTypes = Set.of(
            "http://www.w3.org/2002/07/owl#Ontology",
            "http://www.w3.org/2002/07/owl#Class",
            "http://www.w3.org/2002/07/owl#ObjectProperty",
            "http://www.w3.org/2002/07/owl#DatatypeProperty",
            "http://www.w3.org/2002/07/owl#AnnotationProperty",
            "http://www.w3.org/2002/07/owl#FunctionalProperty",
            "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property",
            "http://www.w3.org/2000/01/rdf-schema#Class"
        );

        List<UndatedIndividual> undated = new ArrayList<>();
        // Find all typed resources
        StmtIterator typeIter = model.listStatements(null, RDF.type, (RDFNode) null);
        Set<String> seen = new HashSet<>();
        while (typeIter.hasNext()) {
            Statement stmt = typeIter.next();
            Resource subject = stmt.getSubject();
            if (!subject.isURIResource())
                continue;
            String uri = subject.getURI();
            if (seen.contains(uri) || datedUris.contains(uri))
                continue;
            // Skip ontology/schema items
            String typeUri = stmt.getObject().isURIResource() ? stmt.getObject().asResource().getURI() : "";
            if (ontologyTypes.contains(typeUri))
                continue;
            seen.add(uri);
            undated.add(new UndatedIndividual(uri, getLabel(subject), localName(typeUri)));
        }
        return undated;
    }

    /**
     * Returns a JSON string of timeline items compatible with Vis.js Timeline.
     */
    public String getTimelineItemsJson() {
        String cached = timelineItemsJsonCache;
        if (cached != null)
            return cached;

        synchronized (this) {
            if (timelineItemsJsonCache != null)
                return timelineItemsJsonCache;

            List<TimelineItem> items = getTimelineItems();
            ArrayNode array = mapper.createArrayNode();

            for (TimelineItem item : items) {
                ObjectNode node = mapper.createObjectNode();
                node.put("id", item.uri());
                node.put("uri", item.uri());
                node.put("label", item.label());
                node.put("content", escapeHtml(item.label()));
                node.put("start", item.start());
                node.put("timestamp", item.timestamp());
                node.put("title", item.content()); // compact tooltip on hover

                String group = item.type();
                String color = colorService.getColorForType(group);
                node.put("group", group);
                node.put("style", "background-color: " + color + "; color: white; border-radius: 4px; padding: 2px 6px;");
                array.add(node);
            }
            timelineItemsJsonCache = array.toString();
            return timelineItemsJsonCache;
        }
    }

    /**
     * Returns a JSON string of timeline groups (one per type).
     */
    public String getTimelineGroupsJson() {
        String cached = timelineGroupsJsonCache;
        if (cached != null)
            return cached;

        synchronized (this) {
            if (timelineGroupsJsonCache != null)
                return timelineGroupsJsonCache;

            List<TimelineItem> items = getTimelineItems();
            Set<String> types = items.stream().map(TimelineItem::type).collect(Collectors.toCollection(LinkedHashSet::new));
            ArrayNode array = mapper.createArrayNode();
            for (String type : types) {
                ObjectNode group = mapper.createObjectNode();
                group.put("id", type);
                group.put("content", type);
                group.put("color", colorService.getColorForType(type));
                array.add(group);
            }
            timelineGroupsJsonCache = array.toString();
            return timelineGroupsJsonCache;
        }
    }

    /**
     * Returns a JSON string of undated individuals for the sidebar.
     */
    public String getUndatedIndividualsJson() {
        String cached = undatedIndividualsJsonCache;
        if (cached != null)
            return cached;

        synchronized (this) {
            if (undatedIndividualsJsonCache != null)
                return undatedIndividualsJsonCache;

            List<UndatedIndividual> undated = getUndatedIndividuals();
            ArrayNode array = mapper.createArrayNode();
            for (UndatedIndividual ind : undated) {
                ObjectNode node = mapper.createObjectNode();
                node.put("uri", ind.uri());
                node.put("label", ind.label());
                node.put("type", ind.type());
                String color = colorService.getColorForType(ind.type());
                node.put("color", color);
                node.put("lighterColor", ColorService.lightenColor(color));
                array.add(node);
            }
            undatedIndividualsJsonCache = array.toString();
            return undatedIndividualsJsonCache;
        }
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
            if (predUri.equals(RDF.type.getURI()))
                continue;

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
            if (predUri.equals(RDF.type.getURI()))
                continue;

            Resource subj = stmt.getSubject();
            if (!subj.isURIResource())
                continue;
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
     * Returns JSON: { "totalCount": N, "literalCount": N, "types": [{ "type":"...",
     * "count":N, "color":"..." }, ...] }
     */
    public String getNeighborSummaryJson(String individualUri) {
        String cached = neighborSummaryCache.get(individualUri);
        if (cached != null)
            return cached;

        Resource resource = model.getResource(individualUri);
        Map<String, Set<String>> typeUris = new LinkedHashMap<>();
        int literalCount = 0;

        // Outgoing edges
        StmtIterator outgoing = resource.listProperties();
        while (outgoing.hasNext()) {
            Statement stmt = outgoing.next();
            if (stmt.getPredicate().getURI().equals(RDF.type.getURI()))
                continue;
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
            if (stmt.getPredicate().getURI().equals(RDF.type.getURI()))
                continue;
            Resource subj = stmt.getSubject();
            if (!subj.isURIResource())
                continue;
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
            t.put("color", colorService.getColorForType(entry.getKey()));
            types.add(t);
        }
        summaryResult.set("types", types);

        String json = summaryResult.toString();
        neighborSummaryCache.put(individualUri, json);
        return json;
    }

    /**
     * Returns a filtered, optionally limited set of neighbors.
     * 
     * @param individualUri    the center individual
     * @param allowedTypesJson JSON array of type names to include (e.g.
     *                         ["Sigma","LogFile"])
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
            LOG.log(Level.WARNING,
                    "Failed to parse allowedTypesJson, returning unfiltered neighbors: " + e.getMessage());
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
            if (predUri.equals(RDF.type.getURI()))
                continue;

            if (stmt.getObject().isURIResource()) {
                Resource obj = stmt.getObject().asResource();
                String objUri = obj.getURI();
                String objType = getType(obj);

                if (filterByType && !allowedTypes.contains(objType))
                    continue;

                Set<String> added = addedPerType.computeIfAbsent(objType, k -> new HashSet<>());
                if (!added.contains(objUri) && maxPerType > 0 && added.size() >= maxPerType)
                    continue;
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
            if (predUri.equals(RDF.type.getURI()))
                continue;

            Resource subj = stmt.getSubject();
            if (!subj.isURIResource())
                continue;
            String subjUri = subj.getURI();
            String subjType = getType(subj);

            if (filterByType && !allowedTypes.contains(subjType))
                continue;

            Set<String> added = addedPerType.computeIfAbsent(subjType, k -> new HashSet<>());
            if (!added.contains(subjUri) && maxPerType > 0 && added.size() >= maxPerType)
                continue;
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
        if (added.contains(uri))
            return;
        added.add(uri);

        ObjectNode node = mapper.createObjectNode();
        node.put("id", uri);
        node.put("label", label);
        node.put("title", type + "\n" + uri); // tooltip

        // Color coding by type
        String color = colorService.getColorForType(type);
        String lighterColor = ColorService.lightenColor(color);
        if (isCenter) {
            ObjectNode colorObj = mapper.createObjectNode();
            colorObj.put("background", color);
            colorObj.put("border", "#333");
            ObjectNode highlight = mapper.createObjectNode();
            highlight.put("background", color);
            highlight.put("border", "#000");
            colorObj.set("highlight", highlight);
            ObjectNode hover = mapper.createObjectNode();
            hover.put("background", lighterColor);
            hover.put("border", "#333");
            colorObj.set("hover", hover);
            node.set("color", colorObj);
            node.put("shape", "dot");
            node.put("size", 25);
            ObjectNode font = mapper.createObjectNode();
            font.put("size", 14);
            font.put("color", "#e0e0e0");
            ObjectNode fontBold = mapper.createObjectNode();
            fontBold.put("mod", "bold");
            font.set("bold", fontBold);
            node.set("font", font);
        } else {
            ObjectNode colorObj = mapper.createObjectNode();
            colorObj.put("background", color);
            colorObj.put("border", lighterColor);
            ObjectNode highlight = mapper.createObjectNode();
            highlight.put("background", lighterColor);
            highlight.put("border", color);
            colorObj.set("highlight", highlight);
            ObjectNode hover = mapper.createObjectNode();
            hover.put("background", lighterColor);
            hover.put("border", color);
            colorObj.set("hover", hover);
            node.set("color", colorObj);
            node.put("shape", "dot");
            node.put("size", 18);
            ObjectNode font = mapper.createObjectNode();
            font.put("size", 12);
            font.put("color", "#e0e0e0");
            node.set("font", font);
        }
        nodes.add(node);
    }

    /**
     * Pre-resolved property references (avoids repeated model.getProperty lookups)
     */
    private final Property propHasName;
    private final Property propRdfsLabel;

    private String getLabel(Resource resource) {
        String uri = resource.getURI();
        if (uri != null) {
            String cached = labelCache.get(uri);
            if (cached != null)
                return cached;
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
        if (uri != null)
            labelCache.put(uri, label);
        return label;
    }

    private String getType(Resource resource) {
        String uri = resource.getURI();
        if (uri != null) {
            String cached = typeCache.get(uri);
            if (cached != null)
                return cached;
        }
        String type;
        Statement typeStmt = resource.getProperty(RDF.type);
        if (typeStmt != null && typeStmt.getObject().isURIResource()) {
            type = localName(typeStmt.getObject().asResource().getURI());
        } else {
            type = "Unknown";
        }
        if (uri != null)
            typeCache.put(uri, type);
        return type;
    }

    private String buildTimelineTooltip(Resource resource, String label, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(escapeHtml(label)).append("</b><br>");
        sb.append("<i>Type: ").append(escapeHtml(type)).append("</i><br>");
        sb.append("<small>").append(escapeHtml(resource.getURI())).append("</small>");
        return sb.toString();
    }

    private static String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }



    /**
     * Check if the neighborhood of an individual has already been cached/explored.
     */
    public boolean isExplored(String uri) {
        return neighborhoodCache.containsKey(uri);
    }

    /**
     * Get detailed properties of an individual as JSON for the detail panel.
     * Returns JSON: { label, type, literals: [{predicate, value}], outgoing:
     * [{predicate, targetUri, targetLabel}], incoming: [{predicate, sourceUri,
     * sourceLabel}] }
     */
    public String getIndividualDetailsJson(String individualUri) {
        Resource resource = model.getResource(individualUri);
        ObjectNode result = mapper.createObjectNode();

        result.put("label", getLabel(resource));
        result.put("type", getType(resource));

        ArrayNode literals = mapper.createArrayNode();
        ArrayNode outgoing = mapper.createArrayNode();
        ArrayNode incoming = mapper.createArrayNode();

        // Outgoing properties
        StmtIterator outIter = resource.listProperties();
        while (outIter.hasNext()) {
            Statement stmt = outIter.next();
            String predUri = stmt.getPredicate().getURI();
            String predName = localName(predUri);
            if (predName.equals("type"))
                continue;

            if (stmt.getObject().isLiteral()) {
                ObjectNode lit = mapper.createObjectNode();
                lit.put("predicate", predName);
                lit.put("value", stmt.getObject().asLiteral().getString());
                literals.add(lit);
            } else if (stmt.getObject().isURIResource()) {
                Resource target = stmt.getObject().asResource();
                ObjectNode rel = mapper.createObjectNode();
                rel.put("predicate", predName);
                rel.put("targetUri", target.getURI());
                rel.put("targetLabel", getLabel(target));
                outgoing.add(rel);
            }
        }

        // Incoming relations
        StmtIterator inIter = model.listStatements(null, null, resource);
        while (inIter.hasNext()) {
            Statement stmt = inIter.next();
            String predUri = stmt.getPredicate().getURI();
            String predName = localName(predUri);
            if (predName.equals("type"))
                continue;

            Resource source = stmt.getSubject();
            if (!source.isURIResource())
                continue;
            ObjectNode rel = mapper.createObjectNode();
            rel.put("predicate", predName);
            rel.put("sourceUri", source.getURI());
            rel.put("sourceLabel", getLabel(source));
            incoming.add(rel);
        }

        result.set("literals", literals);
        result.set("outgoing", outgoing);
        result.set("incoming", incoming);

        return result.toString();
    }

    /**
     * Execute a read-only SPARQL query and return a JSON envelope.
     * Supported query forms: SELECT, ASK, CONSTRUCT, DESCRIBE.
     */
    public String executeSparqlJson(String sparql) {
        ObjectNode response = mapper.createObjectNode();
        if (sparql == null || sparql.isBlank()) {
            response.put("ok", false);
            response.put("message", "Query is empty.");
            return response.toString();
        }

        final int maxRows = 500;

        try {
            Query query = QueryFactory.create(sparql);

            // Guard against updates by allowing only read-only query forms.
            if (!(query.isSelectType() || query.isAskType() || query.isConstructType() || query.isDescribeType())) {
                response.put("ok", false);
                response.put("message", "Only SELECT, ASK, CONSTRUCT, and DESCRIBE queries are allowed.");
                return response.toString();
            }

            try (QueryExecution qexec = QueryExecutionFactory.create(query, model)) {
                response.put("ok", true);

                if (query.isSelectType()) {
                    response.put("queryType", "SELECT");
                    ResultSet rs = qexec.execSelect();
                    ArrayNode columns = mapper.createArrayNode();
                    for (String var : rs.getResultVars()) {
                        columns.add(var);
                    }

                    ArrayNode rows = mapper.createArrayNode();
                    int count = 0;
                    while (rs.hasNext() && count < maxRows) {
                        QuerySolution sol = rs.next();
                        ObjectNode row = mapper.createObjectNode();
                        for (String var : rs.getResultVars()) {
                            RDFNode value = sol.get(var);
                            row.set(var, rdfNodeToJson(value));
                        }
                        rows.add(row);
                        count++;
                    }

                    response.set("columns", columns);
                    response.set("rows", rows);
                    response.put("rowCount", count);
                    response.put("truncated", rs.hasNext());
                    if (rs.hasNext()) {
                        response.put("message", "Result truncated to " + maxRows + " rows.");
                    }
                } else if (query.isAskType()) {
                    response.put("queryType", "ASK");
                    response.put("boolean", qexec.execAsk());
                } else {
                    response.put("queryType", query.isConstructType() ? "CONSTRUCT" : "DESCRIBE");
                    Model resultModel = query.isConstructType() ? qexec.execConstruct() : qexec.execDescribe();
                    StringWriter writer = new StringWriter();
                    resultModel.write(writer, "TURTLE");
                    response.put("ttl", writer.toString());
                    response.put("tripleCount", resultModel.size());
                }
            }
        } catch (Exception e) {
            response.put("ok", false);
            response.put("message", e.getMessage() == null ? "Failed to execute query." : e.getMessage());
        }

        return response.toString();
    }

    private ObjectNode rdfNodeToJson(RDFNode node) {
        ObjectNode out = mapper.createObjectNode();
        if (node == null) {
            out.put("kind", "null");
            out.putNull("value");
            return out;
        }

        if (node.isURIResource()) {
            Resource r = node.asResource();
            out.put("kind", "uri");
            out.put("value", r.getURI());
            out.put("display", localName(r.getURI()));
            return out;
        }

        if (node.isLiteral()) {
            Literal lit = node.asLiteral();
            out.put("kind", "literal");
            out.put("value", lit.getString());
            if (lit.getDatatypeURI() != null) {
                out.put("datatype", lit.getDatatypeURI());
            }
            if (lit.getLanguage() != null && !lit.getLanguage().isBlank()) {
                out.put("lang", lit.getLanguage());
            }
            return out;
        }

        if (node.isAnon()) {
            out.put("kind", "bnode");
            out.put("value", node.asResource().getId().getLabelString());
            return out;
        }

        out.put("kind", "unknown");
        out.put("value", node.toString());
        return out;
    }
}
