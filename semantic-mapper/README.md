# Semantic Mapper - Ingestion Pipeline


## Overview

A Java-based ingestion pipeline that maps heterogeneous input data to RDF according to an ontology, YAML mappers and SHACL shapes. The pipeline is flexible and extensible, and supports optional SHACL validation.

## Features

- Transform input records to RDF using YAML mapper definitions
- Validate generated RDF against SHACL shapes
- Extensible ingestion stages and pluggable mappers

## Prerequisites

- Java Development Kit (JDK) 21 (LTS)
- Maven (tested with 3.9.12)

## Build

Clean and package the project:

```bash
mvn clean package
```

Build without running tests:

```bash
mvn clean package -DskipTests
```


## Run

Run the ingestion pipeline via Maven:

```bash
mvn package exec:java
```

Skip SHACL validation (pass-through args):

```bash
# Linux
mvn package exec:java -Dexec.args="--skip-shacl-validation"

# Windows (PowerShell)
mvn package exec:java "-Dexec.args=--skip-shacl-validation"
```

Or run the packaged JAR directly:

```bash
java -cp target/semantic-mapper-1.0-SNAPSHOT.jar be.ccb_uliege.incd.semantic_mapper.App
```

## Configuration

Mapper configuration is provided as YAML files in the `ingestion-config` module; see [ingestion-config README](../ingestion-config/README.md) for details.

### Configuration: CLI flags, system properties, environment variables

You can configure paths and base IRI via CLI flags, system properties, or environment variables. Precedence is CLI > system property > environment variable > default.

Supported options:

- CLI flags:
	- `--config-dir=PATH` — mapper config directory (default `../ingestion-config`)
	- `--shapes=PATH` — SHACL shapes file path (default `../ontology/shapes.ttl`)
	- `--base-iri=IRI` — base IRI for created resources (default from ontology loader)
	- `--skip-shacl-validation` — skip SHACL validation stage

- System properties:
	- `-Dconfig.dir=PATH`
	- `-Dshapes.path=PATH`
	- `-Dbase.iri=IRI`

- Environment variables:

Configure runtime paths using environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| CONFIG_DIR | Path to mapper configuration directory | ../ingestion-config |
| SHACL_SHAPES_PATH | Path to SHACL shapes file for validation | ../ontology/shapes.ttl |
| BASE_IRI | Base IRI for created resources | (safe default) |
| ONTOLOGY_PATH | Optional path to an ontology file | (none) |

#### Example (Windows PowerShell):

```powershell
$env:CONFIG_DIR="..\ingestion-config"
$env:SHACL_SHAPES_PATH="..\ontology\shapes.ttl"
$env:BASE_IRI="http://example.org/ufr#"
mvn package exec:java -Dexec.args="--skip-shacl-validation"
```

## Project structure

```
semantic-mapper/
├── src/
│   ├── main/java/be/ccb_uliege/incd/semantic_mapper/
│   │   ├── App.java
│   │   ├── ingest/
│   │   ├── owl/
│   │   └── validation/
│   └── test/java/
├── pom.xml
└── README.md
```

## Key modules

- `ingest`: data loading and ingestion pipeline
- `owl`: OWL/ontology utilities
- `validation`: SHACL validation logic

For more details on mapper files see [ingestion-config README](../ingestion-config/README.md).
