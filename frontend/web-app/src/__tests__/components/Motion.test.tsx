import React from 'react';
import { render, screen } from '@testing-library/react';
import { PageTransition, FadeIn, ScaleIn, StaggerContainer, StaggerItem, ButtonMotion } from '@/components/ui/Motion';

describe('Motion Components', () => {
  describe('PageTransition', () => {
    it('renders children with page transition', () => {
      render(
        <PageTransition>
          <div>Page content</div>
        </PageTransition>
      );
      expect(screen.getByText('Page content')).toBeInTheDocument();
    });
  });

  describe('FadeIn', () => {
    it('renders children with fade in animation', () => {
      render(
        <FadeIn>
          <div>Fade in content</div>
        </FadeIn>
      );
      expect(screen.getByText('Fade in content')).toBeInTheDocument();
    });
  });

  describe('ScaleIn', () => {
    it('renders children with scale in animation', () => {
      render(
        <ScaleIn>
          <div>Scale in content</div>
        </ScaleIn>
      );
      expect(screen.getByText('Scale in content')).toBeInTheDocument();
    });
  });

  describe('StaggerContainer', () => {
    it('renders children with stagger animation', () => {
      render(
        <StaggerContainer>
          <StaggerItem>Item 1</StaggerItem>
          <StaggerItem>Item 2</StaggerItem>
        </StaggerContainer>
      );
      expect(screen.getByText('Item 1')).toBeInTheDocument();
      expect(screen.getByText('Item 2')).toBeInTheDocument();
    });
  });

  describe('ButtonMotion', () => {
    it('renders button with motion properties', () => {
      render(
        <ButtonMotion data-testid="animated-button">
          Animated button
        </ButtonMotion>
      );
      expect(screen.getByTestId('animated-button')).toBeInTheDocument();
      expect(screen.getByTestId('animated-button')).toHaveTextContent('Animated button');
    });
  });
});
