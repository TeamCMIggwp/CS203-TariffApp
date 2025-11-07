"use client"

import { useEffect, useRef, useState } from "react"
import { Button } from "@/components/ui/button"
import { Card } from "@/components/ui/card"
import { QuizModal } from "@/components/game/quiz-modal"
import { GameOverModal } from "@/components/game/game-over-modal"

// Game constants
const CELL_SIZE = 20
const GRID_WIDTH = 28
const GRID_HEIGHT = 31
const CANVAS_WIDTH = GRID_WIDTH * CELL_SIZE
const CANVAS_HEIGHT = GRID_HEIGHT * CELL_SIZE

// Game map (1 = wall, 0 = path, 2 = dollar sign, 3 = power-up)
const GAME_MAP = [
  [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
  [1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1],
  [1, 2, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 2, 1],
  [1, 3, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 3, 1],
  [1, 2, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 2, 1],
  [1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1],
  [1, 2, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 2, 1],
  [1, 2, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 2, 1],
  [1, 2, 2, 2, 2, 2, 2, 1, 1, 2, 2, 2, 2, 1, 1, 2, 2, 2, 2, 1, 1, 2, 2, 2, 2, 2, 2, 1],
  [1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1],
  [1, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 1],
  [1, 1, 1, 1, 1, 1, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 2, 1, 1, 1, 1, 1, 1],
  [1, 1, 1, 1, 1, 1, 2, 1, 1, 0, 1, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 2, 1, 1, 1, 1, 1, 1],
  [1, 1, 1, 1, 1, 1, 2, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 2, 1, 1, 1, 1, 1, 1],
  [0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0],
  [1, 1, 1, 1, 1, 1, 2, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 2, 1, 1, 1, 1, 1, 1],
  [1, 1, 1, 1, 1, 1, 2, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 2, 1, 1, 1, 1, 1, 1],
  [1, 1, 1, 1, 1, 1, 2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 2, 1, 1, 1, 1, 1, 1],
  [1, 1, 1, 1, 1, 1, 2, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 2, 1, 1, 1, 1, 1, 1],
  [1, 1, 1, 1, 1, 1, 2, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 2, 1, 1, 1, 1, 1, 1],
  [1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1],
  [1, 2, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 2, 1],
  [1, 2, 1, 1, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 1, 2, 1, 1, 1, 1, 2, 1],
  [1, 3, 2, 2, 1, 1, 2, 2, 2, 2, 2, 2, 2, 0, 0, 2, 2, 2, 2, 2, 2, 2, 1, 1, 2, 2, 3, 1],
  [1, 1, 1, 2, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 2, 1, 1, 1],
  [1, 1, 1, 2, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 2, 1, 1, 1],
  [1, 2, 2, 2, 2, 2, 2, 1, 1, 2, 2, 2, 2, 1, 1, 2, 2, 2, 2, 1, 1, 2, 2, 2, 2, 2, 2, 1],
  [1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1],
  [1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1, 1, 2, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 1],
  [1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 1],
  [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1],
]

type Position = { x: number; y: number }
type Direction = { dx: number; dy: number }
type Enemy = Position & { direction: Direction; color: string; scared: boolean; releaseTime: number; released: boolean }

export default function GamePage() {
  const canvasRef = useRef<HTMLCanvasElement | null>(null)
  const [gameStarted, setGameStarted] = useState(false)
  const [gameOver, setGameOver] = useState(false)
  const [score, setScore] = useState(0)
  const [lives, setLives] = useState(3)
  const [showQuiz, setShowQuiz] = useState(false)

  type Question = {
  id: string
  questionText: string
  options: string[]
  answer: string
}

const [currentQuestion, setCurrentQuestion] = useState<Question | null>(null)


  // Game state refs
  const playerRef = useRef<Position>({ x: 14, y: 23 })
  const directionRef = useRef<Direction>({ dx: 0, dy: 0 })
  const nextDirectionRef = useRef<Direction>({ dx: 0, dy: 0 })
  const enemiesRef = useRef<Enemy[]>([
    { x: 13, y: 13, direction: { dx: 1, dy: 0 }, color: "#FF0000", scared: false, releaseTime: 0, released: false },
    { x: 14, y: 13, direction: { dx: -1, dy: 0 }, color: "#FFB8FF", scared: false, releaseTime: 20, released: false },
    { x: 13, y: 14, direction: { dx: 1, dy: 0 }, color: "#00FFFF", scared: false, releaseTime: 40, released: false },
  ])
  const mapRef = useRef(GAME_MAP.map((row) => [...row]))
  const scaredTimerRef = useRef(0)
  const animationRef = useRef<number | null>(null)
  const gameTimerRef = useRef(0)

  const checkWinCondition = () => {
    return mapRef.current.flat().every((cell) => cell !== 2)
  }

  useEffect(() => {
    if (!gameStarted || gameOver) return

    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext("2d")
    if (!ctx) return

    let lastTime = 0
    const gameSpeed = 150 // ms per move

    const gameLoop = (timestamp: number) => {
      if (timestamp - lastTime < gameSpeed) {
        animationRef.current = requestAnimationFrame(gameLoop)
        return
      }
      lastTime = timestamp

      gameTimerRef.current++

      // Update scared timer
      if (scaredTimerRef.current > 0) {
        scaredTimerRef.current--
        if (scaredTimerRef.current === 0) {
          enemiesRef.current.forEach((enemy) => (enemy.scared = false))
        }
      }

      // Try to change direction
      const player = playerRef.current
      const nextDir = nextDirectionRef.current
      const nextX = player.x + nextDir.dx
      const nextY = player.y + nextDir.dy

      if (isValidMove(nextX, nextY)) {
        directionRef.current = { ...nextDir }
      }

      // Move player
      const dir = directionRef.current
      const newX = player.x + dir.dx
      const newY = player.y + dir.dy

      if (newX < 0) {
        player.x = GRID_WIDTH - 1
      } else if (newX >= GRID_WIDTH) {
        player.x = 0
      } else if (newY < 0) {
        player.y = GRID_HEIGHT - 1
      } else if (newY >= GRID_HEIGHT) {
        player.y = 0
      } else if (isValidMove(newX, newY)) {
        player.x = newX
        player.y = newY

        // Check for dollar sign
        if (mapRef.current[newY][newX] === 2) {
          mapRef.current[newY][newX] = 0
          setScore((s) => s + 10)

          // Check if all dollar signs collected
          if (checkWinCondition()) {
            setGameOver(true)
            return
          }
        }

        // Check for power-up
        if (mapRef.current[newY][newX] === 3) {
          mapRef.current[newY][newX] = 0
          pauseGameForQuiz()
          return
        }
      }

      // Check collisions before moving enemies
      checkCollisions()

      // Move enemies
      moveEnemies()

      // Check collisions after moving enemies
      checkCollisions()

      // Draw everything
      draw(ctx)

      animationRef.current = requestAnimationFrame(gameLoop)
    }

    animationRef.current = requestAnimationFrame(gameLoop)

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current)
      }
    }
  }, [gameStarted, gameOver])

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (!gameStarted || gameOver || showQuiz) return

      switch (e.key) {
        case "ArrowUp":
          nextDirectionRef.current = { dx: 0, dy: -1 }
          e.preventDefault()
          break
        case "ArrowDown":
          nextDirectionRef.current = { dx: 0, dy: 1 }
          e.preventDefault()
          break
        case "ArrowLeft":
          nextDirectionRef.current = { dx: -1, dy: 0 }
          e.preventDefault()
          break
        case "ArrowRight":
          nextDirectionRef.current = { dx: 1, dy: 0 }
          e.preventDefault()
          break
      }
    }

    window.addEventListener("keydown", handleKeyDown)
    return () => window.removeEventListener("keydown", handleKeyDown)
  }, [gameStarted, gameOver, showQuiz])

  const isValidMove = (x: number, y: number) => {
    if (x < 0 || x >= GRID_WIDTH || y < 0 || y >= GRID_HEIGHT) return false
    return mapRef.current[y][x] !== 1
  }

  const isPositionOccupied = (x: number, y: number, currentEnemyIndex: number) => {
    return enemiesRef.current.some((enemy, index) => index !== currentEnemyIndex && enemy.x === x && enemy.y === y)
  }

  const moveEnemies = () => {
    const player = playerRef.current

    // Define the ghost box boundaries
    const boxLeft = 10
    const boxRight = 17
    const boxTop = 13
    const boxBottom = 16

    enemiesRef.current.forEach((enemy, enemyIndex) => {
      // Check if enemy should be released
      if (!enemy.released) {
        if (gameTimerRef.current >= enemy.releaseTime) {
          enemy.released = true
        } else {
          // Stay in box and move in a pattern
          const possibleDirs = [
            { dx: 1, dy: 0 },
            { dx: -1, dy: 0 },
            { dx: 0, dy: 1 },
            { dx: 0, dy: -1 },
          ].filter((dir) => {
            const newX = enemy.x + dir.dx
            const newY = enemy.y + dir.dy
            // Stay within box boundaries
            return (
              newX > boxLeft &&
              newX < boxRight &&
              newY > boxTop &&
              newY < boxBottom &&
              !isPositionOccupied(newX, newY, enemyIndex)
            )
          })

          if (possibleDirs.length > 0) {
            // Try to continue in current direction, otherwise pick random
            const continueDir = possibleDirs.find(
              (dir) => dir.dx === enemy.direction.dx && dir.dy === enemy.direction.dy,
            )
            if (continueDir) {
              enemy.direction = continueDir
            } else {
              enemy.direction = possibleDirs[Math.floor(Math.random() * possibleDirs.length)]
            }
            enemy.x += enemy.direction.dx
            enemy.y += enemy.direction.dy
          }
          return // Don't chase until released
        }
      }

      // Enemy is released - chase or flee from player
      const possibleDirs = [
        { dx: 0, dy: -1 },
        { dx: 0, dy: 1 },
        { dx: -1, dy: 0 },
        { dx: 1, dy: 0 },
      ].filter((dir) => {
        const newX = enemy.x + dir.dx
        const newY = enemy.y + dir.dy
        // Don't reverse direction unless necessary
        const isReverse = dir.dx === -enemy.direction.dx && dir.dy === -enemy.direction.dy
        return isValidMove(newX, newY) && !isPositionOccupied(newX, newY, enemyIndex) && !isReverse
      })

      // If no valid moves without reversing, allow reversing
      if (possibleDirs.length === 0) {
        const allDirs = [
          { dx: 0, dy: -1 },
          { dx: 0, dy: 1 },
          { dx: -1, dy: 0 },
          { dx: 1, dy: 0 },
        ].filter((dir) => {
          const newX = enemy.x + dir.dx
          const newY = enemy.y + dir.dy
          return isValidMove(newX, newY) && !isPositionOccupied(newX, newY, enemyIndex)
        })

        if (allDirs.length === 0) return // Stuck, don't move

        possibleDirs.push(...allDirs)
      }

      // Choose direction based on scared state
      let bestDir = possibleDirs[0]
      if (enemy.scared) {
        // Run away from player
        let maxDist = -1
        possibleDirs.forEach((dir) => {
          const newX = enemy.x + dir.dx
          const newY = enemy.y + dir.dy
          const dist = Math.abs(newX - player.x) + Math.abs(newY - player.y)
          if (dist > maxDist) {
            maxDist = dist
            bestDir = dir
          }
        })
      } else {
        // Chase player - always chase when released
        let minDist = Number.POSITIVE_INFINITY
        possibleDirs.forEach((dir) => {
          const newX = enemy.x + dir.dx
          const newY = enemy.y + dir.dy
          const dist = Math.abs(newX - player.x) + Math.abs(newY - player.y)
          if (dist < minDist) {
            minDist = dist
            bestDir = dir
          }
        })
      }

      enemy.direction = bestDir
      enemy.x += bestDir.dx
      enemy.y += bestDir.dy
    })
  }

  const checkCollisions = () => {
    const player = playerRef.current

    enemiesRef.current.forEach((enemy, index) => {
      // Check if player and enemy are on the same grid cell
      if (enemy.x === player.x && enemy.y === player.y) {
        if (enemy.scared) {
          // Eat the enemy
          setScore((s) => s + 200)
          // Reset enemy to box
          enemy.x = 13 + (index % 2)
          enemy.y = 13 + Math.floor(index / 2)
          enemy.scared = false
          enemy.released = false
          enemy.releaseTime = gameTimerRef.current + 20 // Release after 20 moves
        } else if (enemy.released) {
          // Only lose life if enemy is released
          setLives((l) => {
            const newLives = l - 1
            if (newLives <= 0) {
              setGameOver(true)
            }
            return newLives
          })
          // Reset positions
          playerRef.current = { x: 14, y: 23 }
          directionRef.current = { dx: 0, dy: 0 }
          nextDirectionRef.current = { dx: 0, dy: 0 }
          enemiesRef.current.forEach((e, i) => {
            e.x = 13 + (i % 2)
            e.y = 13 + Math.floor(i / 2)
            e.scared = false
            e.released = false
            e.releaseTime = i * 20 // Stagger releases: 0, 20, 40 moves
          })
          scaredTimerRef.current = 0
          gameTimerRef.current = 0
        }
      }
    })
  }

  const pauseGameForQuiz = () => {
  if (animationRef.current) {
    cancelAnimationFrame(animationRef.current)
  }

  const rawQuestion = getRandomQuestion()
  const formattedQuestion: Question = {
    id: crypto.randomUUID(),          // unique ID
    questionText: rawQuestion.question,
    options: rawQuestion.options,
    answer: rawQuestion.correctAnswer,
  }

  setCurrentQuestion(formattedQuestion)
  setShowQuiz(true)
}

  const handleQuizAnswer = (correct: boolean) => {
    setShowQuiz(false)

    if (correct) {
      setScore((s) => s + 50)
      // Activate power-up
      scaredTimerRef.current = 40 // 40 moves of scared time
      enemiesRef.current.forEach((enemy) => (enemy.scared = true))
    }

    // Resume game
    if (animationRef.current) {
      cancelAnimationFrame(animationRef.current)
    }
    let lastTime = performance.now()
    const gameSpeed = 150

    const gameLoop = (timestamp: number) => {
      if (timestamp - lastTime < gameSpeed) {
        animationRef.current = requestAnimationFrame(gameLoop)
        return
      }
      lastTime = timestamp

      gameTimerRef.current++

      if (scaredTimerRef.current > 0) {
        scaredTimerRef.current--
        if (scaredTimerRef.current === 0) {
          enemiesRef.current.forEach((enemy) => (enemy.scared = false))
        }
      }

      const player = playerRef.current
      const nextDir = nextDirectionRef.current
      const nextX = player.x + nextDir.dx
      const nextY = player.y + nextDir.dy

      if (isValidMove(nextX, nextY)) {
        directionRef.current = { ...nextDir }
      }

      const dir = directionRef.current
      const newX = player.x + dir.dx
      const newY = player.y + dir.dy

      if (newX < 0) {
        player.x = GRID_WIDTH - 1
      } else if (newX >= GRID_WIDTH) {
        player.x = 0
      } else if (newY < 0) {
        player.y = GRID_HEIGHT - 1
      } else if (newY >= GRID_HEIGHT) {
        player.y = 0
      } else if (isValidMove(newX, newY)) {
        player.x = newX
        player.y = newY

        if (mapRef.current[newY][newX] === 2) {
          mapRef.current[newY][newX] = 0
          setScore((s) => s + 10)

          if (checkWinCondition()) {
            setGameOver(true)
            return
          }
        }

        if (mapRef.current[newY][newX] === 3) {
          mapRef.current[newY][newX] = 0
          pauseGameForQuiz()
          return
        }
      }

      checkCollisions()
      moveEnemies()
      checkCollisions()

      const canvas = canvasRef.current
      if (canvas) {
        const ctx = canvas.getContext("2d")
        if (ctx) draw(ctx)
      }

      animationRef.current = requestAnimationFrame(gameLoop)
    }

    animationRef.current = requestAnimationFrame(gameLoop)
  }

  const draw = (ctx: CanvasRenderingContext2D) => {
    // Clear canvas
    ctx.fillStyle = "#000000"
    ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT)

    // Draw map
    for (let y = 0; y < GRID_HEIGHT; y++) {
      for (let x = 0; x < GRID_WIDTH; x++) {
        const cell = mapRef.current[y][x]

        if (cell === 1) {
          // Wall
          ctx.fillStyle = "#2121DE"
          ctx.fillRect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE)
        } else if (cell === 2) {
          // Dollar sign
          ctx.fillStyle = "#FFD700"
          ctx.font = "bold 12px Arial"
          ctx.textAlign = "center"
          ctx.textBaseline = "middle"
          ctx.fillText("$", x * CELL_SIZE + CELL_SIZE / 2, y * CELL_SIZE + CELL_SIZE / 2)
        } else if (cell === 3) {
          // Power-up
          ctx.fillStyle = "#00FF00"
          ctx.beginPath()
          ctx.arc(x * CELL_SIZE + CELL_SIZE / 2, y * CELL_SIZE + CELL_SIZE / 2, 6, 0, Math.PI * 2)
          ctx.fill()
        }
      }
    }

    // Draw player
    const player = playerRef.current
    const direction = directionRef.current
    const playerScreenX = player.x * CELL_SIZE + CELL_SIZE / 2
    const playerScreenY = player.y * CELL_SIZE + CELL_SIZE / 2

    ctx.save()
    ctx.translate(playerScreenX, playerScreenY)

    // Apply rotation/flip based on direction
    if (direction.dx > 0) {
      // Facing right - no rotation
      ctx.rotate(0)
    } else if (direction.dx < 0) {
      // Facing left - flip horizontally
      ctx.scale(-1, 1)
    } else if (direction.dy > 0) {
      // Facing down - rotate 90 degrees
      ctx.rotate(Math.PI / 2)
    } else if (direction.dy < 0) {
      // Facing up - rotate -90 degrees
      ctx.rotate(-Math.PI / 2)
    }

    // Draw Pac-Man mouth
    const mouthStart = 0.2 * Math.PI
    const mouthEnd = 1.8 * Math.PI
    ctx.fillStyle = "#FFFF00"
    ctx.beginPath()
    ctx.arc(0, 0, CELL_SIZE / 2 - 2, mouthStart, mouthEnd)
    ctx.lineTo(0, 0)
    ctx.fill()

    ctx.restore()

    // Draw enemies
    enemiesRef.current.forEach((enemy) => {
      ctx.fillStyle = enemy.scared ? "#0000FF" : enemy.color
      ctx.beginPath()
      ctx.arc(
        enemy.x * CELL_SIZE + CELL_SIZE / 2,
        enemy.y * CELL_SIZE + CELL_SIZE / 2,
        CELL_SIZE / 2 - 2,
        0,
        Math.PI * 2,
      )
      ctx.fill()

      // Draw eyes
      if (!enemy.scared) {
        ctx.fillStyle = "#FFFFFF"
        ctx.fillRect(enemy.x * CELL_SIZE + 6, enemy.y * CELL_SIZE + 6, 4, 6)
        ctx.fillRect(enemy.x * CELL_SIZE + 12, enemy.y * CELL_SIZE + 6, 4, 6)
        ctx.fillStyle = "#000000"
        ctx.fillRect(enemy.x * CELL_SIZE + 7, enemy.y * CELL_SIZE + 8, 2, 3)
        ctx.fillRect(enemy.x * CELL_SIZE + 13, enemy.y * CELL_SIZE + 8, 2, 3)
      }
    })
  }

  const startGame = () => {
    setGameStarted(true)
    setGameOver(false)
    setScore(0)
    setLives(3)
    playerRef.current = { x: 14, y: 23 }
    directionRef.current = { dx: 0, dy: 0 }
    nextDirectionRef.current = { dx: 0, dy: 0 }
    enemiesRef.current = [
      { x: 13, y: 13, direction: { dx: 1, dy: 0 }, color: "#FF0000", scared: false, releaseTime: 0, released: false },
      { x: 14, y: 13, direction: { dx: -1, dy: 0 }, color: "#FFB8FF", scared: false, releaseTime: 20, released: false },
      { x: 13, y: 14, direction: { dx: 1, dy: 0 }, color: "#00FFFF", scared: false, releaseTime: 40, released: false },
    ]
    mapRef.current = GAME_MAP.map((row) => [...row])
    scaredTimerRef.current = 0
    gameTimerRef.current = 0
  }

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center p-4 pb-25">
      <div className="max-w-4xl w-full space-y-6">
        <div className="text-center space-y-2">
          <h1 className="text-4xl font-bold text-foreground">Tariff Pac-Man</h1>
          <p className="text-muted-foreground">Collect dollar signs, answer tariff questions, and avoid the enemies!</p>
        </div>

        <Card className="p-6 space-y-4">
          <div className="flex justify-between items-center">
            <div className="text-lg font-semibold">Score: {score}</div>
            <div className="text-lg font-semibold">Lives: {"❤️".repeat(Math.max(0, lives || 0))}</div>
          </div>

          <div className="flex justify-center">
            <canvas
              ref={canvasRef}
              width={CANVAS_WIDTH}
              height={CANVAS_HEIGHT}
              className="border-2 border-border rounded-lg"
              style={{ maxWidth: "100%", height: "auto" }}
            />
          </div>

          {!gameStarted && !gameOver && (
            <div className="text-center space-y-4">
              <p className="text-sm text-muted-foreground">Use arrow keys to move. Collect all dollar signs to win!</p>
              <Button onClick={startGame} size="lg">
                Start Game
              </Button>
            </div>
          )}
        </Card>

        <div className="text-center text-sm text-muted-foreground space-y-1">
          <p>🟢 Green orbs trigger tariff quiz questions</p>
          <p>Answer correctly to make enemies scared and earn bonus points!</p>
        </div>
      </div>

      {showQuiz && currentQuestion && (
  <QuizModal
    question={{
      question: currentQuestion.questionText,
      type: "multiple-choice",          // or whatever type is appropriate
      correctAnswer: currentQuestion.answer,
      options: currentQuestion.options,
    }}
    onAnswer={handleQuizAnswer}
  />
)}

      {gameOver && (
      <GameOverModal
        score={score}
        won={lives > 0}
        onRestart={startGame}
        onClose={() => setGameOver(false)}
      />
    )}
    </div>
  )
}

// Question bank
function getRandomQuestion() {
  const questions = [
    {
      question: "A tariff is a tax on imported goods.",
      type: "true-false",
      correctAnswer: "true",
      options: ["True", "False"],
    },
    {
      question: "Tariffs are designed to protect domestic industries from foreign competition.",
      type: "true-false",
      correctAnswer: "true",
      options: ["True", "False"],
    },
    {
      question: "Lower tariffs always lead to lower consumer prices.",
      type: "true-false",
      correctAnswer: "false",
      options: ["True", "False"],
    },
    {
      question: "What is the primary purpose of a tariff?",
      type: "multiple-choice",
      correctAnswer: "To generate revenue and protect domestic industries",
      options: [
        "To generate revenue and protect domestic industries",
        "To increase imports",
        "To decrease exports",
        "To eliminate trade",
      ],
    },
    {
      question: "Which organization regulates international trade and tariffs?",
      type: "multiple-choice",
      correctAnswer: "World Trade Organization (WTO)",
      options: [
        "World Trade Organization (WTO)",
        "United Nations (UN)",
        "International Monetary Fund (IMF)",
        "World Bank",
      ],
    },
    {
      question: "A quota is the same as a tariff.",
      type: "true-false",
      correctAnswer: "false",
      options: ["True", "False"],
    },
    {
      question: "What is an ad valorem tariff?",
      type: "multiple-choice",
      correctAnswer: "A tariff based on a percentage of the value of goods",
      options: [
        "A tariff based on a percentage of the value of goods",
        "A fixed fee per unit",
        "A tariff on luxury goods only",
        "A seasonal tariff",
      ],
    },
    {
      question: "Retaliatory tariffs are imposed in response to another country's tariffs.",
      type: "true-false",
      correctAnswer: "true",
      options: ["True", "False"],
    },
    {
      question: "Free trade agreements eliminate all tariffs between member countries.",
      type: "true-false",
      correctAnswer: "false",
      options: ["True", "False"],
    },
    {
      question: "What is a specific tariff?",
      type: "multiple-choice",
      correctAnswer: "A fixed fee per unit of imported goods",
      options: [
        "A fixed fee per unit of imported goods",
        "A percentage-based tax",
        "A tariff on specific countries only",
        "A seasonal tariff",
      ],
    },
  ]

  return questions[Math.floor(Math.random() * questions.length)]
}