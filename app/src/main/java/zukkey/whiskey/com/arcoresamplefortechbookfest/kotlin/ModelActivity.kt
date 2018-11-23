package zukkey.whiskey.com.arcoresamplefortechbookfest.kotlin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Gravity
import android.view.MotionEvent
import android.widget.Toast
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import zukkey.whiskey.com.arcoresamplefortechbookfest.R
import zukkey.whiskey.com.arcoresamplefortechbookfest.java.ModelActivity


class ModelActivity: AppCompatActivity() {
  private val CAMERA_PERMISSION_CODE = 0
  private val CAMERA_PERMISSION = Manifest.permission.CAMERA

  private var arFragment: ArFragment? = null
  private var modelRenderable: ModelRenderable? = null

  fun createIntent(context: Context): Intent {
    return Intent(context, ModelActivity::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_model)
    arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?

    ModelRenderable.builder()
        .setSource(this, R.raw.rin)
        .build()
        .thenAccept { renderable -> modelRenderable = renderable }
        .exceptionally { throwable ->
          val toast = Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG)
          toast.setGravity(Gravity.CENTER, 0, 0)
          toast.show()
          null
        }

    arFragment!!.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
      if (modelRenderable == null) {
        return@setOnTapArPlaneListener
      }

      val anchor = hitResult.createAnchor()
      val anchorNode = AnchorNode(anchor)
      anchorNode.setParent(arFragment!!.arSceneView.scene)

      val model = TransformableNode(arFragment!!.transformationSystem)
      model.setParent(anchorNode)
      model.renderable = modelRenderable
      model.rotationController
      model.scaleController
      model.translationController
      model.select()
    }
  }

  override fun onResume() {
    super.onResume()
    if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE)
    }
  }
}
