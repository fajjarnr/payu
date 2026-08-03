import pytest


@pytest.mark.asyncio
async def test_init_db_creates_tables_before_hypertables(monkeypatch):
    import app.database as database

    calls = []

    class FakeResult:
        def scalar(self):
            return 0

    class FakeConnection:
        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb):
            return False

        async def execute(self, statement, *args, **kwargs):
            statement_text = str(statement)
            calls.append(statement_text)
            return FakeResult()

        async def run_sync(self, fn):
            calls.append(fn.__name__)

    class FakeEngine:
        def begin(self):
            return FakeConnection()

    monkeypatch.setattr(database, "create_async_engine", lambda *args, **kwargs: FakeEngine())
    monkeypatch.setattr(database, "async_sessionmaker", lambda *args, **kwargs: object())

    async def fake_create_hypertables():
        calls.append("hypertables")

    monkeypatch.setattr(database, "_create_hypertables", fake_create_hypertables)

    await database.init_db()

    assert "pg_advisory_lock" in calls[0]
    assert "analytics_schema_version" in calls[1]
    assert "MAX(version)" in calls[2]
    assert calls[3] == "create_all"
    assert "NUMERIC(19,4)" in calls[4]
    assert len([call for call in calls if "analytics_processed_events" in call]) == 2
    assert "pg_advisory_unlock" in calls[-2]
    assert calls[-1] == "hypertables"


@pytest.mark.asyncio
async def test_init_db_skips_create_all_for_versioned_schema(monkeypatch):
    import app.database as database

    calls = []

    class FakeResult:
        def scalar(self):
            return database.CURRENT_SCHEMA_VERSION

    class FakeConnection:
        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb):
            return False

        async def execute(self, statement, *args, **kwargs):
            calls.append(str(statement))
            return FakeResult()

        async def run_sync(self, fn):
            calls.append(fn.__name__)

    class FakeEngine:
        def begin(self):
            return FakeConnection()

    monkeypatch.setattr(database, "create_async_engine", lambda *args, **kwargs: FakeEngine())
    monkeypatch.setattr(database, "async_sessionmaker", lambda *args, **kwargs: object())
    async def fake_create_hypertables():
        calls.append("hypertables")

    monkeypatch.setattr(database, "_create_hypertables", fake_create_hypertables)

    await database.init_db()

    assert "create_all" not in calls
