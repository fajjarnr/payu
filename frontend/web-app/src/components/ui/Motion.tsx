'use client';

import { CSSProperties, ReactNode, forwardRef, useSyncExternalStore } from 'react';

const FRAMER_PROPS: Record<string, true> = {
  whileHover: true, whileTap: true, whileFocus: true, whileInView: true, whileDrag: true,
  drag: true, dragConstraints: true, dragElastic: true, dragMomentum: true,
  initial: true, animate: true, exit: true, variants: true, transition: true,
  layout: true, layoutId: true, onAnimationStart: true, onAnimationComplete: true, viewport: true,
};

function stripFramer<T extends object>(props: T): Partial<T> {
  const clean: Record<string, unknown> = {};
  for (const key of Object.keys(props)) {
    if (!FRAMER_PROPS[key]) clean[key] = (props as Record<string, unknown>)[key];
  }
  return clean as Partial<T>;
}

const REDUCED_MOTION_QUERY = '(prefers-reduced-motion: reduce)';

function subscribeReducedMotion(callback: () => void) {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return () => {};
  const mq = window.matchMedia(REDUCED_MOTION_QUERY);
  mq.addEventListener('change', callback);
  return () => mq.removeEventListener('change', callback);
}

function getReducedMotionSnapshot(): boolean {
  if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return false;
  return window.matchMedia(REDUCED_MOTION_QUERY).matches;
}

function usePrefersReducedMotion(): boolean {
  return useSyncExternalStore(subscribeReducedMotion, getReducedMotionSnapshot, () => false);
}

const KEYFRAMES = `
@keyframes payu-fade-up { from { opacity: 0; transform: translateY(20px); } to { opacity: 1; transform: translateY(0); } }
@keyframes payu-fade-down { from { opacity: 0; transform: translateY(-20px); } to { opacity: 1; transform: translateY(0); } }
@keyframes payu-fade-left { from { opacity: 0; transform: translateX(20px); } to { opacity: 1; transform: translateX(0); } }
@keyframes payu-fade-right { from { opacity: 0; transform: translateX(-20px); } to { opacity: 1; transform: translateX(0); } }
@keyframes payu-scale-in { from { opacity: 0; transform: scale(0.9); } to { opacity: 1; transform: scale(1); } }
@media (prefers-reduced-motion: reduce) {
  .payu-motion { animation: none !important; }
}
`;

type DivExtras = Record<string, unknown>;

interface PageTransitionProps extends DivExtras {
  children: ReactNode;
  className?: string;
}

export const PageTransition = ({ children, className, ...rest }: PageTransitionProps) => {
  const reduce = usePrefersReducedMotion();
  const style: CSSProperties = reduce ? {} : { animation: 'payu-fade-up 0.3s ease-in-out' };
  return (
    <>
      <style>{KEYFRAMES}</style>
      <div className={`payu-motion${className ? ` ${className}` : ''}`} style={style} {...stripFramer(rest)}>
        {children}
      </div>
    </>
  );
};

interface FadeInProps extends DivExtras {
  children: ReactNode;
  delay?: number;
  direction?: 'up' | 'down' | 'left' | 'right';
  className?: string;
}

const FADE_ANIMATION = {
  up: 'payu-fade-up',
  down: 'payu-fade-down',
  left: 'payu-fade-left',
  right: 'payu-fade-right',
} as const;

export const FadeIn = ({ children, delay = 0, direction = 'up', className, ...rest }: FadeInProps) => {
  const reduce = usePrefersReducedMotion();
  const style: CSSProperties = reduce
    ? {}
    : { animation: `${FADE_ANIMATION[direction]} 0.5s ease-out`, animationDelay: `${delay}s` };
  return (
    <>
      <style>{KEYFRAMES}</style>
      <div className={`payu-motion${className ? ` ${className}` : ''}`} style={style} {...stripFramer(rest)}>
        {children}
      </div>
    </>
  );
};

interface ScaleInProps extends DivExtras {
  children: ReactNode;
  delay?: number;
  className?: string;
}

export const ScaleIn = ({ children, delay = 0, className, ...rest }: ScaleInProps) => {
  const reduce = usePrefersReducedMotion();
  const style: CSSProperties = reduce
    ? {}
    : { animation: 'payu-scale-in 0.4s ease-out', animationDelay: `${delay}s` };
  return (
    <>
      <style>{KEYFRAMES}</style>
      <div className={`payu-motion${className ? ` ${className}` : ''}`} style={style} {...stripFramer(rest)}>
        {children}
      </div>
    </>
  );
};

interface StaggerContainerProps extends DivExtras {
  children: ReactNode;
  staggerDelay?: number;
  className?: string;
}

export const StaggerContainer = ({ children, staggerDelay = 0.1, className, ...rest }: StaggerContainerProps) => {
  const reduce = usePrefersReducedMotion();
  const style: CSSProperties = reduce
    ? {}
    : { animation: 'payu-fade-up 0.4s ease-out', animationDelay: `${staggerDelay}s` };
  return (
    <>
      <style>{KEYFRAMES}</style>
      <div className={`payu-motion${className ? ` ${className}` : ''}`} style={style} {...stripFramer(rest)}>
        {children}
      </div>
    </>
  );
};

interface StaggerItemProps extends DivExtras {
  children: ReactNode;
  delay?: number;
  className?: string;
}

export const StaggerItem = ({ children, delay = 0, className, ...rest }: StaggerItemProps) => {
  const reduce = usePrefersReducedMotion();
  const style: CSSProperties = reduce
    ? {}
    : { animation: 'payu-fade-up 0.4s ease-out', animationDelay: `${delay}s` };
  return (
    <>
      <style>{KEYFRAMES}</style>
      <div className={`payu-motion${className ? ` ${className}` : ''}`} style={style} {...stripFramer(rest)}>
        {children}
      </div>
    </>
  );
};

type AnyDivProps = Omit<React.HTMLAttributes<HTMLDivElement>, 'children'> & { children?: ReactNode; [key: string]: unknown };
type AnyButtonProps = Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & { children?: ReactNode; [key: string]: unknown };

export const AnimatedDiv = forwardRef<HTMLDivElement, AnyDivProps>(function AnimatedDiv(props, ref) {
  const { children, ...rest } = props as { children?: ReactNode } & Record<string, unknown>;
  return (
    <div ref={ref} {...stripFramer(rest)}>
      {children}
    </div>
  );
});

export const AnimatedButton = forwardRef<HTMLButtonElement, AnyButtonProps>(function AnimatedButton(props, ref) {
  const { children, ...rest } = props as { children?: ReactNode } & Record<string, unknown>;
  return (
    <button ref={ref} {...stripFramer(rest)}>
      {children}
    </button>
  );
});

export const ButtonMotion = forwardRef<HTMLButtonElement, AnyButtonProps>(function ButtonMotion(props, ref) {
  const { children, className, ...rest } = props as { children?: ReactNode; className?: string } & Record<string, unknown>;
  return (
    <button
      ref={ref}
      className={`transition-transform active:scale-95${className ? ` ${className}` : ''}`}
      {...stripFramer(rest)}
    >
      {children}
    </button>
  );
});
