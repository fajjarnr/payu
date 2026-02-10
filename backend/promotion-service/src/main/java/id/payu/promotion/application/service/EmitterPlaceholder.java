package id.payu.promotion.application.service;

/**
 * Placeholder for Kafka/event emitter.
 * Used as a mock target in tests.
 */
public interface EmitterPlaceholder<T> {
    void send(T event);
}
