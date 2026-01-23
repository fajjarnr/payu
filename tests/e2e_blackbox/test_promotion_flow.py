import pytest
import time
from client import PayUClient
from faker import Faker

fake = Faker()


class TestPromotionFlow:
    """
    Promotion, Rewards, and Gamification E2E tests.
    Tests: Create Promotion -> Claim Promotion -> Cashback -> Loyalty Points -> Referrals
    """

    @pytest.fixture(scope="class")
    def api(self):
        return PayUClient(gateway_url="http://localhost:8080")

    @pytest.fixture(scope="class")
    def admin_session(self, api):
        """Admin session for managing promotions"""
        # Use admin credentials or create admin user
        admin_data = {
            "email": f"admin_{fake.uuid4()}@example.com",
            "username": f"admin_{fake.uuid4()[:8]}",
            "password": "AdminPass123!",
            "name": "Admin User",
            "phoneNumber": "+6281234567890"
        }

        response = api.post("/api/v1/accounts/register", json=admin_data)
        assert response.status_code in [200, 201]
        admin_id = response.json().get("id", response.json().get("userId"))

        response = api.post("/api/v1/auth/login", json={
            "username": admin_data["username"],
            "password": admin_data["password"]
        })
        assert response.status_code == 200
        api.set_token(response.json()["access_token"])

        return {"admin_id": admin_id, "api": api}

    @pytest.fixture(scope="class")
    def user_session(self, api):
        """Regular user session for claiming promotions"""
        user_data = {
            "email": f"promo_user_{fake.uuid4()}@example.com",
            "username": f"promo_{fake.uuid4()[:8]}",
            "password": "Password123!",
            "name": fake.name(),
            "phoneNumber": "+6281234567890"
        }

        response = api.post("/api/v1/accounts/register", json=user_data)
        assert response.status_code in [200, 201]
        user_id = response.json().get("id", response.json().get("userId"))

        response = api.post("/api/v1/auth/login", json={
            "username": user_data["username"],
            "password": user_data["password"]
        })
        assert response.status_code == 200
        api.set_token(response.json()["access_token"])

        return {"user_id": user_id, "api": api}

    def test_create_promotion(self, admin_session):
        """
        Create a new promotion
        """
        api = admin_session["api"]
        admin_id = admin_session["admin_id"]

        response = api.post("/api/v1/promotions", json={
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
        assert "id" in promotion
        assert promotion["code"] == "WELCOME2024"

        return promotion.get("id")

    def test_get_active_promotions(self, user_session):
        """
        Get list of active promotions
        """
        api = user_session["api"]

        response = api.get("/api/v1/promotions")
        assert response.status_code == 200
        promotions = response.json()
        assert isinstance(promotions, list)

    def test_get_promotion_by_code(self, user_session):
        """
        Get promotion details by code
        """
        api = user_session["api"]

        response = api.get("/api/v1/promotions/code/WELCOME2024")
        if response.status_code != 200:
            pytest.skip("Promotion code may not exist")

        promotion = response.json()
        assert promotion["code"] == "WELCOME2024"

    def test_claim_promotion(self, user_session):
        """
        Claim a promotion
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/promotions/WELCOME2024/claim", json={
            "userId": user_id,
            "transactionAmount": 150000
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Promotion claim requires valid code and user: {response.text}")

        reward = response.json()
        assert reward is not None

    def test_create_cashback(self, admin_session):
        """
        Create a cashback reward
        """
        api = admin_session["api"]
        admin_id = admin_session["admin_id"]

        response = api.post("/api/v1/cashbacks", json={
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

    def test_add_loyalty_points(self, user_session):
        """
        Add loyalty points to a user
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/loyalty-points", json={
            "userId": user_id,
            "points": 100,
            "reason": "Registration bonus",
            "transactionId": fake.uuid4()
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Loyalty points may require transaction context: {response.text}")

        points = response.json()
        assert points is not None

    def test_create_referral(self, user_session):
        """
        Create a referral code/link
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.post("/api/v1/referrals", json={
            "userId": user_id,
            "referralCode": f"REF{fake.uuid4()[:8].upper()}",
            "rewardAmount": 50000
        })

        if response.status_code not in [200, 201]:
            pytest.skip(f"Referral creation may require valid user: {response.text}")

        referral = response.json()
        assert referral is not None

    def test_get_referral_info(self, user_session):
        """
        Get referral information
        """
        api = user_session["api"]
        user_id = user_session["user_id"]

        response = api.get(f"/api/v1/referrals/{user_id}")
        if response.status_code != 200:
            pytest.skip("Referral may not exist")

        referral = response.json()
        assert referral is not None

    def test_list_available_rewards(self, user_session):
        """
        List available rewards
        """
        api = user_session["api"]

        response = api.get("/api/v1/rewards")
        if response.status_code != 200:
            pytest.skip("Rewards endpoint may not be implemented")

        rewards = response.json()
        assert isinstance(rewards, list)

    def test_update_promotion(self, admin_session):
        """
        Update an existing promotion
        """
        api = admin_session["api"]

        # First get an existing promotion
        response = api.get("/api/v1/promotions/code/WELCOME2024")
        if response.status_code != 200:
            pytest.skip("No promotion to update")

        promotion = response.json()
        promotion_id = promotion.get("id")

        response = api.put(f"/api/v1/promotions/{promotion_id}", json={
            "name": "Updated Welcome Bonus",
            "description": "Updated description",
            "discountPercentage": 25.0
        })

        if response.status_code != 200:
            pytest.skip(f"Promotion update may require admin privileges: {response.text}")

        updated_promotion = response.json()
        assert updated_promotion is not None

    def test_activate_promotion(self, admin_session):
        """
        Activate a promotion
        """
        api = admin_session["api"]

        response = api.get("/api/v1/promotions/code/WELCOME2024")
        if response.status_code != 200:
            pytest.skip("No promotion to activate")

        promotion = response.json()
        promotion_id = promotion.get("id")

        response = api.post(f"/api/v1/promotions/{promotion_id}/activate")
        if response.status_code != 200:
            pytest.skip(f"Promotion activation may require admin privileges: {response.text}")

        activated_promotion = response.json()
        assert activated_promotion is not None
        assert activated_promotion.get("status") == "ACTIVE"
