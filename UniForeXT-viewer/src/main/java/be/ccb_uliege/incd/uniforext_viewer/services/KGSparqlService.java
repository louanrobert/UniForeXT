package be.ccb_uliege.incd.uniforext_viewer.services;

import java.io.StringWriter;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

final class KGSparqlService {

    private final KGServiceContext context;

    KGSparqlService(KGServiceContext context) {
        this.context = context;
    }

    String executeSparqlJson(String sparql) {
        ObjectNode response = context.mapper.createObjectNode();
        if (sparql == null || sparql.isBlank()) {
            response.put("ok", false);
            response.put("message", "Query is empty.");
            return response.toString();
        }

        final int maxRows = 500;
        long startedAt = System.nanoTime();

        try {
            Query query = QueryFactory.create(sparql);

            if (!(query.isSelectType() || query.isAskType() || query.isConstructType() || query.isDescribeType())) {
                response.put("ok", false);
                response.put("message", "Only SELECT, ASK, CONSTRUCT, and DESCRIBE queries are allowed.");
                return response.toString();
            }

            try (QueryExecution qexec = QueryExecutionFactory.create(query, context.model)) {
                response.put("ok", true);

                if (query.isSelectType()) {
                    response.put("queryType", "SELECT");
                    ResultSet rs = qexec.execSelect();
                    ArrayNode columns = context.mapper.createArrayNode();
                    for (String var : rs.getResultVars()) {
                        columns.add(var);
                    }

                    ArrayNode rows = context.mapper.createArrayNode();
                    int count = 0;
                    while (rs.hasNext() && count < maxRows) {
                        QuerySolution sol = rs.next();
                        ObjectNode row = context.mapper.createObjectNode();
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
        } finally {
            response.put("executionTimeMs", (System.nanoTime() - startedAt) / 1_000_000L);
        }

        return response.toString();
    }

    private ObjectNode rdfNodeToJson(RDFNode node) {
        ObjectNode out = context.mapper.createObjectNode();
        if (node == null) {
            out.put("kind", "null");
            out.putNull("value");
            return out;
        }

        if (node.isURIResource()) {
            Resource r = node.asResource();
            out.put("kind", "uri");
            out.put("value", r.getURI());
            out.put("display", KGService.localName(r.getURI()));
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