package zukkey.whiskey.com.arcoresamplefortechbookfest.java;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.ar.core.Anchor;

import zukkey.whiskey.com.arcoresamplefortechbookfest.R;
import zukkey.whiskey.com.arcoresamplefortechbookfest.misc.AnchorState;

public class CloudAnchorActivity extends AppCompatActivity{

  public CloudAnchorFragment arFragment;
  public Anchor anchor;
  public AnchorState anchorState;
  private FirebaseManager firebaseManager;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_cloud_anchor);
    setUpArFragment();

    firebaseManager = new FirebaseManager(this);
    anchorState = AnchorState.None.INSTANCE;
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
}
