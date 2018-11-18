package zukkey.whiskey.com.arcoresamplefortechbookfest

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.firebase.provider.FirebaseInitProvider
import zukkey.whiskey.com.arcoresamplefortechbookfest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

  private val binding by lazy {
    DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    with(binding) {
      transition3d.setOnClickListener {
        startActivity(ModelActivity.createIntent(this@MainActivity))
      }

      transitionView.setOnClickListener {
        startActivity(ViewActivity.createIntent(this@MainActivity))
      }

      transitionArgumentedImage.setOnClickListener {
        startActivity(AugmentedImageActivity.createIntent(this@MainActivity))
      }
    }
  }
}
