def test_artemis_connection_uses_heartbeats(monkeypatch):
    import app.messaging.artemis_consumer as consumer

    captured = {}

    class FakeConnection:
        def __init__(self, host_and_ports, **kwargs):
            captured["host_and_ports"] = host_and_ports
            captured["heartbeats"] = kwargs.get("heartbeats")

        def set_listener(self, *_args):
            pass

        def connect(self, *_args, **_kwargs):
            pass

        def subscribe(self, **_kwargs):
            pass

    monkeypatch.setattr(consumer.asyncio, "get_running_loop", lambda: object())
    monkeypatch.setattr(consumer.stomp, "Connection", FakeConnection)

    consumer.ArtemisConsumerService().start()

    assert captured["heartbeats"] == (30000, 30000)
