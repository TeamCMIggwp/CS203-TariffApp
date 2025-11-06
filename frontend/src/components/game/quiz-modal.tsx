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
} from "@/components/ui/dialog"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { Label } from "@/components/ui/label"

interface QuizModalProps {
  question: {
    question: string
    type: string
    correctAnswer: string
    options: string[]
  }
  onAnswer: (correct: boolean) => void
}

export function QuizModal({ question, onAnswer }: QuizModalProps) {
  const [selectedAnswer, setSelectedAnswer] = useState<string>("")
  const [showResult, setShowResult] = useState(false)
  const [isCorrect, setIsCorrect] = useState(false)

  const handleSubmit = () => {
    const correct = selectedAnswer.toLowerCase() === question.correctAnswer.toLowerCase()
    setIsCorrect(correct)
    setShowResult(true)
  }

  const handleContinue = () => {
    onAnswer(isCorrect)
    setSelectedAnswer("")
    setShowResult(false)
  }

  return (
    <Dialog open={true}>
      <DialogContent className="sm:max-w-[500px] bg-white text-black">
        <DialogHeader>
          <DialogTitle className="text-xl">Tariff Quiz Question</DialogTitle>
          <DialogDescription>Answer correctly to activate the power-up!</DialogDescription>
        </DialogHeader>

        {!showResult ? (
          <div className="space-y-4 py-4">
            <p className="text-lg font-medium">{question.question}</p>

            <RadioGroup value={selectedAnswer} onValueChange={setSelectedAnswer}>
              <div className="space-y-3">
                {question.options.map((option, index) => (
                  <div key={index} className="flex items-center space-x-2">
                    <RadioGroupItem value={option} id={`option-${index}`} />
                    <Label htmlFor={`option-${index}`} className="cursor-pointer">
                      {option}
                    </Label>
                  </div>
                ))}
              </div>
            </RadioGroup>
          </div>
        ) : (
          <div className="py-6 text-center space-y-4">
            <div className={`text-6xl ${isCorrect ? "text-green-500" : "text-red-500"}`}>{isCorrect ? "✓" : "✗"}</div>
            <p className="text-xl font-semibold">{isCorrect ? "Correct!" : "Incorrect!"}</p>
            <p className="text-muted-foreground">
              {isCorrect ? "Power-up activated! Enemies are now scared!" : "No power-up this time. Keep trying!"}
            </p>
            {!isCorrect && (
              <p className="text-sm">
                Correct answer: <span className="font-semibold">{question.correctAnswer}</span>
              </p>
            )}
          </div>
        )}

        <DialogFooter>
          {!showResult ? (
            <Button onClick={handleSubmit} disabled={!selectedAnswer}>
              Submit Answer
            </Button>
          ) : (
            <Button onClick={handleContinue}>Continue Game</Button>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}