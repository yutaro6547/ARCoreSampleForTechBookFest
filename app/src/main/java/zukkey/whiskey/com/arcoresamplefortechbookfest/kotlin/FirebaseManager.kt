package zukkey.whiskey.com.arcoresamplefortechbookfest.kotlin

import android.content.Context
import com.google.ar.sceneform.utilities.Preconditions
import com.google.firebase.FirebaseApp
import com.google.firebase.database.*
import timber.log.Timber


class FirebaseManager(context: Context) {


  internal interface RoomCodeListener {
    fun onNewRoomCode(newRoomCode: Long?)
    fun onError(databaseError: DatabaseError?)
  }

  internal interface CloudAnchorIdListener {
    fun onNewCloudAnchorId(cloudAnchorId: String)
  }

  private val ROOT_FIREBASE_SPOTS = "spot_list"
  private val ROOT_LAST_ROOM_CODE = "last_room_code"
  private val KEY_ANCHOR_ID = "hosted_anchor_id"
  private val KEY_TIMESTAMP = "timestamp"


  private var app: FirebaseApp? = null
  private var hotspotListRef: DatabaseReference? = null
  private var roomCodeRef: DatabaseReference? = null
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

  // ホストした時にインクリメントして、アンカーを設置する
  internal fun getNewRoomCode(listener: RoomCodeListener) {
    Preconditions.checkNotNull(app!!, "Firebase App was null")
    roomCodeRef!!.runTransaction(
        object : Transaction.Handler {
          override fun doTransaction(currentData: MutableData): Transaction.Result {
            var nextCode: Long? = java.lang.Long.valueOf(1)
            val currVal = currentData.value
            if (currVal != null) {
              val lastCode = java.lang.Long.valueOf(currVal.toString())
              nextCode = lastCode + 1
            }
            currentData.value = nextCode
            return Transaction.success(currentData)
          }

          override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
            if (!committed) {
              listener.onError(error)
              return
            }
            val roomCode = currentData!!.getValue(Long::class.java)
            listener.onNewRoomCode(roomCode)
          }
        })
  }

  internal fun storeAnchorIdInRoom(roomCode: Long?, cloudAnchorId: String) {
    Preconditions.checkNotNull(app!!, "Firebase App was null")
    val roomRef = hotspotListRef!!.child(roomCode.toString())
    roomRef.child(KEY_ANCHOR_ID).setValue(cloudAnchorId)
    roomRef.child(KEY_TIMESTAMP).setValue(System.currentTimeMillis())
  }

  internal fun registerNewListenerForRoom(roomCode: Long?, listener: CloudAnchorIdListener) {
    Preconditions.checkNotNull(app!!, "Firebase App was null")
    clearRoomListener()
    currentRoomRef = hotspotListRef!!.child(roomCode.toString())
    currentRoomListener = object : ValueEventListener {
      override fun onDataChange(dataSnapshot: DataSnapshot) {
        val valObj = dataSnapshot.child(KEY_ANCHOR_ID).value
        if (valObj != null) {
          val anchorId = valObj.toString()
          if (!anchorId.isEmpty()) {
            listener.onNewCloudAnchorId(anchorId)
          }
        }
      }

      override fun onCancelled(databaseError: DatabaseError) {
        Timber.w("The Firebase operation was cancelled.", databaseError.toException())
      }
    }
    currentRoomRef!!.addValueEventListener(currentRoomListener!!)
  }

  internal fun clearRoomListener() {
    if (currentRoomListener != null && currentRoomRef != null) {
      currentRoomRef!!.removeEventListener(currentRoomListener!!)
      currentRoomListener = null
      currentRoomRef = null
    }
  }
}
