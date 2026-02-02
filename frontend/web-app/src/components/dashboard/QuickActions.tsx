'use client';

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
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

import { cn } from '@/lib/utils';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';

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
    <Card className={cn("relative overflow-hidden group", className)}>
      {/* Decorative background */}
      <div className="absolute -bottom-10 -right-10 w-40 h-40 bg-primary/5 rounded-full blur-3xl pointer-events-none" />

      <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-8">
        <div>
          <CardTitle className="text-base sm:text-lg font-bold text-foreground tracking-widest uppercase">
            {t('quickActionsTitle')}
          </CardTitle>
          <CardDescription className="uppercase tracking-widest text-xs sm:text-xs font-bold opacity-60">
            {t('quickActionsSubtitle')}
          </CardDescription>
        </div>

        <Button
          variant={isEditMode ? "default" : "outline"}
          size="sm"
          onClick={() => setIsEditMode(!isEditMode)}
          className="text-xs sm:text-xs px-4"
        >
          {isEditMode ? 'Selesai' : 'Edit'}
        </Button>
      </CardHeader>

      <CardContent>
        {/* Drag hint in edit mode */}
        <AnimatePresence>
          {isEditMode && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="mb-6 p-4 bg-primary/5 border border-primary/20 rounded-xl"
              role="status"
              aria-live="polite"
            >
              <p className="text-xs text-muted-foreground flex items-center gap-3">
                <GripVertical className="h-5 w-5" aria-hidden="true" />
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
            <div className="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-4 xl:grid-cols-6 gap-8">
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
        <div className="mt-8 pt-6 border-t border-border">
          <Button
            variant="ghost"
            className="w-full text-xs sm:text-sm font-bold text-muted-foreground hover:text-foreground justify-center gap-3 h-12"
          >
            <MoreHorizontal className="h-5 w-5" aria-hidden="true" />
            Lihat Semua Fitur
            <ChevronRight className="h-5 w-5" aria-hidden="true" />
          </Button>
        </div>
      </CardContent>
    </Card>
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
          'group relative p-6 rounded-2xl border transition-all flex flex-col items-center text-center',
          'focus:outline-none focus:ring-2 focus:ring-primary focus:ring-inset',
          isDragging && 'opacity-50',
          isEditMode
            ? 'cursor-grab active:cursor-grabbing border-dashed border-primary/30 bg-muted/30'
            : 'border-border bg-card hover:border-primary/30 hover:shadow-xl hover:bg-primary/5 shadow-sm'
        )}
        aria-label={action.ariaLabel}
        draggable={isEditMode}
        {...(isEditMode && { ...attributes, ...listeners })}
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.2, delay: index * 0.05 }}
        whileHover={!isEditMode ? { scale: 1.05 } : undefined}
        tabIndex={isEditMode ? 0 : undefined}
      >
        {/* Drag Handle Indicator */}
        {isEditMode && (
          <div className="absolute top-4 right-4 flex items-center gap-1">
            <GripVertical className="h-5 w-5 text-muted-foreground" aria-hidden="true" />
            <span className="sr-only">Drag untuk mengatur ulang</span>
          </div>
        )}

        {/* Icon */}
        <div
          className={clsx(
            'h-16 w-16 rounded-2xl flex items-center justify-center mb-5 transition-transform group-hover:scale-110 shadow-lg',
            action.bgColor
          )}
        >
          <Icon className={clsx('h-8 w-8', action.color)} aria-hidden="true" />
        </div>

        {/* Label */}
        <p className="text-sm font-bold text-foreground mb-1 shadow-sm">{action.label}</p>
        {action.description && (
          <p className="text-xs sm:text-xs text-muted-foreground font-medium line-clamp-1 opacity-80 uppercase tracking-[0.05em]">{action.description}</p>
        )}
      </motion.a>
    </div>
  );
}

