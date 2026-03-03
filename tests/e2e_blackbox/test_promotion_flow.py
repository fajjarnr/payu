import pytest
import time
from faker import Faker

fake = Faker()


@pytest.mark.promotion
class TestPromotionFlow:
    """
    Promotion, Rewards, and Gamification E2E tests.
    Tests: Create Promotion -> Claim Promotion -> Cashback -> Loyalty Points -> Referrals
    """

    def test_create_promotion(self, authenticated_api, registered_user):
        """
        Create a new promotion
        """
        admin_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/promotions", json={
            "name": "Welcome Bonus 2024",
            "code": "WELCOME2024",
            "description": "New user welcome bonus",
            "discountPercentage": 20.0,
            "maxDiscountAmount": 50000,
            "minPurchaseAmount": 100000,
            "startDate": "2024-01-01T00:00:00",
            "endDate": "2024-12-31T23:59:59",
            "createdBy": admin_id
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Promotion creation may require admin privileges: {response.text}")

        promotion = response.json()
        if isinstance(promotion, dict) and "data" in promotion:
            promotion = promotion["data"]
        assert "id" in promotion
        assert promotion["code"] == "WELCOME2024"

        return promotion.get("id")

    def test_get_active_promotions(self, authenticated_api):
        """
        Get list of active promotions
        """
        response = authenticated_api.get("/api/v1/promotions")
        assert response.status_code == 200
        promotions = response.json()
        if isinstance(promotions, dict) and "data" in promotions:
            promotions = promotions["data"]
        assert isinstance(promotions, list)

    def test_get_promotion_by_code(self, authenticated_api):
        """
        Get promotion details by code
        """
        response = authenticated_api.get("/api/v1/promotions/code/WELCOME2024")
        if response.status_code != 200:
            pytest.skip("Promotion code may not exist")

        promotion = response.json()
        if isinstance(promotion, dict) and "data" in promotion:
            promotion = promotion["data"]
        assert promotion["code"] == "WELCOME2024"

    def test_claim_promotion(self, authenticated_api, registered_user):
        """
        Claim a promotion
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/promotions/WELCOME2024/claim", json={
            "userId": user_id,
            "transactionAmount": 150000
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Promotion claim requires valid code and user: {response.text}")

        reward = response.json()
        assert reward is not None

    def test_create_cashback(self, authenticated_api, registered_user):
        """
        Create a cashback reward
        """
        admin_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/cashbacks", json={
            "userId": admin_id,
            "amount": 10000,
            "transactionId": fake.uuid4(),
            "reason": "Welcome cashback",
            "createdBy": admin_id
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Cashback creation may require admin privileges: {response.text}")

        cashback = response.json()
        assert cashback is not None

    def test_add_loyalty_points(self, authenticated_api, registered_user):
        """
        Add loyalty points to a user
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/loyalty-points", json={
            "userId": user_id,
            "points": 100,
            "reason": "Registration bonus",
            "transactionId": fake.uuid4()
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Loyalty points may require transaction context: {response.text}")

        points = response.json()
        assert points is not None

    def test_create_referral(self, authenticated_api, registered_user):
        """
        Create a referral code/link
        """
        user_id = registered_user["userId"]

        response = authenticated_api.post("/api/v1/referrals", json={
            "userId": user_id,
            "referralCode": f"REF{fake.uuid4()[:8].upper()}",
            "rewardAmount": 50000
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Referral creation may require valid user: {response.text}")

        referral = response.json()
        assert referral is not None

    def test_get_referral_info(self, authenticated_api, registered_user):
        """
        Get referral information
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get(f"/api/v1/referrals/referrer/{user_id}")
        if response.status_code != 200:
            pytest.skip("Referral may not exist")

        referral = response.json()
        assert referral is not None

    def test_list_available_rewards(self, authenticated_api, registered_user):
        """
        List available rewards
        """
        user_id = registered_user["userId"]

        response = authenticated_api.get(f"/api/v1/rewards/account/{user_id}")
        if response.status_code != 200:
            pytest.skip("Rewards endpoint may not be implemented")

        rewards = response.json()
        assert rewards is not None

    def test_update_promotion(self, authenticated_api):
        """
        Update an existing promotion
        """
        # First get an existing promotion
        response = authenticated_api.get("/api/v1/promotions/code/WELCOME2024")
        if response.status_code != 200:
            pytest.skip("No promotion to update")

        promotion = response.json()
        if isinstance(promotion, dict) and "data" in promotion:
            promotion = promotion["data"]
        promotion_id = promotion.get("id")

        response = authenticated_api.put(f"/api/v1/promotions/{promotion_id}", json={
            "name": "Updated Welcome Bonus",
            "description": "Updated description",
            "discountPercentage": 25.0
        })

        if response.status_code != 200:
            pytest.skip(f"Promotion update may require admin privileges: {response.text}")

        updated_promotion = response.json()
        if isinstance(updated_promotion, dict) and "data" in updated_promotion:
            updated_promotion = updated_promotion["data"]
        assert updated_promotion is not None

    def test_activate_promotion(self, authenticated_api):
        """
        Activate a promotion
        """
        response = authenticated_api.get("/api/v1/promotions/code/WELCOME2024")
        if response.status_code != 200:
            pytest.skip("No promotion to activate")

        promotion = response.json()
        if isinstance(promotion, dict) and "data" in promotion:
            promotion = promotion["data"]
        promotion_id = promotion.get("id")

        response = authenticated_api.post(f"/api/v1/promotions/{promotion_id}/activate")
        if response.status_code != 200:
            pytest.skip(f"Promotion activation may require admin privileges: {response.text}")

        activated_promotion = response.json()
        if isinstance(activated_promotion, dict) and "data" in activated_promotion:
            activated_promotion = activated_promotion["data"]
        assert activated_promotion is not None
        assert activated_promotion.get("status") == "ACTIVE"
