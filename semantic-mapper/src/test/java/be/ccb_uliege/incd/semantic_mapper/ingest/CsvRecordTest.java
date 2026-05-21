package be.ccb_uliege.incd.semantic_mapper.ingest;

import be.ccb_uliege.incd.semantic_mapper.ingest.implementations.csv.CsvRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvRecordTest {

    @Test
    void getAndHasAndGetHashedBehaveAsExpected() throws Exception {
        String csv = "id,name,empty\n1, Alice ,   \n";
        CSVFormat.Builder builder = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader()
                .setSkipHeaderRecord(true);
        try (CSVParser parser = builder.build().parse(new StringReader(csv))) {
            List<CSVRecord> records = parser.getRecords();
            assertEquals(1, records.size());
            CSVRecord underlying = records.get(0);
            CsvRecord rec = new CsvRecord(underlying);

            assertEquals("Alice", rec.get("name"));
            String hashed = rec.getHashed("name");
            assertEquals(DigestUtils.sha256Hex("Alice"), hashed);

            assertTrue(rec.has("name"));
            assertFalse(rec.has("nonexistent"));
            // empty field should be considered not present
            assertFalse(rec.has("empty"));
        }
    }
}
