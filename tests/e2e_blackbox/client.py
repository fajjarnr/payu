import requests
from typing import Optional, Dict, Any
import json

class PayUClient:
    def __init__(self, gateway_url: str = "http://localhost:8080"):
        self.gateway_url = gateway_url.rstrip('/')
        self.session = requests.Session()
        self.token = None
        self.default_timeout = 30

    def set_token(self, token: str):
        self.token = token
        self.session.headers.update({"Authorization": f"Bearer {token}"})

    def clear_token(self):
        self.token = None
        self.session.headers.pop("Authorization", None)

    def get(self, path: str, params: Optional[Dict] = None, timeout: Optional[int] = None) -> requests.Response:
        url = f"{self.gateway_url}{path}"
        timeout = timeout or self.default_timeout
        return self.session.get(url, params=params, timeout=timeout)

    def post(self, path: str, json: Optional[Dict] = None, data: Optional[Dict] = None, 
             files: Optional[Dict] = None, params: Optional[Dict] = None, 
             timeout: Optional[int] = None) -> requests.Response:
        url = f"{self.gateway_url}{path}"
        timeout = timeout or self.default_timeout
        return self.session.post(url, json=json, data=data, files=files, params=params, timeout=timeout)

    def put(self, path: str, json: Optional[Dict] = None, params: Optional[Dict] = None,
            timeout: Optional[int] = None) -> requests.Response:
        url = f"{self.gateway_url}{path}"
        timeout = timeout or self.default_timeout
        return self.session.put(url, json=json, params=params, timeout=timeout)

    def delete(self, path: str, params: Optional[Dict] = None, 
               timeout: Optional[int] = None) -> requests.Response:
        url = f"{self.gateway_url}{path}"
        timeout = timeout or self.default_timeout
        return self.session.delete(url, params=params, timeout=timeout)

    def patch(self, path: str, json: Optional[Dict] = None, params: Optional[Dict] = None,
              timeout: Optional[int] = None) -> requests.Response:
        url = f"{self.gateway_url}{path}"
        timeout = timeout or self.default_timeout
        return self.session.patch(url, json=json, params=params, timeout=timeout)

    def options(self, path: str, timeout: Optional[int] = None) -> requests.Response:
        url = f"{self.gateway_url}{path}"
        timeout = timeout or self.default_timeout
        return self.session.options(url, timeout=timeout)
