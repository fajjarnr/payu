package id.payu.transaction.application.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArchivalResult {
    private int archivedCount;
    private Long batchId;
    private String status;
}
