package zukkey.whiskey.com.arcoresamplefortechbookfest.misc

import android.app.AlertDialog
import android.app.Dialog
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import zukkey.whiskey.com.arcoresamplefortechbookfest.R
import zukkey.whiskey.com.arcoresamplefortechbookfest.databinding.SearchDialogBinding


class SearchDialog : DialogFragment() {

  interface PositiveButtonCallBack {
    fun onPositiveButtonClicked(roomCode: String)
  }

  private lateinit var binding: SearchDialogBinding

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val builder = AlertDialog.Builder(activity)
    binding = DataBindingUtil.inflate(LayoutInflater.from(activity), R.layout.search_dialog, null, false)
    builder.setTitle("既存のRoomCodeを入力してください")
        .setView(binding.root)
        .setPositiveButton(
            "OK") { _, _ ->
          if (binding.editText.text.isNotEmpty()) {
            (activity as PositiveButtonCallBack).onPositiveButtonClicked(binding.editText.text.toString())
          } else {
            (activity as PositiveButtonCallBack).onPositiveButtonClicked(0.toString())
          }
        }.setNegativeButton(
            "キャンセル") { _, _ ->
          this.dismiss()
        }
    return builder.create()
  }
}
