import pytest


@pytest.mark.asyncio
async def test_init_db_creates_tables_before_hypertables(monkeypatch):
    import app.database as database

    calls = []

    class FakeConnection:
        async def __aenter__(self):
            return self

        async def __aexit__(self, exc_type, exc, tb):
            return False

        async def execute(self, statement):
            calls.append(str(statement))

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
    assert calls[1] == "create_all"
    assert "pg_advisory_unlock" in calls[2]
    assert calls[3] == "hypertables"
