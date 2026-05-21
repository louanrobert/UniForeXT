# Mappers configuration

The mappers configuration is defined in YAML files that specify how to process and analyze the data from the analysis folder. These YAML files use environment variables to reference paths and values, making the configuration flexible and adaptable to different environments.

## Configuration Files
Configuration files are YAML-based definitions that specify how to map CSV data to RDF/OWL ontology concepts. Each configuration file contains two main sections: **Generic Mappings** and **Mappers**.

### File Structure

A typical configuration file follows this structure:

```yaml
genericMappings:
  # Reusable mapping templates
  addTimestamp:
    - sourceField: "FieldName"
      type: dataProperty
      owlProperty: propertyName
      dataType: xsd:dateTimeStamp

mappers:
  # Specific mapper definitions
  - name: MapperName
    file: "${ANALYSIS_ROOT}/path/to/*.csv"
    owlClass: ClassName
    # ... additional properties
```

### Generic Mappings

Generic mappings define reusable components that can be applied to multiple mappers. They handle common data transformations like timestamps, hostnames, and users.

**Example:**
```yaml
genericMappings:
  addTimestamp:
    - sourceField: "timestamp"
      type: dataProperty
      owlProperty: hasTimestamp
      dataType: xsd:dateTimeStamp

  addComputer:
    - type: linkedIndividual
      owlClass: Computer
      linkProperty: hasComputer
      identifier:
        fields: "Computer"
      dataProperties:
        - sourceField: "Computer"
          type: dataProperty
          owlProperty: hasHostname
```

### Mapper Definitions

Each mapper represents how to transform a CSV file or set of files into RDF triples. Key properties include:

| Property | Type | Description |
|----------|------|-------------|
| `name` | string | Unique name for the mapper |
| `file` | string | File path pattern (supports glob and environment variables: `${ANALYSIS_ROOT}`, etc.) |
| `owlClass` | string | OWL class to instantiate for each CSV record |
| `delimiter` | string | CSV delimiter character (default: comma) |
| `identifier` | object | Specifies which fields uniquely identify each instance |
| `staticProperties` | array | Properties with fixed values for all instances |
| `generics` | array | References to generic mapping templates |
| `fieldMappings` | array | Defines how CSV columns map to OWL properties |

**Example Mapper:**
```yaml
mappers:
  - name: HayabusaMapper
    file: "${ANALYSIS_ROOT}/QuickWins/*-haya.csv"
    owlClass: Detection
    delimiter: ';'
    identifier:
      fields:
        - "RecordID"
      separator: "_"
    staticProperties:
      - owlProperty: hasDescription
        value: "Hayabusa detection"
    generics:
      - addTimestamp
      - addComputer
    fieldMappings:
      - sourceField: "RuleTitle"
        type: dataProperty
        owlProperty: hasName
      - sourceField: "EventID"
        type: dataProperty
        owlProperty: hasEventID
        dataType: xsd:unsignedLong
```

### Field Mapping Properties

Field mappings define how individual CSV columns are transformed:

| Property | Type | Description |
|----------|------|-------------|
| `sourceField` | string | Name of the CSV column |
| `type` | string | Mapping type: `dataProperty` (literal value) or `linkedIndividual` (reference to another class) |
| `owlProperty` | string | Target OWL property name |
| `dataType` | string | XSD data type (e.g., `xsd:string`, `xsd:dateTimeStamp`, `xsd:unsignedLong`) |
| `unique` | boolean | Whether this property should be unique (default: true) |
| `prefix` | string | Text prefix to add to the value |

---
## Environment Variables

### `ANALYSIS_ROOT`

The mapper YAML files use an environment variable `ANALYSIS_ROOT` to reference the root folder of the analysis data. This makes the configuration portable and eliminates hardcoded paths.

#### Setting the Environment Variable

##### Windows (Command Prompt)
```cmd
set ANALYSIS_ROOT=C:\path\to\your\analysis\folder
```

##### Windows (PowerShell)
```powershell
$env:ANALYSIS_ROOT="C:\path\to\your\analysis\folder"
```

##### Linux/macOS
```bash
export ANALYSIS_ROOT=/path/to/your/analysis/folder
```

#### Example

If your analysis folder structure is:
```
C:\Users\Robert_Louan\Downloads\DFIR_Automation_Results_2\wks02\

QuickWins\
├── Chainsaw_results/
│   ├── antivirus.csv
│   ├── sigma.csv
│   └── ...
├── wks02-haya.csv
...
```

Then set:
```
ANALYSIS_ROOT=C:\Users\Robert_Louan\Downloads\DFIR_Automation_Results_2\wks02\
```

### Custom environment variables

In addition to `ANALYSIS_ROOT`, you can define custom environment variables for specific paths or values used in your mappers. They behave the same way as `ANALYSIS_ROOT` and can be referenced in the YAML files.