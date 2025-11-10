"use client"

import React from "react"

export type PageShellProps = {
  title?: string
  subtitle?: string
  actions?: React.ReactNode
  children: React.ReactNode
  maxWidthClassName?: string // e.g., "max-w-6xl" (defaults to 6xl)
}

/**
 * PageShell: A standard page wrapper matching the News page aesthetic
 * - Full-height section with spacing
 * - Centered container
 * - Optional header card (title, subtitle, actions)
 */
export default function PageShell({
  title,
  subtitle,
  actions,
  children,
  maxWidthClassName = "max-w-6xl",
}: PageShellProps) {
  return (
    <section className="py-20 min-h-screen relative z-10">
      <div className={`${maxWidthClassName} mx-auto px-4`}>
        {(title || subtitle || actions) && (
          <div className="text-center mb-10 bg-black/30 backdrop-blur-md p-6 rounded-2xl border border-white/30">
            {title && (
              <h1 className="text-4xl md:text-5xl font-extrabold text-white mb-2">{title}</h1>
            )}
            {subtitle && (
              <p className="text-white/80 text-base md:text-lg">{subtitle}</p>
            )}
            {actions && (
              <div className="mt-4 flex items-center justify-center gap-3">{actions}</div>
            )}
          </div>
        )}

        {children}
      </div>
    </section>
  )
}
