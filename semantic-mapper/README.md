# Semantic Mapper - Ingestion Pipeline

A Java-based semantic data ingestion pipeline that processes and maps data according to an ontology, mappers definitions and SHACL validation rules.

## Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Building](#building)
- [Running](#running)
- [Configuration](#configuration)
- [Project Structure](#project-structure)
- [Troubleshooting](#troubleshooting)

## Overview

The Semantic Mapper provides the ingestion pipeline for processing the heterogeneous data and mapping it the ontology. It uses the mappers defined in the `ingestion-config` module to transform input data into RDF format, which is then validated against SHACL shapes defined in the `shapes.ttl` file. The pipeline is designed to be flexible and extensible, allowing for easy integration of new data sources and mapping rules.

## Building

### Clean and Package

To build the project and create an executable JAR:

```bash
mvn clean package
```

### Build Without Tests

If you want to skip running tests during the build:

```bash
mvn clean package -DskipTests
```

## Running

### Execute the Ingestion Pipeline

To run the ingestion pipeline:

```bash
mvn package exec:java
```

This command will:
1. Compile the source code
2. Package the application
3. Execute the main application entry point

### Command-Line Execution

Alternatively, run the packaged JAR directly:

```bash
java -cp target/semantic-mapper-1.0-SNAPSHOT.jar be.ccb_uliege.incd.semantic_mapper.App
```

## Configuration

### Mapper Configuration

The ingestion pipeline is configured through YAML mapper files. For detailed configuration instructions, see the [ingestion-config README](../ingestion-config/README.md).

### Environment Variables

Configure the pipeline behavior with the following environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `CONFIG_DIR` | Path to the directory containing mapper configuration files | `../ingestion-config` |
| `SHACL_SHAPES_PATH` | Path to the SHACL shapes file for validation | `../shapes.ttl` |

#### Example

```bash
# Windows
set CONFIG_DIR=..\ingestion-config
set SHACL_SHAPES_PATH=..\shapes.ttl
set ONTOLOGY_PATH=..\ontology.ttl
$env:ANALYSIS_ROOT="C:/path/to/your/analysis/folder" #if needed; use \\ for Windows paths
mvn package exec:java

# Linux/macOS
export CONFIG_DIR=../ingestion-config
export SHACL_SHAPES_PATH=../shapes.ttl
mvn package exec:java
```

## Project Structure

```
semantic-mapper/
├── src/
│   ├── main/java/be/ccb_uliege/incd/semantic_mapper/
│   │   ├── App.java              # Main application entry point
│   │   ├── ingest/               # Data ingestion logic
│   │   ├── owl/                  # OWL ontology handling
│   │   └── validation/           # SHACL validation
│   └── test/java/                # Unit tests
├── pom.xml                        # Maven configuration
└── README.md                      # This file
```

### Key Modules

- **ingest**: Handles data loading and ingestion from various sources
- **owl**: Manages OWL ontology operations
- **validation**: Performs SHACL shape validation on RDF data