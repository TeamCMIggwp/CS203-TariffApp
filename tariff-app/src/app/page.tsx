"use client"

import WorldMap from "@/components/ui/world-map"
import LandingSection from "@/components/landing-section"
import CalculatorSection from "@/components/calculator-section"
import { motion, useScroll, useTransform } from 'framer-motion';

export default function TariffCalculator() {
  const { scrollY } = useScroll()

  const titleY = useTransform(scrollY, [0, 800], [0, -400])
  const titleOpacity = useTransform(scrollY, [0, 400], [1, 0])
  const mapOpacity = useTransform(scrollY, [0, 1600], [1, 0.2])
  const calculatorY = useTransform(scrollY, [400, 1200], [800, 0])

  return (
    <div className="h-screen w-screen overflow-y-auto overflow-x-hidden bg-white font-sans text-white relative">
      {/* World Map Background */}
      <div className="fixed top-0 left-0 w-full h-full z-0 pointer-events-none">
        <motion.div style={{ opacity: mapOpacity }} className="w-full h-full">
          <WorldMap
            dots={[
              { start: { lat: 40.7128, lng: -74.006 }, end: { lat: 39.9042, lng: 116.4074 } },
              { start: { lat: 51.5074, lng: -0.1278 }, end: { lat: 35.6762, lng: 139.6503 } },
              { start: { lat: -23.5505, lng: -46.6333 }, end: { lat: 52.52, lng: 13.405 } },
              { start: { lat: 28.6139, lng: 77.209 }, end: { lat: -33.8688, lng: 151.2093 } },
            ]}
          />
        </motion.div>
      </div>

      {/* Main content sections*/}
      <div className="relative z-10 h-screen min-h-screen flex items-center justify-center">
        <LandingSection titleY={titleY} titleOpacity={titleOpacity} />
      </div>

      <div className="relative z-10 min-h-screen">
        <CalculatorSection calculatorY={calculatorY} />
      </div>

    </div>
  );
}