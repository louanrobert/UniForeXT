package be.ccb_uliege.incd.uniforext_viewer.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;

final class KGNeighborhoodService {

    private final KGServiceContext context;

    KGNeighborhoodService(KGServiceContext context) {
        this.context = context;
    }

    boolean isExplored(String uri) {
        return context.neighborhoodCache.containsKey(uri);
    }

    String getNeighborsJson(String individualUri) {
        String cached = context.neighborhoodCache.get(individualUri);
        if (cached != null) {
            return cached;
        }

        Resource resource = context.model.getResource(individualUri);
        ObjectNode result = context.mapper.createObjectNode();
        ArrayNode nodes = context.mapper.createArrayNode();
        ArrayNode edges = context.mapper.createArrayNode();

        Set<String> nodeUris = new HashSet<>();
        int edgeId = 0;

        context.addNode(nodes, individualUri, context.getLabel(resource), context.getType(resource), true, nodeUris);

        StmtIterator outgoing = resource.listProperties();
        while (outgoing.hasNext()) {
            Statement stmt = outgoing.next();
            String predUri = stmt.getPredicate().getURI();
            if (predUri.equals(RDF.type.getURI()))
                continue;

            if (stmt.getObject().isURIResource()) {
                Resource obj = stmt.getObject().asResource();
                String objUri = obj.getURI();
                context.addNode(nodes, objUri, context.getLabel(obj), context.getType(obj), false, nodeUris);

                ObjectNode edge = context.mapper.createObjectNode();
                edge.put("id", "e" + (edgeId++));
                edge.put("from", individualUri);
                edge.put("to", objUri);
                edge.put("label", KGService.localName(predUri));
                edge.put("arrows", "to");
                edges.add(edge);
            } else if (stmt.getObject().isLiteral()) {
                String litVal = stmt.getObject().asLiteral().getString();
                String litNodeId = individualUri + "_lit_" + edgeId;
                String shortVal = litVal.length() > 60 ? litVal.substring(0, 57) + "..." : litVal;

                ObjectNode litNode = context.mapper.createObjectNode();
                litNode.put("id", litNodeId);
                litNode.put("label", shortVal);
                litNode.put("title", litVal);
                litNode.put("shape", "box");
                litNode.put("color", "#f0f0f0");
                ObjectNode font = context.mapper.createObjectNode();
                font.put("size", 11);
                font.put("color", "#333");
                litNode.set("font", font);
                nodes.add(litNode);

                ObjectNode edge = context.mapper.createObjectNode();
                edge.put("id", "e" + (edgeId++));
                edge.put("from", individualUri);
                edge.put("to", litNodeId);
                edge.put("label", KGService.localName(predUri));
                edge.put("arrows", "to");
                ObjectNode edgeColor = context.mapper.createObjectNode();
                edgeColor.put("color", "#ccc");
                edge.set("color", edgeColor);
                edge.put("dashes", true);
                edges.add(edge);
            }
        }

        StmtIterator incoming = context.model.listStatements(null, null, resource);
        while (incoming.hasNext()) {
            Statement stmt = incoming.next();
            String predUri = stmt.getPredicate().getURI();
            if (predUri.equals(RDF.type.getURI()))
                continue;

            Resource subj = stmt.getSubject();
            if (!subj.isURIResource())
                continue;
            String subjUri = subj.getURI();
            context.addNode(nodes, subjUri, context.getLabel(subj), context.getType(subj), false, nodeUris);

            ObjectNode edge = context.mapper.createObjectNode();
            edge.put("id", "e" + (edgeId++));
            edge.put("from", subjUri);
            edge.put("to", individualUri);
            edge.put("label", KGService.localName(predUri));
            edge.put("arrows", "to");
            edges.add(edge);
        }

        result.set("nodes", nodes);
        result.set("edges", edges);

        String json = result.toString();
        context.neighborhoodCache.put(individualUri, json);
        return json;
    }

    String getNeighborSummaryJson(String individualUri) {
        String cached = context.neighborSummaryCache.get(individualUri);
        if (cached != null)
            return cached;

        Resource resource = context.model.getResource(individualUri);
        Map<String, Set<String>> typeUris = new LinkedHashMap<>();
        int literalCount = 0;

        StmtIterator outgoing = resource.listProperties();
        while (outgoing.hasNext()) {
            Statement stmt = outgoing.next();
            if (stmt.getPredicate().getURI().equals(RDF.type.getURI()))
                continue;
            if (stmt.getObject().isURIResource()) {
                Resource obj = stmt.getObject().asResource();
                String type = context.getType(obj);
                typeUris.computeIfAbsent(type, k -> new HashSet<>()).add(obj.getURI());
            } else if (stmt.getObject().isLiteral()) {
                literalCount++;
            }
        }

        StmtIterator incoming = context.model.listStatements(null, null, resource);
        while (incoming.hasNext()) {
            Statement stmt = incoming.next();
            if (stmt.getPredicate().getURI().equals(RDF.type.getURI()))
                continue;
            Resource subj = stmt.getSubject();
            if (!subj.isURIResource())
                continue;
            String type = context.getType(subj);
            typeUris.computeIfAbsent(type, k -> new HashSet<>()).add(subj.getURI());
        }

        ObjectNode summaryResult = context.mapper.createObjectNode();
        int totalNodes = typeUris.values().stream().mapToInt(Set::size).sum();
        summaryResult.put("totalCount", totalNodes + literalCount);
        summaryResult.put("literalCount", literalCount);

        ArrayNode types = context.mapper.createArrayNode();
        for (Map.Entry<String, Set<String>> entry : typeUris.entrySet()) {
            ObjectNode t = context.mapper.createObjectNode();
            t.put("type", entry.getKey());
            t.put("count", entry.getValue().size());
            t.put("color", context.colorService.getColorForType(entry.getKey()));
            types.add(t);
        }
        summaryResult.set("types", types);

        String json = summaryResult.toString();
        context.neighborSummaryCache.put(individualUri, json);
        return json;
    }

    String getFilteredNeighborsJson(String individualUri, String allowedTypesJson,
            int maxPerType, boolean includeLiterals) {
        Set<String> allowedTypes = new HashSet<>();
        boolean filterByType = false;
        try {
            ArrayNode arr = (ArrayNode) context.mapper.readTree(allowedTypesJson);
            for (int i = 0; i < arr.size(); i++) {
                allowedTypes.add(arr.get(i).asText());
            }
            filterByType = !allowedTypes.isEmpty();
        } catch (Exception e) {
            filterByType = false;
        }

        Resource resource = context.model.getResource(individualUri);
        ObjectNode filteredResult = context.mapper.createObjectNode();
        ArrayNode nodes = context.mapper.createArrayNode();
        ArrayNode edges = context.mapper.createArrayNode();

        Set<String> nodeUris = new HashSet<>();
        Map<String, Set<String>> addedPerType = new HashMap<>();
        int edgeId = 0;

        context.addNode(nodes, individualUri, context.getLabel(resource), context.getType(resource), true, nodeUris);

        StmtIterator outgoing = resource.listProperties();
        while (outgoing.hasNext()) {
            Statement stmt = outgoing.next();
            String predUri = stmt.getPredicate().getURI();
            if (predUri.equals(RDF.type.getURI()))
                continue;

            if (stmt.getObject().isURIResource()) {
                Resource obj = stmt.getObject().asResource();
                String objUri = obj.getURI();
                String objType = context.getType(obj);

                if (filterByType && !allowedTypes.contains(objType))
                    continue;

                Set<String> added = addedPerType.computeIfAbsent(objType, k -> new HashSet<>());
                if (!added.contains(objUri) && maxPerType > 0 && added.size() >= maxPerType)
                    continue;
                added.add(objUri);

                context.addNode(nodes, objUri, context.getLabel(obj), objType, false, nodeUris);

                ObjectNode edge = context.mapper.createObjectNode();
                edge.put("id", "ef" + (edgeId++));
                edge.put("from", individualUri);
                edge.put("to", objUri);
                edge.put("label", KGService.localName(predUri));
                edge.put("arrows", "to");
                edges.add(edge);
            } else if (stmt.getObject().isLiteral() && includeLiterals) {
                String litVal = stmt.getObject().asLiteral().getString();
                String litNodeId = individualUri + "_lit_" + edgeId;
                String shortVal = litVal.length() > 60 ? litVal.substring(0, 57) + "..." : litVal;

                ObjectNode litNode = context.mapper.createObjectNode();
                litNode.put("id", litNodeId);
                litNode.put("label", shortVal);
                litNode.put("title", litVal);
                litNode.put("shape", "box");
                litNode.put("color", "#f0f0f0");
                ObjectNode font = context.mapper.createObjectNode();
                font.put("size", 11);
                font.put("color", "#333");
                litNode.set("font", font);
                nodes.add(litNode);

                ObjectNode edge = context.mapper.createObjectNode();
                edge.put("id", "ef" + (edgeId++));
                edge.put("from", individualUri);
                edge.put("to", litNodeId);
                edge.put("label", KGService.localName(predUri));
                edge.put("arrows", "to");
                ObjectNode edgeColor = context.mapper.createObjectNode();
                edgeColor.put("color", "#ccc");
                edge.set("color", edgeColor);
                edge.put("dashes", true);
                edges.add(edge);
            }
        }

        StmtIterator incoming = context.model.listStatements(null, null, resource);
        while (incoming.hasNext()) {
            Statement stmt = incoming.next();
            String predUri = stmt.getPredicate().getURI();
            if (predUri.equals(RDF.type.getURI()))
                continue;

            Resource subj = stmt.getSubject();
            if (!subj.isURIResource())
                continue;
            String subjUri = subj.getURI();
            String subjType = context.getType(subj);

            if (filterByType && !allowedTypes.contains(subjType))
                continue;

            Set<String> added = addedPerType.computeIfAbsent(subjType, k -> new HashSet<>());
            if (!added.contains(subjUri) && maxPerType > 0 && added.size() >= maxPerType)
                continue;
            added.add(subjUri);

            context.addNode(nodes, subjUri, context.getLabel(subj), subjType, false, nodeUris);

            ObjectNode edge = context.mapper.createObjectNode();
            edge.put("id", "ef" + (edgeId++));
            edge.put("from", subjUri);
            edge.put("to", individualUri);
            edge.put("label", KGService.localName(predUri));
            edge.put("arrows", "to");
            edges.add(edge);
        }

        filteredResult.set("nodes", nodes);
        filteredResult.set("edges", edges);
        return filteredResult.toString();
    }
}