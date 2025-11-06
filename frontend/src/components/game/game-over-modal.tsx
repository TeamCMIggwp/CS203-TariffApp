"use client"

import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

interface GameOverModalProps {
  score: number
  won: boolean
  onRestart: () => void
}

export function GameOverModal({ score, won, onRestart }: GameOverModalProps) {
  return (
    <Dialog open={true}>
      <DialogContent className="sm:max-w-[400px] bg-white text-black dark:bg-gray-900 dark:text-white">
        <DialogHeader>
          <DialogTitle className="text-2xl text-center">{won ? "🎉 Congratulations!" : "💀 Game Over"}</DialogTitle>
          <DialogDescription className="text-center text-lg">
            {won ? "You collected all the dollar signs!" : "Better luck next time!"}
          </DialogDescription>
        </DialogHeader>

        <div className="py-6 text-center">
          <p className="text-sm text-muted-foreground mb-2">Final Score</p>
          <p className="text-5xl font-bold text-primary">{score}</p>
        </div>

        <DialogFooter className="sm:justify-center">
          <Button onClick={onRestart} size="lg">
            Play Again
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
