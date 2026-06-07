package be.ccb_uliege.incd.semantic_mapper.ingest.implementations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceMapper;
import be.ccb_uliege.incd.semantic_mapper.ingest.interfaces.SourceRecord;

class MultiFormatIngesterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void ingestDelegatesCsvFilesCaseInsensitively() throws Exception {
        Path csv = tempDirectory.resolve("events.CSV");
        Files.writeString(csv, "id;name\n1;Alice\n");
        RecordingMapper mapper = new RecordingMapper();

        new MultiFormatIngester().ingest(csv, mapper, ';', null);

        assertEquals(List.of("Alice"), mapper.values);
    }

    @Test
    void ingestIgnoresUnsupportedFileTypesAndExtensionlessFiles() throws Exception {
        Path textFile = tempDirectory.resolve("events.txt");
        Path extensionlessFile = tempDirectory.resolve("events");
        Files.writeString(textFile, "id;name\n1;Alice\n");
        Files.writeString(extensionlessFile, "id;name\n1;Alice\n");
        RecordingMapper mapper = new RecordingMapper();

        MultiFormatIngester ingester = new MultiFormatIngester();
        ingester.ingest(textFile, mapper, ';', null);
        ingester.ingest(extensionlessFile, mapper, ';', null);

        assertTrue(mapper.values.isEmpty());
    }

    private static class RecordingMapper implements SourceMapper {
        private final List<String> values = new ArrayList<>();

        @Override
        public void map(SourceRecord record) {
            values.add(record.get("name"));
        }
    }
}
