package be.ccb_uliege.incd.uniforext_viewer.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import be.ccb_uliege.incd.uniforext_viewer.services.records.ParsedDate;
import be.ccb_uliege.incd.uniforext_viewer.services.records.TimelineItem;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

final class KGServiceContext {

    private static final Logger LOG = Logger.getLogger(KGServiceContext.class.getName());

    final Model model;
    final ObjectMapper mapper = new ObjectMapper();

    final Map<String, String> neighborhoodCache = new ConcurrentHashMap<>();
    final Map<String, String> neighborSummaryCache = new ConcurrentHashMap<>();
    final Map<String, String> labelCache = new ConcurrentHashMap<>();
    final Map<String, String> typeCache = new ConcurrentHashMap<>();

    volatile Set<Property> datePropertiesCache;
    volatile java.util.List<TimelineItem> timelineItemsCache;
    volatile String timelineItemsJsonCache;
    volatile String timelineGroupsJsonCache;
    volatile String undatedIndividualsJsonCache;

    final ColorService colorService = new ColorService();
    final Property propHasName;
    final Property propRdfsLabel;

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

    private static final DateTimeFormatter DD_MM_YYYY_HH_MM = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final Pattern DATE_LIKE_PATTERN = Pattern.compile("^\\d{2,4}[/\\-T]");

    KGServiceContext(String ttlFilePath) {
        model = ModelFactory.createDefaultModel();
        try {
            model.read(ttlFilePath);
            LOG.info("Successfully loaded ontology from: " + ttlFilePath);
        } catch (Exception e) {
            String msg = "Failed to parse Turtle file: " + ttlFilePath;
            LOG.log(Level.SEVERE, msg, e);
            throw new IllegalArgumentException(msg + "\n\nDetails: " + e.getMessage(), e);
        }
        propHasName = model.getProperty(
                "http://www.semanticweb.org/robert_louan/ontologies/2026/1/unified-forensics-results#hasName");
        propRdfsLabel = model.getProperty("http://www.w3.org/2000/01/rdf-schema#label");
    }

    static String localName(String uri) {
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

    Set<Property> detectDateProperties() {
        if (datePropertiesCache != null)
            return datePropertiesCache;

        synchronized (this) {
            if (datePropertiesCache != null)
                return datePropertiesCache;
            Set<Property> result = new HashSet<>();
            for (String uri : DATE_PROPERTIES) {
                Property property = model.getProperty(uri);
                if (model.contains(null, property, (RDFNode) null)) {
                    result.add(property);
                }
            }
            Set<String> checkedNonDateProps = new HashSet<>();
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
        }
    }

    ParsedDate parseDate(String value) {
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
            return new ParsedDate(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDate ld = LocalDate.parse(value, DD_MM_YYYY);
            LocalDateTime ldt = ld.atStartOfDay();
            return new ParsedDate(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        } catch (DateTimeParseException ignored) {
        }
        try {
            LocalDateTime ldt = LocalDateTime.parse(value);
            return new ParsedDate(ldt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    String getLabel(Resource resource) {
        String uri = resource.getURI();
        if (uri != null) {
            String cached = labelCache.get(uri);
            if (cached != null)
                return cached;
        }
        String label;
        Statement nameStmt = resource.getProperty(propHasName);
        if (nameStmt != null && nameStmt.getObject().isLiteral()) {
            label = nameStmt.getObject().asLiteral().getString();
        } else {
            Statement labelStmt = resource.getProperty(propRdfsLabel);
            if (labelStmt != null && labelStmt.getObject().isLiteral()) {
                label = labelStmt.getObject().asLiteral().getString();
            } else {
                label = localName(uri);
            }
        }
        if (uri != null)
            labelCache.put(uri, label);
        return label;
    }

    String getType(Resource resource) {
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

    String buildTimelineTooltip(Resource resource, String label, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(escapeHtml(label)).append("</b><br>");
        sb.append("<i>Type: ").append(escapeHtml(type)).append("</i><br>");
        sb.append("<small>").append(escapeHtml(resource.getURI())).append("</small>");
        return sb.toString();
    }

    static String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    void addNode(ArrayNode nodes, String uri, String label, String type,
            boolean isCenter, Set<String> added) {
        if (added.contains(uri))
            return;
        added.add(uri);

        ObjectNode node = mapper.createObjectNode();
        node.put("id", uri);
        node.put("label", label);
        node.put("title", type + "\n" + uri);

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

}