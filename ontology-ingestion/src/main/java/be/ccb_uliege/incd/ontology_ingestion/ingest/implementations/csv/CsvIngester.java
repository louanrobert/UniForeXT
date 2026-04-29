package be.ccb_uliege.incd.ontology_ingestion.ingest.implementations.csv;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceIngester;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;

/**
 * Implements the ingestion logic for CSV files. This class reads a CSV file,
 * parses it using Apache Commons CSV, and applies a provided SourceMapper to
 * each record to populate a Jena Model with RDF triples.
 */
public class CsvIngester implements SourceIngester {
    @Override
    public void ingest(Path file, SourceMapper mapper, Character delimiter) {
        try {
            char effectiveDelimiter = delimiter != null ? delimiter : ';';
            var reader = Files.newBufferedReader(file);
            var csvParser = new CSVParser(reader, CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setDelimiter(effectiveDelimiter)
                    .build());

            for (var csvRecord : csvParser) {
                mapper.map(new CsvRecord(csvRecord));
            }
            csvParser.close();
        } catch (Exception e) {
            System.err.println("Error ingesting CSV file: " + file);
            e.printStackTrace();
        }
    }
}
