-- MVP-006: webhook delivery idempotency.
-- Outbox is at-least-once (OutboxPublisher doc: a crash after Kafka ack but before DB commit
-- produces a duplicate on retry). A re-consumed financial/QRIS event must not create a second
-- webhook_deliveries row nor re-send the webhook to the integrator.
-- Guard: WebhookDispatcherService.dispatch skips when existsByEventIdAndSubscription_Id.

BEGIN;

DELETE FROM webhook_deliveries a
 USING webhook_deliveries b
 WHERE a.id > b.id
   AND a.event_id = b.event_id
   AND a.subscription_id = b.subscription_id;

CREATE UNIQUE INDEX uq_webhook_delivery_event
    ON webhook_deliveries (event_id, subscription_id);

COMMIT;
