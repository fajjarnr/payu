"use client"

import * as React from "react"
import { ArrowLeft, ArrowRight } from "lucide-react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"

// ponytail: embla-carousel 8.6 (30kb) → native CSS scroll-snap, 0 deps. Keep API compat for BannerCarousel.
// If complex drag/loop needed, re-add embla.

export type CarouselApi = {
  selectedScrollSnap: () => number
  scrollSnapList: () => number[]
  scrollTo: (index: number) => void
  on: (event: string, cb: () => void) => void
  off: (event: string, cb: () => void) => void
}

type CarouselProps = {
  opts?: unknown
  plugins?: unknown
  orientation?: "horizontal" | "vertical"
  setApi?: (api: CarouselApi) => void
}

type CarouselContextProps = {
  api: CarouselApi | null
  scrollPrev: () => void
  scrollNext: () => void
  canScrollPrev: boolean
  canScrollNext: boolean
} & CarouselProps

const CarouselContext = React.createContext<CarouselContextProps | null>(null)

function useCarousel() {
  const context = React.useContext(CarouselContext)
  if (!context) throw new Error("useCarousel must be used within <Carousel />")
  return context
}

const Carousel = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement> & CarouselProps
>(({ orientation = "horizontal", opts: _opts, setApi, plugins: _plugins, className, children, ...props }, ref) => {
  const scrollRef = React.useRef<HTMLDivElement>(null)
  const [canScrollPrev, setCanScrollPrev] = React.useState(false)
  const [canScrollNext, setCanScrollNext] = React.useState(false)
  const listeners = React.useRef<Map<string, Set<() => void>>>(new Map())

  const getIndex = React.useCallback(() => {
    const el = scrollRef.current
    if (!el) return 0
    if (!el.clientWidth) return 0
    return Math.round(el.scrollLeft / el.clientWidth)
  }, [])

  const scrollTo = React.useCallback((index: number) => {
    const el = scrollRef.current
    if (!el) return
    const left = index * (el.clientWidth || 300)
    // jsdom has no scrollTo — guard via in check
    if (el && typeof el === "object" && "scrollTo" in el && typeof (el as unknown as HTMLElement & { scrollTo?: unknown }).scrollTo === "function") {
      el.scrollTo({ left, behavior: "smooth" })
    } else {
      el.scrollLeft = left
    }
    // manually notify select listeners (jsdom scroll event not fired)
    listeners.current.get("select")?.forEach((cb) => { try { cb() } catch {} })
    // update canScroll after scroll
    setTimeout(() => {
      if (!el) return
      setCanScrollPrev(el.scrollLeft > 5)
      setCanScrollNext(el.scrollLeft + el.clientWidth < el.scrollWidth - 5)
    }, 0)
  }, [])

  const scrollPrev = React.useCallback(() => scrollTo(getIndex() - 1), [getIndex, scrollTo])
  const scrollNext = React.useCallback(() => scrollTo(getIndex() + 1), [getIndex, scrollTo])

  const updateCanScroll = React.useCallback(() => {
    const el = scrollRef.current
    if (!el) return
    setCanScrollPrev(el.scrollLeft > 5)
    setCanScrollNext(el.scrollLeft + el.clientWidth < el.scrollWidth - 5)
  }, [])

  const api = React.useMemo<CarouselApi>(() => ({
    selectedScrollSnap: getIndex,
    scrollSnapList: () => {
      const el = scrollRef.current
      if (!el) return []
      const count = el.querySelectorAll("[data-carousel-item]").length
      return Array.from({ length: count }, (_, i) => i)
    },
    scrollTo,
    on: (event: string, cb: () => void) => {
      if (!listeners.current.has(event)) listeners.current.set(event, new Set())
      listeners.current.get(event)!.add(cb)
      if (event === "select") {
        scrollRef.current?.addEventListener("scroll", cb as EventListener)
      }
    },
    off: (event: string, cb: () => void) => {
      listeners.current.get(event)?.delete(cb)
      if (event === "select") {
        scrollRef.current?.removeEventListener("scroll", cb as EventListener)
      }
    },
  }), [getIndex, scrollTo])

  React.useEffect(() => {
    setApi?.(api)
  }, [api, setApi])

  React.useEffect(() => {
    const el = scrollRef.current
    if (!el) return
    updateCanScroll()
    el.addEventListener("scroll", updateCanScroll)
    window.addEventListener("resize", updateCanScroll)
    return () => {
      el.removeEventListener("scroll", updateCanScroll)
      window.removeEventListener("resize", updateCanScroll)
    }
  }, [updateCanScroll])

  return (
    <CarouselContext.Provider value={{ api, scrollPrev, scrollNext, canScrollPrev, canScrollNext, orientation }}>
      <div
        ref={ref}
        className={cn("relative", className)}
        role="region"
        aria-roledescription="carousel"
        {...props}
      >
        <div
          ref={scrollRef}
          className={cn(
            "flex overflow-x-auto scroll-smooth snap-x snap-mandatory scrollbar-hide",
            orientation === "vertical" && "flex-col overflow-y-auto overflow-x-hidden snap-y"
          )}
          style={{ scrollbarWidth: "none", msOverflowStyle: "none" }}
        >
          {children}
        </div>
      </div>
    </CarouselContext.Provider>
  )
})
Carousel.displayName = "Carousel"

const CarouselContent = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => {
    return <div ref={ref} className={cn("flex", className)} {...props} />
  }
)
CarouselContent.displayName = "CarouselContent"

const CarouselItem = React.forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(
  ({ className, ...props }, ref) => {
    return (
      <div
        ref={ref}
        data-carousel-item=""
        role="group"
        aria-roledescription="slide"
        className={cn("min-w-0 shrink-0 grow-0 basis-full snap-center", className)}
        {...props}
      />
    )
  }
)
CarouselItem.displayName = "CarouselItem"

const CarouselPrevious = React.forwardRef<HTMLButtonElement, React.ComponentProps<typeof Button>>(
  ({ className, variant = "outline", size = "icon", ...props }, ref) => {
    const { scrollPrev, canScrollPrev } = useCarousel()
    return (
      <Button
        ref={ref}
        variant={variant}
        size={size}
        className={cn("absolute left-2 top-1/2 h-8 w-8 -translate-y-1/2 rounded-full", className)}
        disabled={!canScrollPrev}
        onClick={scrollPrev}
        {...props}
      >
        <ArrowLeft className="h-4 w-4" />
        <span className="sr-only">Previous slide</span>
      </Button>
    )
  }
)
CarouselPrevious.displayName = "CarouselPrevious"

const CarouselNext = React.forwardRef<HTMLButtonElement, React.ComponentProps<typeof Button>>(
  ({ className, variant = "outline", size = "icon", ...props }, ref) => {
    const { scrollNext, canScrollNext } = useCarousel()
    return (
      <Button
        ref={ref}
        variant={variant}
        size={size}
        className={cn("absolute right-2 top-1/2 h-8 w-8 -translate-y-1/2 rounded-full", className)}
        disabled={!canScrollNext}
        onClick={scrollNext}
        {...props}
      >
        <ArrowRight className="h-4 w-4" />
        <span className="sr-only">Next slide</span>
      </Button>
    )
  }
)
CarouselNext.displayName = "CarouselNext"

export { Carousel, CarouselContent, CarouselItem, CarouselPrevious, CarouselNext }
