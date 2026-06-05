package be.ccb_uliege.incd.ontology_viewer.services;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

final class KGDetailService {

    private final KGServiceContext context;

    KGDetailService(KGServiceContext context) {
        this.context = context;
    }

    String getIndividualDetailsJson(String individualUri) {
        Resource resource = context.model.getResource(individualUri);
        ObjectNode result = context.mapper.createObjectNode();

        result.put("label", context.getLabel(resource));
        result.put("type", context.getType(resource));

        ArrayNode literals = context.mapper.createArrayNode();
        ArrayNode outgoing = context.mapper.createArrayNode();
        ArrayNode incoming = context.mapper.createArrayNode();

        StmtIterator outIter = resource.listProperties();
        while (outIter.hasNext()) {
            Statement stmt = outIter.next();
            String predUri = stmt.getPredicate().getURI();
            String predName = KGService.localName(predUri);
            if (predName.equals("type"))
                continue;

            if (stmt.getObject().isLiteral()) {
                ObjectNode lit = context.mapper.createObjectNode();
                lit.put("predicate", predName);
                lit.put("value", stmt.getObject().asLiteral().getString());
                literals.add(lit);
            } else if (stmt.getObject().isURIResource()) {
                Resource target = stmt.getObject().asResource();
                ObjectNode rel = context.mapper.createObjectNode();
                rel.put("predicate", predName);
                rel.put("targetUri", target.getURI());
                rel.put("targetLabel", context.getLabel(target));
                outgoing.add(rel);
            }
        }

        StmtIterator inIter = context.model.listStatements(null, null, resource);
        while (inIter.hasNext()) {
            Statement stmt = inIter.next();
            String predUri = stmt.getPredicate().getURI();
            String predName = KGService.localName(predUri);
            if (predName.equals("type"))
                continue;

            Resource source = stmt.getSubject();
            if (!source.isURIResource())
                continue;
            ObjectNode rel = context.mapper.createObjectNode();
            rel.put("predicate", predName);
            rel.put("sourceUri", source.getURI());
            rel.put("sourceLabel", context.getLabel(source));
            incoming.add(rel);
        }

        result.set("literals", literals);
        result.set("outgoing", outgoing);
        result.set("incoming", incoming);

        return result.toString();
    }
}