#!/usr/bin/env python3
"""
Engineering Team Scaling Calculator - Optimize team growth and structure
"""

import json
import math
from typing import Dict, List, Tuple

class TeamScalingCalculator:
    def __init__(self):
        self.conway_factor = 1.5  # Conway's Law impact factor
        self.brooks_factor = 0.75  # Brooks' Law diminishing returns

        # Optimal team structures based on size
        self.team_structures = {
            'startup': {'min': 1, 'max': 10, 'structure': 'flat'},
            'growth': {'min': 11, 'max': 50, 'structure': 'team_leads'},
            'scale': {'min': 51, 'max': 150, 'structure': 'departments'},
            'enterprise': {'min': 151, 'max': 9999, 'structure': 'divisions'}
        }

        # Role ratios for balanced teams
        self.role_ratios = {
            'engineering_manager': 0.125,  # 1:8 ratio
            'tech_lead': 0.167,  # 1:6 ratio
            'senior_engineer': 0.3,
            'mid_engineer': 0.4,
            'junior_engineer': 0.2,
            'devops': 0.1,
            'qa': 0.15,
            'product_manager': 0.1,
            'designer': 0.08,
            'data_engineer': 0.05
        }

    def calculate_scaling_plan(self, current_state: Dict, growth_targets: Dict) -> Dict:
        """Calculate optimal scaling plan"""
        results = {
            'current_analysis': self._analyze_current_state(current_state),
            'growth_timeline': self._create_growth_timeline(current_state, growth_targets),
            'hiring_plan': {},
            'team_structure': {},
            'budget_projection': {},
            'risk_factors': [],
            'recommendations': []
        }

        # Generate hiring plan
        results['hiring_plan'] = self._generate_hiring_plan(
            current_state,
            growth_targets
        )

        # Design team structure
        results['team_structure'] = self._design_team_structure(
            growth_targets['target_headcount']
        )

        # Calculate budget
        results['budget_projection'] = self._calculate_budget(
            results['hiring_plan'],
            current_state.get('location', 'US')
        )

        # Assess risks
        results['risk_factors'] = self._assess_scaling_risks(
            current_state,
            growth_targets
        )

        # Generate recommendations
        results['recommendations'] = self._generate_recommendations(results)

        return results

    def _analyze_current_state(self, current_state: Dict) -> Dict:
        """Analyze current team state"""
        total_engineers = current_state.get('headcount', 0)

        analysis = {
            'total_headcount': total_engineers,
            'team_stage': self._get_team_stage(total_engineers),
            'productivity_index': 0,
            'balance_score': 0,
            'issues': []
        }

        # Calculate productivity index
        if total_engineers > 0:
            velocity = current_state.get('velocity', 100)
            expected_velocity = total_engineers * 20  # baseline 20 points per engineer
            analysis['productivity_index'] = (velocity / expected_velocity) * 100

        # Check team balance
        roles = current_state.get('roles', {})
        analysis['balance_score'] = self._calculate_balance_score(roles, total_engineers)

        # Identify issues
        if analysis['productivity_index'] < 70:
            analysis['issues'].append('Low productivity - possible process or tooling issues')

        if analysis['balance_score'] < 60:
            analysis['issues'].append('Team imbalance - review role distribution')

        manager_ratio = roles.get('managers', 0) / max(total_engineers, 1)
        if manager_ratio > 0.2:
            analysis['issues'].append('Over-managed - too many managers')
        elif manager_ratio < 0.08 and total_engineers > 20:
            analysis['issues'].append('Under-managed - need more engineering managers')

        return analysis

    def _get_team_stage(self, headcount: int) -> str:
        """Determine team stage based on size"""
        for stage, config in self.team_structures.items():
            if config['min'] <= headcount <= config['max']:
                return stage
        return 'startup'

    def _calculate_balance_score(self, roles: Dict, total: int) -> float:
        """Calculate team balance score"""
        if total == 0:
            return 0

        score = 100
        ideal_ratios = self.role_ratios
        for role, ideal_ratio in ideal_ratios.items():
            actual_count = roles.get(role, 0)
            actual_ratio = actual_count / total

            # Penalize deviation from ideal ratio
            deviation = abs(actual_ratio - ideal_ratio)
            penalty = deviation * 100
            score -= min(penalty, 20)  # Max 20 point penalty per role

        return max(0, score)

    def _create_growth_timeline(self, current: Dict, targets: Dict) -> List[Dict]:
        """Create quarterly growth timeline"""
        current_headcount = current.get('headcount', 0)
        target_headcount = targets.get('target_headcount', current_headcount)
        timeline_quarters = targets.get('timeline_quarters', 4)

        growth_needed = target_headcount - current_headcount
        timeline = []

        for quarter in range(1, timeline_quarters + 1):
            if quarter == 1:
                quarterly_growth = math.ceil(growth_needed * 0.4)
            else:
                remaining_growth = target_headcount - current_headcount
                quarters_left = timeline_quarters - quarter + 1
                quarterly_growth = math.ceil(remaining_growth / quarters_left)

            max_onboarding = math.ceil(current_headcount * 0.25)
            quarterly_growth = min(quarterly_growth, max_onboarding)

            current_headcount += quarterly_growth

            timeline.append({
                'quarter': f'Q{quarter}',
                'headcount': current_headcount,
                'new_hires': quarterly_growth,
                'onboarding_capacity': max_onboarding,
                'productivity_factor': 1.0 - (0.2 * (quarterly_growth / max(current_headcount, 1)))
            })

        return timeline

    def _generate_hiring_plan(self, current: Dict, targets: Dict) -> Dict:
        """Generate detailed hiring plan"""
        current_roles = current.get('roles', {})
        target_headcount = targets.get('target_headcount', 0)

        hiring_plan = {
            'total_hires_needed': target_headcount - current.get('headcount', 0),
            'by_role': {},
            'by_quarter': {},
            'interview_capacity_needed': 0,
            'recruiting_resources': 0
        }

        # Calculate ideal role distribution
        for role, ideal_ratio in self.role_ratios.items():
            ideal_count = math.ceil(target_headcount * ideal_ratio)
            current_count = current_roles.get(role, 0)
            hires_needed = max(0, ideal_count - current_count)

            if hires_needed > 0:
                hiring_plan['by_role'][role] = {
                    'current': current_count,
                    'target': ideal_count,
                    'hires_needed': hires_needed,
                    'priority': self._get_role_priority(role, current_roles, target_headcount)
                }

        # Distribute hires across quarters
        timeline = self._create_growth_timeline(current, targets)
        for quarter_data in timeline:
            quarter = quarter_data['quarter']
            hires = quarter_data['new_hires']

            hiring_plan['by_quarter'][quarter] = {
                'total_hires': hires,
                'breakdown': self._distribute_quarterly_hires(hires, hiring_plan['by_role'])
            }

        hiring_plan['interview_capacity_needed'] = hiring_plan['total_hires_needed'] * 5
        annual_hires = hiring_plan['total_hires_needed'] * (4 / max(targets.get('timeline_quarters', 4), 1))
        hiring_plan['recruiting_resources'] = math.ceil(annual_hires / 50)

        return hiring_plan

    def _get_role_priority(self, role: str, current_roles: Dict, target_size: int) -> int:
        priorities = {
            'engineering_manager': 10 if target_size > 20 else 5,
            'tech_lead': 9,
            'senior_engineer': 8,
            'devops': 7 if current_roles.get('devops', 0) == 0 else 5,
            'qa': 6,
            'mid_engineer': 5,
            'product_manager': 6,
            'designer': 5,
            'data_engineer': 4,
            'junior_engineer': 3
        }

        return priorities.get(role, 5)

    def _distribute_quarterly_hires(self, total_hires: int, role_needs: Dict) -> Dict:
        distribution = {}
        sorted_roles = sorted(
            role_needs.items(),
            key=lambda x: x[1]['priority'],
            reverse=True
        )

        remaining_hires = total_hires
        for role, needs in sorted_roles:
            if remaining_hires <= 0:
                break
            hires = min(needs['hires_needed'], max(1, remaining_hires // 3))
            distribution[role] = hires
            remaining_hires -= hires
        return distribution

    def _design_team_structure(self, target_headcount: int) -> Dict:
        stage = self._get_team_stage(target_headcount)
        structure = {
            'organizational_model': self.team_structures[stage]['structure'],
            'teams': [],
            'reporting_structure': {},
            'communication_paths': 0
        }

        if stage == 'startup':
            structure['teams'] = [{'name': 'Core Team', 'size': target_headcount, 'focus': 'Full-stack'}]

        elif stage == 'growth':
            team_size = 6
            num_teams = math.ceil(target_headcount / team_size)
            structure['teams'] = [
                {
                    'name': f'Team {i+1}',
                    'size': team_size,
                    'focus': ['Platform', 'Product', 'Infrastructure', 'Growth'][i % 4]
                }
                for i in range(num_teams)
            ]
        # simplified for scale/enterprise
        structure['communication_paths'] = (target_headcount * (target_headcount - 1)) // 2
        structure['management_layers'] = math.ceil(math.log(target_headcount, 7)) if target_headcount > 0 else 0

        return structure

    def _calculate_budget(self, hiring_plan: Dict, location: str) -> Dict:
        salary_bands = {
            'US': {
                'engineering_manager': 200000,
                'tech_lead': 180000,
                'senior_engineer': 160000,
                'mid_engineer': 120000,
                'junior_engineer': 85000,
                'devops': 150000,
                'qa': 100000,
                'product_manager': 150000,
                'designer': 120000,
                'data_engineer': 140000
            },
            'EU': {}, 'APAC': {}
        }
        for loc in ['EU', 'APAC']:
            factor = 0.8 if loc == 'EU' else 0.6
            salary_bands[loc] = {k: v * factor for k, v in salary_bands['US'].items()}

        location_salaries = salary_bands.get(location, salary_bands['US'])
        budget = {
            'annual_salary_cost': 0, 'benefits_cost': 0, 'equipment_cost': 0,
            'recruiting_cost': 0, 'onboarding_cost': 0, 'total_cost': 0, 'cost_per_hire': 0
        }

        for role, details in hiring_plan['by_role'].items():
            hires = details['hires_needed']
            salary = location_salaries.get(role, 100000)
            budget['annual_salary_cost'] += hires * salary
            budget['recruiting_cost'] += hires * salary * 0.2

        budget['benefits_cost'] = budget['annual_salary_cost'] * 0.3
        budget['equipment_cost'] = hiring_plan['total_hires_needed'] * 5000
        budget['onboarding_cost'] = hiring_plan['total_hires_needed'] * 10000
        budget['total_cost'] = sum([
            budget['annual_salary_cost'], budget['benefits_cost'],
            budget['equipment_cost'], budget['recruiting_cost'],
            budget['onboarding_cost']
        ])

        if hiring_plan['total_hires_needed'] > 0:
            budget['cost_per_hire'] = budget['total_cost'] / hiring_plan['total_hires_needed']

        return budget

    def _assess_scaling_risks(self, current: Dict, targets: Dict) -> List[Dict]:
        risks = []
        growth_rate = (targets['target_headcount'] - current['headcount']) / max(current['headcount'], 1)
        if growth_rate > 1.0:
            risks.append({'risk': 'Rapid growth dilution', 'impact': 'High', 'mitigation': 'Strong onboarding'})
        return risks

    def _generate_recommendations(self, results: Dict) -> List[str]:
        return ["Focus on senior hires first to establish culture", "Implement continuous integration early"]

def calculate_team_scaling(current_state: Dict, growth_targets: Dict) -> str:
    calculator = TeamScalingCalculator()
    results = calculator.calculate_scaling_plan(current_state, growth_targets)
    return str(results)

if __name__ == "__main__":
    example_current = {'headcount': 25, 'velocity': 450, 'roles': {'senior_engineer': 8}, 'location': 'US'}
    example_targets = {'target_headcount': 75, 'timeline_quarters': 4}
    print(calculate_team_scaling(example_current, example_targets))
