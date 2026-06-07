package be.ccb_uliege.incd.uniforext_viewer.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import be.ccb_uliege.incd.uniforext_viewer.services.records.TimelineItem;
import be.ccb_uliege.incd.uniforext_viewer.services.records.UndatedIndividual;

class KGServiceTest {

    private static final String EX = "http://example.test/";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    private KGService service;

    @BeforeEach
    void setUp() throws IOException {
        Path ttl = tempDir.resolve("graph.ttl");
        Files.writeString(ttl, """
                @prefix ex: <http://example.test/> .
                @prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
                @prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
                @prefix ufor: <http://www.semanticweb.org/robert_louan/ontologies/2026/1/unified-forensics-results#> .

                ex:event1 a ex:Event ;
                    ufor:hasName "Login <Event>" ;
                    ufor:hasTimestamp "2026-06-07T10:15:30Z"^^xsd:dateTime ;
                    ex:performedBy ex:user1 ;
                    ex:description "A user logged in with a long description that should remain available in literal details." .

                ex:user1 a ex:User ;
                    rdfs:label "Alice" ;
                    ex:memberOf ex:group1 .

                ex:group1 a ex:Group ;
                    ufor:hasName "Administrators" .

                ex:event2 a ex:Event ;
                    ufor:hasName "File Created" ;
                    ufor:hasTimestamp "07/06/2026 11:45" .
                """);
        service = new KGService(ttl.toUri().toString());
    }

    @Test
    void localNameHandlesCommonUriShapes() {
        assertEquals("Class", KGService.localName("http://example.test/ns#Class"));
        assertEquals("resource", KGService.localName("http://example.test/resource"));
        assertEquals("plain", KGService.localName("plain"));
        assertEquals("", KGService.localName(null));
    }

    @Test
    void getTimelineItemsReturnsDatedResourcesSortedByStart() {
        List<TimelineItem> items = service.getTimelineItems();

        assertEquals(2, items.size());
        assertEquals(EX + "event1", items.get(0).uri());
        assertEquals("Login <Event>", items.get(0).label());
        assertEquals("Event", items.get(0).type());
        assertEquals("2026-06-07T10:15:30", items.get(0).start());
        assertTrue(items.get(0).content().contains("Login &lt;Event&gt;"));
        assertEquals(EX + "event2", items.get(1).uri());
    }

    @Test
    void getTimelineItemsJsonIncludesEscapedContentAndGroupStyle() throws Exception {
        JsonNode items = MAPPER.readTree(service.getTimelineItemsJson());

        assertEquals(2, items.size());
        JsonNode first = items.get(0);
        assertEquals(EX + "event1", first.get("id").asText());
        assertEquals("Login &lt;Event&gt;", first.get("content").asText());
        assertEquals("Event", first.get("group").asText());
        assertTrue(first.get("style").asText().contains("background-color: #"));
    }

    @Test
    void getTimelineGroupsJsonReturnsDistinctTypes() throws Exception {
        JsonNode groups = MAPPER.readTree(service.getTimelineGroupsJson());

        assertEquals(1, groups.size());
        assertEquals("Event", groups.get(0).get("id").asText());
        assertTrue(groups.get(0).get("color").asText().matches("#[0-9A-Fa-f]{6}"));
    }

    @Test
    void getUndatedIndividualsExcludesDatedResources() {
        List<UndatedIndividual> undated = service.getUndatedIndividuals();

        assertEquals(2, undated.size());
        assertTrue(undated.stream().anyMatch(ind -> ind.uri().equals(EX + "user1")
                && ind.label().equals("Alice")
                && ind.type().equals("User")));
        assertTrue(undated.stream().anyMatch(ind -> ind.uri().equals(EX + "group1")
                && ind.label().equals("Administrators")
                && ind.type().equals("Group")));
        assertFalse(undated.stream().anyMatch(ind -> ind.uri().equals(EX + "event1")));
    }

    @Test
    void getUndatedIndividualsJsonIncludesColors() throws Exception {
        JsonNode undated = MAPPER.readTree(service.getUndatedIndividualsJson());

        assertEquals(2, undated.size());
        assertTrue(undated.get(0).hasNonNull("color"));
        assertTrue(undated.get(0).hasNonNull("lighterColor"));
    }

    @Test
    void getIndividualDetailsJsonSeparatesLiteralsOutgoingAndIncomingRelations() throws Exception {
        JsonNode details = MAPPER.readTree(service.getIndividualDetailsJson(EX + "user1"));

        assertEquals("Alice", details.get("label").asText());
        assertEquals("User", details.get("type").asText());
        assertEquals(1, details.get("outgoing").size());
        assertEquals("memberOf", details.get("outgoing").get(0).get("predicate").asText());
        assertEquals(EX + "group1", details.get("outgoing").get(0).get("targetUri").asText());
        assertEquals(1, details.get("incoming").size());
        assertEquals("performedBy", details.get("incoming").get(0).get("predicate").asText());
        assertEquals(EX + "event1", details.get("incoming").get(0).get("sourceUri").asText());
    }

    @Test
    void getNeighborsJsonCachesExploredNeighborhood() throws Exception {
        assertFalse(service.isExplored(EX + "event1"));

        JsonNode graph = MAPPER.readTree(service.getNeighborsJson(EX + "event1"));

        assertTrue(service.isExplored(EX + "event1"));
        assertTrue(graph.get("nodes").size() >= 4);
        assertTrue(graph.get("edges").size() >= 3);
        assertTrue(containsNode(graph, EX + "event1"));
        assertTrue(containsNode(graph, EX + "user1"));
        assertTrue(containsEdgeLabel(graph, "performedBy"));
        assertTrue(containsEdgeLabel(graph, "description"));
    }

    @Test
    void getNeighborSummaryJsonCountsNeighborTypesAndLiterals() throws Exception {
        JsonNode summary = MAPPER.readTree(service.getNeighborSummaryJson(EX + "event1"));

        assertEquals(4, summary.get("totalCount").asInt());
        assertEquals(3, summary.get("literalCount").asInt());
        assertEquals(1, summary.get("types").size());
        assertEquals("User", summary.get("types").get(0).get("type").asText());
        assertEquals(1, summary.get("types").get(0).get("count").asInt());
    }

    @Test
    void getFilteredNeighborsJsonFiltersByTypeAndLiteralFlag() throws Exception {
        JsonNode onlyUsers = MAPPER.readTree(service.getFilteredNeighborsJson(EX + "event1", "[\"User\"]", 10, false));

        assertTrue(containsNode(onlyUsers, EX + "event1"));
        assertTrue(containsNode(onlyUsers, EX + "user1"));
        assertFalse(hasLiteralNode(onlyUsers));
        assertEquals(1, onlyUsers.get("edges").size());

        JsonNode withLiterals = MAPPER.readTree(service.getFilteredNeighborsJson(EX + "event1", "not-json", 10, true));
        assertTrue(hasLiteralNode(withLiterals));
    }

    @Test
    void getFilteredNeighborsJsonHonorsMaxPerType() throws Exception {
        JsonNode filtered = MAPPER.readTree(service.getFilteredNeighborsJson(EX + "event1", "[]", 0, false));
        assertTrue(containsNode(filtered, EX + "user1"));

        JsonNode limited = MAPPER.readTree(service.getFilteredNeighborsJson(EX + "event1", "[]", 1, false));
        assertTrue(containsNode(limited, EX + "user1"));
    }

    @Test
    void executeSparqlJsonSupportsSelectAskConstructAndErrors() throws Exception {
        JsonNode select = MAPPER.readTree(service.executeSparqlJson("""
                PREFIX ex: <http://example.test/>
                SELECT ?label WHERE { ex:user1 <http://www.w3.org/2000/01/rdf-schema#label> ?label }
                """));
        assertTrue(select.get("ok").asBoolean());
        assertEquals("SELECT", select.get("queryType").asText());
        assertEquals(1, select.get("rowCount").asInt());
        assertEquals("literal", select.get("rows").get(0).get("label").get("kind").asText());
        assertEquals("Alice", select.get("rows").get(0).get("label").get("value").asText());

        JsonNode ask = MAPPER.readTree(service.executeSparqlJson("ASK { <http://example.test/user1> ?p ?o }"));
        assertTrue(ask.get("ok").asBoolean());
        assertEquals("ASK", ask.get("queryType").asText());
        assertTrue(ask.get("boolean").asBoolean());

        JsonNode construct = MAPPER.readTree(service.executeSparqlJson("CONSTRUCT { <http://example.test/user1> ?p ?o } WHERE { <http://example.test/user1> ?p ?o }"));
        assertTrue(construct.get("ok").asBoolean());
        assertEquals("CONSTRUCT", construct.get("queryType").asText());
        assertTrue(construct.get("tripleCount").asLong() > 0);
        assertNotNull(construct.get("ttl").asText());

        JsonNode empty = MAPPER.readTree(service.executeSparqlJson(" "));
        assertFalse(empty.get("ok").asBoolean());
        assertEquals("Query is empty.", empty.get("message").asText());

        JsonNode malformed = MAPPER.readTree(service.executeSparqlJson("SELECT WHERE"));
        assertFalse(malformed.get("ok").asBoolean());
    }

    private static boolean containsNode(JsonNode graph, String id) {
        for (JsonNode node : graph.get("nodes")) {
            if (id.equals(node.get("id").asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsEdgeLabel(JsonNode graph, String label) {
        for (JsonNode edge : graph.get("edges")) {
            if (label.equals(edge.get("label").asText())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasLiteralNode(JsonNode graph) {
        for (JsonNode node : graph.get("nodes")) {
            if (node.has("shape") && "box".equals(node.get("shape").asText())) {
                return true;
            }
        }
        return false;
    }
}
