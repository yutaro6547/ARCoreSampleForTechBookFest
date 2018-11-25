package zukkey.whiskey.com.arcoresamplefortechbookfest.kotlin

import android.content.Context
import android.widget.Toast
import com.google.ar.sceneform.utilities.Preconditions
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import timber.log.Timber


class FirebaseManager(context: Context) {
  interface RoomCodeListener {
    fun onNewRoomCode(newRoomCode: Long?)
    fun onError(databaseError: DatabaseError?)
  }

  interface CloudAnchorIdListener {
    fun onNewCloudAnchorId(cloudAnchorId: String)
    fun onSetMemo(memo: String, cloudAnchorId: String)
  }

  private val ROOT_FIREBASE_SPOTS = "spot_list"
  private val ROOT_LAST_ROOM_CODE = "last_room_code"
  private val KEY_ANCHOR_ID = "hosted_anchor_id"
  private val KEY_MEMO = "memo"


  private var app: FirebaseApp?
  private var hotspotListRef: DatabaseReference?
  private var roomCodeRef: DatabaseReference?
  private var currentRoomRef: DatabaseReference? = null
  private var currentRoomListener: ValueEventListener? = null

  // 初期化
  init {
    app = FirebaseApp.initializeApp(context)
    if (app != null) {
      val rootRef = FirebaseDatabase.getInstance(app!!).reference
      hotspotListRef = rootRef.child(ROOT_FIREBASE_SPOTS)
      roomCodeRef = rootRef.child(ROOT_LAST_ROOM_CODE)

      DatabaseReference.goOnline()
    } else {
      Timber.d("Could not connect to Firebase Database!")
      hotspotListRef = null
      roomCodeRef = null
    }
  }

  fun createNewRoom(newRoomCode: Long?, listener: RoomCodeListener) {
    Preconditions.checkNotNull(app!!, "Firebase App was null")
    if (newRoomCode == null) {
      Toast.makeText(app!!.applicationContext, "New Room Code is null.", Toast.LENGTH_SHORT).show()
      return
    }

    roomCodeRef!!.runTransaction(
        object : Transaction.Handler {
          override fun doTransaction(mutableData: MutableData): Transaction.Result {
            mutableData.value = newRoomCode
            return Transaction.success(mutableData)
          }

          override fun onComplete(databaseError: DatabaseError?, completed: Boolean, dataSnapshot: DataSnapshot?) {
            if (!completed) {
              listener.onError(databaseError)
              return
            }
            listener.onNewRoomCode(dataSnapshot!!.getValue(Long::class.java))
          }
        }
    )
  }

  fun storeAnchorIdInRoom(roomCode: Long?, cloudAnchorId: String, memo: String) {
    Preconditions.checkNotNull(app!!, "Firebase App was null")
    val roomRef = hotspotListRef!!.child(roomCode.toString())
    roomRef.child(KEY_ANCHOR_ID).setValue(cloudAnchorId)
    roomRef.child(KEY_MEMO).setValue(memo)
  }

  fun registerNewListenerForRoom(roomCode: Long?, listener: CloudAnchorIdListener) {
    Preconditions.checkNotNull(app!!, "Firebase App was null")
    clearRoomListener()
    currentRoomRef = hotspotListRef!!.child(roomCode.toString())
    currentRoomListener = object : ValueEventListener {
      override fun onDataChange(dataSnapshot: DataSnapshot) {
        val valObj = dataSnapshot.child(KEY_ANCHOR_ID).value
        val valMemo = dataSnapshot.child(KEY_MEMO).value
        if (valObj != null && valMemo != null) {
          val anchorId = valObj.toString()
          val memoText = valMemo.toString()
          if (!anchorId.isEmpty()) {
            listener.onNewCloudAnchorId(anchorId)
            listener.onSetMemo(memoText, anchorId)
          }
        }
      }

      override fun onCancelled(databaseError: DatabaseError) {
        Timber.e(databaseError.message, databaseError)
      }
    }
    currentRoomRef!!.addValueEventListener(currentRoomListener!!)
  }

  fun clearRoomListener() {
    if (currentRoomListener != null && currentRoomRef != null) {
      currentRoomRef!!.removeEventListener(currentRoomListener!!)
      currentRoomListener = null
      currentRoomRef = null
    }
  }
}
