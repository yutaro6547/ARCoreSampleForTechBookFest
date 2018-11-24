package zukkey.whiskey.com.arcoresamplefortechbookfest.java;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.database.DatabaseError;

import timber.log.Timber;
import zukkey.whiskey.com.arcoresamplefortechbookfest.R;
import zukkey.whiskey.com.arcoresamplefortechbookfest.misc.AnchorState;

public class CloudAnchorActivity extends AppCompatActivity{

  public CloudAnchorFragment arFragment;
  public Anchor anchor;
  public AnchorState anchorState;
  private FirebaseManager firebaseManager;
  private ViewRenderable memoViewRenderable;

  public static Intent createIntent(Context context) {
    return new Intent(context, CloudAnchorActivity.class);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_cloud_anchor);
    setUpArFragment();

    EditText inputCodeForm = findViewById(R.id.room_code_edit);
    TextView header = findViewById(R.id.room_header);

    firebaseManager = new FirebaseManager(this);
    anchorState = AnchorState.None.INSTANCE;

    Button clearButton = findViewById(R.id.clear_button);
    clearButton.setOnClickListener(view -> {
      setCloudAnchor(null);
      anchorState = AnchorState.None.INSTANCE;
      header.setText(R.string.default_room);
    });

    Button sendButton = findViewById(R.id.send_button);
    sendButton.setOnClickListener(view -> {
      Long inputCode = Long.parseLong(inputCodeForm.getText().toString());
      firebaseManager.createNewRoom(inputCode, new FirebaseManager.RoomCodeListener() {
        @Override
        public void onNewRoomCode(Long newRoomCode) {
          if (newRoomCode == null) {
            Toast.makeText(CloudAnchorActivity.this, "Room Code is null.", Toast.LENGTH_SHORT).show();
          }
          header.setText(String.valueOf(newRoomCode));
        }

        @Override
        public void onError(DatabaseError databaseError) {
          Toast.makeText(CloudAnchorActivity.this, "Database error.", Toast.LENGTH_SHORT).show();
          Timber.e(databaseError.getMessage(), databaseError);
          header.setText(R.string.default_room);
        }
      });
    });

    FloatingActionButton searchButton = findViewById(R.id.search_button);
    searchButton.setOnClickListener(view -> {
      // TODO: 既存のRoomが合った時の処理を書く
    });

    arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {
      Anchor newAnchor = arFragment.getArSceneView().getSession().hostCloudAnchor(hitResult.createAnchor());
      setCloudAnchor(newAnchor);
      setUpRendering(newAnchor, header.getText().toString());
      anchorState = AnchorState.Hosting.INSTANCE;
    });

  }

  public void setUpArFragment() {
    arFragment = (CloudAnchorFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);
    arFragment.getPlaneDiscoveryController().hide();
    arFragment.getArSceneView().getScene().addOnUpdateListener(frameTime -> {
      if (anchorState != AnchorState.Hosting.INSTANCE || anchorState != AnchorState.Searching.INSTANCE) {
        return;
      }
      Anchor.CloudAnchorState state = anchor.getCloudAnchorState();
      if (anchorState == AnchorState.Hosting.INSTANCE) {
        if (state.isError()) {
          Toast.makeText(this, "Error hosting", Toast.LENGTH_SHORT).show();
          anchorState = AnchorState.None.INSTANCE;
        } else if (state == Anchor.CloudAnchorState.SUCCESS) {
          Toast.makeText(this, "Hosting is success!", Toast.LENGTH_SHORT).show();
          anchorState = AnchorState.Hosted.INSTANCE;
        }
      } else if (anchorState == AnchorState.Searching.INSTANCE) {
        if (state.isError()) {
          Toast.makeText(this, "Error Searching", Toast.LENGTH_SHORT).show();
          anchorState = AnchorState.None.INSTANCE;
        } else if (state == Anchor.CloudAnchorState.SUCCESS) {
          Toast.makeText(this, "Searched is success!", Toast.LENGTH_SHORT).show();
          anchorState = AnchorState.Searched.INSTANCE;
        }
      }
    });
  }

  public void setCloudAnchor(Anchor newAnchor) {
    if (anchor != null) {
      anchor.detach();
    }
    anchor = newAnchor;
    anchorState = AnchorState.None.INSTANCE;
  }

  public void setUpRendering(Anchor newAnchor, String roomCode){
    ViewRenderable.builder()
        .setView(this, R.layout.item_memo)
        .build()
        .thenAccept(renderable -> memoViewRenderable = renderable);

    if (memoViewRenderable == null) {
      return;
    }

    AnchorNode anchorNode = new AnchorNode(newAnchor);
    anchorNode.setParent(arFragment.getArSceneView().getScene());
    TransformableNode memo = new TransformableNode(arFragment.getTransformationSystem());
    memo.setParent(anchorNode);
    memo.setRenderable(memoViewRenderable);
    memo.getRotationController();
    memo.getScaleController();
    memo.getTranslationController();
    memo.select();

    EditText inputText = memoViewRenderable.getView().findViewById(R.id.memo_edit);
    inputText.requestFocus();
    inputText.setOnClickListener(view -> {
      Toast.makeText(getApplicationContext(), "Clicked", Toast.LENGTH_SHORT).show();
    });
  }
}
