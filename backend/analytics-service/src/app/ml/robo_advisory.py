from typing import List, Dict, Any
from decimal import Decimal
from structlog import get_logger

from app.models.schemas import (
    RiskProfile,
    InvestmentTimeHorizon,
    AssetClass,
    PortfolioAllocation,
    RiskAssessmentQuestions,
    RiskAssessmentResult,
    RoboAdvisoryResponse
)

logger = get_logger(__name__)


class RoboAdvisoryEngine:
    def __init__(self):
        self.portfolio_templates = {
            RiskProfile.CONSERVATIVE: {
                AssetClass.CASH: 30.0,
                AssetClass.FIXED_INCOME: 40.0,
                AssetClass.MUTUAL_FUNDS: 20.0,
                AssetClass.DIGITAL_GOLD: 10.0,
                AssetClass.STOCKS: 0.0,
                AssetClass.BONDS: 0.0
            },
            RiskProfile.MODERATE: {
                AssetClass.CASH: 15.0,
                AssetClass.FIXED_INCOME: 25.0,
                AssetClass.MUTUAL_FUNDS: 35.0,
                AssetClass.DIGITAL_GOLD: 10.0,
                AssetClass.STOCKS: 10.0,
                AssetClass.BONDS: 5.0
            },
            RiskProfile.AGGRESSIVE: {
                AssetClass.CASH: 5.0,
                AssetClass.FIXED_INCOME: 10.0,
                AssetClass.MUTUAL_FUNDS: 35.0,
                AssetClass.DIGITAL_GOLD: 5.0,
                AssetClass.STOCKS: 40.0,
                AssetClass.BONDS: 5.0
            }
        }

        self.expected_returns = {
            AssetClass.CASH: (4.0, 5.0),
            AssetClass.FIXED_INCOME: (5.0, 7.0),
            AssetClass.MUTUAL_FUNDS: (8.0, 12.0),
            AssetClass.DIGITAL_GOLD: (6.0, 10.0),
            AssetClass.STOCKS: (10.0, 18.0),
            AssetClass.BONDS: (6.0, 9.0)
        }

        self.investment_products = {
            AssetClass.CASH: [
                {"name": "Tabungan Haji PayU", "type": "Savings", "min_amount": 100000, "risk": "LOW"},
                {"name": "PayU Digital Savings", "type": "Savings", "min_amount": 50000, "risk": "LOW"}
            ],
            AssetClass.FIXED_INCOME: [
                {"name": "SBN Retail (SBR)", "type": "Government Bonds", "min_amount": 1000000, "risk": "LOW"},
                {"name": "ORI", "type": "Government Bonds", "min_amount": 1000000, "risk": "LOW"},
                {"name": "Deposit Berjangka", "type": "Time Deposit", "min_amount": 1000000, "risk": "LOW"}
            ],
            AssetClass.MUTUAL_FUNDS: [
                {"name": "Reksadana Pendapatan Tetap", "type": "Fixed Income Fund", "min_amount": 10000, "risk": "MEDIUM"},
                {"name": "Reksadana Pasar Uang", "type": "Money Market Fund", "min_amount": 10000, "risk": "LOW"},
                {"name": "Reksadana Campuran", "type": "Mixed Fund", "min_amount": 10000, "risk": "MEDIUM"},
                {"name": "Reksadana Saham", "type": "Equity Fund", "min_amount": 10000, "risk": "HIGH"}
            ],
            AssetClass.DIGITAL_GOLD: [
                {"name": "PayU Digital Gold", "type": "Digital Gold", "min_amount": 10000, "risk": "MEDIUM"},
                {"name": "Emas Pegadaian Digital", "type": "Digital Gold", "min_amount": 1000, "risk": "MEDIUM"}
            ],
            AssetClass.STOCKS: [
                {"name": "Saham Blue Chip (LQ45)", "type": "Stocks", "min_amount": 5000000, "risk": "HIGH"},
                {"name": "ETF IDX30", "type": "ETF", "min_amount": 100000, "risk": "HIGH"}
            ],
            AssetClass.BONDS: [
                {"name": "Obligasi Korporasi", "type": "Corporate Bonds", "min_amount": 5000000, "risk": "MEDIUM"},
                {"name": "SBN Seri T", "type": "Government Bonds", "min_amount": 1000000, "risk": "LOW"}
            ]
        }

        self.descriptions = {
            AssetClass.CASH: "Tabungan dan deposito untuk kebutuhan likuiditas jangka pendek",
            AssetClass.FIXED_INCOME: "Instrumen pendapatan tetap seperti deposito dan obligasi dengan risiko rendah",
            AssetClass.MUTUAL_FUNDS: "Diversifikasi investasi melalui reksadana dikelola oleh manajer investasi profesional",
            AssetClass.DIGITAL_GOLD: "Investasi emas digital sebagai perlindungan nilai dan diversifikasi",
            AssetClass.STOCKS: "Investasi saham untuk pertumbuhan jangka panjang dengan risiko lebih tinggi",
            AssetClass.BONDS: "Obligasi pemerintah dan korporasi untuk pendapatan tetap berkala"
        }

    def assess_risk_profile(self, questions: RiskAssessmentQuestions) -> RiskAssessmentResult:
        risk_score = 0.0
        log = logger.bind(user_age=questions.age)

        log.info("Starting risk assessment")

        age_score = max(0, (100 - questions.age)) / 80 * 20
        risk_score += age_score

        experience_score = questions.investment_experience * 2
        risk_score += experience_score

        savings_ratio = questions.total_savings / (questions.monthly_income * 6) if questions.monthly_income > 0 else 0
        savings_score = min(savings_ratio, 1) * 15
        risk_score += savings_score

        if questions.risk_tolerance.lower() == "high":
            risk_score += 25
        elif questions.risk_tolerance.lower() == "medium":
            risk_score += 15
        else:
            risk_score += 5

        if questions.investment_goal.lower() == "retirement":
            risk_score += 10
        elif questions.investment_goal.lower() == "wealth_growth":
            risk_score += 15
        else:
            risk_score += 5

        if questions.time_horizon == InvestmentTimeHorizon.LONG_TERM:
            risk_score += 15
        elif questions.time_horizon == InvestmentTimeHorizon.MEDIUM_TERM:
            risk_score += 10
        else:
            risk_score += 0

        risk_score = min(risk_score, 100)

        if risk_score < 35:
            risk_profile = RiskProfile.CONSERVATIVE
            description = "Profil konservatif: Fokus pada perlindungan modal dengan risiko rendah. Cocok untuk investor yang ingin menjaga nilai investasi dengan fluktuasi minimal."
            suitable_assets = [AssetClass.CASH, AssetClass.FIXED_INCOME, AssetClass.MUTUAL_FUNDS]
        elif risk_score < 65:
            risk_profile = RiskProfile.MODERATE
            description = "Profil moderat: Keseimbangan antara pertumbuhan dan perlindungan modal. Cocok untuk investor yang bersedia menerima risiko sedang untuk potensi return yang lebih tinggi."
            suitable_assets = [AssetClass.CASH, AssetClass.FIXED_INCOME, AssetClass.MUTUAL_FUNDS, AssetClass.DIGITAL_GOLD, AssetClass.BONDS]
        else:
            risk_profile = RiskProfile.AGGRESSIVE
            description = "Profil agresif: Fokus pada pertumbuhan maksimal dengan risiko tinggi. Cocok untuk investor yang memiliki pengalaman dan horison investasi jangka panjang."
            suitable_assets = [AssetClass.FIXED_INCOME, AssetClass.MUTUAL_FUNDS, AssetClass.DIGITAL_GOLD, AssetClass.STOCKS, AssetClass.BONDS]

        log.info("Risk assessment completed", risk_score=risk_score, risk_profile=risk_profile)

        return RiskAssessmentResult(
            risk_profile=risk_profile,
            risk_score=round(risk_score, 2),
            description=description,
            suitable_asset_classes=suitable_assets
        )

    def generate_portfolio_allocation(
        self,
        risk_profile: RiskProfile,
        time_horizon: InvestmentTimeHorizon
    ) -> List[PortfolioAllocation]:
        base_allocation = self.portfolio_templates[risk_profile].copy()

        if time_horizon == InvestmentTimeHorizon.SHORT_TERM:
            base_allocation[AssetClass.CASH] += 10
            base_allocation[AssetClass.STOCKS] = max(0, base_allocation[AssetClass.STOCKS] - 10)
        elif time_horizon == InvestmentTimeHorizon.LONG_TERM:
            base_allocation[AssetClass.STOCKS] += 10
            base_allocation[AssetClass.CASH] = max(0, base_allocation[AssetClass.CASH] - 10)

        portfolio = []
        total = sum(base_allocation.values())

        for asset_class, allocation in base_allocation.items():
            if allocation > 0:
                normalized_allocation = (allocation / total) * 100
                expected_return_min, expected_return_max = self.expected_returns[asset_class]

                portfolio.append(PortfolioAllocation(
                    asset_class=asset_class,
                    allocation_percentage=round(normalized_allocation, 2),
                    expected_return=round((expected_return_min + expected_return_max) / 2, 2),
                    risk_level=risk_profile,
                    description=self.descriptions[asset_class]
                ))

        portfolio.sort(key=lambda x: x.allocation_percentage, reverse=True)

        return portfolio

    def generate_recommendations(
        self,
        risk_profile: RiskProfile,
        portfolio_allocation: List[PortfolioAllocation],
        monthly_investment: float,
        questions: RiskAssessmentQuestions
    ) -> List[str]:
        recommendations = []

        if questions.total_savings < (questions.monthly_expenses * 3):
            recommendations.append(
                "Prioritaskan dana darurat setidaknya 3x pengeluaran bulanan sebelum berinvestasi."
            )

        recommendations.append(
            "Lakukan Dollar Cost Averaging (DCA) dengan berinvestasi secara rutin setiap bulan."
        )

        if risk_profile == RiskProfile.CONSERVATIVE:
            recommendations.append(
                "Pertimbangkan Reksadana Pendapatan Tetap untuk pendapatan yang stabil dan risiko rendah."
            )
        elif risk_profile == RiskProfile.MODERATE:
            recommendations.append(
                "Pertimbangkan Reksadana Campuran untuk diversifikasi antara saham dan pendapatan tetap."
            )
        else:
            recommendations.append(
                "Pertimbangkan Reksadana Saham atau ETF untuk pertumbuhan jangka panjang yang lebih tinggi."
            )

        if questions.time_horizon == InvestmentTimeHorizon.LONG_TERM:
            recommendations.append(
                "Untuk tujuan pensiun, pertimbangkan untuk meningkatkan alokasi saham secara bertahap."
            )

        if portfolio_allocation[0].asset_class == AssetClass.STOCKS:
            recommendations.append(
                "Perhatikan volatilitas pasar saham dan pastikan Anda memiliki horison investasi yang cukup panjang."
            )

        if AssetClass.DIGITAL_GOLD in [p.asset_class for p in portfolio_allocation]:
            recommendations.append(
                "Emas digital dapat menjadi perlindungan nilai terhadap inflasi dan ketidakpastian ekonomi."
            )

        recommendations.append(
            f"Investasikan minimal Rp {monthly_investment:,.0f} setiap bulan untuk mencapai tujuan investasi Anda."
        )

        return recommendations

    def get_recommended_products(
        self,
        portfolio_allocation: List[PortfolioAllocation]
    ) -> List[Dict[str, Any]]:
        recommended = []

        for allocation in portfolio_allocation:
            if allocation.asset_class in self.investment_products:
                for product in self.investment_products[allocation.asset_class]:
                    recommended.append({
                        "asset_class": allocation.asset_class.value,
                        "name": product["name"],
                        "type": product["type"],
                        "min_amount": product["min_amount"],
                        "risk": product["risk"],
                        "allocation_percentage": allocation.allocation_percentage
                    })

        return sorted(recommended, key=lambda x: x["allocation_percentage"], reverse=True)

    def generate_robo_advisory(
        self,
        user_id: str,
        questions: RiskAssessmentQuestions,
        monthly_investment_amount: float
    ) -> RoboAdvisoryResponse:
        log = logger.bind(user_id=user_id, monthly_investment=monthly_investment_amount)
        log.info("Generating robo-advisory recommendations")

        risk_assessment = self.assess_risk_profile(questions)

        portfolio_allocation = self.generate_portfolio_allocation(
            risk_assessment.risk_profile,
            questions.time_horizon
        )

        investment_recommendations = self.generate_recommendations(
            risk_assessment.risk_profile,
            portfolio_allocation,
            monthly_investment_amount,
            questions
        )

        recommended_products = self.get_recommended_products(portfolio_allocation)

        expected_annual_return = sum(
            p.allocation_percentage / 100 * p.expected_return
            for p in portfolio_allocation
        )

        log.info("Robo-advisory generated", risk_profile=risk_assessment.risk_profile, expected_return=expected_annual_return)

        return RoboAdvisoryResponse(
            user_id=user_id,
            risk_assessment=risk_assessment,
            portfolio_allocation=portfolio_allocation,
            investment_recommendations=investment_recommendations,
            monthly_investment_amount=monthly_investment_amount,
            expected_annual_return=round(expected_annual_return, 2),
            recommended_investment_products=recommended_products
        )