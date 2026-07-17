package id.payu.promotion.domain.port.in;
import id.payu.promotion.domain.model.*;
public interface CashbackSagaUseCase { CashbackSagaOutcome execute(CashbackCommand command); }
