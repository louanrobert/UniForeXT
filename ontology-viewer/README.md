# Ontology Viewer

A JavaFX desktop application for visualizing and exploring OWL ontology individuals from Turtle (.ttl) files.

## Overview

Ontology Viewer is an interactive visualization tool that allows you to browse, search, and analyze RDF/OWL knowledge graphs. It provides multiple views for different exploration patterns and supports rich visualization of ontology data including timelines, event relationships, and graph structures.

## Features

### Multiple Visualization Views

- **Timeline View**: Visualize events and entities on an interactive timeline, with support for time-based filtering and navigation
- **Fast Timeline View**: A high-performance alternative to the standard timeline for large datasets
- **Event Explorer**: Explore events and their relationships in a tabular format with filtering and sorting capabilities
- **Query View**: Execute SPARQL queries directly against the loaded ontology
- **Graph View**: Visualize the RDF graph structure with nodes and relationships (accessible via double-click on entities)

### Core Capabilities

- Load and parse Turtle (.ttl) ontology files
- Browse ontology individuals and their properties
- Color-coded visualization for different entity types
- Multiple parallel graph windows for detailed exploration
- Full-text search and filtering
- Command-line file path support for batch processing

## Prerequisites

- **Java 21** or later
- **Maven 3.6+**
- **JavaFX 22.0.2+** (included as dependency)

## Installation & Setup

### 1. Clone/Navigate to Project

```bash
cd ontology-viewer
```

### 2. Build the Project

Using Maven:

```bash
mvn clean package
```

This builds a fat JAR with all dependencies included.

## Running the Application

```bash
mvn javafx:run -Dexec.args="/path/to/ontology.ttl"
```

Or if using the packaged JAR:

```bash
java -jar target/ontology-viewer-1.0-SNAPSHOT.jar /path/to/ontology.ttl
```

## Usage

1. **Launch the application** using one of the methods above
2. **Select or provide a Turtle file**:
   - If no file is specified via command line, a file chooser dialog will open
   - Select your .ttl ontology file
3. **Choose a visualization view**:
   - Timeline View
   - Fast Timeline View
   - Event Explorer
   - Query View
4. **Explore your data**:
   - Double-click entities to open detailed Graph views in separate windows
   - Use search and filter options to narrow down results
   - Execute SPARQL queries for advanced exploration

## Key Dependencies

- **JavaFX 22.0.2**: Modern UI toolkit for desktop applications
- **Apache Jena 4.10.0**: RDF/OWL processing and SPARQL query execution
- **Jackson 2.16.1**: JSON processing
- **Log4j 2.22.1**: Logging framework
- **JUnit 5**: Testing framework

## Architecture

### Core Components

**App.java**: Main JavaFX application that manages the primary stage and view navigation.

**KGService.java**: Service layer that handles:
- Loading and parsing Turtle files using Jena
- Querying the ontology
- Managing ontology individuals and their relationships

**JavaBridge.java**: Bidirectional communication layer between Java backend and HTML/JavaScript views in the embedded web view.

**View Classes**: Each view provides a specific visualization interface:
- Time-based: Timeline, FastTimeline
- Tabular: EventExplorer
- Query: QueryView
- Graph: GraphView

## Building for Production

The project uses Maven Shade Plugin to create a fat JAR with all dependencies:

```bash
mvn clean package
```

The resulting JAR at `target/ontology-viewer-1.0-SNAPSHOT.jar` can be distributed as a standalone application. For end users, you may want to create platform-specific installers or bundles using tools like jpackage.

The following command packages the jar into a .exe for Windows:

```bash
jpackage `
   --type exe `
   --name "Ontology Viewer" `
   --input target `
   --main-jar ontology-viewer-1.0-SNAPSHOT.jar `
   --main-class be.ccb_uliege.incd.ontology_viewer.Launcher `
   --java-options "--enable-native-access=javafx.graphics,javafx.web,ALL-UNNAMED" `
   --java-options "--add-opens=java.base/sun.misc=ALL-UNNAMED" `
   --dest target/installer `
   --win-dir-chooser `
   --win-shortcut
```

Diverse jpackage options are available for customizing the installer, including icons, versioning, and more.

## Configuration

### JVM Options

Custom JVM options can be passed when running the JAR:

```bash
java -Xmx2G -jar ontology-viewer-1.0-SNAPSHOT.jar
```
