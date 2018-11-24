package zukkey.whiskey.com.arcoresamplefortechbookfest.misc


sealed class AnchorState {
  object None: AnchorState()
  object Hosting: AnchorState()
  object Hosted: AnchorState()
  object Searching: AnchorState()
  object Searched: AnchorState()
}
