package com.example.test.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "game.db"
        private const val DATABASE_VERSION = 1

        // Table: Store
        const val TABLE_STORE = "store"
        const val COLUMN_STORE_ID = "id"
        const val COLUMN_BEST_SCORE = "best_score"
        const val COLUMN_LAST_SCORE = "last_score"
        const val COLUMN_CREATED_AT = "created_at"
        const val COLUMN_UPDATED_AT = "updated_at"

        // Table: Missions
        const val TABLE_MISSIONS = "missions"
        const val COLUMN_MISSION_ID = "id"
        const val COLUMN_DESCRIPTION = "description"
        const val COLUMN_COMPLETED = "completed"
        const val COLUMN_TYPE = "type"
        const val COLUMN_ID_PROGRESSION = "id_progression"
        const val COLUMN_REWARD = "reward"

        // Table: Progressions
        const val TABLE_PROGRESSIONS = "progressions"
        const val COLUMN_PROGRESSION_ID = "id"
        const val COLUMN_ID_MISSION = "id_mission"
        const val COLUMN_PROGRESS_NUMBER = "progression_number"

        // Table: Shop
        const val TABLE_SHOP = "shop"
        const val COLUMN_ITEM_ID = "item_id"
        const val COLUMN_ITEM_TYPE = "item_type"
        const val COLUMN_PRICE = "price"
        const val COLUMN_PURCHASE_DATE = "purchase_date"
        const val COLUMN_PAYMENT_METHOD = "payment_method"
        const val COLUMN_STATUS = "status"

        // Table: Skins
        const val TABLE_SKINS = "skins"
        const val COLUMN_SKIN_ID = "id"
        const val COLUMN_SKIN_NAME = "name"
        const val COLUMN_IMAGE_URL = "image_url"
        const val COLUMN_OWNED = "owned"
        const val COLUMN_SKIN_PRICE = "price"

        // Table: Game_Coins
        const val TABLE_GAME_COINS = "game_coins"
        const val COLUMN_COINS_ID = "id"
        const val COLUMN_COIN_AMOUNT = "coin_amount"
        const val COLUMN_COINS_PRICE = "price"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Table Store (Scores)
        val createStoreTableQuery = """
            CREATE TABLE $TABLE_STORE (
                $COLUMN_STORE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_BEST_SCORE NUMERIC NOT NULL,
                $COLUMN_LAST_SCORE NUMERIC NOT NULL,
                $COLUMN_CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        db.execSQL(createStoreTableQuery)

        // Table Missions
        val createMissionsTableQuery = """
            CREATE TABLE $TABLE_MISSIONS (
                $COLUMN_MISSION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DESCRIPTION TEXT NOT NULL,
                $COLUMN_COMPLETED BOOLEAN DEFAULT FALSE,
                $COLUMN_TYPE VARCHAR(50) CHECK ($COLUMN_TYPE IN ('Daily', 'General')),
                $COLUMN_ID_PROGRESSION INTEGER REFERENCES $TABLE_PROGRESSIONS($COLUMN_PROGRESSION_ID),
                $COLUMN_REWARD NUMERIC,
                $COLUMN_CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        db.execSQL(createMissionsTableQuery)

        // Table Progressions
        val createProgressionsTableQuery = """
            CREATE TABLE $TABLE_PROGRESSIONS (
                $COLUMN_PROGRESSION_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ID_MISSION INTEGER REFERENCES $TABLE_MISSIONS($COLUMN_MISSION_ID),
                $COLUMN_PROGRESS_NUMBER INTEGER NOT NULL,
                $COLUMN_CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        db.execSQL(createProgressionsTableQuery)

        // Table Shop
        val createShopTableQuery = """
            CREATE TABLE $TABLE_SHOP (
                $COLUMN_STORE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_ITEM_ID INTEGER NOT NULL,
                $COLUMN_ITEM_TYPE VARCHAR(50) CHECK ($COLUMN_ITEM_TYPE IN ('Skin', 'Coin')),
                $COLUMN_PRICE NUMERIC NOT NULL,
                $COLUMN_PURCHASE_DATE TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_PAYMENT_METHOD VARCHAR(50) CHECK ($COLUMN_PAYMENT_METHOD IN ('Coin', 'Real Money')),
                $COLUMN_STATUS VARCHAR(50) CHECK ($COLUMN_STATUS IN ('Completed', 'Failed')),
                $COLUMN_CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        db.execSQL(createShopTableQuery)

        // Table Skins
        val createSkinsTableQuery = """
            CREATE TABLE $TABLE_SKINS (
                $COLUMN_SKIN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_SKIN_NAME TEXT NOT NULL,
                $COLUMN_IMAGE_URL TEXT NOT NULL,
                $COLUMN_OWNED BOOLEAN DEFAULT FALSE,
                $COLUMN_SKIN_PRICE NUMERIC NOT NULL,
                $COLUMN_CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        db.execSQL(createSkinsTableQuery)

        // Table Game_Coins
        val createGameCoinsTableQuery = """
            CREATE TABLE $TABLE_GAME_COINS (
                $COLUMN_COINS_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_COIN_AMOUNT NUMERIC NOT NULL,
                $COLUMN_COINS_PRICE NUMERIC NOT NULL,
                $COLUMN_CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                $COLUMN_UPDATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()
        db.execSQL(createGameCoinsTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Drop older tables if they exist
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STORE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MISSIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROGRESSIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SHOP")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SKINS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_GAME_COINS")

        // Create tables again
        onCreate(db)
    }

    // Ajouter un score dans la table Store
    fun addScore(bestScore: Int, lastScore: Double): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_BEST_SCORE, bestScore)
            put(COLUMN_LAST_SCORE, lastScore)
        }
        return db.insert(TABLE_STORE, null, values)
    }

    // Récupérer les scores (meilleur et dernier score)
    fun getScores(): List<Pair<String, Int>> {
        val scoresList = mutableListOf<Pair<String, Int>>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_STORE, null, null, null, null, null,
            "$COLUMN_BEST_SCORE DESC" // Trier par score décroissant
        )

        cursor.use {
            while (it.moveToNext()) {
                val bestScore = it.getInt(it.getColumnIndexOrThrow(COLUMN_BEST_SCORE))
                val lastScore = it.getInt(it.getColumnIndexOrThrow(COLUMN_LAST_SCORE))
                scoresList.add("Best Score" to bestScore)
                scoresList.add("Last Score" to lastScore)
            }
        }
        return scoresList
    }

    // Supprimer tous les scores dans la table Store
    fun clearScores() {
        val db = writableDatabase
        db.delete(TABLE_STORE, null, null)
    }

    // Récupère le meilleur score
    fun getHighestScoreAsInt(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT MAX($COLUMN_BEST_SCORE) AS highest FROM $TABLE_STORE", null)
        var highestScore = 0
        cursor.use {
            if (it.moveToFirst()) {
                highestScore = it.getInt(it.getColumnIndexOrThrow("highest")) // "highest" is the alias used in the query
            }
        }
        return highestScore
    }

    // Récupère le dernier score enregistré
    fun getLastScoreAsInt(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_LAST_SCORE FROM $TABLE_STORE ORDER BY $COLUMN_STORE_ID DESC LIMIT 1", null)
        var lastScore = 0
        cursor.use {
            if (it.moveToFirst()) {
                lastScore = it.getInt(it.getColumnIndexOrThrow(COLUMN_LAST_SCORE))
            }
        }
        return lastScore
    }
}
