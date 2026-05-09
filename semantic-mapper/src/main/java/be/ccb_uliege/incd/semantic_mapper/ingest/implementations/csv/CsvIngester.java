package be.ccb_uliege.incd.semantic_mapper.ingest.implementations.csv;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceIngester;
import java.util.logging.Level;
import java.util.logging.Logger;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;

/**
 * Implements the ingestion logic for CSV files. This class reads a CSV file,
 * parses it using Apache Commons CSV, and applies a provided SourceMapper to
 * each record to populate a Jena Model with RDF triples.
 */
public class CsvIngester implements SourceIngester {
    private static final Logger LOG = Logger.getLogger(CsvIngester.class.getName());
    @Override
    public void ingest(Path file, SourceMapper mapper, Character delimiter) {
        try {
            char effectiveDelimiter = delimiter != null ? delimiter : ';';
            var reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1);
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
            LOG.log(Level.WARNING, "Error ingesting CSV file: " + file + " - " + e.getMessage(), e);
        }
    }
}
