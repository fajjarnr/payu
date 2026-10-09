"use client";

import { useTranslations } from "next-intl";
import { useEffect } from "react";

/**
 * SkipLink component for accessibility (WCAG 2.1 compliance)
 * Allows keyboard users to skip navigation and go directly to main content
 *
 * @see https://www.w3.org/WAI/WCAG21/Techniques/general/G1
 */
export function SkipLink({
  href = "#main-content",
  className = "",
  children,
}: {
  href?: string;
  className?: string;
  children?: React.ReactNode;
}) {
  const t = useTranslations("nav");
  return (
    <a
      href={href}
      className={`
        sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50
        focus:px-4 focus:py-2 focus:bg-primary-dark focus:text-surface focus:rounded-md
        focus:font-medium focus:shadow-lg focus:outline-none
        transition-all duration-200
        ${className}
      `}
    >
      {children || t("skipToMain")}
    </a>
  );
}

/**
 * useFocusTrap hook to trap focus within a component (modals, dialogs)
 */
export function useFocusTrap(
  active: boolean = true,
  containerRef?: React.RefObject<HTMLElement | null>,
) {
  useEffect(() => {
    if (!active) return;

    const handleTab = (e: KeyboardEvent) => {
      if (e.key !== "Tab") return;

      // Get focusable elements, scoped to container if provided
      const scope = containerRef?.current || document;
      const focusableElements = scope.querySelectorAll(
        'a[href], button:not([disabled]), textarea:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])',
      );

      if (focusableElements.length === 0) return;

      const firstElement = focusableElements[0] as HTMLElement;
      const lastElement = focusableElements[
        focusableElements.length - 1
      ] as HTMLElement;

      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          lastElement?.focus();
          e.preventDefault();
        }
      } else {
        if (document.activeElement === lastElement) {
          firstElement?.focus();
          e.preventDefault();
        }
      }
    };

    document.addEventListener("keydown", handleTab);
    return () => document.removeEventListener("keydown", handleTab);
  }, [active, containerRef]);
}

export function getA11yProps(config: {
  label?: string;
  describedBy?: string;
  expanded?: boolean;
  pressed?: boolean;
  hasPopup?:
    | boolean
    | "false"
    | "true"
    | "menu"
    | "listbox"
    | "tree"
    | "grid"
    | "dialog";
  current?: string | boolean;
  live?: string | boolean;
}) {
  const props: Record<string, string | boolean> = {};

  if (config.label) props["aria-label"] = config.label;
  if (config.describedBy) props["aria-describedby"] = config.describedBy;
  if (config.expanded !== undefined) props["aria-expanded"] = config.expanded;
  if (config.pressed !== undefined) props["aria-pressed"] = config.pressed;
  if (config.hasPopup !== undefined) props["aria-haspopup"] = config.hasPopup;
  if (config.current !== undefined) props["aria-current"] = config.current;
  if (config.live) props["aria-live"] = config.live;

  return props;
}

export const a11yUtils = {
  useFocusTrap,
  getA11yProps,
};
