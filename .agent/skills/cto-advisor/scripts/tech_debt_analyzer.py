#!/usr/bin/env python3
"""
Technical Debt Analyzer - Assess and prioritize technical debt across systems
"""

import json
from typing import Dict, List, Tuple
from datetime import datetime
import math

class TechDebtAnalyzer:
    def __init__(self):
        self.debt_categories = {
            'architecture': {
                'weight': 0.25,
                'indicators': [
                    'monolithic_design', 'tight_coupling', 'no_microservices',
                    'legacy_patterns', 'no_api_gateway', 'synchronous_only'
                ]
            },
            'code_quality': {
                'weight': 0.20,
                'indicators': [
                    'low_test_coverage', 'high_complexity', 'code_duplication',
                    'no_documentation', 'inconsistent_standards', 'legacy_language'
                ]
            },
            'infrastructure': {
                'weight': 0.20,
                'indicators': [
                    'manual_deployments', 'no_ci_cd', 'single_points_failure',
                    'no_monitoring', 'no_auto_scaling', 'outdated_servers'
                ]
            },
            'security': {
                'weight': 0.20,
                'indicators': [
                    'outdated_dependencies', 'no_security_scans', 'plain_text_secrets',
                    'no_encryption', 'missing_auth', 'no_audit_logs'
                ]
            },
            'performance': {
                'weight': 0.15,
                'indicators': [
                    'slow_response_times', 'no_caching', 'inefficient_queries',
                    'memory_leaks', 'no_optimization', 'blocking_operations'
                ]
            }
        }

    def analyze_system(self, system_data: Dict) -> Dict:
        """Analyze a system for technical debt"""
        results = {
            'timestamp': datetime.now().isoformat(),
            'system_name': system_data.get('name', 'Unknown'),
            'debt_score': 0,
            'debt_level': '',
            'category_scores': {},
            'recommendations': []
        }

        total_debt_score = 0
        for category, config in self.debt_categories.items():
            category_score = self._calculate_category_score(
                system_data.get(category, {}),
                config['indicators']
            )
            weighted_score = category_score * config['weight']
            results['category_scores'][category] = {
                'raw_score': category_score,
                'weighted_score': weighted_score,
                'level': self._get_level(category_score)
            }
            total_debt_score += weighted_score

        results['debt_score'] = round(total_debt_score, 2)
        results['debt_level'] = self._get_level(total_debt_score)
        results['recommendations'] = self._generate_recommendations(results)

        return results

    def _calculate_category_score(self, category_data: Dict, indicators: List) -> float:
        if not category_data:
            return 50.0
        total_score = 0
        count = 0
        for indicator in indicators:
            if indicator in category_data:
                total_score += category_data[indicator]
                count += 1
        return (total_score / count) if count > 0 else 50.0

    def _get_level(self, score: float) -> str:
        if score < 20: return 'Low'
        elif score < 40: return 'Medium-Low'
        elif score < 60: return 'Medium'
        elif score < 80: return 'Medium-High'
        else: return 'Critical'

    def _generate_recommendations(self, results: Dict) -> List[str]:
        recommendations = []
        if results['debt_level'] == 'Critical':
            recommendations.append('🚨 URGENT: Dedicate 40% of engineering capacity to debt reduction')
        elif results['debt_level'] in ['Medium-High', 'High']:
            recommendations.append('Allocate 25-30% of sprints to debt reduction')
        else:
            recommendations.append('Maintain 15-20% ongoing debt reduction allocation')
        return recommendations

def analyze_technical_debt(system_config: Dict) -> str:
    analyzer = TechDebtAnalyzer()
    results = analyzer.analyze_system(system_config)
    return str(results)

if __name__ == "__main__":
    example_system = {
        'name': 'Legacy Platform',
        'architecture': {'monolithic_design': 80, 'tight_coupling': 70},
        'code_quality': {'low_test_coverage': 75},
        'security': {'outdated_dependencies': 85},
        'team_size': 8,
        'system_criticality': 'high'
    }
    print(analyze_technical_debt(example_system))
