# C4 Mermaid Diagram Syntax Reference

## 1. System Context Elements
```
Person(alias, "Label", "Description")
System(alias, "Label", "Description")
System_Ext(alias, "Label", "Description")
SystemDb(alias, "Label", "Description")
SystemQueue(alias, "Label", "Description")
```

## 2. Container Elements
```
Container(alias, "Label", "Technology", "Description")
ContainerDb(alias, "Label", "Technology", "Description")
ContainerQueue(alias, "Label", "Technology", "Description")
```

## 3. Relationships
```
Rel(from, to, "Label", "Technology")
BiRel(from, to, "Label", "Technology")
Rel_U(from, to, "Label") # Up
Rel_D(from, to, "Label") # Down
Rel_L(from, to, "Label") # Left
Rel_R(from, to, "Label") # Right
```

## 4. Boundaries
```
System_Boundary(alias, "Label") { ... }
Container_Boundary(alias, "Label") { ... }
Enterprise_Boundary(alias, "Label") { ... }
```

## 5. Deployment Elements
```
Deployment_Node(alias, "Label", "Type") { ... }
Node(alias, "Label", "Type") { ... }
```

---
*Reference: Mermaid C4 Documentation*
