package zukkey.whiskey.com.arcoresamplefortechbookfest.kotlin

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import zukkey.whiskey.com.arcoresamplefortechbookfest.R
import zukkey.whiskey.com.arcoresamplefortechbookfest.databinding.ActivityWebBinding
import zukkey.whiskey.com.arcoresamplefortechbookfest.java.WebActivity


class WebActivity: AppCompatActivity() {

  private var binding: ActivityWebBinding? = null

  fun createIntent(context: Context): Intent {
    return Intent(context, WebActivity::class.java)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = DataBindingUtil.setContentView(this, R.layout.activity_web)
    binding!!.webView.loadUrl("https://booth.pm/ja/items/830454")
  }
}
