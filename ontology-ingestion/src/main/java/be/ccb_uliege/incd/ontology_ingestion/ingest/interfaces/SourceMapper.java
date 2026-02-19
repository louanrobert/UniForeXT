package be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces;

import org.apache.jena.rdf.model.Model;

// This interface defines a mapper that takes a SourceRecord and maps it to a Jena Model. Implementations of this interface will contain the logic to transform the source data into RDF triples.
public interface SourceMapper {
    void map(SourceRecord record, Model model);
}
