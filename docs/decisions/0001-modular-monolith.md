# ADR 0001: Use a feature-based modular monolith

Status: accepted

## Context

Csports is a long-term learning project intended to exercise Redis, Kafka,
Elasticsearch, containers, testing, and other backend techniques. The current
domain and team size do not require independently deployed services.

## Decision

Keep one Spring Boot deployment and organize Java packages by business feature.
Keep cross-cutting code in `common`, security in `security`, and technology
adapters in `infrastructure`.

## Consequences

- Features are easier to locate and change.
- Transactions remain local and simple.
- Advanced infrastructure can still be practiced behind clear adapters.
- A feature can be extracted later if an explicit distributed-systems exercise
  justifies the additional operational complexity.
