# Ontology Viewer


## Overview

Ontology Viewer is a JavaFX desktop application for visualizing and exploring OWL/RDF knowledge graphs (Turtle `.ttl` files). It offers multiple views for timeline-based, tabular and graph exploration and supports SPARQL queries.

## Features

- Timeline and fast timeline visualizations for events
- Event Explorer table with filtering and sorting
- Graph view with interactive node navigation and multiple windows
- Built-in SPARQL Query view and full-text search

## Prerequisites

- Java 21 or later
- Maven 3.6+
- JavaFX 22.0.2+ (included as a dependency)

## Build

Build a fat JAR with Maven:

```bash
mvn clean package
```

## Run

Run via Maven (pass a Turtle file path):

```bash
mvn javafx:run -Dexec.args="/path/to/ontology.ttl"
```

Or run the packaged JAR:

```bash
java -jar target/ontology-viewer-1.0-SNAPSHOT.jar /path/to/ontology.ttl
```

## Usage

1. Launch the application using one of the commands above.
2. Provide a Turtle file on the command line or via the file chooser.
3. Choose a view (Timeline, Fast Timeline, Event Explorer, Query, Graph).
4. Double-click entities to open graph windows, use search and filters, or run SPARQL queries.

## Key dependencies

- JavaFX 22.0.2
- Apache Jena 4.10.0
- Jackson 2.16.1
- Log4j 2.22.1
- JUnit 5 (tests)

## Architecture

### Core components

- `App.java`: Main JavaFX application and view navigation
- `KGService.java`: Loads/parses Turtle files, queries the ontology, and manages individuals
- `JavaBridge.java`: Bridge between Java and embedded HTML/JavaScript views
- View classes: `Timeline`, `FastTimeline`, `EventExplorer`, `QueryView`, `GraphView`

## Building for production

Use the Maven Shade Plugin to create a fat JAR:

```bash
mvn clean package
```

Optionally package a platform installer using `jpackage` (Windows example available in the repo).

## Configuration

Pass JVM options when running the JAR, for example:

```bash
java -Xmx2G -jar target/ontology-viewer-1.0-SNAPSHOT.jar
```

See the source and launcher for further packaging examples.
