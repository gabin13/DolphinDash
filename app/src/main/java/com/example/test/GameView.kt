package com.example.test

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
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

    // Positions et mouvements du dauphin
    private var dolphinY = 0f
    private var velocity = 0f
    private var isTouching = false

    // Animation du dauphin
    private val dolphinFrames = arrayOf(
        R.drawable.frame1,
        R.drawable.frame2,
        R.drawable.frame3,
        R.drawable.frame4,
        R.drawable.frame5
    )
    private var currentFrame = 0
    private lateinit var dolphinBitmaps: List<Bitmap>
    private var lastFrameChangeTime = 0L
    private val frameChangeInterval = 100L  // en ms

    // Animation du requin
    private val sharkFrames = arrayOf(
        R.drawable.frame01_shark,
        R.drawable.frame02_shark,
        R.drawable.frame03_shark,
        R.drawable.frame04_shark,
        R.drawable.frame05_shark,
        R.drawable.frame06_shark,
        R.drawable.frame07_shark,
        R.drawable.frame08_shark,
        R.drawable.frame09_shark,
        R.drawable.frame10_shark
    )
    private lateinit var sharkBitmaps: List<Bitmap>

    // Image statique de danger pour le requin
    private lateinit var dangerBitmap: Bitmap

    // Score et temps
    private var score: Double = 0.0
    private var scoreUpdateListener: ((Double) -> Unit)? = null
    private var lastScoreUpdateTime: Long = System.currentTimeMillis()

    // Base de données
    private val dbHelper = DatabaseHelper(context)

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
        // Variables d'animation propres à chaque requin
        var currentFrame = 0
        var lastFrameChangeTime = 0L

        private val hitboxScale = 0.6f
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
                }
            } else {
                x -= scrollSpeed * 1.5f
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastFrameChangeTime >= frameChangeInterval) {
                    currentFrame = (currentFrame + 1) % sharkBitmaps.size
                    lastFrameChangeTime = currentTime
                }
            }
        }

        fun isOffScreen(): Boolean = x < -width
    }

    init {
        mediaPlayer = MediaPlayer.create(context, R.raw.background_music)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                initializeGame()
                startGame()
            }
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                screenWidth = width
                screenHeight = height
                initializeGame()
            }
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                stopGame()
                mediaPlayer?.stop()
                mediaPlayer?.release()
            }
        })
    }

    private fun initializeGame() {
        screenWidth = width
        screenHeight = height
        score = 0.0
        lastScoreUpdateTime = System.currentTimeMillis()

        // Calculer la taille du dauphin proportionnellement à l'écran
        dolphinWidth = (screenWidth * 0.05f).toInt()
        dolphinWidth = maxOf(dolphinWidth, minDolphinSize)
        dolphinHeight = dolphinWidth

        // Charger les frames pour l'animation du dauphin
        dolphinBitmaps = dolphinFrames.map { resId ->
            Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(resources, resId),
                dolphinWidth,
                dolphinHeight,
                false
            )
        }

        // Charger les bitmaps pour l'animation du requin
        val sharkSize = (dolphinWidth * 2).toInt()
        sharkBitmaps = sharkFrames.map { resId ->
            Bitmap.createScaledBitmap(
                BitmapFactory.decodeResource(resources, resId),
                sharkSize,
                sharkSize,
                false
            )
        }
        // Charger l'image de danger pour le requin
        dangerBitmap = Bitmap.createScaledBitmap(
            BitmapFactory.decodeResource(resources, R.drawable.ic_danger),
            sharkSize,
            sharkSize,
            false
        )

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
        val gravity = screenHeight * 0.003f
        velocity += gravity
        dolphinY += velocity

        if (dolphinY < 0) {
            dolphinY = 0f
            velocity = 0f
        }
        if (dolphinY > screenHeight - dolphinHeight) {
            dolphinY = (screenHeight - dolphinHeight).toFloat()
            velocity = 0f
        }

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
        sharks.add(
            Shark(
                x = screenWidth.toFloat(),
                y = y,
                width = sharkSize,
                height = sharkSize
            )
        )
    }

    private fun draw() {
        if (holder.surface.isValid) {
            val canvas = holder.lockCanvas()
            canvas.drawColor(Color.CYAN)

            // Dessiner les requins et leur état danger
            sharks.forEach { shark ->
                if (shark.showingDanger) {
                    canvas.drawBitmap(dangerBitmap, shark.x, shark.y, paint)
                } else {
                    canvas.drawBitmap(sharkBitmaps[shark.currentFrame], shark.x, shark.y, paint)
                }
            }

            // Mise à jour de l'animation du dauphin
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastFrameChangeTime >= frameChangeInterval) {
                currentFrame = (currentFrame + 1) % dolphinBitmaps.size
                lastFrameChangeTime = currentTime
            }

            val dolphinX = screenWidth * 0.15f
            canvas.drawBitmap(dolphinBitmaps[currentFrame], dolphinX, dolphinY, paint)

            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun checkCollisions() {
        val dolphinX = screenWidth * 0.15f
        val dolphinHitbox = RectF(
            dolphinX, dolphinY, dolphinX + dolphinWidth, dolphinY + dolphinHeight
        )
        sharks.forEach { shark ->
            if (!shark.showingDanger && RectF.intersects(dolphinHitbox, shark.getHitbox())) {
                isPlaying = false
                saveScore()
                showGameOverDialog()
                vibrateOnCollision()
            }
        }
    }

    private fun saveScore() {
        val scoreValue = score
        val playerName = "Player"
        var bestScore = dbHelper.getHighestScoreAsInt()
        val lastScore = score
        if(score > bestScore){
            bestScore = score.toInt()
        }
        dbHelper.addScore(bestScore, lastScore)
        Log.d("GameView", "Score saved: $playerName - $scoreValue")
        Log.d("GameView", "Best Score: $bestScore, Last Score: $lastScore")
        onGameEndListener?.invoke()
    }

    // Load vibration setting (called from GameView)
    private fun loadVibrationSetting(): Boolean {
        val sharedPreferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        val vibrationEnabled = sharedPreferences.getBoolean("vibration_enabled", true)  // Default to true if not set
        Log.d("GameView", "Vibration enabled: $vibrationEnabled")  // Debugging line
        return vibrationEnabled
    }
    private fun vibrateOnCollision() {
        if (loadVibrationSetting()) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator?.vibrate(vibrationEffect)
            } else {
                vibrator?.vibrate(500)
            }
        }
    }
//    private fun vibrateOnCollision2() {
//        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
//            vibrator?.vibrate(vibrationEffect)
//        } else {
//            vibrator?.vibrate(500)
//        }
//    }

    private fun showGameOverDialog() {
        pauseGame()
        post {
            AlertDialog.Builder(context)
                .setTitle("Game Over")
                .setNegativeButton("Quitter") { _, _ ->
                    val intent = Intent(context, MainActivity::class.java)
                    intent.putExtra("lastScore", score)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    (context as? GameActivity)?.finish()
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
        mediaPlayer?.pause()
    }

    fun resumeGame() {
        if (!isPlaying) {
            lastScoreUpdateTime = System.currentTimeMillis()
            isPlaying = true
            thread = Thread(this)
            thread?.start()
        }
        mediaPlayer?.start()
    }

    private var onGameEndListener: (() -> Unit)? = null
    fun setOnGameEndListener(listener: () -> Unit) {
        onGameEndListener = listener
    }
}
