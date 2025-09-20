"use client";

import { motion } from "framer-motion";

export default function HomePage() {
  return (
    <section className="home-section">
      <motion.div
        initial={{ opacity: 0, y: 50 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 1, delay: 0.5 }}
        className="text-center relative z-10"
      >
        <motion.h1
          initial={{ opacity: 0, y: 50 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1, delay: 0.5 }}
          className="home-title"
        >
          TARIFF
        </motion.h1>
        <motion.p
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 1, delay: 0.8 }}
          className="home-subtitle"
        >
          Trade Agreements Regulating Imports and Foreign Fees
        </motion.p>
      </motion.div>
    </section>
  );
}
