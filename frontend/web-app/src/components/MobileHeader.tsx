"use client";

import { ArrowLeft } from '@/components/icons';
import { useRouter } from "@/lib/navigation";
import { Button, Typography, theme } from "antd";

interface MobileHeaderProps {
  title: string;
  showBack?: boolean;
}

export default function MobileHeader({
  title,
  showBack = true,
}: MobileHeaderProps) {
  const router = useRouter();
  const { token } = theme.useToken();

  return (
    <header
      className="sticky top-0 z-40 border-b px-4 h-16 flex items-center justify-between"
      style={{ backgroundColor: token.colorBgContainer, borderBottomColor: token.colorBorderSecondary }}
    >
      <div className="flex items-center gap-3">
        {showBack && (
          <Button
            type="text"
            shape="circle"
            onClick={() => router.back()}
            aria-label="Kembali"
            icon={<ArrowLeft className="h-5 w-5" />}
            className="-ml-2 rounded-full transition-colors min-h-[44px] min-w-[44px]"
            style={{ color: token.colorText }}
          />
        )}
        <Typography.Title
          level={1}
          style={{ margin: 0, color: token.colorText }}
          className="text-lg font-bold"
        >
          {title}
        </Typography.Title>
      </div>
    </header>
  );
}
