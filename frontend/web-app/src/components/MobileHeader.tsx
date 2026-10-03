"use client";

import { ArrowLeft } from '@/components/icons';
import { useRouter } from "@/lib/navigation";
import { Button, Typography } from "antd";

interface MobileHeaderProps {
  title: string;
  showBack?: boolean;
}

export default function MobileHeader({
  title,
  showBack = true,
}: MobileHeaderProps) {
  const router = useRouter();

  return (
    <header className="sticky top-0 z-40 bg-card/80 backdrop-blur-md border-b border-border px-4 h-16 flex items-center justify-between">
      <div className="flex items-center gap-3">
        {showBack && (
          <Button
            type="text"
            shape="circle"
            onClick={() => router.back()}
            aria-label="Kembali"
            icon={<ArrowLeft className="h-5 w-5" />}
            className="-ml-2 rounded-full hover:bg-muted transition-colors"
          />
        )}
        <Typography.Title
          level={1}
          style={{ margin: 0 }}
          className="text-lg font-bold text-foreground"
        >
          {title}
        </Typography.Title>
      </div>
    </header>
  );
}
