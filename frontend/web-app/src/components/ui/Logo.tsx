import Image from 'next/image';


interface LogoProps {
  variant?: 'light' | 'dark' | 'color';
  className?: string;
  width?: number;
  height?: number;
}

export default function Logo({ variant = 'color', className = '', width = 40, height = 40 }: LogoProps) {
  return (
    <div className={`flex items-center gap-2 ${className}`}>
      <div className="relative hover:scale-105 transition-transform duration-300">
        <Image 
          src="/logo.svg" 
          alt="PayU Logo" 
          width={width} 
          height={height}
          priority
          className="w-auto h-auto"
        />
      </div>
      <span className={`font-bold tracking-tight ${variant === 'light' ? 'text-white' : 'text-slate-900'} text-xl`}>
        PayU
      </span>
    </div>
  );
}
