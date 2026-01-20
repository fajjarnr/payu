import requests
from typing import Optional, Dict, Any

class PayUClient:
    def __init__(self, gateway_url: str = "http://localhost:8080"):
        self.gateway_url = gateway_url.rstrip('/')
        self.session = requests.Session()
        self.token = None

    def set_token(self, token: str):
        self.token = token
        self.session.headers.update({"Authorization": f"Bearer {token}"})

    def get(self, path: str, params: Optional[Dict] = None) -> requests.Response:
        return self.session.get(f"{self.gateway_url}{path}", params=params)

    def post(self, path: str, json: Optional[Dict] = None, data: Optional[Dict] = None, files: Optional[Dict] = None) -> requests.Response:
        return self.session.post(f"{self.gateway_url}{path}", json=json, data=data, files=files)

    def put(self, path: str, json: Optional[Dict] = None) -> requests.Response:
        return self.session.put(f"{self.gateway_url}{path}", json=json)
    
    def options(self, path: str) -> requests.Response: # Useful for health checks
        return self.session.options(f"{self.gateway_url}{path}")
