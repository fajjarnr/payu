# SQLAlchemy Mock Fixtures Guide

This guide explains the mock helper fixtures available in `tests/conftest.py` for testing SQLAlchemy queries in the analytics-service.

## Overview

The mock fixtures are designed to reduce boilerplate code and make tests more maintainable by providing reusable, type-safe helper functions for creating SQLAlchemy mock objects.

## Available Fixtures

### 1. `mock_scalar_result`

**Purpose**: Create mock results for queries that return a single value (scalar queries).

**Use Cases**:
- COUNT, SUM, AVG aggregate queries
- Queries using `.scalar()` or `.scalar_one_or_none()`
- Single entity lookups

**Example**:
```python
@pytest.mark.asyncio
async def test_get_total_balance(mock_scalar_result, mock_db_session):
    # Setup: Query returns a single Decimal value
    mock_db_session.execute.return_value = mock_scalar_result(Decimal("1000000.00"))

    # Execute
    result = await service.get_total_balance("user_123")

    # Verify
    assert result == Decimal("1000000.00")
```

**Type Signature**:
```python
def mock_scalar_result(value: Optional[T]) -> AsyncMock
```

### 2. `mock_scalars_result`

**Purpose**: Create mock results for queries that return multiple rows via `.scalars()`.

**Use Cases**:
- List queries returning entity objects
- Queries using `.scalars().all()`
- Batch retrievals

**Example**:
```python
@pytest.mark.asyncio
async def test_get_recent_transactions(mock_scalars_result, mock_db_session):
    # Setup: Query returns multiple transaction entities
    transactions = [txn1, txn2, txn3]
    mock_db_session.execute.return_value = mock_scalars_result(transactions)

    # Execute
    result = await service.get_recent_transactions("user_123", limit=10)

    # Verify
    assert len(result) == 3
```

**Type Signature**:
```python
def mock_scalars_result(rows: List[T]) -> AsyncMock
```

### 3. `mock_query_result`

**Purpose**: Create comprehensive mock results supporting both scalar and multi-row queries.

**Use Cases**:
- Complex queries with column selections
- GROUP BY queries returning custom rows
- Queries requiring iteration support

**Example**:
```python
@pytest.mark.asyncio
async def test_get_spending_by_category(mock_query_result, mock_db_session):
    from tests.conftest import create_mock_row

    # Setup: Create mock rows using helper
    mock_rows = [
        create_mock_row(category="FOOD", total_amount=Decimal("3000000")),
        create_mock_row(category="SHOPPING", total_amount=Decimal("5000000")),
    ]
    mock_db_session.execute.return_value = mock_query_result(mock_rows)

    # Execute
    result = await service.get_spending_by_category("user_123")

    # Verify
    assert len(result) == 2
```

**Type Signature**:
```python
def mock_query_result(rows: List[T]) -> AsyncMock
```

### 4. `mock_execute_sequence`

**Purpose**: Mock multiple `execute()` calls that return different results in sequence.

**Use Cases**:
- Methods executing multiple queries
- Transactions with multiple statements
- Complex business logic with sequential DB calls

**Example**:
```python
@pytest.mark.asyncio
async def test_cash_flow_analysis(
    mock_execute_sequence,
    mock_scalar_result,
    mock_db_session
):
    # Setup: First call returns income, second returns expenses
    income_result = mock_scalar_result(Decimal("10000000.00"))
    expenses_result = mock_scalar_result(Decimal("7000000.00"))
    mock_execute_sequence(mock_db_session, [income_result, expenses_result])

    # Execute: Method will call execute() twice
    result = await service.get_cash_flow_analysis("user_123")

    # Verify
    assert result.income == Decimal("10000000.00")
    assert result.expenses == Decimal("7000000.00")
```

**Type Signature**:
```python
def mock_execute_sequence(session: AsyncMock, results: List[Any]) -> None
```

### 5. `create_mock_row` (Helper Function)

**Purpose**: Create MagicMock objects with specified attributes for query result rows.

**Use Cases**:
- GROUP BY queries with custom columns
- Aggregate queries with calculated fields
- Any query returning non-entity rows

**Example**:
```python
from tests.conftest import create_mock_row

@pytest.mark.asyncio
async def test_category_breakdown(mock_query_result, mock_db_session):
    # Create mock rows with specific attributes
    mock_rows = [
        create_mock_row(
            category="FOOD",
            total_amount=Decimal("3000000"),
            transaction_count=30,
            avg_transaction=Decimal("100000")
        ),
        create_mock_row(
            category="TRANSPORT",
            total_amount=Decimal("1500000"),
            transaction_count=15,
            avg_transaction=Decimal("100000")
        ),
    ]
    mock_db_session.execute.return_value = mock_query_result(mock_rows)

    result = await service.get_category_breakdown("user_123")
```

## Type Safety

All fixtures use Python type hints to provide better IDE support:

```python
from typing import Any, List, Optional, TypeVar

T = TypeVar('T')
MockRow = TypeVar('MockRow', bound=Any)
```

This enables:
- Autocompletion in IDEs
- Type checking with mypy/pyright
- Better inline documentation

## Migration Guide

### Before (Manual Mock Setup)

```python
async def test_old_way(mock_db_session):
    # Complex manual setup
    mock_result = MagicMock()
    mock_result.scalar_one_or_none.return_value = sample_entity
    mock_result.scalars.return_value.all.return_value = []
    mock_result.__iter__ = lambda self: iter(mock_rows)
    mock_result.column_descriptions = [MagicMock(__getitem__=...)]
    mock_db_session.execute.return_value = mock_result
```

### After (Using Fixtures)

```python
async def test_new_way(mock_scalar_result, mock_db_session):
    # Simple, readable setup
    mock_db_session.execute.return_value = mock_scalar_result(sample_entity)
```

## Best Practices

### 1. Use Specific Fixtures
- **Single value**: Use `mock_scalar_result`
- **Multiple rows**: Use `mock_scalars_result`
- **Complex queries**: Use `mock_query_result`

### 2. Use Helper Functions
```python
from tests.conftest import create_mock_row

# Good: Clear and maintainable
rows = [create_mock_row(category="FOOD", amount=Decimal("100"))]

# Avoid: Verbose and error-prone
rows = [MagicMock(category="FOOD", amount=Decimal("100"))]
```

### 3. Sequence Mocking for Multiple Queries
```python
# Good: Explicit sequence
mock_execute_sequence(mock_db_session, [result1, result2])

# Avoid: Complex side_effect functions
mock_db_session.execute.side_effect = lambda q: result1 if ... else result2
```

### 4. Fixture Injection Order
```python
# Good: Dependencies come first
async def test_example(
    mock_db_session,      # Base dependency
    mock_scalar_result,   # Helper fixture
    analytics_service,    # Service using DB
):
    ...

# Note: Pytest handles fixture resolution automatically
```

## Common Patterns

### Pattern 1: Empty Results
```python
async def test_no_data_found(mock_scalar_result, mock_db_session):
    mock_db_session.execute.return_value = mock_scalar_result(None)
    result = await service.find_user("nonexistent")
    assert result is None
```

### Pattern 2: Multiple Entities
```python
async def test_list_with_pagination(mock_scalars_result, mock_db_session):
    entities = [entity1, entity2, entity3]
    mock_db_session.execute.return_value = mock_scalars_result(entities)
    result = await service.list_users(page=1, page_size=10)
    assert len(result) == 3
```

### Pattern 3: Aggregated Data
```python
async def test_aggregate_query(mock_query_result, mock_db_session):
    from tests.conftest import create_mock_row

    rows = [
        create_mock_row(category="FOOD", total=Decimal("1000"), count=10),
        create_mock_row(category="SHOPPING", total=Decimal("2000"), count=5),
    ]
    mock_db_session.execute.return_value = mock_query_result(rows)

    result = await service.get_category_totals("user_123")
    assert len(result) == 2
```

### Pattern 4: Sequential Queries
```python
async def test_complex_workflow(
    mock_execute_sequence,
    mock_scalar_result,
    mock_db_session
):
    # Setup multiple query results
    balance_result = mock_scalar_result(Decimal("5000"))
    transactions_result = mock_scalar_result(Decimal("3000"))
    mock_execute_sequence(mock_db_session, [balance_result, transactions_result])

    # Execute workflow
    result = await service.analyze_cash_flow("user_123")

    assert result.starting_balance == Decimal("5000")
    assert result.total_spent == Decimal("3000")
```

## Troubleshooting

### Issue: "AttributeError: Mock object has no attribute 'scalars'"

**Solution**: Use `mock_scalars_result` or `mock_query_result` instead of `mock_scalar_result`.

### Issue: "Unexpected execute call"

**Solution**: Ensure the sequence length matches the number of execute() calls:
```python
# If your test calls execute() 3 times, provide 3 results
mock_execute_sequence(mock_db_session, [result1, result2, result3])
```

### Issue: Iteration over mock fails

**Solution**: Use `mock_query_result` which properly implements `__iter__`:
```python
mock_db_session.execute.return_value = mock_query_result(rows)
# Now you can iterate: for row in result: ...
```

## Contributing

When adding new mock patterns:

1. Add the fixture to `tests/conftest.py`
2. Document it in this guide
3. Add type hints for IDE support
4. Include usage examples
5. Update this guide with the new pattern

## References

- [SQLAlchemy Async Documentation](https://docs.sqlalchemy.org/en/20/orm/extensions/asyncio.html)
- [Pytest Fixture Documentation](https://docs.pytest.org/en/stable/explanation/fixtures.html)
- [unittest.mock Documentation](https://docs.python.org/3/library/unittest.mock.html)
