"use client"

import { motion, type MotionValue } from "framer-motion"

interface LandingSectionProps {
  titleY: MotionValue<number>
  titleOpacity: MotionValue<number>
}

export default function LandingSection({ titleY, titleOpacity }: LandingSectionProps) {
  return (
    <section className="landing-section">
      <motion.div style={{ y: titleY, opacity: titleOpacity }} className="text-center relative z-10">
        <motion.h1
          initial={{ opacity: 0, y: 50 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1, delay: 0.5 }}
          className="landing-title"
        >
          Tariff Calculator
        </motion.h1>
        <motion.p
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1, delay: 0.8 }}
          className="landing-subtitle"
        >
          Calculate import tariffs and fees for agricultural products between countries
        </motion.p>
      </motion.div>
    </section>
  )
}