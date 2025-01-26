package com.example.test

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.example.test.database.DatabaseHelper


class GameView(context: Context) : SurfaceView(context), Runnable {

    private var thread: Thread? = null
    private var isPlaying = true
    private val paint = Paint()

    // Dimensions de l'écran
    private var screenWidth = 0
    private var screenHeight = 0
    private var dolphinWidth = 0
    private var dolphinHeight = 0

    // Positions et mouvements
    private var dolphinY = 0f
    private var velocity = 0f
    private var isTouching = false

    // Score et temps
    private var score: Double = 0.0
    private var scoreUpdateListener: ((Double) -> Unit)? = null
    private var lastScoreUpdateTime: Long = System.currentTimeMillis()

    // Base de données
    private val dbHelper = DatabaseHelper(context) // Instanciation de l'helper SQLite

    // Bitmaps
    private lateinit var dolphinBitmap: Bitmap
    private lateinit var sharkBitmap: Bitmap
    private lateinit var dangerBitmap: Bitmap

    // Requins
    private val sharks = mutableListOf<Shark>()
    private val minSharkSpawnTime = 1000L
    private val maxSharkSpawnTime = 2000L
    private var lastSharkSpawnTime = 0L
    private var nextSharkSpawnTime = 0L

    // Taille minimale du dauphin
    private val minDolphinSize = resources.getDimensionPixelSize(R.dimen.game_dolphin_min_size)

    // Musique de fond
    private var mediaPlayer: MediaPlayer? = null

    private inner class Shark(
        var x: Float,
        var y: Float,
        val width: Int,
        val height: Int,
        var showingDanger: Boolean = true,
        var dangerTimer: Long = System.currentTimeMillis(),
        val dangerDuration: Long = 1000
    ) {
        private val hitboxScale = 0f  // Hitbox réduite à 60% de la taille
        private val hitboxOffsetX = (width * (1 - hitboxScale) / 2)
        private val hitboxOffsetY = (height * (1 - hitboxScale) / 2)
        private val hitboxWidth = width * hitboxScale
        private val hitboxHeight = height * hitboxScale

        fun getHitbox(): RectF {
            return RectF(
                x + hitboxOffsetX,
                y + hitboxOffsetY,
                x + hitboxOffsetX + hitboxWidth,
                y + hitboxOffsetY + hitboxHeight
            )
        }

        fun update(scrollSpeed: Float) {
            if (showingDanger) {
                if (System.currentTimeMillis() - dangerTimer >= dangerDuration) {
                    showingDanger = false
                    Log.d("Shark", "Danger masqué après ${dangerDuration}ms")
                }
            } else {
                x -= scrollSpeed * 1.5f
            }
        }


        fun isOffScreen(): Boolean = x < -width
    }

    init {
        // Charger les images originales
        val originalDolphinBitmap = BitmapFactory.decodeResource(resources, R.drawable.dolphin)
        val originalSharkBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_shark)
        val originalDangerBitmap = BitmapFactory.decodeResource(resources, R.drawable.ic_danger)

        // Charger la musique de fond
        mediaPlayer = MediaPlayer.create(context, R.raw.background_music)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                initializeGame(originalDolphinBitmap, originalSharkBitmap, originalDangerBitmap)
                startGame()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                screenWidth = width
                screenHeight = height
                initializeGame(originalDolphinBitmap, originalSharkBitmap, originalDangerBitmap)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopGame()
                // Libérer les ressources de la musique
                mediaPlayer?.stop()
                mediaPlayer?.release()
            }
        })
    }

    private fun initializeGame(
        originalDolphinBitmap: Bitmap,
        originalSharkBitmap: Bitmap,
        originalDangerBitmap: Bitmap
    ) {
        screenWidth = width
        screenHeight = height
        score = 0.0
        lastScoreUpdateTime = System.currentTimeMillis()

        // Calculer la taille du dauphin proportionnellement à l'écran
        dolphinWidth = (screenWidth * 0.05f).toInt()
        dolphinWidth = maxOf(dolphinWidth, minDolphinSize)
        dolphinHeight = dolphinWidth

        // Redimensionner les bitmaps
        dolphinBitmap = Bitmap.createScaledBitmap(originalDolphinBitmap, dolphinWidth, dolphinHeight, false)

        val sharkSize = (dolphinWidth * 2).toInt()
        sharkBitmap = Bitmap.createScaledBitmap(originalSharkBitmap, sharkSize, sharkSize, false)
        dangerBitmap = Bitmap.createScaledBitmap(originalDangerBitmap, sharkSize, sharkSize, false)

        // Position initiale du dauphin
        dolphinY = screenHeight / 2f - dolphinHeight / 2f

        // Réinitialiser les requins
        sharks.clear()
        lastSharkSpawnTime = System.currentTimeMillis()
        nextSharkSpawnTime = generateNextSpawnTime()
    }

    private fun generateNextSpawnTime(): Long {
        return minSharkSpawnTime + (Math.random() * (maxSharkSpawnTime - minSharkSpawnTime)).toLong()
    }

    private fun update() {
        if (isTouching) {
            velocity = -(screenHeight * 0.02f)
        }

        // Gravité adaptée à la taille de l'écran
        val gravity = screenHeight * 0.003f
        velocity += gravity
        dolphinY += velocity

        // Limites de l'écran
        if (dolphinY < 0) {
            dolphinY = 0f
            velocity = 0f
        }
        if (dolphinY > screenHeight - dolphinHeight) {
            dolphinY = (screenHeight - dolphinHeight).toFloat()
            velocity = 0f
        }

        // Gestion des requins
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastSharkSpawnTime >= nextSharkSpawnTime) {
            spawnShark()
            lastSharkSpawnTime = currentTime
            nextSharkSpawnTime = generateNextSpawnTime()
        }

        sharks.forEach { it.update(screenWidth * 0.005f) }
        sharks.removeAll { it.isOffScreen() }

        checkCollisions()
    }

    private fun spawnShark() {
        val sharkSize = dolphinWidth
        val y = (Math.random() * (screenHeight - sharkSize)).toFloat()
        sharks.add(Shark(
            x = screenWidth.toFloat(),
            y = y,
            width = sharkSize,
            height = sharkSize
        ))
    }

    private fun draw() {
        if (holder.surface.isValid) {
            val canvas = holder.lockCanvas()
            canvas.drawColor(Color.CYAN)

            // Dessiner les requins et les dangers
            sharks.forEach { shark ->
                if (shark.showingDanger) {
                    canvas.drawBitmap(dangerBitmap, shark.x, shark.y, paint)
                } else {
                    canvas.drawBitmap(sharkBitmap, shark.x, shark.y, paint)
                }
            }

            // Position du dauphin
            val dolphinX = screenWidth * 0.15f
            canvas.drawBitmap(dolphinBitmap, dolphinX, dolphinY, paint)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun saveScore() {
        val playerName = "Player"
        dbHelper.addScore(playerName, score.toInt())
        Log.d("GameView", "Score sauvegardé: $playerName - $score")
        onGameEndListener?.invoke() // Notifie GameActivity
    }


    private fun checkCollisions() {
        val dolphinX = screenWidth * 0.15f
        val dolphinHitbox = RectF(
            dolphinX, dolphinY, dolphinX + dolphinWidth, dolphinY + dolphinHeight
        )

        sharks.forEach { shark ->
            if (!shark.showingDanger && RectF.intersects(dolphinHitbox, shark.getHitbox())) {
                isPlaying = false
                saveScore() // Sauvegarde lors de la collision
                showGameOverDialog()

                // Vibration lors de la collision
                vibrateOnCollision()
            }
        }
    }

    private fun vibrateOnCollision() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

        // Si l'API est compatible (Android 26+), utiliser la vibration avec un effet personnalisé
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator?.vibrate(vibrationEffect)
        } else {
            // Pour les versions inférieures à Android 26, utiliser la méthode classique
            vibrator?.vibrate(500)
        }
    }

    private fun showGameOverDialog() {
        pauseGame()

        post {
            AlertDialog.Builder(context)
                .setTitle("Game Over")
                .setNegativeButton("Quitter") { _, _ ->
                    // Lancer MainActivity avec le score actuel
                    val intent = Intent(context, MainActivity::class.java)
                    intent.putExtra("lastScore", score) // Ajouter le dernier score
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    (context as? GameActivity)?.finish() // Fermer GameActivity
                }
                .setCancelable(false)
                .show()
        }
    }



    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> isTouching = true
            MotionEvent.ACTION_UP -> isTouching = false
        }
        return true
    }

    fun setScoreUpdateListener(listener: (Double) -> Unit) {
        scoreUpdateListener = listener
        listener.invoke(score)
    }

    private fun updateScore() {
        if (isPlaying) {
            val currentTime = System.currentTimeMillis()
            val deltaTime = currentTime - lastScoreUpdateTime
            score += deltaTime / 100.0
            scoreUpdateListener?.invoke(score)
            lastScoreUpdateTime = currentTime
        }
    }

    override fun run() {
        while (isPlaying) {
            update()
            updateScore()
            draw()
            Thread.sleep(16)
        }
    }

    private fun startGame() {
        isPlaying = true
        lastScoreUpdateTime = System.currentTimeMillis()
        thread = Thread(this)
        thread?.start()
    }

    private fun stopGame() {
        isPlaying = false
        try {
            thread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun pauseGame() {
        isPlaying = false
        try {
            thread?.join(500)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        mediaPlayer?.pause()  // Pause la musique
    }

    fun resumeGame() {
        if (!isPlaying) {
            lastScoreUpdateTime = System.currentTimeMillis()
            isPlaying = true
            thread = Thread(this)
            thread?.start()
        }
        mediaPlayer?.start()  // Reprend la musique
    }

    private var onGameEndListener: (() -> Unit)? = null

    fun setOnGameEndListener(listener: () -> Unit) {
        onGameEndListener = listener
    }


}
