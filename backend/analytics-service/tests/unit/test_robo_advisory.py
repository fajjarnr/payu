import pytest
from unittest.mock import MagicMock

from app.models.schemas import (
    RiskProfile,
    InvestmentTimeHorizon,
    AssetClass,
    RiskAssessmentQuestions,
    PortfolioAllocation,
    RiskAssessmentResult,
    RoboAdvisoryResponse
)


@pytest.mark.unit
class TestRoboAdvisoryEngine:
    """Unit tests for Robo-Advisory engine"""

    @pytest.fixture
    def robo_advisory_engine(self):
        from app.ml.robo_advisory import RoboAdvisoryEngine
        return RoboAdvisoryEngine()

    def test_assess_risk_profile_conservative(self, robo_advisory_engine):
        """Test risk assessment for conservative profile"""
        questions = RiskAssessmentQuestions(
            age=60,
            monthly_income=4000000.0,
            monthly_expenses=3500000.0,
            total_savings=5000000.0,
            investment_experience=0,
            risk_tolerance="low",
            investment_goal="emergency_fund",
            time_horizon=InvestmentTimeHorizon.SHORT_TERM
        )

        result = robo_advisory_engine.assess_risk_profile(questions)

        assert result.risk_profile == RiskProfile.CONSERVATIVE
        assert 0 <= result.risk_score < 35
        assert len(result.suitable_asset_classes) > 0
        assert AssetClass.CASH in result.suitable_asset_classes
        assert "konservatif" in result.description.lower()

    def test_assess_risk_profile_moderate(self, robo_advisory_engine):
        """Test risk assessment for moderate profile"""
        questions = RiskAssessmentQuestions(
            age=45,
            monthly_income=12000000.0,
            monthly_expenses=8000000.0,
            total_savings=30000000.0,
            investment_experience=2,
            risk_tolerance="medium",
            investment_goal="wealth_growth",
            time_horizon=InvestmentTimeHorizon.MEDIUM_TERM
        )

        result = robo_advisory_engine.assess_risk_profile(questions)

        assert result.risk_profile == RiskProfile.MODERATE
        assert 35 <= result.risk_score < 65
        assert len(result.suitable_asset_classes) > 0
        assert AssetClass.MUTUAL_FUNDS in result.suitable_asset_classes

    def test_assess_risk_profile_aggressive(self, robo_advisory_engine):
        """Test risk assessment for aggressive profile"""
        questions = RiskAssessmentQuestions(
            age=25,
            monthly_income=20000000.0,
            monthly_expenses=10000000.0,
            total_savings=200000000.0,
            investment_experience=8,
            risk_tolerance="high",
            investment_goal="wealth_growth",
            time_horizon=InvestmentTimeHorizon.LONG_TERM
        )

        result = robo_advisory_engine.assess_risk_profile(questions)

        assert result.risk_profile == RiskProfile.AGGRESSIVE
        assert result.risk_score >= 65
        assert len(result.suitable_asset_classes) > 0
        assert AssetClass.STOCKS in result.suitable_asset_classes

    def test_generate_portfolio_allocation_conservative(self, robo_advisory_engine):
        """Test portfolio allocation for conservative profile"""
        portfolio = robo_advisory_engine.generate_portfolio_allocation(
            RiskProfile.CONSERVATIVE,
            InvestmentTimeHorizon.MEDIUM_TERM
        )

        assert len(portfolio) > 0
        total_allocation = sum(p.allocation_percentage for p in portfolio)
        assert abs(total_allocation - 100.0) < 0.1

        cash_allocation = next((p for p in portfolio if p.asset_class == AssetClass.CASH), None)
        assert cash_allocation is not None
        assert cash_allocation.allocation_percentage > 20

        stocks_allocation = next((p for p in portfolio if p.asset_class == AssetClass.STOCKS), None)
        assert stocks_allocation is None or stocks_allocation.allocation_percentage == 0

    def test_generate_portfolio_allocation_moderate(self, robo_advisory_engine):
        """Test portfolio allocation for moderate profile"""
        portfolio = robo_advisory_engine.generate_portfolio_allocation(
            RiskProfile.MODERATE,
            InvestmentTimeHorizon.MEDIUM_TERM
        )

        assert len(portfolio) > 0
        total_allocation = sum(p.allocation_percentage for p in portfolio)
        assert abs(total_allocation - 100.0) < 0.1

        mutual_funds_allocation = next((p for p in portfolio if p.asset_class == AssetClass.MUTUAL_FUNDS), None)
        assert mutual_funds_allocation is not None
        assert mutual_funds_allocation.allocation_percentage > 0

    def test_generate_portfolio_allocation_aggressive(self, robo_advisory_engine):
        """Test portfolio allocation for aggressive profile"""
        portfolio = robo_advisory_engine.generate_portfolio_allocation(
            RiskProfile.AGGRESSIVE,
            InvestmentTimeHorizon.LONG_TERM
        )

        assert len(portfolio) > 0
        total_allocation = sum(p.allocation_percentage for p in portfolio)
        assert abs(total_allocation - 100.0) < 0.1

        stocks_allocation = next((p for p in portfolio if p.asset_class == AssetClass.STOCKS), None)
        assert stocks_allocation is not None
        assert stocks_allocation.allocation_percentage > 30

        cash_allocation = next((p for p in portfolio if p.asset_class == AssetClass.CASH), None)
        if cash_allocation is not None:
            assert cash_allocation.allocation_percentage < 10

    def test_generate_portfolio_allocation_short_term(self, robo_advisory_engine):
        """Test portfolio allocation for short term horizon"""
        portfolio = robo_advisory_engine.generate_portfolio_allocation(
            RiskProfile.MODERATE,
            InvestmentTimeHorizon.SHORT_TERM
        )

        cash_allocation = next((p for p in portfolio if p.asset_class == AssetClass.CASH), None)
        assert cash_allocation is not None

        assert all(p.allocation_percentage >= 0 for p in portfolio)

    def test_generate_portfolio_allocation_long_term(self, robo_advisory_engine):
        """Test portfolio allocation for long term horizon"""
        portfolio = robo_advisory_engine.generate_portfolio_allocation(
            RiskProfile.MODERATE,
            InvestmentTimeHorizon.LONG_TERM
        )

        stocks_allocation = next((p for p in portfolio if p.asset_class == AssetClass.STOCKS), None)
        assert stocks_allocation is not None

        assert all(p.allocation_percentage >= 0 for p in portfolio)

    def test_generate_recommendations_conservative(self, robo_advisory_engine):
        """Test recommendations for conservative profile"""
        portfolio = robo_advisory_engine.generate_portfolio_allocation(
            RiskProfile.CONSERVATIVE,
            InvestmentTimeHorizon.MEDIUM_TERM
        )

        questions = RiskAssessmentQuestions(
            age=50,
            monthly_income=10000000.0,
            monthly_expenses=6000000.0,
            total_savings=50000000.0,
            investment_experience=2,
            risk_tolerance="low",
            investment_goal="retirement",
            time_horizon=InvestmentTimeHorizon.MEDIUM_TERM
        )

        recommendations = robo_advisory_engine.generate_recommendations(
            RiskProfile.CONSERVATIVE,
            portfolio,
            1000000.0,
            questions
        )

        assert len(recommendations) > 0
        assert all(isinstance(r, str) for r in recommendations)

    def test_generate_recommendations_aggressive(self, robo_advisory_engine):
        """Test recommendations for aggressive profile"""
        portfolio = robo_advisory_engine.generate_portfolio_allocation(
            RiskProfile.AGGRESSIVE,
            InvestmentTimeHorizon.LONG_TERM
        )

        questions = RiskAssessmentQuestions(
            age=30,
            monthly_income=20000000.0,
            monthly_expenses=10000000.0,
            total_savings=200000000.0,
            investment_experience=7,
            risk_tolerance="high",
            investment_goal="wealth_growth",
            time_horizon=InvestmentTimeHorizon.LONG_TERM
        )

        recommendations = robo_advisory_engine.generate_recommendations(
            RiskProfile.AGGRESSIVE,
            portfolio,
            5000000.0,
            questions
        )

        assert len(recommendations) > 0
        assert any("saham" in r.lower() or "volatilitas" in r.lower() for r in recommendations)

    def test_generate_recommendations_insufficient_emergency_fund(self, robo_advisory_engine):
        """Test recommendations for insufficient emergency fund"""
        portfolio = robo_advisory_engine.generate_portfolio_allocation(
            RiskProfile.MODERATE,
            InvestmentTimeHorizon.MEDIUM_TERM
        )

        questions = RiskAssessmentQuestions(
            age=35,
            monthly_income=15000000.0,
            monthly_expenses=10000000.0,
            total_savings=10000000.0,
            investment_experience=3,
            risk_tolerance="medium",
            investment_goal="wealth_growth",
            time_horizon=InvestmentTimeHorizon.MEDIUM_TERM
        )

        recommendations = robo_advisory_engine.generate_recommendations(
            RiskProfile.MODERATE,
            portfolio,
            1000000.0,
            questions
        )

        assert len(recommendations) > 0
        assert any("dana darurat" in r.lower() for r in recommendations)

    def test_get_recommended_products(self, robo_advisory_engine):
        """Test recommended products retrieval"""
        portfolio = robo_advisory_engine.generate_portfolio_allocation(
            RiskProfile.MODERATE,
            InvestmentTimeHorizon.MEDIUM_TERM
        )

        products = robo_advisory_engine.get_recommended_products(portfolio)

        assert len(products) > 0
        assert all("name" in p for p in products)
        assert all("type" in p for p in products)
        assert all("min_amount" in p for p in products)
        assert all("asset_class" in p for p in products)

        assert any(p["asset_class"] == AssetClass.MUTUAL_FUNDS.value for p in products)

    def test_generate_robo_advisory_full_flow(self, robo_advisory_engine):
        """Test full robo-advisory generation flow"""
        questions = RiskAssessmentQuestions(
            age=35,
            monthly_income=15000000.0,
            monthly_expenses=10000000.0,
            total_savings=100000000.0,
            investment_experience=5,
            risk_tolerance="medium",
            investment_goal="wealth_growth",
            time_horizon=InvestmentTimeHorizon.MEDIUM_TERM
        )

        advisory = robo_advisory_engine.generate_robo_advisory(
            user_id="test-user-123",
            questions=questions,
            monthly_investment_amount=2000000.0
        )

        assert isinstance(advisory, RoboAdvisoryResponse)
        assert advisory.user_id == "test-user-123"
        assert advisory.monthly_investment_amount == 2000000.0
        assert advisory.risk_assessment is not None
        assert len(advisory.portfolio_allocation) > 0
        assert len(advisory.investment_recommendations) > 0
        assert len(advisory.recommended_investment_products) > 0
        assert advisory.expected_annual_return > 0

    def test_generate_robo_advisory_young_investor(self, robo_advisory_engine):
        """Test robo-advisory for young investor"""
        questions = RiskAssessmentQuestions(
            age=25,
            monthly_income=10000000.0,
            monthly_expenses=7000000.0,
            total_savings=50000000.0,
            investment_experience=2,
            risk_tolerance="high",
            investment_goal="wealth_growth",
            time_horizon=InvestmentTimeHorizon.LONG_TERM
        )

        advisory = robo_advisory_engine.generate_robo_advisory(
            user_id="young-investor-123",
            questions=questions,
            monthly_investment_amount=1000000.0
        )

        assert advisory.risk_assessment.risk_profile in [RiskProfile.AGGRESSIVE, RiskProfile.MODERATE]
        assert any(p.asset_class == AssetClass.STOCKS for p in advisory.portfolio_allocation)

    def test_generate_robo_advisory_retiree(self, robo_advisory_engine):
        """Test robo-advisory for retiree"""
        questions = RiskAssessmentQuestions(
            age=70,
            monthly_income=6000000.0,
            monthly_expenses=5000000.0,
            total_savings=20000000.0,
            investment_experience=0,
            risk_tolerance="low",
            investment_goal="retirement",
            time_horizon=InvestmentTimeHorizon.SHORT_TERM
        )

        advisory = robo_advisory_engine.generate_robo_advisory(
            user_id="retiree-123",
            questions=questions,
            monthly_investment_amount=500000.0
        )

        assert advisory.risk_assessment.risk_profile == RiskProfile.CONSERVATIVE
        stocks_allocation = next((p for p in advisory.portfolio_allocation if p.asset_class == AssetClass.STOCKS), None)
        assert stocks_allocation is None or stocks_allocation.allocation_percentage == 0

    def test_portfolio_allocation_percentage_validation(self, robo_advisory_engine):
        """Test that portfolio allocations sum to 100%"""
        for risk_profile in [RiskProfile.CONSERVATIVE, RiskProfile.MODERATE, RiskProfile.AGGRESSIVE]:
            for horizon in [InvestmentTimeHorizon.SHORT_TERM, InvestmentTimeHorizon.MEDIUM_TERM, InvestmentTimeHorizon.LONG_TERM]:
                portfolio = robo_advisory_engine.generate_portfolio_allocation(risk_profile, horizon)
                total = sum(p.allocation_percentage for p in portfolio)
                assert abs(total - 100.0) < 0.1, f"Portfolio for {risk_profile} {horizon} sums to {total}, expected 100"

    def test_expected_return_positive(self, robo_advisory_engine):
        """Test that all expected returns are positive"""
        for risk_profile in [RiskProfile.CONSERVATIVE, RiskProfile.MODERATE, RiskProfile.AGGRESSIVE]:
            portfolio = robo_advisory_engine.generate_portfolio_allocation(risk_profile, InvestmentTimeHorizon.MEDIUM_TERM)
            assert all(p.expected_return > 0 for p in portfolio), "All expected returns should be positive"

    def test_investment_products_contain_required_fields(self, robo_advisory_engine):
        """Test that investment products contain all required fields"""
        for asset_class in AssetClass:
            portfolio = [PortfolioAllocation(
                asset_class=asset_class,
                allocation_percentage=10.0,
                expected_return=5.0,
                risk_level=RiskProfile.MODERATE,
                description="Test"
            )]
            products = robo_advisory_engine.get_recommended_products(portfolio)
            for product in products:
                assert "name" in product
                assert "type" in product
                assert "min_amount" in product
                assert "risk" in product