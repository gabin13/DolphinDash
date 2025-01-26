package com.example.test.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "game_scores.db"
        private const val DATABASE_VERSION = 1

        // Table des scores
        const val TABLE_SCORES = "scores"
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"
        const val COLUMN_SCORE = "score"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Création de la table "scores"
        val createTableQuery = """
            CREATE TABLE $TABLE_SCORES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_SCORE INTEGER NOT NULL
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Supprime et recrée la table si la base est mise à jour
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SCORES")
        onCreate(db)
    }

    // Ajouter un score dans la base
    fun addScore(name: String, score: Int): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_SCORE, score)
        }
        return db.insert(TABLE_SCORES, null, values)
    }

    // Récupérer les scores triés par ordre décroissant
    fun getScores(): List<Pair<String, Int>> {
        val scoresList = mutableListOf<Pair<String, Int>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_SCORES, null, null, null, null, null,
            "$COLUMN_SCORE DESC" // Trier par score décroissant
        )
        cursor.use {
            while (it.moveToNext()) {
                val name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME))
                val score = it.getInt(it.getColumnIndexOrThrow(COLUMN_SCORE))
                scoresList.add(name to score)
            }
        }
        return scoresList
    }

    // Supprimer tous les scores
    fun clearScores() {
        val db = writableDatabase
        db.delete(TABLE_SCORES, null, null)
    }

    // Récupère le meilleur score
    fun getHighestScoreAsInt(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT MAX($COLUMN_SCORE) AS highest FROM $TABLE_SCORES", null)
        var highestScore = 0
        cursor.use {
            if (it.moveToFirst()) {
                highestScore = it.getInt(it.getColumnIndexOrThrow("highest"))
            }
        }
        return highestScore
    }

    // Récupère le dernier score enregistré
    fun getLastScoreAsInt(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_SCORE FROM $TABLE_SCORES ORDER BY $COLUMN_ID DESC LIMIT 1", null)
        var lastScore = 0
        cursor.use {
            if (it.moveToFirst()) {
                lastScore = it.getInt(it.getColumnIndexOrThrow(COLUMN_SCORE))
            }
        }
        return lastScore
    }
}