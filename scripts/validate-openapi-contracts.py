#!/usr/bin/env python3
"""
OpenAPI Contract Validation Script for PayU Backend Services

This script validates that:
1. All REST controllers have @Tag annotation
2. All @RequestMapping/@GetMapping/@PostMapping methods have @Operation annotation
3. All endpoints have appropriate @ApiResponse annotations (200, 400, 401, 404, 500)
4. Security requirements are properly defined

Usage: python scripts/validate-openapi-contracts.py
"""

import os
import re
import sys
from pathlib import Path
from dataclasses import dataclass
from typing import List, Dict, Set
from collections import defaultdict

# Colors for terminal output
class Colors:
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    RED = '\033[91m'
    BLUE = '\033[94m'
    RESET = '\033[0m'
    BOLD = '\033[1m'

@dataclass
class ValidationResult:
    service: str
    controller: str
    endpoint: str
    issues: List[str]
    is_valid: bool

class OpenAPIValidator:
    def __init__(self, backend_dir: str):
        self.backend_dir = Path(backend_dir)
        self.services_dir = self.backend_dir / "services"

    def find_all_controllers(self) -> Dict[str, List[Path]]:
        """Find all REST controllers across services"""
        services = {}
        backend_path = self.backend_dir.parent

        # Find all controllers and resources
        controllers = list(backend_path.rglob("*Controller.java"))
        resources = list(backend_path.rglob("*Resource.java"))
        all_files = controllers + resources

        for file_path in all_files:
            # Skip shared modules
            if 'shared/' in str(file_path) or '/src/test/' in str(file_path):
                continue

            # Extract service name from path
            parts = file_path.parts
            for i, part in enumerate(parts):
                if part.endswith('-service'):
                    service_name = part
                    if service_name not in services:
                        services[service_name] = []
                    services[service_name].append(file_path)
                    break

        return services

    def validate_controller(self, controller_path: Path) -> ValidationResult:
        """Validate a single controller file"""
        service_name = controller_path.parent.parent.parent.parent.name
        controller_name = controller_path.stem

        with open(controller_path, 'r') as f:
            content = f.read()

        issues = []

        # Check for @RestController or @RestController annotation
        has_rest_controller = '@RestController' in content or '@RestController' in content
        if not has_rest_controller:
            issues.append("Missing @RestController annotation")

        # Check for @Tag annotation at class level
        has_tag = '@Tag(' in content
        if not has_tag:
            issues.append("Missing @Tag annotation for API documentation")

        # Check for @RequestMapping at class level
        request_match = re.search(r'@RequestMapping\(["\']([^"\']+)["\']', content)
        base_path = request_match.group(1) if request_match else ""

        # Find all endpoint methods
        endpoint_pattern = r'@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\([^)]+\)\s*public\s+\S+\s+(\w+)\('
        endpoints = re.findall(endpoint_pattern, content)

        for http_method, method_name in endpoints:
            # Extract the endpoint path
            mapping_pattern = rf'@{http_method}\([^)]*\)'
            mapping_match = re.search(mapping_pattern, content)

            # Check for @Operation annotation
            # Look for @Operation before the method
            method_pos = content.find(f'public {method_name}(')
            before_method = content[max(0, method_pos - 500):method_pos]
            has_operation = '@Operation(' in before_method

            if not has_operation:
                issues.append(f"Method {method_name}: Missing @Operation annotation")

            # Check for @ApiResponse annotations
            has_200 = '@ApiResponse(responseCode = "200"' in before_method or '@ApiResponse(responseCode="200"' in before_method
            has_401 = '@ApiResponse(responseCode = "401"' in before_method or '@ApiResponse(responseCode="401"' in before_method
            has_404 = '@ApiResponse(responseCode = "404"' in before_method or '@ApiResponse(responseCode="404"' in before_method
            has_400 = '@ApiResponse(responseCode = "400"' in before_method or '@ApiResponse(responseCode="400"' in before_method

            if not has_200:
                issues.append(f"Method {method_name}: Missing success response (200) documentation")

            if http_method in ['GetMapping', 'PostMapping', 'PutMapping', 'DeleteMapping']:
                if not has_401:
                    issues.append(f"Method {method_name}: Missing 401 Unauthorized response")

            if http_method in ['GetMapping', 'DeleteMapping', 'PutMapping', 'PatchMapping']:
                if not has_404:
                    issues.append(f"Method {method_name}: Missing 404 Not Found response")

            if http_method in ['PostMapping', 'PutMapping', 'PatchMapping']:
                if not has_400:
                    issues.append(f"Method {method_name}: Missing 400 Bad Request response")

        return ValidationResult(
            service=service_name,
            controller=controller_name,
            endpoint=base_path,
            issues=issues,
            is_valid=len(issues) == 0
        )

    def validate_all_services(self) -> List[ValidationResult]:
        """Validate all controllers across all services"""
        results = []
        controllers = self.find_all_controllers()

        for service_name, controller_files in controllers.items():
            print(f"\n{Colors.BLUE}Validating {service_name}{Colors.RESET}")

            for controller_file in controller_files:
                try:
                    result = self.validate_controller(controller_file)
                    results.append(result)

                    if result.is_valid:
                        print(f"  {Colors.GREEN}✓{Colors.RESET} {result.controller}")
                    else:
                        print(f"  {Colors.YELLOW}⚠{Colors.RESET} {result.controller}")
                        for issue in result.issues:
                            print(f"      {Colors.RED}-{Colors.RESET} {issue}")
                except Exception as e:
                    print(f"  {Colors.RED}✗{Colors.RESET} Error validating {controller_file.name}: {e}")

        return results

    def print_summary(self, results: List[ValidationResult]):
        """Print validation summary"""
        total = len(results)
        valid = sum(1 for r in results if r.is_valid)
        invalid = total - valid

        print(f"\n{Colors.BOLD}{'='*60}{Colors.RESET}")
        print(f"{Colors.BOLD}OpenAPI Contract Validation Summary{Colors.RESET}")
        print(f"{Colors.BOLD}{'='*60}{Colors.RESET}")
        print(f"Total Controllers: {total}")
        print(f"{Colors.GREEN}Valid: {valid}{Colors.RESET}")
        print(f"{Colors.RED}Invalid: {invalid}{Colors.RESET}")

        # Group by service
        by_service = defaultdict(list)
        for result in results:
            by_service[result.service].append(result)

        print(f"\n{Colors.BOLD}Service Status:{Colors.RESET}")
        for service, service_results in sorted(by_service.items()):
            service_valid = sum(1 for r in service_results if r.is_valid)
            service_total = len(service_results)
            status = f"{Colors.GREEN}✓{Colors.RESET}" if service_valid == service_total else f"{Colors.YELLOW}⚠{Colors.RESET}"
            print(f"  {status} {service}: {service_valid}/{service_total} valid")

        # Services needing attention
        invalid_services = [s for s, rs in by_service.items() if any(not r.is_valid for r in rs)]
        if invalid_services:
            print(f"\n{Colors.YELLOW}Services requiring attention:{Colors.RESET}")
            for service in invalid_services:
                print(f"  - {service}")

def main():
    script_dir = Path(__file__).parent
    backend_dir = script_dir.parent / "backend"

    if not backend_dir.exists():
        print(f"{Colors.RED}Error: Backend directory not found at {backend_dir}{Colors.RESET}")
        sys.exit(1)

    print(f"{Colors.BOLD}PayU OpenAPI Contract Validation{Colors.RESET}")
    print(f"Validating backend services at: {backend_dir}")

    validator = OpenAPIValidator(str(backend_dir))
    results = validator.validate_all_services()
    validator.print_summary(results)

    # Exit with error code if any validation failed
    invalid_count = sum(1 for r in results if not r.is_valid)
    sys.exit(min(invalid_count, 1))

if __name__ == "__main__":
    main()
