"use client";

import { useTranslations } from "next-intl";

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
    <>
      {/* ponytail: focus colors live in CSS because Tailwind has no var() focus
          utilities without arbitrary values; sr-only→visible behavior unchanged. */}
      <style data-skiplink-focus>{`.payu-skiplink:focus-visible{background-color:var(--ant-color-primary) !important;color:#06281F !important;}`}</style>
      <a
        href={href}
        className={`
        sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50
        focus:px-4 focus:py-2 payu-skiplink focus:rounded-md
        focus:font-medium focus:shadow-lg focus:outline-none
        transition-all duration-200
        ${className}
      `}
      >
        {children || t("skipToMain")}
      </a>
    </>
  );
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
  getA11yProps,
};
