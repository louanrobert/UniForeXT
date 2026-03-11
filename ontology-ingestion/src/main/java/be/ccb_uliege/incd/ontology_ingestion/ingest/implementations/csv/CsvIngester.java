package be.ccb_uliege.incd.ontology_ingestion.ingest.implementations.csv;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.jena.rdf.model.Model;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceIngester;
import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;

/**
 * Implements the ingestion logic for CSV files. This class reads a CSV file,
 * parses it using Apache Commons CSV, and applies a provided SourceMapper to
 * each record to populate a Jena Model with RDF triples.
 */
public class CsvIngester implements SourceIngester {
    @Override
    public void ingest(Path file, SourceMapper mapper, Model model, Character delimiter) {
        try {
            var reader = Files.newBufferedReader(file);
            var csvParser = new CSVParser(reader, CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setDelimiter(delimiter)
                    .build());

            for (var csvRecord : csvParser) {
                // for each row, create a CsvRecord and pass it to the mapper
                // with the model to be populated
                mapper.map(new CsvRecord(csvRecord), model);
            }
            csvParser.close();
        } catch (Exception e) {
            System.err.println("Error ingesting CSV file: " + file);
            e.printStackTrace();
        }
    }
}
