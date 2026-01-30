# Common C4 Model Mistakes to Avoid

## 1. Wrong Level of Detail
- **Mistake**: Showing classes or internal functions in a Container diagram.
- **Fix**: Move them to a Component diagram (Level 3) or omit if not critical.

## 2. Generic "Cloud" Boxes
- **Mistake**: Using a single box for "AWS" or "Kafka".
- **Fix**: Use `Deployment_Node` for AWS regions/AZs and `ContainerQueue` for specific Kafka topics.

## 3. Directionless Lines
- **Mistake**: Using lines without arrows or labels.
- **Fix**: Always use `Rel` with a direction and a verb-based label (e.g., "Authenticates with").

## 4. Mixing Environments
- **Mistake**: Showing production infra and staging DBs in the same diagram.
- **Fix**: Use separate `C4Deployment` diagrams for each environment.

---
*Reference: C4 Design Principles*
