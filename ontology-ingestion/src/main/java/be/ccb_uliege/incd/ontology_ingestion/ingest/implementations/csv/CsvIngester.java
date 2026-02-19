package be.ccb_uliege.incd.ontology_ingestion.ingest.implementations.csv;

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.csv.CSVFormat;
import org.apache.jena.rdf.model.Model;

import be.ccb_uliege.incd.ontology_ingestion.ingest.interfaces.SourceMapper;

public class CsvIngester {
   public void ingest(Path file, SourceMapper mapper, Model model, Character delimiter) {
      try {
         var reader = Files.newBufferedReader(file);
         var csvParser = new org.apache.commons.csv.CSVParser(reader, CSVFormat.Builder.create(CSVFormat.DEFAULT).setHeader().setSkipHeaderRecord(true).setDelimiter(delimiter).build());

         for (var csvRecord : csvParser) {
            mapper.map(new CsvRecord(csvRecord), model); // for each row, create a CsvRecord and pass it to the mapper with the model to be populated
         }
         csvParser.close();
       } catch (Exception e) {
         e.printStackTrace();
       }

   }
}
