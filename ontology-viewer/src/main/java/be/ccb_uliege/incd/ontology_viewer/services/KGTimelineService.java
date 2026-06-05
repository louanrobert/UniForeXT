package be.ccb_uliege.incd.ontology_viewer.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import be.ccb_uliege.incd.ontology_viewer.services.records.ParsedDate;
import be.ccb_uliege.incd.ontology_viewer.services.records.TimelineItem;
import be.ccb_uliege.incd.ontology_viewer.services.records.UndatedIndividual;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

final class KGTimelineService {

    private final KGServiceContext context;

    KGTimelineService(KGServiceContext context) {
        this.context = context;
    }

    List<TimelineItem> getTimelineItems() {
        if (context.timelineItemsCache != null)
            return context.timelineItemsCache;
        synchronized (context) {
            if (context.timelineItemsCache != null)
                return context.timelineItemsCache;

            Set<Property> dateProps = context.detectDateProperties();
            List<TimelineItem> items = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (Property dateProp : dateProps) {
                StmtIterator iter = context.model.listStatements(null, dateProp, (RDFNode) null);
                while (iter.hasNext()) {
                    Statement stmt = iter.next();
                    Resource subject = stmt.getSubject();
                    if (!subject.isURIResource())
                        continue;
                    String uri = subject.getURI();
                    if (seen.contains(uri))
                        continue;

                    String dateStr = stmt.getObject().asLiteral().getString();
                    ParsedDate parsedDate = context.parseDate(dateStr);
                    if (parsedDate == null)
                        continue;
                    seen.add(uri);

                    String label = context.getLabel(subject);
                    String type = context.getType(subject);
                    String tooltip = context.buildTimelineTooltip(subject, label, type);

                    items.add(new TimelineItem(
                            uri, uri, label, type, parsedDate.iso(), parsedDate.epochMillis(), tooltip));
                }
            }
            items.sort(Comparator.comparing(TimelineItem::start));
            context.timelineItemsCache = List.copyOf(items);
            return context.timelineItemsCache;
        }
    }

    List<UndatedIndividual> getUndatedIndividuals() {
        Set<Property> dateProps = context.detectDateProperties();
        Set<String> datedUris = new HashSet<>();

        for (Property dateProp : dateProps) {
            StmtIterator iter = context.model.listStatements(null, dateProp, (RDFNode) null);
            while (iter.hasNext()) {
                Resource subject = iter.next().getSubject();
                if (subject.isURIResource())
                    datedUris.add(subject.getURI());
            }
        }

        Set<String> ontologyTypes = Set.of(
                "http://www.w3.org/2002/07/owl#Ontology",
                "http://www.w3.org/2002/07/owl#Class",
                "http://www.w3.org/2002/07/owl#ObjectProperty",
                "http://www.w3.org/2002/07/owl#DatatypeProperty",
                "http://www.w3.org/2002/07/owl#AnnotationProperty",
                "http://www.w3.org/2002/07/owl#FunctionalProperty",
                "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property",
                "http://www.w3.org/2000/01/rdf-schema#Class");

        List<UndatedIndividual> undated = new ArrayList<>();
        StmtIterator typeIter = context.model.listStatements(null, RDF.type, (RDFNode) null);
        Set<String> seen = new HashSet<>();
        while (typeIter.hasNext()) {
            Statement stmt = typeIter.next();
            Resource subject = stmt.getSubject();
            if (!subject.isURIResource())
                continue;
            String uri = subject.getURI();
            if (seen.contains(uri) || datedUris.contains(uri))
                continue;
            String typeUri = stmt.getObject().isURIResource() ? stmt.getObject().asResource().getURI() : "";
            if (ontologyTypes.contains(typeUri))
                continue;
            seen.add(uri);
            undated.add(new UndatedIndividual(uri, context.getLabel(subject), KGService.localName(typeUri)));
        }
        return undated;
    }

    String getTimelineItemsJson() {
        String cached = context.timelineItemsJsonCache;
        if (cached != null)
            return cached;

        synchronized (context) {
            if (context.timelineItemsJsonCache != null)
                return context.timelineItemsJsonCache;

            List<TimelineItem> items = getTimelineItems();
            ArrayNode array = context.mapper.createArrayNode();

            for (TimelineItem item : items) {
                ObjectNode node = context.mapper.createObjectNode();
                node.put("id", item.uri());
                node.put("uri", item.uri());
                node.put("label", item.label());
                node.put("content", KGServiceContext.escapeHtml(item.label()));
                node.put("start", item.start());
                node.put("timestamp", item.timestamp());
                node.put("title", item.content());

                String group = item.type();
                String color = context.colorService.getColorForType(group);
                node.put("group", group);
                node.put("style", "background-color: " + color + "; color: white; border-radius: 4px; padding: 2px 6px;");
                array.add(node);
            }
            context.timelineItemsJsonCache = array.toString();
            return context.timelineItemsJsonCache;
        }
    }

    String getTimelineGroupsJson() {
        String cached = context.timelineGroupsJsonCache;
        if (cached != null)
            return cached;

        synchronized (context) {
            if (context.timelineGroupsJsonCache != null)
                return context.timelineGroupsJsonCache;

            List<TimelineItem> items = getTimelineItems();
            Set<String> types = items.stream().map(TimelineItem::type).collect(Collectors.toCollection(LinkedHashSet::new));
            ArrayNode array = context.mapper.createArrayNode();
            for (String type : types) {
                ObjectNode group = context.mapper.createObjectNode();
                group.put("id", type);
                group.put("content", type);
                group.put("color", context.colorService.getColorForType(type));
                array.add(group);
            }
            context.timelineGroupsJsonCache = array.toString();
            return context.timelineGroupsJsonCache;
        }
    }

    String getUndatedIndividualsJson() {
        String cached = context.undatedIndividualsJsonCache;
        if (cached != null)
            return cached;

        synchronized (context) {
            if (context.undatedIndividualsJsonCache != null)
                return context.undatedIndividualsJsonCache;

            List<UndatedIndividual> undated = getUndatedIndividuals();
            ArrayNode array = context.mapper.createArrayNode();
            for (UndatedIndividual ind : undated) {
                ObjectNode node = context.mapper.createObjectNode();
                node.put("uri", ind.uri());
                node.put("label", ind.label());
                node.put("type", ind.type());
                String color = context.colorService.getColorForType(ind.type());
                node.put("color", color);
                node.put("lighterColor", ColorService.lightenColor(color));
                array.add(node);
            }
            context.undatedIndividualsJsonCache = array.toString();
            return context.undatedIndividualsJsonCache;
        }
    }
}