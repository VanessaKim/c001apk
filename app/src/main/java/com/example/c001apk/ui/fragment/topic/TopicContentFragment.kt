package com.example.c001apk.ui.fragment.topic

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.ThemeUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.c001apk.R
import com.example.c001apk.adapter.AppAdapter
import com.example.c001apk.constant.RecyclerView.checkForGaps
import com.example.c001apk.constant.RecyclerView.markItemDecorInsetsDirty
import com.example.c001apk.databinding.FragmentTopicContentBinding
import com.example.c001apk.ui.fragment.minterface.AppListener
import com.example.c001apk.ui.fragment.minterface.IOnSearchMenuClickContainer
import com.example.c001apk.ui.fragment.minterface.IOnSearchMenuClickListener
import com.example.c001apk.ui.fragment.minterface.IOnTabClickContainer
import com.example.c001apk.ui.fragment.minterface.IOnTabClickListener
import com.example.c001apk.util.BlackListUtil
import com.example.c001apk.util.TopicBlackListUtil
import com.example.c001apk.view.LinearItemDecoration
import com.example.c001apk.view.StaggerItemDecoration
import com.example.c001apk.viewmodel.AppViewModel
import java.lang.reflect.Method

class TopicContentFragment : Fragment(), AppListener, IOnSearchMenuClickListener,
    IOnTabClickListener {

    private lateinit var binding: FragmentTopicContentBinding
    private val viewModel by lazy { ViewModelProvider(this)[AppViewModel::class.java] }
    private lateinit var mAdapter: AppAdapter
    private lateinit var mLayoutManager: LinearLayoutManager
    private lateinit var sLayoutManager: StaggeredGridLayoutManager
    private lateinit var mCheckForGapMethod: Method
    private lateinit var mMarkItemDecorInsetsDirtyMethod: Method

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.url = it.getString("url")!!
            viewModel.title = it.getString("title")!!
            viewModel.isEnable = it.getBoolean("isEnable")
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(url: String, title: String, isEnable: Boolean) =
            TopicContentFragment().apply {
                arguments = Bundle().apply {
                    putString("url", url)
                    putString("title", title)
                    putBoolean("isEnable", isEnable)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTopicContentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.isEnable)
            (requireParentFragment() as IOnTabClickContainer).tabController = this

        if (viewModel.title == "讨论")
            (requireParentFragment() as IOnSearchMenuClickContainer).controller = this

        if (viewModel.isInit) {
            viewModel.isInit = false
            initData()
            initView()
            initRefresh()
            initScroll()
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!viewModel.isInit) {
            initData()
            initView()
            initRefresh()
            initScroll()
        }

        viewModel.topicDataLiveData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isNew) {
                viewModel.isNew = false

                val data = result.getOrNull()
                if (!data.isNullOrEmpty()) {
                    if (viewModel.isRefreshing) {
                        viewModel.topicDataList.clear()
                    }
                    if (viewModel.isRefreshing || viewModel.isLoadMore) {
                        viewModel.lastItem = data.last().id
                        viewModel.listSize = viewModel.topicDataList.size
                        for (element in data) {
                            if (element.id == viewModel.lastItem)
                                continue
                            if (element.entityType == "feed"
                                || element.entityType == "topic"
                                || element.entityType == "product"
                                || element.entityType == "user"
                            )
                                if (!BlackListUtil.checkUid(element.userInfo?.uid.toString()) && !TopicBlackListUtil.checkTopic(
                                        element.tags + element.ttitle
                                    )
                                )
                                    viewModel.topicDataList.add(element)
                        }
                    }
                    mAdapter.setLoadState(mAdapter.LOADING_COMPLETE, null)
                } else {
                    mAdapter.setLoadState(mAdapter.LOADING_END, null)
                    viewModel.isEnd = true
                    result.exceptionOrNull()?.printStackTrace()
                }
                if (viewModel.isLoadMore)
                    if (viewModel.isEnd)
                        mAdapter.notifyItemChanged(viewModel.topicDataList.size)
                    else
                        mAdapter.notifyItemRangeChanged(
                            viewModel.listSize,
                            viewModel.topicDataList.size - viewModel.listSize + 1
                        )
                else
                    mAdapter.notifyDataSetChanged()
                binding.indicator.isIndeterminate = false
                binding.indicator.visibility = View.GONE
                viewModel.isLoadMore = false
                viewModel.isRefreshing = false
                binding.swipeRefresh.isRefreshing = false
            }
        }

        viewModel.likeFeedData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isPostLikeFeed) {
                viewModel.isPostLikeFeed = false

                val response = result.getOrNull()
                if (response != null) {
                    if (response.data != null) {
                        viewModel.topicDataList[viewModel.likePosition].likenum =
                            response.data.count
                        viewModel.topicDataList[viewModel.likePosition].userAction?.like = 1
                        mAdapter.notifyItemChanged(viewModel.likePosition, "like")
                    } else
                        Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

        viewModel.unLikeFeedData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isPostUnLikeFeed) {
                viewModel.isPostUnLikeFeed = false

                val response = result.getOrNull()
                if (response != null) {
                    if (response.data != null) {
                        viewModel.topicDataList[viewModel.likePosition].likenum =
                            response.data.count
                        viewModel.topicDataList[viewModel.likePosition].userAction?.like = 0
                        mAdapter.notifyItemChanged(viewModel.likePosition, "like")
                    } else
                        Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

    }

    private fun initScroll() {
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            @SuppressLint("NotifyDataSetChanged")
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (viewModel.lastVisibleItemPosition == viewModel.topicDataList.size
                        && !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
                    ) {
                        mAdapter.setLoadState(mAdapter.LOADING, null)
                        mAdapter.notifyItemChanged(viewModel.topicDataList.size)
                        viewModel.isLoadMore = true
                        viewModel.page++
                        viewModel.isNew = true
                        viewModel.getTopicData()
                    }
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (viewModel.topicDataList.isNotEmpty()) {
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

    @SuppressLint("RestrictedApi")
    private fun initRefresh() {
        binding.swipeRefresh.setColorSchemeColors(
            ThemeUtils.getThemeAttrColor(
                requireContext(),
                rikka.preference.simplemenu.R.attr.colorPrimary
            )
        )
        binding.swipeRefresh.setOnRefreshListener {
            binding.indicator.isIndeterminate = false
            binding.indicator.visibility = View.GONE
            refreshData()
        }
    }

    private fun initView() {
        val space = resources.getDimensionPixelSize(R.dimen.normal_space)
        mAdapter = AppAdapter(requireContext(), viewModel.topicDataList)
        mAdapter.setAppListener(this)
        mLayoutManager = LinearLayoutManager(activity)
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

    private fun initData() {
        if (viewModel.topicDataList.isEmpty()) {
            binding.indicator.visibility = View.VISIBLE
            binding.indicator.isIndeterminate = true
            refreshData()
        }
    }

    private fun refreshData() {
        viewModel.subtitle = ""
        viewModel.firstVisibleItemPosition = -1
        viewModel.lastVisibleItemPosition = -1
        viewModel.lastItem = null
        viewModel.page = 1
        viewModel.isEnd = false
        viewModel.isRefreshing = true
        viewModel.isLoadMore = false
        viewModel.isNew = true
        viewModel.getTopicData()
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

    @SuppressLint("NotifyDataSetChanged")
    override fun onSearch(type: String, value: String, id: String?) {
        viewModel.title = value
        when (value) {
            "最近回复" -> viewModel.url =
                "/page?url=/product/feedList?type=feed&id=$id&ignoreEntityById=1"

            "热度排序" -> viewModel.url =
                "/page?url=/product/feedList?type=feed&id=$id&listType=rank_score"

            "最新发布" -> viewModel.url =
                "/page?url=/product/feedList?type=feed&id=$id&ignoreEntityById=1&listType=dateline_desc"
        }
        viewModel.topicDataList.clear()
        mAdapter.notifyDataSetChanged()
        binding.indicator.visibility = View.VISIBLE
        binding.indicator.isIndeterminate = true
        refreshData()
    }

    override fun onReturnTop(isRefresh: Boolean?) {
        if (viewModel.firstCompletelyVisibleItemPosition == 0) {
            binding.swipeRefresh.isRefreshing = true
            refreshData()
        } else
            binding.recyclerView.scrollToPosition(0)
    }

}