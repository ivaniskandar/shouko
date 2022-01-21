package xyz.ivaniskandar.shouko.item

import androidx.compose.ui.graphics.ImageBitmap

data class LinkHandlerAppItem(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
    val linkHandlingAllowed: Boolean,
    val verifiedDomains: Set<String>,
    val userSelectedDomains: Set<String>,
    val unapprovedDomains: Set<String>
) {
    /**
     * True if this target has verified and/or user selected domains regardless
     * of [linkHandlingAllowed] value.
     */
    val isApproved: Boolean = verifiedDomains.isNotEmpty() || userSelectedDomains.isNotEmpty()

    /**
     * True if this target has only unapproved domains regardless of
     * [linkHandlingAllowed] value.
     */
    val isUnapproved: Boolean = !isApproved && unapprovedDomains.isNotEmpty()
}
