#!/usr/bin/env python3
"""
PayU OpenAPI Contract Validation Script
========================================
Compares implemented endpoints against OpenAPI documentation.

Usage:
    ./scripts/validate-openapi.py              # Full validation
    ./scripts/validate-openapi.py --service account-service  # Single service
    ./scripts/validate-openapi.py --summary    # Summary only
"""

import os
import re
import sys
import json
from pathlib import Path
from dataclasses import dataclass, field
from typing import List, Dict, Set, Optional
from collections import defaultdict

# Colors for terminal output
RED = '\033[0;31m'
GREEN = '\033[0;32m'
YELLOW = '\033[1;33m'
CYAN = '\033[0;36m'
MAGENTA = '\033[0;35m'
NC = '\033[0m'  # No Color


@dataclass
class Endpoint:
    """Represents a REST API endpoint."""
    service: str
    controller: str
    path: str
    method: str
    has_operation: bool = False
    operation_summary: str = ""
    operation_tags: List[str] = field(default_factory=list)
    line_number: int = 0


@dataclass
class ValidationResult:
    """Validation results for a service."""
    service: str
    total_endpoints: int = 0
    documented_endpoints: int = 0
    undocumented_endpoints: List[Endpoint] = field(default_factory=list)
    controllers: Dict[str, List[Endpoint]] = field(default_factory=dict)


class OpenAPIValidator:
    """Validates OpenAPI annotations against implemented endpoints."""

    def __init__(self, backend_dir: str = "backend"):
        self.backend_dir = Path(backend_dir)
        self.services = self._find_services()
        self.results: Dict[str, ValidationResult] = {}

    def _find_services(self) -> List[str]:
        """Find all backend services."""
        services = []
        for item in self.backend_dir.iterdir():
            if item.is_dir() and not item.name.startswith('.'):
                # Check if it has a src/main/java structure
                java_dir = item / "src" / "main" / "java"
                if java_dir.exists():
                    services.append(item.name)
        return sorted(services)

    def _get_controller_package(self, service: str) -> Optional[Path]:
        """Find the Java package directory for controllers."""
        # Normalize service name for package lookup (try both with and without -service suffix)
        package_names = [
            service.replace("-", ""),           # auth-service -> authservice
            service.replace("-service", ""),    # auth-service -> auth
            service,                            # gateway-service -> gateway-service
        ]

        base_dirs = [
            self.backend_dir / service / "src" / "main" / "java",
            self.backend_dir / service / "src" / "main" / "kotlin",
        ]

        for base_dir in base_dirs:
            if not base_dir.exists():
                continue

            # Common controller package patterns - try all package name variants
            for pkg_name in package_names:
                patterns = [
                    base_dir / "id" / "payu" / pkg_name / "adapter" / "web",
                    base_dir / "id" / "payu" / pkg_name / "controller",
                    base_dir / "id" / "payu" / pkg_name / "interfaces" / "rest",
                    base_dir / "id" / "payu" / pkg_name / "resource",
                    base_dir / "id" / "payu" / pkg_name / "api",
                ]

                for pattern in patterns:
                    if pattern.exists():
                        return pattern

                # Also check for rest subdirectory under adapter/web
                rest_pattern = base_dir / "id" / "payu" / pkg_name / "adapter" / "web" / "rest"
                if rest_pattern.exists():
                    return rest_pattern

        return None

    def _extract_endpoints_from_file(self, file_path: Path, service: str) -> List[Endpoint]:
        """Extract all REST endpoints from a controller file."""
        endpoints = []
        controller_name = file_path.stem

        try:
            content = file_path.read_text()
            lines = content.split('\n')

            # Extract base path from @RequestMapping on class
            base_path = ""
            class_match = re.search(r'@RequestMapping\s*\(\s*["\']([^"\']+)["\']', content)
            if class_match:
                base_path = class_match.group(1).strip()

            # Find all method-level mappings
            for i, line in enumerate(lines, 1):
                # Check for mapping annotations
                mapping_patterns = [
                    (r'@GetMapping\s*\(\s*["\']([^"\']+)["\']', 'GET'),
                    (r'@PostMapping\s*\(\s*["\']([^"\']+)["\']', 'POST'),
                    (r'@PutMapping\s*\(\s*["\']([^"\']+)["\']', 'PUT'),
                    (r'@DeleteMapping\s*\(\s*["\']([^"\']+)["\']', 'DELETE'),
                    (r'@PatchMapping\s*\(\s*["\']([^"\']+)["\']', 'PATCH'),
                    (r'@RequestMapping\s*\(\s*method\s*=\s*RequestMethod\.(\w+).*?["\']([^"\']+)["\']', 'CUSTOM'),
                ]

                for pattern, http_method in mapping_patterns:
                    match = re.search(pattern, line)
                    if match:
                        if http_method == 'CUSTOM':
                            http_method = match.group(1)
                            path = match.group(2)
                        else:
                            path = match.group(1)

                        # Combine base path with method path
                        full_path = base_path + path
                        full_path = full_path.replace("//", "/")

                        endpoint = Endpoint(
                            service=service,
                            controller=controller_name,
                            path=full_path,
                            method=http_method,
                            line_number=i
                        )

                        # Look ahead for @Operation annotation
                        has_operation, summary, tags = self._check_operation_annotation(lines, i)
                        endpoint.has_operation = has_operation
                        endpoint.operation_summary = summary
                        endpoint.operation_tags = tags

                        endpoints.append(endpoint)
                        break

        except Exception as e:
            print(f"{YELLOW}Warning: Could not parse {file_path}: {e}{NC}")

        return endpoints

    def _check_operation_annotation(self, lines: List[str], start_idx: int) -> tuple[bool, str, List[str]]:
        """Check for @Operation annotation before the mapping."""
        # Look back up to 10 lines for @Operation
        for i in range(max(0, start_idx - 15), start_idx):
            line = lines[i - 1]  # Adjust for 0-based indexing

            # Check for @Operation
            if '@Operation' in line:
                # Extract summary
                summary_match = re.search(r'summary\s*=\s*["\']([^"\']+)["\']', line)
                summary = summary_match.group(1) if summary_match else ""

                # Extract tags
                tags = []
                tags_match = re.search(r'tags\s*=\s*{([^}]+)}', line)
                if tags_match:
                    tags_str = tags_match.group(1)
                    tags = [t.strip().strip('"\'') for t in tags_str.split(',')]

                return True, summary, tags

            # Stop if we hit another method annotation
            if '@' in line and 'Mapping' in line:
                break

        return False, "", []

    def validate_service(self, service: str) -> ValidationResult:
        """Validate a single service's OpenAPI documentation."""
        result = ValidationResult(service=service)
        controller_dir = self._get_controller_package(service)

        if not controller_dir:
            return result

        # Find all controller files
        controller_files = list(controller_dir.glob("*Controller.java"))
        if not controller_files:
            # Try parent directory
            controller_files = list(controller_dir.parent.glob("*Controller.java"))

        for controller_file in controller_files:
            # Skip BaseController
            if controller_file.stem == "BaseController":
                continue

            endpoints = self._extract_endpoints_from_file(controller_file, service)

            for endpoint in endpoints:
                result.total_endpoints += 1

                if endpoint.has_operation:
                    result.documented_endpoints += 1
                else:
                    result.undocumented_endpoints.append(endpoint)

                # Track by controller
                if endpoint.controller not in result.controllers:
                    result.controllers[endpoint.controller] = []
                result.controllers[endpoint.controller].append(endpoint)

        return result

    def validate_all(self) -> Dict[str, ValidationResult]:
        """Validate all services."""
        print(f"{CYAN}Validating OpenAPI contracts across all services...{NC}\n")

        for service in self.services:
            result = self.validate_service(service)
            self.results[service] = result

        return self.results

    def print_results(self, summary_only: bool = False):
        """Print validation results."""
        total_endpoints = 0
        total_documented = 0
        total_undocumented = 0
        services_with_issues = []

        for service, result in self.results.items():
            if result.total_endpoints == 0:
                continue

            total_endpoints += result.total_endpoints
            total_documented += result.documented_endpoints
            undocumented_count = len(result.undocumented_endpoints)
            total_undocumented += undocumented_count

            coverage = (result.documented_endpoints / result.total_endpoints * 100) if result.total_endpoints > 0 else 100

            if undocumented_count > 0:
                services_with_issues.append(service)

                if not summary_only:
                    print(f"{RED}✗ {service}:{NC} {coverage:.0f}% documented")
                    print(f"   {result.documented_endpoints}/{result.total_endpoints} endpoints, {undocumented_count} missing documentation\n")

                    for endpoint in result.undocumented_endpoints[:5]:  # Show first 5
                        print(f"     {YELLOW}{endpoint.method}{NC} {endpoint.path} ({endpoint.controller}:{endpoint.line_number})")

                    if len(result.undocumented_endpoints) > 5:
                        print(f"     ... and {len(result.undocumented_endpoints) - 5} more")
                    print()
            else:
                if not summary_only:
                    print(f"{GREEN}✓ {service}:{NC} 100% documented ({result.total_endpoints} endpoints)")

        # Print summary
        print(f"\n{CYAN}{'=' * 60}{NC}")
        print(f"{CYAN}SUMMARY{NC}")
        print(f"{CYAN}{'=' * 60}{NC}")
        print(f"Total Services:       {len([r for r in self.results.values() if r.total_endpoints > 0])}")
        print(f"Total Endpoints:      {total_endpoints}")
        if total_endpoints > 0:
            print(f"Documented:           {total_documented} ({total_documented/total_endpoints*100:.1f}%)")
            print(f"Undocumented:         {total_undocumented} ({total_undocumented/total_endpoints*100:.1f}%)")
        print(f"Services with Issues: {len(services_with_issues)}")

        if services_with_issues:
            print(f"\n{YELLOW}Services requiring attention:{NC} {', '.join(services_with_issues)}")
        else:
            print(f"\n{GREEN}✓ All endpoints are documented!{NC}")

    def generate_json_report(self, output_file: str = "openapi-validation-report.json"):
        """Generate JSON report for CI/CD pipelines."""
        report = {
            "timestamp": str(Path.cwd()),
            "summary": {
                "total_services": len([r for r in self.results.values() if r.total_endpoints > 0]),
                "total_endpoints": sum(r.total_endpoints for r in self.results.values()),
                "documented_endpoints": sum(r.documented_endpoints for r in self.results.values()),
                "undocumented_endpoints": sum(len(r.undocumented_endpoints) for r in self.results.values()),
            },
            "services": {}
        }

        for service, result in self.results.items():
            if result.total_endpoints == 0:
                continue

            report["services"][service] = {
                "coverage_percent": round(result.documented_endpoints / result.total_endpoints * 100, 1),
                "total_endpoints": result.total_endpoints,
                "documented_endpoints": result.documented_endpoints,
                "undocumented_endpoints": [
                    {
                        "controller": e.controller,
                        "method": e.method,
                        "path": e.path,
                        "line_number": e.line_number
                    }
                    for e in result.undocumented_endpoints
                ]
            }

        Path(output_file).write_text(json.dumps(report, indent=2))
        print(f"\n{CYAN}JSON report saved to: {output_file}{NC}")


def main():
    """Main entry point."""
    import argparse

    parser = argparse.ArgumentParser(description="Validate OpenAPI contracts")
    parser.add_argument("--service", help="Validate specific service only")
    parser.add_argument("--summary", action="store_true", help="Show summary only")
    parser.add_argument("--json", action="store_true", help="Generate JSON report")
    parser.add_argument("--backend-dir", default="backend", help="Backend directory path")

    args = parser.parse_args()

    validator = OpenAPIValidator(args.backend_dir)

    if args.service:
        result = validator.validate_service(args.service)
        validator.results[args.service] = result
    else:
        validator.validate_all()

    validator.print_results(summary_only=args.summary)

    if args.json:
        validator.generate_json_report()

    # Exit with error code if issues found
    total_undocumented = sum(len(r.undocumented_endpoints) for r in validator.results.values())
    sys.exit(1 if total_undocumented > 0 else 0)


if __name__ == "__main__":
    main()
