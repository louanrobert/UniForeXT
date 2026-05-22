# UniForeXT

This repository contains the code and resources of UniForeXT, a master's thesis project. The project focuses on semantic mapping of heterogeneous event/log data to a unified model. To do so, it defines an ontology (UniForeXT) and associated SHACL shapes, implements a Java-based semantic mapper to convert various input formats to triples conforming to the ontology annd provides a JavaFX-based viewer to explore the resulting knowledge graph.



---
## Repository layout

- `ontology/`: contains the ontology files used by the project
	- `ontology.rdf`: the main ontology (RDF/XML)
	- `shapes.ttl`, `astrea-shapes.ttl`: SHACL shapes and constraints used for validation
- `semantic-mapper/`: maven Java project that performs mapping from input formats to the ontology and produces RDF output
	- [README.md](semantic-mapper/README.md): module-specific instructions and details
- `ontology-viewer/`: maven Java/JavaFX project that provides an interactive viewer for knowledge graphs
	- [README.md](ontology-viewer/README.md): module-specific instructions and details
- `ingestion-config/`: example mappers/configurations used by the semantic-mapper for different input sources
	- [README.md](ingestion-config/README.md): details on the example configurations and how to use them


## Prerequisites:
- JDK 21+ (or the version declared in each module's `pom.xml`)
- Maven 3.6+

See each module's README for specific dependencies and setup instructions.

---
## Semantic mapper

The semantic mapper is a Java application that takes various input formats (CSV, JSON, XML, etc.) and maps them to RDF triples conforming to the UniForeXT ontology. It uses configuration files to define the mapping rules and can be extended to support new formats or mapping logic.

## Ontology viewer

The viewer includes pages for:
- Event explorer
- Graph view
- Query view
- Timelines and undated events

---
## Versioning
The project follows semantic versioning. Major changes that break backward compatibility (in terms of configuration or UI changes) will increment the major version, while minor features and bug fixes will increment the minor and patch versions respectively.

It follows this template for versioning:
```
vX.Y.Z
```
Where:
- `X` is the major version (incremented for breaking changes)
- `Y` is the minor version (incremented for new features)
- `Z` is the patch version (incremented for bug fixes)