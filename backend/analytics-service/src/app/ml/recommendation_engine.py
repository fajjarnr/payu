from uuid import uuid4
from datetime import datetime, timedelta
from structlog import get_logger
from typing import List, Dict, Any

logger = get_logger(__name__)


class RecommendationEngine:
    def __init__(self):
        self.budget_threshold_percentage = 0.8
        self.savings_goal_amount = 1000000

    def generate_recommendations(
        self,
        user_metrics,
        spending_trends
    ) -> List[Dict[str, Any]]:
        recommendations = []

        if user_metrics is None:
            return []

        recommendations.extend(self._check_spending_patterns(spending_trends))
        recommendations.extend(self._check_account_activity(user_metrics))
        recommendations.extend(self._check_budget_overruns(spending_trends))
        recommendations.extend(self._generate_savings_recommendations(user_metrics))

        recommendations.sort(key=lambda x: x.get('priority', 0), reverse=True)

        return recommendations[:5]

    def _check_spending_patterns(self, spending_trends) -> List[Dict]:
        recommendations = []
        
        mom_change = spending_trends.month_over_month_change
        
        if mom_change and mom_change > 20:
            recommendations.append({
                'recommendation_id': str(uuid4()),
                'recommendation_type': 'SPENDING_TREND',
                'title': 'Pengeluaran Anda meningkat',
                'description': f'Pengeluaran Anda naik {mom_change:.1f}% dibanding bulan lalu. Pertimbangkan untuk meninjau kembali pengeluaran Anda.',
                'priority': 2,
                'metadata': {'mom_change': mom_change}
            })

        for pattern in spending_trends.spending_by_category:
            if pattern.percentage > 30:
                recommendations.append({
                    'recommendation_id': str(uuid4()),
                    'recommendation_type': 'SPENDING_TREND',
                    'title': f'Pengeluaran {pattern.category} tinggi',
                    'description': f'Anda menghabiskan {pattern.percentage:.1f}% total pengeluaran untuk {pattern.category}.',
                    'priority': 3,
                    'metadata': {'category': pattern.category, 'percentage': pattern.percentage}
                })

        return recommendations

    def _check_account_activity(self, user_metrics) -> List[Dict]:
        recommendations = []

        if user_metrics.last_transaction_date:
            days_since_last = (datetime.utcnow() - user_metrics.last_transaction_date).days

            if days_since_last > 30:
                recommendations.append({
                    'recommendation_id': str(uuid4()),
                    'recommendation_type': 'NEW_FEATURE',
                    'title': 'Aktifkan kembali akun Anda',
                    'description': f'Anda belum bertransaksi selama {days_since_last} hari. Cek fitur-fitur baru kami!',
                    'priority': 1,
                    'metadata': {'days_inactive': days_since_last}
                })

        if user_metrics.kyc_status != 'VERIFIED':
            recommendations.append({
                'recommendation_id': str(uuid4()),
                'recommendation_type': 'NEW_FEATURE',
                'title': 'Selesaikan eKYC Anda',
                'description': 'Lengkapi verifikasi eKYC untuk membuka fitur lengkap PayU.',
                'priority': 5,
                'metadata': {'kyc_status': user_metrics.kyc_status}
            })

        return recommendations

    def _check_budget_overruns(self, spending_trends) -> List[Dict]:
        recommendations = []

        for pattern in spending_trends.spending_by_category:
            if pattern.trend == 'increasing' and pattern.percentage > 25:
                recommendations.append({
                    'recommendation_id': str(uuid4()),
                    'recommendation_type': 'BUDGET_ALERT',
                    'title': f'Perhatian: {pattern.category}',
                    'description': f'Pengeluaran {pattern.category} meningkat. Pertimbangkan untuk membuat anggaran.',
                    'priority': 4,
                    'metadata': {'category': pattern.category, 'trend': pattern.trend}
                })

        return recommendations

    def _generate_savings_recommendations(self, user_metrics) -> List[Dict]:
        recommendations = []

        if user_metrics.total_amount > self.savings_goal_amount * 2:
            recommendations.append({
                'recommendation_id': str(uuid4()),
                'recommendation_type': 'SAVINGS_GOAL',
                'title': 'Mulai investasi',
                'description': 'Anda memiliki saldo yang cukup untuk mulai berinvestasi. Cek produk investasi kami!',
                'priority': 4,
                'action_url': '/investments',
                'metadata': {'total_balance': float(user_metrics.total_amount)}
            })

        return recommendations
