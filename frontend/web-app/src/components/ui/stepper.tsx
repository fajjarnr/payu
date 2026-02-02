"use client"

import * as React from "react"
import { Check } from "lucide-react"
import { cn } from "@/lib/utils"

interface StepperProps {
  steps: string[]
  currentStep: number
  className?: string
}

export function Stepper({ steps, currentStep, className }: StepperProps) {
  return (
    <div className={cn("flex w-full items-start justify-between", className)}>
      {steps.map((step, index) => {
        const isCompleted = index < currentStep
        const isActive = index === currentStep
        
        return (
          <React.Fragment key={step}>
            <div className="flex flex-col items-center flex-1 relative group">
              {/* Connector Line */}
              {index > 0 && (
                <div 
                  className={cn(
                    "absolute top-5 -left-1/2 w-full h-[2px] -translate-y-1/2 z-0",
                    index <= currentStep ? "bg-primary" : "bg-muted"
                  )} 
                />
              )}
              
              {/* Step Circle */}
              <div 
                className={cn(
                  "relative z-10 flex h-10 w-10 items-center justify-center rounded-xl border-2 transition-all duration-300",
                  isCompleted 
                    ? "bg-primary border-primary text-white shadow-lg shadow-primary/20" 
                    : isActive
                      ? "bg-background border-primary text-primary shadow-lg shadow-primary/10"
                      : "bg-muted/30 border-border text-muted-foreground"
                )}
              >
                {isCompleted ? (
                  <Check className="h-5 w-5" />
                ) : (
                  <span className="text-xs font-black">{index + 1}</span>
                )}
              </div>
              
              {/* Step Label */}
              <div className="mt-4 text-center">
                <span className={cn(
                  "text-[10px] sm:text-xs font-bold uppercase tracking-[0.2em] transition-colors duration-300",
                  isActive ? "text-foreground" : "text-muted-foreground"
                )}>
                  {step}
                </span>
              </div>
            </div>
          </React.Fragment>
        )
      })}
    </div>
  )
}
