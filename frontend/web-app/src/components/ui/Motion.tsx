'use client';

import { motion, HTMLMotionProps, useReducedMotion } from 'framer-motion';
import { ReactNode } from 'react';

interface PageTransitionProps {
 children: ReactNode;
 className?: string;
}

export const PageTransition = ({ children, className }: PageTransitionProps) => {
 const shouldReduceMotion = useReducedMotion();
 return (
 <motion.div
  initial={shouldReduceMotion ? false : { opacity: 0, y: 20 }}
  animate={{ opacity: 1, y: 0 }}
  exit={{ opacity: 0, y: -20 }}
  transition={{ duration: shouldReduceMotion ? 0 : 0.3, ease: 'easeInOut' }}
  className={className}
 >
  {children}
 </motion.div>
 );
};

interface FadeInProps {
 children: ReactNode;
 delay?: number;
 direction?: 'up' | 'down' | 'left' | 'right';
 className?: string;
}

export const FadeIn = ({ children, delay = 0, direction = 'up', className }: FadeInProps) => {
 const shouldReduceMotion = useReducedMotion();
 const directions = {
  up: { y: 20 },
  down: { y: -20 },
  left: { x: 20 },
  right: { x: -20 },
 };

 return (
  <motion.div
   initial={shouldReduceMotion ? false : { opacity: 0, ...directions[direction] }}
   whileInView={{ opacity: 1, x: 0, y: 0 }}
   viewport={{ once: true, margin: '-50px' }}
   transition={{ duration: shouldReduceMotion ? 0 : 0.5, delay: shouldReduceMotion ? 0 : delay, ease: 'easeOut' }}
   className={className}
  >
   {children}
  </motion.div>
 );
};

interface ScaleInProps {
 children: ReactNode;
 delay?: number;
 className?: string;
}

export const ScaleIn = ({ children, delay = 0, className }: ScaleInProps) => {
 const shouldReduceMotion = useReducedMotion();
 return (
 <motion.div
  initial={shouldReduceMotion ? false : { opacity: 0, scale: 0.9 }}
  whileInView={{ opacity: 1, scale: 1 }}
  viewport={{ once: true }}
  transition={{ duration: 0.4, delay, ease: 'easeOut' }}
  className={className}
 >
  {children}
 </motion.div>
 );
};

interface StaggerContainerProps {
 children: ReactNode;
 staggerDelay?: number;
 className?: string;
}

export const StaggerContainer = ({ children, staggerDelay = 0.1, className }: StaggerContainerProps) => {
 const shouldReduceMotion = useReducedMotion();
 return (
 <motion.div
  initial={shouldReduceMotion ? false : "hidden"}
  whileInView="visible"
  viewport={{ once: true, margin: '-50px' }}
  variants={{
   hidden: { opacity: 0 },
   visible: {
    opacity: 1,
    transition: {
     staggerChildren: shouldReduceMotion ? 0 : staggerDelay,
    },
   },
  }}
  className={className}
 >
  {children}
 </motion.div>
 );
};

interface StaggerItemProps {
 children: ReactNode;
 className?: string;
}

export const StaggerItem = ({ children, className }: StaggerItemProps) => {
 const shouldReduceMotion = useReducedMotion();
 return (
 <motion.div
  initial={shouldReduceMotion ? false : "hidden"}
  variants={{
   hidden: { opacity: 0, y: 20 },
   visible: { opacity: 1, y: 0 },
  }}
  transition={{ duration: shouldReduceMotion ? 0 : 0.4, ease: 'easeOut' }}
  className={className}
 >
  {children}
 </motion.div>
 );
};

export const AnimatedButton = motion.button;
export const AnimatedDiv = motion.div;

type ButtonMotionProps = HTMLMotionProps<'button'>;
export const ButtonMotion = ({ children, ...props }: ButtonMotionProps) => {
 const shouldReduceMotion = useReducedMotion();
 return (
 <motion.button
  whileHover={shouldReduceMotion ? undefined : { scale: 1.02 }}
  whileTap={shouldReduceMotion ? undefined : { scale: 0.98 }}
  transition={shouldReduceMotion ? { duration: 0 } : { type: 'spring', stiffness: 400, damping: 17 }}
  {...props}
 >
  {children}
 </motion.button>
);
};
