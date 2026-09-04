"""Consumer must resolve the DB session factory live, not at import time.

Regression: ``from app.database import async_session_maker`` binds the
module-level ``None`` once; ``init_db()`` reassigns the name in
``app.database`` later, so the consumer called ``None()`` for every
message (``TypeError: 'NoneType' object is not callable``) while offsets
kept committing — silent total ingestion loss.
"""

import app.database as database_module
import app.messaging.kafka_consumer as consumer_module


def test_consumer_does_not_hold_stale_session_factory():
    assert not hasattr(consumer_module, "async_session_maker"), (
        "consumer binds async_session_maker at import time; "
        "it must look it up on app.database per call"
    )
    assert hasattr(database_module, "async_session_maker")
