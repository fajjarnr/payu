from decimal import Decimal

from sqlalchemy import Numeric

from app.database import (
    TransactionAnalyticsEntity,
    WalletBalanceEntity,
    UserMetricsEntity,
)
from app.models.schemas import TransactionCompletedEvent


def test_money_columns_and_events_preserve_decimal_precision():
    for column in (
        TransactionAnalyticsEntity.amount,
        WalletBalanceEntity.balance,
        WalletBalanceEntity.change_amount,
        UserMetricsEntity.total_amount,
        UserMetricsEntity.average_transaction,
    ):
        assert isinstance(column.type, Numeric)
        assert column.type.precision == 19
        assert column.type.scale == 4

    event = TransactionCompletedEvent(
        transaction_id="txn-1",
        amount="0.10",
        transaction_type="TRANSFER",
    )
    assert event.amount == Decimal("0.10")
