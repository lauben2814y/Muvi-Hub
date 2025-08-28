package muvi.anime.hub.managers

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.LinearProgressIndicator
import muvi.anime.hub.R

class SyncDialog(private val context: Context) {

    enum class SyncStatus {
        SYNCING, SUCCESS, ERROR
    }

    private var dialog: Dialog? = null
    private var currentStatus = SyncStatus.SYNCING
    private var progress = 0
    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null
    private var autoDismissRunnable: Runnable? = null

    // Views
    private lateinit var ivSyncIcon: ImageView
    private lateinit var btnClose: ImageButton
    private lateinit var tvSyncMessage: TextView
    private lateinit var layoutProgress: LinearLayout
    private lateinit var tvProgressPercent: TextView
    private lateinit var progressSync: LinearProgressIndicator
    private lateinit var cardStatus: MaterialCardView
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvStatusMessage: TextView
    private lateinit var layoutButtons: LinearLayout
    private lateinit var btnRetry: MaterialButton
    private lateinit var btnDismiss: MaterialButton

    // Callbacks
    var onRetryCallback: (() -> Unit)? = null
    var onDismissCallback: (() -> Unit)? = null

    fun show() {
        if (dialog?.isShowing == true) return

        dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val view = LayoutInflater.from(context).inflate(R.layout.dialog_sync, null)
            setContentView(view)

            // Make dialog width responsive
            window?.setLayout(
                (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            setCancelable(false)
            setCanceledOnTouchOutside(false)
        }

        initViews()
        setupListeners()
        updateUI()

        dialog?.show()

        // Start progress animation
        if (currentStatus == SyncStatus.SYNCING) {
            startProgressAnimation()
        }
    }

    // NEW METHOD: Position dialog at percentage from top of screen
    fun setDialogPositionPercentage(verticalPercent: Float) {
        dialog?.window?.let { window ->
            val displayMetrics = context.resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels
            val yOffset = (screenHeight * verticalPercent).toInt()

            window.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            val layoutParams = window.attributes
            layoutParams.y = yOffset
            window.attributes = layoutParams
        }
    }

    fun dismiss() {
        stopProgressAnimation()
        stopAutoDismiss()

        // Check if dialog is still valid and attached
        try {
            if (dialog?.isShowing == true) {
                dialog?.dismiss()
            }
        } catch (e: Exception) {
            // Ignore exceptions when window is already detached
        }

        dialog = null
        onDismissCallback?.invoke()
    }

    fun updateStatus(status: SyncStatus, message: String? = null) {
        currentStatus = status

        // Check if dialog and views are initialized
        if (dialog?.isShowing != true || !::tvSyncMessage.isInitialized) {
            // If views aren't ready, store the status and message to apply later
            pendingStatusUpdate = Pair(status, message)
            return
        }

        applyStatusUpdate(status, message)
    }

    private var pendingStatusUpdate: Pair<SyncStatus, String?>? = null

    private fun applyStatusUpdate(status: SyncStatus, message: String?) {
        when (status) {
            SyncStatus.SYNCING -> {
                tvSyncMessage.text = message ?: "Synchronizing with database..."
                startProgressAnimation()
            }
            SyncStatus.SUCCESS -> {
                tvSyncMessage.text = message ?: "Successfully synced with database!"
                stopProgressAnimation()
                progress = 100
                // Don't auto dismiss - let the caller handle it
            }
            SyncStatus.ERROR -> {
                tvSyncMessage.text = message ?: "Failed to sync with database. Please try again."
                stopProgressAnimation()
            }
        }

        updateUI()
    }

    // New method to manually dismiss after showing success
    fun dismissAfterDelay(delayMs: Long = 2000) {
        stopAutoDismiss() // Cancel any existing auto dismiss

        autoDismissRunnable = Runnable {
            dismiss()
        }
        handler.postDelayed(autoDismissRunnable!!, delayMs)
    }

    private fun initViews() {
        dialog?.let { d ->
            ivSyncIcon = d.findViewById(R.id.iv_sync_icon)
            btnClose = d.findViewById(R.id.btn_close)
            tvSyncMessage = d.findViewById(R.id.tv_sync_message)
            layoutProgress = d.findViewById(R.id.layout_progress)
            tvProgressPercent = d.findViewById(R.id.tv_progress_percent)
            progressSync = d.findViewById(R.id.progress_sync)
            cardStatus = d.findViewById(R.id.card_status)
            ivStatusIcon = d.findViewById(R.id.iv_status_icon)
            tvStatusMessage = d.findViewById(R.id.tv_status_message)
            layoutButtons = d.findViewById(R.id.layout_buttons)
            btnRetry = d.findViewById(R.id.btn_retry)
            btnDismiss = d.findViewById(R.id.btn_dismiss)

            // Apply any pending status update
            pendingStatusUpdate?.let { (status, message) ->
                applyStatusUpdate(status, message)
                pendingStatusUpdate = null
            }
        }
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { dismiss() }
        btnRetry.setOnClickListener {
            onRetryCallback?.invoke()
            dismiss()
        }
        btnDismiss.setOnClickListener { dismiss() }
    }

    private fun updateUI() {
        when (currentStatus) {
            SyncStatus.SYNCING -> {
                // Show sync icon with rotation animation
                ivSyncIcon.setImageResource(R.drawable.autorenew_24px)
                ivSyncIcon.animate().rotationBy(360f).setDuration(1000).withEndAction {
                    if (currentStatus == SyncStatus.SYNCING) {
                        ivSyncIcon.animate().rotationBy(360f).setDuration(1000).withEndAction {
                            updateUI() // Continue rotation
                        }
                    }
                }

                btnClose.visibility = View.GONE
                layoutProgress.visibility = View.VISIBLE
                cardStatus.visibility = View.GONE
                layoutButtons.visibility = View.GONE

                progressSync.progress = progress
                tvProgressPercent.text = "$progress%"
            }

            SyncStatus.SUCCESS -> {
                ivSyncIcon.clearAnimation()
                ivSyncIcon.setImageResource(R.drawable.check_circle_24px)
                ivSyncIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.success_color)

                btnClose.visibility = View.VISIBLE
                layoutProgress.visibility = View.GONE
                cardStatus.visibility = View.VISIBLE
                layoutButtons.visibility = View.GONE

                // Update status card for success
                cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.success_background))
                cardStatus.strokeColor = ContextCompat.getColor(context, R.color.success_border)
                ivStatusIcon.setImageResource(R.drawable.check_circle_24px)
                ivStatusIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.success_color)
                tvStatusMessage.text = "Data synchronized successfully"
                tvStatusMessage.setTextColor(ContextCompat.getColor(context, R.color.success_text))
            }

            SyncStatus.ERROR -> {
                ivSyncIcon.clearAnimation()
                ivSyncIcon.setImageResource(R.drawable.error_24px)
                ivSyncIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.error_color)

                btnClose.visibility = View.VISIBLE
                layoutProgress.visibility = View.GONE
                cardStatus.visibility = View.VISIBLE
                layoutButtons.visibility = View.VISIBLE

                // Update status card for error
                cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.error_background))
                cardStatus.strokeColor = ContextCompat.getColor(context, R.color.error_border)
                ivStatusIcon.setImageResource(R.drawable.error_24px)
                ivStatusIcon.imageTintList = ContextCompat.getColorStateList(context, R.color.error_color)
                tvStatusMessage.text = "Sync failed"
                tvStatusMessage.setTextColor(ContextCompat.getColor(context, R.color.error_text))
            }
        }
    }

    private fun startProgressAnimation() {
        stopProgressAnimation() // Stop any existing animation

        progressRunnable = object : Runnable {
            override fun run() {
                if (currentStatus == SyncStatus.SYNCING && progress < 100) {
                    progress += 10
                    progressSync.progress = progress
                    tvProgressPercent.text = "$progress%"

                    if (progress < 100) {
                        handler.postDelayed(this, 300)
                    } else {
                        // Simulate completion
                        updateStatus(SyncStatus.SUCCESS)
                    }
                }
            }
        }

        handler.post(progressRunnable!!)
    }

    private fun stopProgressAnimation() {
        progressRunnable?.let { handler.removeCallbacks(it) }
        progressRunnable = null
    }

    private fun stopAutoDismiss() {
        autoDismissRunnable?.let { handler.removeCallbacks(it) }
        autoDismissRunnable = null
    }
}

// Extension function for easier usage
fun Context.showSyncDialog(
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
): SyncDialog {
    return SyncDialog(this).apply {
        onRetryCallback = onRetry
        onDismissCallback = onDismiss
        show()
    }
}