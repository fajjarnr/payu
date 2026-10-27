"use client";

import {
  BadgeCheck,
  BarChart3,
  Fingerprint,
  Globe,
  Lock,
  QrCode,
  ReceiptText,
  Shield,
  Wallet,
} from "@/components/icons";
import type { CSSProperties } from "react";

export type LandingIconName =
  | "barChart3"
  | "qrCode"
  | "wallet"
  | "receiptText"
  | "globe"
  | "shield"
  | "lock"
  | "fingerprint"
  | "badgeCheck";

const ICONS = {
  barChart3: BarChart3,
  qrCode: QrCode,
  wallet: Wallet,
  receiptText: ReceiptText,
  globe: Globe,
  shield: Shield,
  lock: Lock,
  fingerprint: Fingerprint,
  badgeCheck: BadgeCheck,
} as const;

export interface LandingIconProps {
  name: LandingIconName;
  style?: CSSProperties;
}

/**
 * Icon leaf for the server-rendered landing page. `@ant-design/icons` calls
 * `createContext` at module top level, so it cannot be imported by a Server
 * Component — the server passes serializable names, this leaf resolves them.
 */
export function LandingIcon({ name, style }: LandingIconProps) {
  const Icon = ICONS[name];
  return <Icon style={style} />;
}
