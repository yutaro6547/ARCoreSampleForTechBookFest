package zukkey.whiskey.com.arcoresamplefortechbookfest.kotlin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.TransformableNode
import com.google.firebase.database.DatabaseError

import timber.log.Timber
import zukkey.whiskey.com.arcoresamplefortechbookfest.R
import zukkey.whiskey.com.arcoresamplefortechbookfest.java.CloudAnchorFragment
import zukkey.whiskey.com.arcoresamplefortechbookfest.kotlin.FirebaseManager
import zukkey.whiskey.com.arcoresamplefortechbookfest.misc.AnchorState
import zukkey.whiskey.com.arcoresamplefortechbookfest.misc.EditTextDialog
import zukkey.whiskey.com.arcoresamplefortechbookfest.misc.SearchDialog

class CloudAnchorActivity : AppCompatActivity(), SearchDialog.PositiveButtonCallBack, EditTextDialog.EditTextPositiveButtonCallBack {

  private lateinit var arFragment: CloudAnchorFragment
  private var anchor: Anchor? = null
  private lateinit var anchorState: AnchorState
  private lateinit var firebaseManager: FirebaseManager
  private lateinit var memoViewRenderable: ViewRenderable
  private lateinit var inputCodeForm: EditText
  private lateinit var header: TextView
  private lateinit var progressBar: ProgressBar
  private var inputCode: Long = 0L
  private lateinit var inputText: EditText
  private lateinit var editorMemo: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_cloud_anchor)
    setUpArFragment()

    inputCodeForm = findViewById(R.id.room_code_edit)
    header = findViewById(R.id.room_header)
    progressBar = findViewById(R.id.progress_bar)
    editorMemo = ""

    firebaseManager = FirebaseManager(this)
    anchorState = AnchorState.None

    val clearButton = findViewById<Button>(R.id.clear_button)
    clearButton.setOnClickListener { view ->
      setCloudAnchor(null)
      anchorState = AnchorState.None
      header.setText(R.string.default_room)
    }

    val sendButton = findViewById<Button>(R.id.send_button)
    sendButton.setOnClickListener { view ->
      inputCode = java.lang.Long.parseLong(inputCodeForm.text.toString())
      inputCodeForm.text.clear()
      Toast.makeText(this, "Planeをタップしてください", Toast.LENGTH_SHORT).show()
    }

    val searchButton = findViewById<FloatingActionButton>(R.id.search_button)
    searchButton.setOnClickListener { view ->
      val dialog = SearchDialog()
      dialog.show(supportFragmentManager, "Search")
    }

    arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
      val newAnchor = arFragment.arSceneView.session.hostCloudAnchor(hitResult.createAnchor())
      setCloudAnchor(newAnchor)
      setUpRendering(newAnchor)
      anchorState = AnchorState.Hosting
      progressBar.visibility = View.VISIBLE
    }

  }

  fun setUpArFragment() {
    arFragment = supportFragmentManager.findFragmentById(R.id.ar_fragment) as CloudAnchorFragment
    arFragment.planeDiscoveryController.hide()
    arFragment.arSceneView.scene.addOnUpdateListener(Scene.OnUpdateListener { this.onUpdatingFrame(it) })
  }

  fun setCloudAnchor(newAnchor: Anchor?) {
    if (anchor != null) {
      anchor!!.detach()
    }
    anchor = newAnchor
    anchorState = AnchorState.None
  }

  fun setUpRendering(newAnchor: Anchor) {
    ViewRenderable.builder()
        .setView(this, R.layout.item_memo)
        .build()
        .thenAccept { renderable -> memoViewRenderable = renderable }

    if (memoViewRenderable == null) {
      return
    }

    val anchorNode = AnchorNode(newAnchor)
    anchorNode.setParent(arFragment.arSceneView.scene)
    val memo = TransformableNode(arFragment.transformationSystem)
    memo.setParent(anchorNode)
    memo.renderable = memoViewRenderable
    memo.rotationController
    memo.scaleController
    memo.translationController
    memo.select()

    inputText = memoViewRenderable.view.findViewById(R.id.memo_edit)
    if (editorMemo != "") {
      inputText.setText(editorMemo)
    }
    inputText.setOnClickListener { view ->
      val dialog = EditTextDialog()
      if (inputText.text != null || inputText.text.toString() != "") {
        dialog.setDefaultText(inputText.text.toString())
      }
      dialog.show(supportFragmentManager, "Edit")
    }
  }

  @Synchronized
  fun onUpdatingFrame(frameTime: FrameTime) {
    if (anchorState === AnchorState.Hosting) {
      Timber.i("Hosting......")
    }
    if (anchorState !== AnchorState.Hosting && anchorState !== AnchorState.Searching) {
      Timber.i("Hosting Failed")
      return
    }
    val state = anchor!!.cloudAnchorState
    if (anchorState === AnchorState.Hosting) {
      if (state.isError) {
        Toast.makeText(this, "Error hosting", Toast.LENGTH_SHORT).show()
        anchorState = AnchorState.None
        progressBar.visibility = View.GONE
      } else if (state == Anchor.CloudAnchorState.SUCCESS) {
        Toast.makeText(this, "Hosting is success!", Toast.LENGTH_SHORT).show()
        firebaseManager.createNewRoom(inputCode, object : FirebaseManager.RoomCodeListener {

          override fun onNewRoomCode(newRoomCode: Long?) {
            if (newRoomCode == null) {
              Toast.makeText(this@CloudAnchorActivity, "Room Code is null.", Toast.LENGTH_SHORT).show()
            }
            header.text = newRoomCode.toString()
            firebaseManager.storeAnchorIdInRoom(inputCode, anchor!!.cloudAnchorId, editorMemo)
            progressBar.visibility = View.GONE
          }

          override fun onError(databaseError: DatabaseError?) {
            Toast.makeText(this@CloudAnchorActivity, "Database error.", Toast.LENGTH_SHORT).show()
            Timber.e(databaseError!!.message, databaseError)
            header.setText(R.string.default_room)
            progressBar.visibility = View.GONE
          }
        })
        anchorState = AnchorState.Hosted
      }
    } else if (anchorState === AnchorState.Searching) {
      if (state.isError) {
        Toast.makeText(this, "Error Searching", Toast.LENGTH_SHORT).show()
        anchorState = AnchorState.None
      } else if (state == Anchor.CloudAnchorState.SUCCESS) {
        Toast.makeText(this, "Searched is success!", Toast.LENGTH_SHORT).show()
        anchorState = AnchorState.Searched
      }
    }
  }

  override fun onPositiveButtonClicked(roomCode: String) {
    firebaseManager.registerNewListenerForRoom(java.lang.Long.parseLong(roomCode), object : FirebaseManager.CloudAnchorIdListener {
      override fun onNewCloudAnchorId(cloudAnchorId: String) {
        header.text = roomCode
        val resolvedAnchor = arFragment.arSceneView.session.resolveCloudAnchor(cloudAnchorId)
        setCloudAnchor(resolvedAnchor)
        setUpRendering(resolvedAnchor)
        anchorState = AnchorState.Searching
      }

      override fun onSetMemo(memo: String, cloudAnchorId: String) {
        inputText.setText(memo)
      }
    })
  }

  override fun onEditTextPositiveButtonClicked(text: String) {
    editorMemo = text
    inputText.setText(editorMemo)
  }

  companion object {

    fun createIntent(context: Context): Intent {
      return Intent(context, CloudAnchorActivity::class.java)
    }
  }
}
