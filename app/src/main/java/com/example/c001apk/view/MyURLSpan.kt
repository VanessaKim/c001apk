package com.example.c001apk.view

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.ThemeUtils
import com.example.c001apk.ui.activity.AppActivity
import com.example.c001apk.ui.activity.CoolPicActivity
import com.example.c001apk.ui.activity.FeedActivity
import com.example.c001apk.ui.activity.TopicActivity
import com.example.c001apk.ui.activity.UserActivity
import com.example.c001apk.ui.activity.WebViewActivity
import com.example.c001apk.ui.fragment.minterface.IOnShowMoreReplyContainer
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.http2https

internal class MyURLSpan(
    private val mContext: Context,
    private val mUrl: String,
    private val imgList: List<String>?
) :
    ClickableSpan() {

    private var position = 0
    private var uid = ""
    fun setData(position: Int, uid: String) {
        this.position = position
        this.uid = uid
    }

    var isReturn = false
    var isColor = false

    override fun onClick(widget: View) {
        if (mUrl == "") {
            return
        } else if (mUrl.contains("/feed/replyList")) {
            if (isReturn)
                return
            val id = mUrl.replace("/feed/replyList?id=", "")
            IOnShowMoreReplyContainer.controller?.onShowMoreReply(position, uid, id)
        } else if (mUrl.contains("coolapk.com/u/")) {
            val uid = mUrl.replace("coolapk.com/u/", "")
            val intent = Intent(mContext, UserActivity::class.java)
            intent.putExtra("id", uid)
            mContext.startActivity(intent)
        } else if (mUrl.contains("coolapk.com/apk/")) {
            val id = mUrl.replace("coolapk.com/apk/", "")
            val intent = Intent(mContext, AppActivity::class.java)
            intent.putExtra("id", id)
            mContext.startActivity(intent)
        } else if (mUrl.startsWith("/t/")) {
            if (mUrl.contains("?type=8")) {
                val intent = Intent(mContext, CoolPicActivity::class.java)
                intent.putExtra("title", mUrl.replace("/t/", "").replace("?type=8", ""))
                mContext.startActivity(intent)
            } else {
                val intent = Intent(mContext, TopicActivity::class.java)
                val index = mUrl.indexOf("?")
                intent.putExtra("url", mUrl.substring(3, index))
                intent.putExtra("title", mUrl.substring(3, index))
                intent.putExtra("type", "topic")
                intent.putExtra("id", "")
                mContext.startActivity(intent)
            }
        } else if (mUrl.startsWith("/u/")) {
            val intent = Intent(mContext, UserActivity::class.java)
            intent.putExtra("id", mUrl.replace("/u/", ""))
            mContext.startActivity(intent)
        } else if (mUrl.contains("image.coolapk.com")) {
            if (imgList == null) {
                ImageUtil.startBigImgViewSimple(mContext, mUrl.http2https())
            } else {
                ImageUtil.startBigImgViewSimple(mContext, imgList)
            }
        } else if (mUrl.contains("www.coolapk.com/feed/")) {
            val id = if (mUrl.contains("shareKey")) {
                mUrl.substring(mUrl.lastIndexOf("/feed/") + 6, mUrl.lastIndexOf("?shareKey"))
            } else {
                mUrl.substring(mUrl.lastIndexOf("/feed/") + 6)
            }
            val intent = Intent(mContext, FeedActivity::class.java)
            intent.putExtra("type", "feed")
            intent.putExtra("id", id)
            intent.putExtra("uid", "")
            intent.putExtra("uname", "")
            mContext.startActivity(intent)
        } else {
            if (PrefManager.isOpenLinkOutside) {
                val intent = Intent()
                intent.action = Intent.ACTION_VIEW
                intent.data = Uri.parse(mUrl)
                try {
                    mContext.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(mContext, "打开失败", Toast.LENGTH_SHORT).show()
                    Log.w("error", "Activity was not found for intent, $intent")
                }
            } else {
                val intent = Intent(mContext, WebViewActivity::class.java)
                intent.putExtra("url", mUrl)
                mContext.startActivity(intent)
            }
        }
    }

    @SuppressLint("RestrictedApi")
    override fun updateDrawState(ds: TextPaint) {
        super.updateDrawState(ds)
        if (isColor)
            ds.color = ThemeUtils.getThemeAttrColor(
                mContext,
                com.google.android.material.R.attr.colorControlNormal
            ) //设置文本颜色
        ds.isUnderlineText = false //取消下划线
    }
}