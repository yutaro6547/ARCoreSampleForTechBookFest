package zukkey.whiskey.com.arcoresamplefortechbookfest.java;

import android.content.Context;
import com.google.ar.sceneform.utilities.Preconditions;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import timber.log.Timber;

public class FirebaseManager {

  interface RoomCodeListener {
    void onNewRoomCode(Long newRoomCode);
    void onError(DatabaseError databaseError);
  }

  interface CloudAnchorIdListener {
    void onNewCloudAnchorId(String cloudAnchorId);
  }

  private static final String ROOT_FIREBASE_SPOTS = "spot_list";
  private static final String ROOT_LAST_ROOM_CODE = "last_room_code";
  private static final String KEY_DISPLAY_NAME = "display_name";
  private static final String KEY_ANCHOR_ID = "hosted_anchor_id";
  private static final String KEY_TIMESTAMP = "timestamp";


  private final FirebaseApp app;
  private final DatabaseReference hotspotListRef;
  private final DatabaseReference roomCodeRef;
  private DatabaseReference currentRoomRef = null;
  private ValueEventListener currentRoomListener = null;

  // 初期化
  FirebaseManager(Context context) {
    app = FirebaseApp.initializeApp(context);
    if (app != null) {
      DatabaseReference rootRef = FirebaseDatabase.getInstance(app).getReference();
      hotspotListRef = rootRef.child(ROOT_FIREBASE_SPOTS);
      roomCodeRef = rootRef.child(ROOT_LAST_ROOM_CODE);

      DatabaseReference.goOnline();
    } else {
      Timber.d("Could not connect to Firebase Database!");
      hotspotListRef = null;
      roomCodeRef = null;
    }
  }

  // ホストした時にインクリメントして、アンカーを設置する
  void getNewRoomCode(RoomCodeListener listener) {
    Preconditions.checkNotNull(app, "Firebase App was null");
    roomCodeRef.runTransaction(
        new Transaction.Handler() {
          @Override
          public Transaction.Result doTransaction(MutableData currentData) {
            Long nextCode = Long.valueOf(1);
            Object currVal = currentData.getValue();
            if (currVal != null) {
              Long lastCode = Long.valueOf(currVal.toString());
              nextCode = lastCode + 1;
            }
            currentData.setValue(nextCode);
            return Transaction.success(currentData);
          }

          @Override
          public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {
            if (!committed) {
              listener.onError(error);
              return;
            }
            Long roomCode = currentData.getValue(Long.class);
            listener.onNewRoomCode(roomCode);
          }
        });
  }

  void storeAnchorIdInRoom(Long roomCode, String cloudAnchorId) {
    Preconditions.checkNotNull(app, "Firebase App was null");
    DatabaseReference roomRef = hotspotListRef.child(String.valueOf(roomCode));
    roomRef.child(KEY_ANCHOR_ID).setValue(cloudAnchorId);
    roomRef.child(KEY_TIMESTAMP).setValue(System.currentTimeMillis());
  }

  void registerNewListenerForRoom(Long roomCode, CloudAnchorIdListener listener) {
    Preconditions.checkNotNull(app, "Firebase App was null");
    clearRoomListener();
    currentRoomRef = hotspotListRef.child(String.valueOf(roomCode));
    currentRoomListener =
        new ValueEventListener() {
          @Override
          public void onDataChange(DataSnapshot dataSnapshot) {
            Object valObj = dataSnapshot.child(KEY_ANCHOR_ID).getValue();
            if (valObj != null) {
              String anchorId = String.valueOf(valObj);
              if (!anchorId.isEmpty()) {
                listener.onNewCloudAnchorId(anchorId);
              }
            }
          }

          @Override
          public void onCancelled(DatabaseError databaseError) {
            Timber.w("The Firebase operation was cancelled.", databaseError.toException());
          }
        };
    currentRoomRef.addValueEventListener(currentRoomListener);
  }

  void clearRoomListener() {
    if (currentRoomListener != null && currentRoomRef != null) {
      currentRoomRef.removeEventListener(currentRoomListener);
      currentRoomListener = null;
      currentRoomRef = null;
    }
  }
}
