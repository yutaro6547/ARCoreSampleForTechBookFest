package zukkey.whiskey.com.arcoresamplefortechbookfest.kotlin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.widget.Button
import android.widget.Toast
import com.google.ar.core.*
import com.google.ar.core.exceptions.UnavailableApkTooOldException
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException
import com.google.ar.core.exceptions.UnavailableSdkTooOldException
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.ArSceneView
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import zukkey.whiskey.com.arcoresamplefortechbookfest.R
import zukkey.whiskey.com.arcoresamplefortechbookfest.java.AugmentedImageActivity
import zukkey.whiskey.com.arcoresamplefortechbookfest.java.WebActivity
import java.io.IOException
import java.io.InputStream


class AugmentedImageActivity: AppCompatActivity() {
  private var textViewRenderable: ViewRenderable? = null
  private var arFragment: ArFragment? = null
  private var session: Session? = null
  private var sessionConfigured: Boolean = false
  private var arSceneView: ArSceneView? = null
  private var isAttachedModel: Boolean = false

  fun createIntent(context: Context): Intent {
    return Intent(context, AugmentedImageActivity::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_augumented_image)
    arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as ArFragment?

    if (arFragment != null) {
      // デフォルトだとPlaneの検出が始まるが、Planeを検出しない
      arFragment!!.planeDiscoveryController.hide()
      arFragment!!.planeDiscoveryController.setInstructionView(null)
      arSceneView = arFragment!!.arSceneView

      arFragment!!.arSceneView.scene.addOnUpdateListener { frameTime ->
        val frame = arFragment!!.arSceneView.arFrame
        val updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage::class.java)

        for (img in updatedAugmentedImages) {
          if (img.trackingState == TrackingState.TRACKING) {
            if (img.name.contains("eure") && !isAttachedModel) {
              setUp3DModel(img.createAnchor(img.centerPose))
            }
          }
        }
      }
    }

  }

  override fun onResume() {
    super.onResume()
    if (session == null) {
      try {
        session = Session(this)
      } catch (e: UnavailableArcoreNotInstalledException) {
        e.printStackTrace()
      } catch (e: UnavailableApkTooOldException) {
        e.printStackTrace()
      } catch (e: UnavailableSdkTooOldException) {
        e.printStackTrace()
      }

      sessionConfigured = true
    }

    if (sessionConfigured) {
      configureSession()
      sessionConfigured = false
    }
  }

  private fun setUp3DModel(anchor: Anchor) {
    ViewRenderable.builder()
        .setView(this, R.layout.item_text)
        .build()
        .thenAccept { renderable -> textViewRenderable = renderable }
        .exceptionally { throwable ->
          val toast = Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG)
          toast.setGravity(Gravity.CENTER, 0, 0)
          toast.show()
          null
        }

    if (textViewRenderable == null) {
      return
    }

    val anchorNode = AnchorNode(anchor)
    anchorNode.setParent(arFragment!!.arSceneView.scene)

    val text = TransformableNode(arFragment!!.transformationSystem)
    text.setParent(anchorNode)
    text.renderable = textViewRenderable
    text.rotationController
    text.scaleController
    text.select()
    text.localRotation = Quaternion.lookRotation(Vector3.down(), Vector3.up())
    val btn = textViewRenderable!!.view as Button
    btn.setOnClickListener { view ->
      Toast.makeText(this, "ボタンをタップしました！詳細へ遷移します", Toast.LENGTH_SHORT).show()
      startActivity(WebActivity.createIntent(this))
      finish()
    }
    isAttachedModel = true
  }

  private fun configureSession() {
    val config = Config(session!!)
    val inputStream: InputStream
    try {
      inputStream = assets.open("sample_database.imgdb")
      val imageDatabase = AugmentedImageDatabase.deserialize(session!!, inputStream)
      config.augmentedImageDatabase = imageDatabase
      config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
      session!!.configure(config)
    } catch (e: IOException) {
      Toast.makeText(this, "Don't configure Session", Toast.LENGTH_SHORT).show()
      e.printStackTrace()
    }

    arSceneView!!.setupSession(session)
  }
}
