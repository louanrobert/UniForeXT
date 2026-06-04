package be.ccb_uliege.incd.semantic_mapper.ingest.implementations.csv;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.function.BiConsumer;

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
    public void ingest(Path file, SourceMapper mapper, Character delimiter,
            BiConsumer<Integer, Integer> progressListener) {
        try {
            char effectiveDelimiter = delimiter != null ? delimiter : ';';
            CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setDelimiter(effectiveDelimiter)
                    .setTrim(true)
                    .setIgnoreEmptyLines(true)
                    .build();

            int totalRecords = countRecords(file, csvFormat);
            if (progressListener != null) {
                progressListener.accept(0, totalRecords);
            }

            try (var reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1);
                    var csvParser = new CSVParser(reader, csvFormat)) {
                int processedRecords = 0;
                for (var csvRecord : csvParser) {
                    mapper.map(new CsvRecord(csvRecord));
                    processedRecords++;
                    if (progressListener != null) {
                        progressListener.accept(processedRecords, totalRecords);
                    }
                }
            }
        } catch (NoSuchFileException e) {
            LOG.log(Level.SEVERE, "CSV file not found: " + file);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error ingesting CSV file: " + file + " - " + e.getMessage(), e);
        }
    }

    private int countRecords(Path file, CSVFormat csvFormat) throws Exception {
        try (var reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1);
                var csvParser = new CSVParser(reader, csvFormat)) {
            return csvParser.getRecords().size();
        }
    }
}
