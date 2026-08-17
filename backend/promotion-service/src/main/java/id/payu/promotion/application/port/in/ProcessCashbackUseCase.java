package id.payu.promotion.application.port.in;
import id.payu.promotion.domain.model.CashbackResult; import id.payu.promotion.interfaces.dto.TransactionCompletedEvent;
public interface ProcessCashbackUseCase { CashbackResult process(TransactionCompletedEvent event); }
