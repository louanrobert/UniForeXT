package be.ccb_uliege.incd.semantic_mapper.ingest.implementations.csv;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceRecord;

class CsvIngesterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void ingestUsesDefaultSemicolonDelimiterAndReportsProgress() throws Exception {
        Path csv = tempDirectory.resolve("events.csv");
        Files.writeString(csv, "id;name\n1;Alice\n2;Bob\n");
        RecordingMapper mapper = new RecordingMapper();
        List<String> progressEvents = new ArrayList<>();

        new CsvIngester().ingest(csv, mapper, null, (completed, total) -> progressEvents.add(completed + "/" + total));

        assertEquals(List.of("Alice", "Bob"), mapper.values);
        assertEquals(List.of("0/2", "2/2"), progressEvents);
    }

    @Test
    void ingestUsesCustomDelimiterAndContinuesAfterRecordMappingFailure() throws Exception {
        Path csv = tempDirectory.resolve("events.csv");
        Files.writeString(csv, "id,name\n1,Alice\n2,FAIL\n3,Charlie\n");
        RecordingMapper mapper = new RecordingMapper("FAIL");

        new CsvIngester().ingest(csv, mapper, ',', null);

        assertEquals(List.of("Alice", "FAIL", "Charlie"), mapper.values);
    }

    @Test
    void ingestMissingFileDoesNotThrow() {
        Path missingCsv = tempDirectory.resolve("missing.csv");

        assertDoesNotThrow(() -> new CsvIngester().ingest(missingCsv, new RecordingMapper(), ';', null));
    }

    private static class RecordingMapper implements SourceMapper {
        private final List<String> values = new ArrayList<>();
        private final String valueThatShouldFail;

        RecordingMapper() {
            this(null);
        }

        RecordingMapper(String valueThatShouldFail) {
            this.valueThatShouldFail = valueThatShouldFail;
        }

        @Override
        public void map(SourceRecord record) {
            String value = record.get("name");
            values.add(value);
            if (value.equals(valueThatShouldFail)) {
                throw new IllegalArgumentException("simulated bad record");
            }
        }
    }
}
