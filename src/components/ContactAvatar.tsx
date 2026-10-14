import React from 'react';
import { User } from 'lucide-react';

interface ContactAvatarProps {
  name: string;
  size?: 'sm' | 'md' | 'lg' | 'xl';
  className?: string;
}

export function ContactAvatar({ name, size = 'md', className = '' }: ContactAvatarProps) {
  const colors = [
    'from-[#6743A5] to-[#432A70]',
    'from-[#375E83] to-[#203D58]',
    'from-[#466A62] to-[#25423C]',
    'from-[#83516D] to-[#552E43]'
  ];

  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = (hash << 5) - hash + name.charCodeAt(i);
    hash |= 0;
  }
  const colorClass = colors[Math.abs(hash) % colors.length];

  const sizeClasses = {
    sm: 'w-8 h-8 text-xs',
    md: 'w-11 h-11 text-base',
    lg: 'w-16 h-16 text-2xl',
    xl: 'w-24 h-24 text-3xl'
  };

  const initial = name.trim().charAt(0).toUpperCase();
  const hasLetter = /[a-zA-ZąćęłńóśźżĄĆĘŁŃÓŚŹŻ]/.test(initial);

  return (
    <div
      className={`rounded-full bg-gradient-to-br ${colorClass} flex items-center justify-center font-bold text-white shadow-sm flex-shrink-0 ${sizeClasses[size]} ${className}`}
    >
      {hasLetter ? (
        initial
      ) : (
        <User className="opacity-80 w-1/2 h-1/2" />
      )}
    </div>
  );
}
