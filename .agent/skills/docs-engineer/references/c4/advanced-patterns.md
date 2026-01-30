# Advanced C4 Architecture Patterns

## 1. Microservices - Single Team Ownership
Model each microservice as a **container** within a system boundary:
```mermaid
C4Container
  System_Boundary(platform, "Digital Banking") {
    Container(orderApi, "Order Service", "Java", "Handles orders")
    ContainerDb(orderDb, "Order DB", "PostgreSQL", "Stores orders")
  }
```

## 2. Event-Driven Architecture (EDA)
Show individual topics as containers to visualize decoupled dependencies:
```mermaid
C4Container
  Container(producer, "Producer Service", "Go")
  ContainerQueue(topic, "events.v1", "Kafka", "Central event log")
  Container(consumer, "Consumer Service", "Node")
  
  Rel(producer, topic, "Publishes to")
  Rel(consumer, topic, "Subscribes to")
```

## 3. Dynamic Request Flow (Dynamic)
Use numbered relationships to show the sequence of events:
```mermaid
C4Dynamic
  Rel(user, api, "1. Request transfer")
  Rel(api, auth, "2. Validate token")
  Rel(api, db, "3. Check balance")
  Rel(api, kafka, "4. Publish event")
```

---
*Reference: Strategic Engineering Visualization*
