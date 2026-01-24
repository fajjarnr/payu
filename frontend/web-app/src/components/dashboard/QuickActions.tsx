'use client';

import React, { useState } from 'react';
import { motion } from 'framer-motion';
import {
  ArrowRightLeft,
  QrCode,
  Receipt,
  Wallet,
  CreditCard,
  Smartphone,
  MoreHorizontal,
  GripVertical,
  ChevronRight,
} from 'lucide-react';
import { useTranslations } from 'next-intl';
import {
  DndContext,
  closestCenter,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
  DragEndEvent,
} from '@dnd-kit/core';
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import clsx from 'clsx';

interface QuickAction {
  id: string;
  label: string;
  icon: React.ElementType;
  href: string;
  color: string;
  bgColor: string;
  description?: string;
  ariaLabel: string;
}

interface QuickActionsProps {
  actions?: QuickAction[];
  maxActions?: number;
  className?: string;
  onReorder?: (actions: QuickAction[]) => void;
}

const defaultActions: QuickAction[] = [
  {
    id: 'transfer',
    label: 'Transfer',
    icon: ArrowRightLeft,
    href: '/transfer',
    color: 'text-primary',
    bgColor: 'bg-success-light',
    description: 'Kirim uang instan',
    ariaLabel: 'Transfer uang ke akun lain',
  },
  {
    id: 'qris',
    label: 'QRIS',
    icon: QrCode,
    href: '/qris',
    color: 'text-primary',
    bgColor: 'bg-chart-2',
    description: 'Scan QR untuk bayar',
    ariaLabel: 'Pembayaran QRIS',
  },
  {
    id: 'bills',
    label: 'Tagihan',
    icon: Receipt,
    href: '/bills',
    color: 'text-primary',
    bgColor: 'bg-chart-3',
    description: 'Bayar tagihan & isi ulang',
    ariaLabel: 'Bayar tagihan dan isi ulang',
  },
  {
    id: 'pockets',
    label: 'Kantong',
    icon: Wallet,
    href: '/pockets',
    color: 'text-primary',
    bgColor: 'bg-chart-green1',
    description: 'Kelola kantong uang',
    ariaLabel: 'Kelola kantong',
  },
  {
    id: 'cards',
    label: 'Kartu',
    icon: CreditCard,
    href: '/cards',
    color: 'text-primary',
    bgColor: 'bg-chart-green2',
    description: 'Kartu virtual',
    ariaLabel: 'Kelola kartu virtual',
  },
  {
    id: 'topup',
    label: 'Isi Ulang',
    icon: Smartphone,
    href: '/bills?category=pulsa',
    color: 'text-primary',
    bgColor: 'bg-chart-green3',
    description: 'Isi pulsa & paket data',
    ariaLabel: 'Isi ulang pulsa',
  },
];

export default function QuickActions({
  actions = defaultActions,
  maxActions = 6,
  className = '',
  onReorder,
}: QuickActionsProps) {
  const t = useTranslations('dashboard');
  const [items, setItems] = useState(actions.slice(0, maxActions));
  const [isEditMode, setIsEditMode] = useState(false);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (over && active.id !== over.id) {
      setItems((items) => {
        const oldIndex = items.findIndex((item) => item.id === active.id);
        const newIndex = items.findIndex((item) => item.id === over.id);
        const newItems = arrayMove(items, oldIndex, newIndex);

        onReorder?.(newItems);
        return newItems;
      });
    }
  };

  return (
    <div
      className={clsx(
        'bg-card p-6 sm:p-8 rounded-xl border border-border shadow-card relative overflow-hidden',
        className
      )}
      role="region"
      aria-labelledby="quick-actions-title"
    >
      {/* Decorative background */}
      <div className="absolute -bottom-10 -right-10 w-40 h-40 bg-primary/5 rounded-full blur-3xl" />

      {/* Header */}
      <div className="flex justify-between items-start mb-6">
        <div>
          <h2
            id="quick-actions-title"
            className="text-xs font-semibold text-muted-foreground tracking-widest mb-1"
          >
            {t('quickActionsTitle')}
          </h2>
          <p className="text-[10px] text-muted-foreground">
            Akses cepat ke fitur favorit Anda
          </p>
        </div>

        {/* Edit Toggle */}
        <button
          onClick={() => setIsEditMode(!isEditMode)}
          className={clsx(
            'px-3 py-1.5 rounded-lg text-[10px] font-bold transition-all',
            isEditMode
              ? 'bg-primary text-primary-foreground'
              : 'bg-muted/50 text-muted-foreground hover:bg-muted hover:text-foreground'
          )}
          aria-pressed={isEditMode}
          aria-label={isEditMode ? 'Selesai mengedit' : 'Edit urutan aksi cepat'}
        >
          {isEditMode ? 'Selesai' : 'Edit'}
        </button>
      </div>

      {/* Drag hint in edit mode */}
      <AnimatePresence>
        {isEditMode && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            className="mb-4 p-3 bg-primary/5 border border-primary/20 rounded-lg"
            role="status"
            aria-live="polite"
          >
            <p className="text-[10px] text-muted-foreground flex items-center gap-2">
              <GripVertical className="h-4 w-4" aria-hidden="true" />
              {t('quickActionsDragHint')} - Gunakan Tab untuk navigasi, Spasi/Enter untuk drag
            </p>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Actions Grid */}
      <DndContext
        sensors={sensors}
        collisionDetection={closestCenter}
        onDragEnd={handleDragEnd}
      >
        <SortableContext items={items.map((i) => i.id)} strategy={verticalListSortingStrategy}>
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            {items.map((action, index) => (
              <SortableQuickAction
                key={action.id}
                action={action}
                isEditMode={isEditMode}
                index={index}
              />
            ))}
          </div>
        </SortableContext>
      </DndContext>

      {/* More Actions Link */}
      <div className="mt-6 pt-4 border-t border-border">
        <button
          className="w-full flex items-center justify-center gap-2 text-xs font-bold text-muted-foreground hover:text-foreground transition-colors focus:outline-none focus:ring-2 focus:ring-primary focus:ring-inset rounded-lg py-2"
          aria-label="Lihat semua fitur"
        >
          <MoreHorizontal className="h-4 w-4" aria-hidden="true" />
          Lihat Semua Fitur
          <ChevronRight className="h-4 w-4" aria-hidden="true" />
        </button>
      </div>
    </div>
  );
}

interface SortableQuickActionProps {
  action: QuickAction;
  isEditMode: boolean;
  index: number;
}

function SortableQuickAction({ action, isEditMode, index }: SortableQuickActionProps) {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: action.id,
  });

  const Icon = action.icon;

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div ref={setNodeRef} style={style} className="relative">
      <motion.a
        href={action.href}
        className={clsx(
          'group relative p-4 rounded-xl border transition-all',
          'focus:outline-none focus:ring-2 focus:ring-primary focus:ring-inset',
          isDragging && 'opacity-50',
          isEditMode
            ? 'cursor-grab active:cursor-grabbing border-dashed border-primary/30 bg-muted/30'
            : 'border-border bg-card hover:border-primary/30 hover:shadow-md'
        )}
        aria-label={action.ariaLabel}
        draggable={isEditMode}
        {...(isEditMode && { ...attributes, ...listeners })}
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.2, delay: index * 0.05 }}
        tabIndex={isEditMode ? 0 : undefined}
      >
        {/* Drag Handle Indicator */}
        {isEditMode && (
          <div className="absolute top-2 right-2 flex items-center gap-1">
            <GripVertical className="h-4 w-4 text-muted-foreground" aria-hidden="true" />
            <span className="sr-only">Drag untuk mengatur ulang</span>
          </div>
        )}

        {/* Icon */}
        <div
          className={clsx(
            'h-12 w-12 rounded-xl flex items-center justify-center mb-3 transition-transform group-hover:scale-110',
            action.bgColor
          )}
        >
          <Icon className={clsx('h-6 w-6', action.color)} aria-hidden="true" />
        </div>

        {/* Label */}
        <p className="text-xs font-bold text-foreground mb-0.5">{action.label}</p>
        {action.description && (
          <p className="text-[10px] text-muted-foreground line-clamp-1">{action.description}</p>
        )}

        {/* Hover Effect */}
        {!isEditMode && (
          <motion.div
            className="absolute inset-0 rounded-xl bg-primary/5 opacity-0 group-hover:opacity-100 transition-opacity"
            initial={false}
            whileHover={{ scale: 1.02 }}
          />
        )}
      </motion.a>
    </div>
  );
}

function AnimatePresence({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
