package zukkey.whiskey.com.arcoresamplefortechbookfest.kotlin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.widget.Button
import android.widget.Toast
import com.google.ar.core.HitResult
import com.google.ar.core.Plane
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import zukkey.whiskey.com.arcoresamplefortechbookfest.R
import zukkey.whiskey.com.arcoresamplefortechbookfest.java.ViewActivity
import zukkey.whiskey.com.arcoresamplefortechbookfest.java.WebActivity


class ViewActivity: AppCompatActivity() {
  private val CAMERA_PERMISSION_CODE = 0
  private val CAMERA_PERMISSION = Manifest.permission.CAMERA
  private var arFragment: ArFragment? = null
  private var imageViewRenderable: ViewRenderable? = null
  private var textViewRenderable: ViewRenderable? = null
  private var count: Int = 0

  fun createIntent(context: Context): Intent {
    return Intent(context, ViewActivity::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_view)
    arFragment = supportFragmentManager.findFragmentById(R.id.ux_fragment) as ArFragment?

    ViewRenderable.builder()
        .setView(this, R.layout.item_image)
        .build()
        .thenAccept { renderable -> imageViewRenderable = renderable }

    ViewRenderable.builder()
        .setView(this, R.layout.item_text)
        .build()
        .thenAccept { renderable -> textViewRenderable = renderable }

    arFragment!!.setOnTapArPlaneListener { hitResult: HitResult, plane: Plane, motionEvent: MotionEvent ->
      if (imageViewRenderable == null || textViewRenderable == null) {
        return@setOnTapArPlaneListener
      }

      val anchor = hitResult.createAnchor()
      val anchorNode = AnchorNode(anchor)
      anchorNode.setParent(arFragment!!.arSceneView.scene)

      if (count == 0) {
        val text = TransformableNode(arFragment!!.transformationSystem)
        text.setParent(anchorNode)
        text.renderable = textViewRenderable
        text.rotationController
        text.scaleController
        text.select()
        val btn = textViewRenderable!!.view as Button
        btn.setOnClickListener { view ->
          Toast.makeText(this, "ボタンをタップしました！詳細へ遷移します", Toast.LENGTH_SHORT).show()
          startActivity(WebActivity.createIntent(this))
          finish()
        }
      } else {
        val image = TransformableNode(arFragment!!.transformationSystem)
        image.setParent(anchorNode)
        image.renderable = imageViewRenderable
        image.rotationController
        image.scaleController
        image.translationController
        image.select()
      }
      count += 1
    }
  }


  override fun onResume() {
    super.onResume()
    if (ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, arrayOf(CAMERA_PERMISSION), CAMERA_PERMISSION_CODE)
    }
  }
}
