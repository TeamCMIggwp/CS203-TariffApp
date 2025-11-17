"use client"

import { useState } from "react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogClose,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"

interface GameOverModalProps {
  score: number
  won: boolean
  onRestart: () => void
  onClose: () => void
  onScoreUploaded?: () => void
}

export function GameOverModal({ score, won, onRestart, onClose, onScoreUploaded }: GameOverModalProps) {
  const [name, setName] = useState("")
  const [isUploading, setIsUploading] = useState(false)
  const [uploaded, setUploaded] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const API_BASE_URL = process.env.NEXT_PUBLIC_BACKEND_URL

  const handleUpload = async () => {
  if (!name.trim()) {
    setError("Please enter your name before uploading.")
    return
  }

  setIsUploading(true)
  setError(null)

  try {
    const res = await fetch(`${process.env.NEXT_PUBLIC_BACKEND_URL}/api/v1/leaderboard`, {
      method: "PUT",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ name: name.trim(), score }),
    })

    if (!res.ok) {
      throw new Error(`Upload failed with status ${res.status}`)
    }

    setUploaded(true)

    // 🔁 trigger leaderboard refresh in parent
    if (onScoreUploaded) {
      onScoreUploaded()
    }
  } catch (err) {
  if (err instanceof Error) {
    setError(err.message || "Failed to upload score. Please try again.")
  } else {
    setError("Failed to upload score. Please try again.")
  }
} finally {
  setIsUploading(false)
}
}


  return (
    <Dialog open={true} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-[420px] bg-white text-black dark:bg-gray-900 dark:text-white">
        <DialogHeader>
          <DialogTitle className="text-2xl text-center">
            {won ? "🎉 Congratulations!" : "💀 Game Over"}
          </DialogTitle>
          <DialogDescription className="text-center text-lg">
            {won ? "You collected all the dollar signs!" : "Better luck next time!"}
          </DialogDescription>
        </DialogHeader>

        <div className="py-4 text-center">
          <p className="text-sm text-muted-foreground mb-1">Final Score</p>
          <p className="text-5xl font-bold text-primary">{score}</p>
        </div>

        <div className="mt-2 space-y-2">
          <Label htmlFor="name" className="text-sm">
            Save to leaderboard
          </Label>
          <Input
            id="name"
            placeholder="Enter your name"
            value={name}
            onChange={(e) => {
              setName(e.target.value)
              setError(null)
            }}
            disabled={isUploading || uploaded}
          />
          {error && <p className="text-xs text-red-500">{error}</p>}
          {uploaded && (
            <p className="text-xs text-emerald-500">
              ✅ Score uploaded to leaderboard!
            </p>
          )}
        </div>

        <DialogFooter className="sm:justify-center gap-2 mt-4">
          {/* Upload Score first */}
          <Button
            size="lg"
            variant="secondary"
            onClick={handleUpload}
            disabled={isUploading || uploaded}
          >
            {uploaded ? "Uploaded" : isUploading ? "Uploading..." : "Upload Score"}
          </Button>

          {/* Play Again second */}
          <Button onClick={onRestart} size="lg">
            Play Again
          </Button>

          {/* Close */}
          <DialogClose asChild>
            <Button variant="outline" size="lg">
              Close
            </Button>
          </DialogClose>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
