import mysql from "mysql2/promise";

// Connection for Accounts DB
export const accountsDb = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASS,
  database: process.env.DB_NAME1,
  port: Number(process.env.DB_PORT) || 3306,
});

// Connection for wto_tariffs DB
export const tariffDb = mysql.createPool({
  host: process.env.DB_HOST,
  user: process.env.DB_USER,
  password: process.env.DB_PASSWORD,
  database: process.env.DB_NAME2,
  port: Number(process.env.DB_PORT) || 3306,
});
