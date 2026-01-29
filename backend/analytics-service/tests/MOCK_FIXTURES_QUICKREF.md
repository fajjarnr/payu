# Mock Fixtures Quick Reference

Quick reference guide for SQLAlchemy mock fixtures in analytics-service tests.

## TL;DR

```python
# Single value (scalar query)
mock_db_session.execute.return_value = mock_scalar_result(value)

# Multiple rows (scalars query)
mock_db_session.execute.return_value = mock_scalars_result([entity1, entity2])

# Complex query with iteration
mock_db_session.execute.return_value = mock_query_result(mock_rows)

# Multiple sequential queries
mock_execute_sequence(mock_db_session, [result1, result2, result3])

# Create mock rows
row = create_mock_row(column1="value1", column2="value2")
```

## Fixture Injection

Just add the fixture to your test parameters:

```python
@pytest.mark.asyncio
async def test_something(
    mock_db_session,      # Base session mock
    mock_scalar_result,   # For single values
    analytics_service,    # Service under test
):
    # Your test code here
    pass
```

## Common Patterns

### Pattern 1: Single Entity Lookup

```python
async def test_find_user(mock_scalar_result, mock_db_session):
    user = UserEntity(id="123", name="John")
    mock_db_session.execute.return_value = mock_scalar_result(user)

    result = await service.find_user("123")
    assert result.id == "123"
```

### Pattern 2: List of Entities

```python
async def test_list_users(mock_scalars_result, mock_db_session):
    users = [user1, user2, user3]
    mock_db_session.execute.return_value = mock_scalars_result(users)

    result = await service.list_users()
    assert len(result) == 3
```

### Pattern 3: Custom Query Results

```python
async def test_aggregate_query(mock_query_result, mock_db_session):
    from tests.conftest import create_mock_row

    rows = [
        create_mock_row(category="FOOD", total=Decimal("1000")),
        create_mock_row(category="SHOPPING", total=Decimal("2000")),
    ]
    mock_db_session.execute.return_value = mock_query_result(rows)

    result = await service.get_category_totals()
    assert len(result) == 2
```

### Pattern 4: Empty Results

```python
async def test_not_found(mock_scalar_result, mock_db_session):
    mock_db_session.execute.return_value = mock_scalar_result(None)

    result = await service.find_user("nonexistent")
    assert result is None
```

### Pattern 5: Multiple Queries

```python
async def test_complex_workflow(
    mock_execute_sequence,
    mock_scalar_result,
    mock_db_session
):
    income = mock_scalar_result(Decimal("10000"))
    expenses = mock_scalar_result(Decimal("7000"))
    mock_execute_sequence(mock_db_session, [income, expenses])

    result = await service.analyze_cash_flow("user_123")
    assert result.income == Decimal("10000")
    assert result.expenses == Decimal("7000")
```

## Fixture Reference

| Fixture | Use Case | Return Type |
|---------|----------|-------------|
| `mock_scalar_result(value)` | COUNT, SUM, single entity | `MagicMock` |
| `mock_scalars_result(rows)` | List queries, multiple rows | `MagicMock` |
| `mock_query_result(rows)` | GROUP BY, custom columns | `MagicMock` |
| `mock_execute_sequence(session, results)` | Multiple DB calls | `None` |
| `create_mock_row(**kwargs)` | Create mock row objects | `MagicMock` |

## See Also

- Full documentation: `tests/MOCK_FIXTURES_GUIDE.md`
- Summary: `tests/REFACTORING_SUMMARY.md`
- Shared fixtures: `tests/conftest.py`
