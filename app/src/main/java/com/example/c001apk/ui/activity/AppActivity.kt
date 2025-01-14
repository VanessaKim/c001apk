package com.example.c001apk.ui.activity

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.ThemeUtils
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.c001apk.R
import com.example.c001apk.adapter.AppAdapter
import com.example.c001apk.constant.RecyclerView.checkForGaps
import com.example.c001apk.constant.RecyclerView.markItemDecorInsetsDirty
import com.example.c001apk.databinding.ActivityAppBinding
import com.example.c001apk.ui.fragment.minterface.AppListener
import com.example.c001apk.util.BlackListUtil
import com.example.c001apk.util.DateUtils
import com.example.c001apk.util.ImageUtil
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.TopicBlackListUtil
import com.example.c001apk.view.LinearItemDecoration
import com.example.c001apk.view.StaggerItemDecoration
import com.example.c001apk.viewmodel.AppViewModel
import java.lang.reflect.Method

class AppActivity : BaseActivity(), AppListener {

    private lateinit var binding: ActivityAppBinding
    private val viewModel by lazy { ViewModelProvider(this)[AppViewModel::class.java] }
    private lateinit var mAdapter: AppAdapter
    private lateinit var mLayoutManager: LinearLayoutManager
    private lateinit var subscribe: MenuItem
    private lateinit var sLayoutManager: StaggeredGridLayoutManager
    private lateinit var mCheckForGapMethod: Method
    private lateinit var mMarkItemDecorInsetsDirtyMethod: Method

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        if (viewModel.title != "") {
            showAppInfo()
            binding.appLayout.visibility = View.VISIBLE
        } else if (viewModel.errorMessage != null) {
            showErrorMessage()
        }
        initView()
        initData()
        initRefresh()
        initScroll()

        viewModel.appInfoData.observe(this) { result ->
            if (viewModel.isNew) {
                viewModel.isNew = false

                val appInfo = result.getOrNull()
                if (appInfo?.message != null) {
                    viewModel.errorMessage = appInfo.message
                    binding.indicator.isIndeterminate = false
                    binding.indicator.visibility = View.GONE
                    showErrorMessage()
                    return@observe
                } else if (appInfo?.data != null) {
                    viewModel.isFollow = appInfo.data.userAction?.follow == 1
                    viewModel.commentStatusText = appInfo.data.commentStatusText
                    viewModel.title = appInfo.data.title
                    viewModel.version =
                        "版本: ${appInfo.data.version}(${appInfo.data.apkversioncode})"
                    viewModel.size = "大小: ${appInfo.data.apksize}"
                    viewModel.lastupdate = if (appInfo.data.lastupdate == null) "更新时间: null"
                    else "更新时间: ${DateUtils.fromToday(appInfo.data.lastupdate)}"
                    viewModel.logo = appInfo.data.logo
                    viewModel.appId = appInfo.data.id
                    viewModel.packageName = appInfo.data.apkname
                    viewModel.versionCode = appInfo.data.apkversioncode
                    showAppInfo()

                    if (viewModel.commentStatusText == "允许评论") {
                        viewModel.isRefreshing = true
                        viewModel.isNew = true
                        viewModel.getAppComment()
                    } else {
                        viewModel.isEnd = true
                        binding.indicator.isIndeterminate = false
                        binding.indicator.visibility = View.GONE
                        binding.appLayout.visibility = View.VISIBLE
                        binding.swipeRefresh.isEnabled = false
                        binding.swipeRefresh.isRefreshing = false
                        viewModel.isRefreshing = false
                        viewModel.isLoadMore = false
                        mAdapter.setLoadState(mAdapter.LOADING_ERROR, viewModel.commentStatusText)
                        mAdapter.notifyItemChanged(0)
                    }
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

        viewModel.appCommentData.observe(this) { result ->
            if (viewModel.isNew) {
                viewModel.isNew = false

                val comment = result.getOrNull()
                if (!comment.isNullOrEmpty()) {
                    if (viewModel.isRefreshing)
                        viewModel.appCommentList.clear()
                    if (viewModel.isRefreshing || viewModel.isLoadMore) {
                        viewModel.listSize = viewModel.appCommentList.size
                        for (element in comment)
                            if (element.entityType == "feed")
                                if (!BlackListUtil.checkUid(element.userInfo?.uid.toString()) && !TopicBlackListUtil.checkTopic(
                                        element.tags + element.ttitle
                                    )
                                )
                                    viewModel.appCommentList.add(element)

                    }
                    mAdapter.setLoadState(mAdapter.LOADING_COMPLETE, null)
                } else {
                    mAdapter.setLoadState(mAdapter.LOADING_END, null)
                    viewModel.isEnd = true
                    result.exceptionOrNull()?.printStackTrace()
                }
                if (viewModel.isLoadMore)
                    if (viewModel.isEnd)
                        mAdapter.notifyItemChanged(viewModel.appCommentList.size)
                    else
                        mAdapter.notifyItemRangeChanged(
                            viewModel.listSize,
                            viewModel.appCommentList.size - viewModel.listSize + 1
                        )
                else
                    mAdapter.notifyDataSetChanged()
                binding.indicator.isIndeterminate = false
                binding.indicator.visibility = View.GONE
                binding.appLayout.visibility = View.VISIBLE
                binding.swipeRefresh.isRefreshing = false
                viewModel.isRefreshing = false
                viewModel.isLoadMore = false
            }
        }

        viewModel.likeFeedData.observe(this) { result ->
            if (viewModel.isPostLikeFeed) {
                viewModel.isPostLikeFeed = false

                val response = result.getOrNull()
                if (response != null) {
                    if (response.data != null) {
                        viewModel.appCommentList[viewModel.likePosition].likenum =
                            response.data.count
                        viewModel.appCommentList[viewModel.likePosition].userAction?.like = 1
                        mAdapter.notifyItemChanged(viewModel.likePosition, "like")
                    } else
                        Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

        viewModel.unLikeFeedData.observe(this) { result ->
            if (viewModel.isPostUnLikeFeed) {
                viewModel.isPostUnLikeFeed = false

                val response = result.getOrNull()
                if (response != null) {
                    if (response.data != null) {
                        viewModel.appCommentList[viewModel.likePosition].likenum =
                            response.data.count
                        viewModel.appCommentList[viewModel.likePosition].userAction?.like = 0
                        mAdapter.notifyItemChanged(viewModel.likePosition, "like")
                    } else
                        Toast.makeText(this, response.message, Toast.LENGTH_SHORT).show()
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

        viewModel.getFollowData.observe(this) { result ->
            if (viewModel.isNew) {
                viewModel.isNew = false

                val response = result.getOrNull()
                if (response != null) {
                    response.data?.follow?.let {
                        viewModel.isFollow = !viewModel.isFollow
                        initSub()
                        Toast.makeText(
                            this, if (response.data.follow == 1) "关注成功"
                            else "取消关注成功", Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

    }

    private fun showErrorMessage() {
        binding.swipeRefresh.isEnabled = false
        binding.errorMessage.visibility = View.VISIBLE
        binding.errorMessage.text = viewModel.errorMessage
    }

    private fun showAppInfo() {
        binding.name.text = viewModel.title
        binding.version.text = viewModel.version
        binding.size.text = viewModel.size
        binding.updateTime.text = viewModel.lastupdate
        binding.collapsingToolbar.title = viewModel.title
        binding.collapsingToolbar.setExpandedTitleColor(this.getColor(com.google.android.material.R.color.mtrl_btn_transparent_bg_color))
        ImageUtil.showIMG(binding.logo, viewModel.logo)
        binding.btnDownload.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                viewModel.downloadLinkData.observe(this@AppActivity) { result ->
                    val link = result.getOrNull()
                    if (link != null) {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                        try {
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(this@AppActivity, "打开失败", Toast.LENGTH_SHORT).show()
                            Log.w("error", "Activity was not found for intent, $intent")
                        }
                    } else {
                        result.exceptionOrNull()?.printStackTrace()
                    }
                }
                viewModel.getDownloadLink()
            }
        }
    }

    private fun initView() {
        val space = resources.getDimensionPixelSize(R.dimen.normal_space)
        mAdapter = AppAdapter(this, viewModel.appCommentList)
        mAdapter.setAppListener(this)
        mLayoutManager = LinearLayoutManager(this)
        sLayoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // https://codeantenna.com/a/2NDTnG37Vg
            mCheckForGapMethod = checkForGaps
            mCheckForGapMethod.isAccessible = true
            mMarkItemDecorInsetsDirtyMethod = markItemDecorInsetsDirty
            mMarkItemDecorInsetsDirtyMethod.isAccessible = true
        }
        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager =
                if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                    mLayoutManager
                else sLayoutManager
            if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
                addItemDecoration(LinearItemDecoration(space))
            else
                addItemDecoration(StaggerItemDecoration(space))
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initData() {
        if (viewModel.isInit) {
            viewModel.isInit = false
            binding.indicator.isIndeterminate = true
            binding.indicator.visibility = View.VISIBLE
            refreshData()
        } else if (viewModel.commentStatusText != "允许评论") {
            binding.swipeRefresh.isEnabled = false
            mAdapter.setLoadState(mAdapter.LOADING_ERROR, viewModel.commentStatusText)
            mAdapter.notifyItemChanged(0)
        }
    }

    private fun refreshData() {
        viewModel.firstVisibleItemPosition = -1
        viewModel.lastVisibleItemPosition = -1
        viewModel.page = 1
        viewModel.isRefreshing = true
        viewModel.isEnd = false
        val id = intent.getStringExtra("id")!!
        viewModel.id = id
        viewModel.isNew = true
        viewModel.getAppInfo()
    }

    @SuppressLint("RestrictedApi")
    private fun initRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ThemeUtils.getThemeAttrColor(
                this,
                rikka.preference.simplemenu.R.attr.colorPrimary
            )
        )
        binding.swipeRefresh.setOnRefreshListener {
            binding.indicator.isIndeterminate = false
            binding.indicator.visibility = View.GONE
            viewModel.page = 1
            viewModel.isRefreshing = true
            viewModel.isEnd = false
            viewModel.isNew = true
            viewModel.getAppComment()
        }
    }

    private fun initScroll() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (viewModel.lastVisibleItemPosition == viewModel.appCommentList.size
                        && !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
                    ) {
                        mAdapter.setLoadState(mAdapter.LOADING, null)
                        mAdapter.notifyItemChanged(viewModel.appCommentList.size)
                        viewModel.isLoadMore = true
                        viewModel.page++
                        viewModel.isNew = true
                        viewModel.getAppComment()

                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (viewModel.appCommentList.isNotEmpty()) {
                    if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                        viewModel.lastVisibleItemPosition =
                            mLayoutManager.findLastVisibleItemPosition()
                        viewModel.firstCompletelyVisibleItemPosition =
                            mLayoutManager.findFirstCompletelyVisibleItemPosition()
                    } else {
                        val result =
                            mCheckForGapMethod.invoke(binding.recyclerView.layoutManager) as Boolean
                        if (result)
                            mMarkItemDecorInsetsDirtyMethod.invoke(binding.recyclerView)

                        val positions = sLayoutManager.findLastVisibleItemPositions(null)
                        for (pos in positions) {
                            if (pos > viewModel.lastVisibleItemPosition) {
                                viewModel.lastVisibleItemPosition = pos
                            }
                        }
                    }
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.topic_product_menu, menu)
        subscribe = menu!!.findItem(R.id.subscribe)
        subscribe.isVisible = PrefManager.isLogin
        return true
    }

    private fun initSub() {
        subscribe.title = if (viewModel.isFollow) "取消关注"
        else "关注"
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {

        initSub()

        menu.findItem(
            when (viewModel.appCommentTitle) {
                "最近回复" -> R.id.topicLatestReply
                "热度排序" -> R.id.topicHot
                "最新发布" -> R.id.topicLatestPublish
                else -> throw IllegalArgumentException("type error")
            }
        )?.isChecked = true
        return super.onPrepareOptionsMenu(menu)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()

            R.id.search -> {
                val intent = Intent(this, SearchActivity::class.java)
                intent.putExtra("pageType", "apk")
                intent.putExtra("pageParam", viewModel.appId)
                intent.putExtra("title", viewModel.title)
                startActivity(intent)
            }

            R.id.topicLatestReply -> {
                viewModel.appCommentSort = ""
                viewModel.appCommentTitle = "最近回复"
                viewModel.appCommentList.clear()
                mAdapter.notifyDataSetChanged()
                binding.indicator.visibility = View.VISIBLE
                binding.indicator.isIndeterminate = true
                viewModel.isNew = true
                viewModel.isRefreshing = true
                viewModel.isLoadMore = false
                viewModel.isEnd = false
                viewModel.page = 1
                viewModel.getAppComment()
            }

            R.id.topicHot -> {
                viewModel.appCommentSort = "%26sort%3Dpopular"
                viewModel.appCommentTitle = "热度排序"
                viewModel.appCommentList.clear()
                mAdapter.notifyDataSetChanged()
                binding.indicator.visibility = View.VISIBLE
                binding.indicator.isIndeterminate = true
                viewModel.isNew = true
                viewModel.isRefreshing = true
                viewModel.isLoadMore = false
                viewModel.isEnd = false
                viewModel.page = 1
                viewModel.getAppComment()
            }

            R.id.topicLatestPublish -> {
                viewModel.appCommentSort = "%26sort%3Ddateline_desc"
                viewModel.appCommentTitle = "最新发布"
                viewModel.appCommentList.clear()
                mAdapter.notifyDataSetChanged()
                binding.indicator.visibility = View.VISIBLE
                binding.indicator.isIndeterminate = true
                viewModel.isNew = true
                viewModel.isRefreshing = true
                viewModel.isLoadMore = false
                viewModel.isEnd = false
                viewModel.page = 1
                viewModel.getAppComment()
            }


            R.id.subscribe -> {
                viewModel.isNew = true
                viewModel.url = if (viewModel.isFollow) "/v6/apk/unFollow"
                else "/v6/apk/follow"
                viewModel.fid = viewModel.appId
                viewModel.getFollow()
            }
        }
        return true
    }

    override fun onShowTotalReply(position: Int, uid: String, id: String, rPosition: Int?) {}

    override fun onPostFollow(isFollow: Boolean, uid: String, position: Int) {}

    override fun onReply2Reply(
        rPosition: Int,
        r2rPosition: Int?,
        id: String,
        uid: String,
        uname: String,
        type: String
    ) {
    }

    override fun onPostLike(type: String?, isLike: Boolean, id: String, position: Int?) {
        viewModel.likeFeedId = id
        viewModel.likePosition = position!!
        if (isLike) {
            viewModel.isPostUnLikeFeed = true
            viewModel.postUnLikeFeed()
        } else {
            viewModel.isPostLikeFeed = true
            viewModel.postLikeFeed()
        }
    }

    override fun onRefreshReply(listType: String) {}

    override fun onDeleteFeedReply(id: String, position: Int, rPosition: Int?) {}

    override fun onShowCollection(id: String, title: String) {}

}