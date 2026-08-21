package id.payu.support.interfaces.dto;

import java.time.Instant;
import java.util.UUID;

public record FaqResponse(UUID id, String question, String answer, String category, Instant createdAt) {}
