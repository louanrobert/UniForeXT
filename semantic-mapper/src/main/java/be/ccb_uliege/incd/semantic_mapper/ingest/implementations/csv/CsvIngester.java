package be.ccb_uliege.incd.semantic_mapper.ingest.implementations.csv;

import java.io.BufferedReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.function.BiConsumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceIngester;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;

/**
 * Implements the ingestion logic for CSV files. This class reads a CSV file,
 * parses it using Apache Commons CSV, and applies a provided SourceMapper to
 * each record to populate a Jena Model with RDF triples.
 */
public class CsvIngester implements SourceIngester {
    private static final Logger LOG = LoggerFactory.getLogger(CsvIngester.class);

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

            // Detect BOM and choose charset; default to UTF-8, fallback ISO-8859-1
            Charset charset = detectCharset(file);
            int totalRecords = countRecords(file, csvFormat);

            int processedRecords = 0;
            if (progressListener != null) {
                progressListener.accept(0, totalRecords);
            }

            try (BufferedReader reader = Files.newBufferedReader(file, charset);
                 CSVParser csvParser = new CSVParser(reader, csvFormat)) {
                for (CSVRecord csvRecord : csvParser) {
                    try {
                        mapper.map(new CsvRecord(csvRecord));
                    } catch (Exception perRecordEx) {
                        // per-record resilience: log and continue
                        LOG.warn("Record mapping failed at line {} in {}: {}", csvRecord.getRecordNumber(), file, perRecordEx.getMessage());
                        // continue to next record
                    }
                    processedRecords++;
                    if (progressListener != null && (processedRecords % 1000 == 0 || processedRecords == totalRecords)) {
                        progressListener.accept(processedRecords, totalRecords);
                    }
                }
            }
        } catch (NoSuchFileException e) {
            LOG.error("CSV file not found: {}", file);
        } catch (Exception e) {
            LOG.warn("Error ingesting CSV file {}: {}", file, e.getMessage(), e);
        }
    }

    private int countRecords(Path file, CSVFormat csvFormat) throws Exception {
        try (var reader = Files.newBufferedReader(file, StandardCharsets.ISO_8859_1);
                var csvParser = new CSVParser(reader, csvFormat)) {
            return csvParser.getRecords().size();
        }
    }

    private Charset detectCharset(Path file) {
        try (var in = Files.newInputStream(file)) {
            byte[] first3 = in.readNBytes(3);
            if (first3.length >= 3 && (first3[0] & 0xFF) == 0xEF && (first3[1] & 0xFF) == 0xBB && (first3[2] & 0xFF) == 0xBF) {
                return StandardCharsets.UTF_8; // UTF-8 BOM
            }
        } catch (Exception ignored) {}
        // Prefer UTF-8; fallback to ISO-8859-1 only when necessary
        return StandardCharsets.UTF_8;
    }
}
