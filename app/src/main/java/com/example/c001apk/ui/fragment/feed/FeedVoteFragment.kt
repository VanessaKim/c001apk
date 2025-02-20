package com.example.c001apk.ui.fragment.feed

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.ThemeUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.example.c001apk.R
import com.example.c001apk.adapter.FeedContentAdapter
import com.example.c001apk.constant.RecyclerView.checkForGaps
import com.example.c001apk.constant.RecyclerView.markItemDecorInsetsDirty
import com.example.c001apk.databinding.FragmentFeedVoteBinding
import com.example.c001apk.logic.database.FeedFavoriteDatabase
import com.example.c001apk.logic.model.FeedFavorite
import com.example.c001apk.ui.activity.FeedActivity
import com.example.c001apk.ui.activity.WebViewActivity
import com.example.c001apk.ui.fragment.minterface.AppListener
import com.example.c001apk.util.BlackListUtil
import com.example.c001apk.util.ClipboardUtil
import com.example.c001apk.util.IntentUtil
import com.example.c001apk.util.ToastUtil
import com.example.c001apk.view.VoteItemDecoration
import com.example.c001apk.viewmodel.AppViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.reflect.Method

class FeedVoteFragment : Fragment(), AppListener {

    private lateinit var binding: FragmentFeedVoteBinding
    private val viewModel by lazy { ViewModelProvider(this)[AppViewModel::class.java] }
    private lateinit var mAdapter: FeedContentAdapter
    private lateinit var mLayoutManager: StaggeredGridLayoutManager
    private val feedFavoriteDao by lazy {
        FeedFavoriteDatabase.getDatabase(this@FeedVoteFragment.requireContext()).feedFavoriteDao()
    }
    private lateinit var mCheckForGapMethod: Method
    private lateinit var mMarkItemDecorInsetsDirtyMethod: Method

    companion object {
        @JvmStatic
        fun newInstance(id: String) =
            FeedVoteFragment().apply {
                arguments = Bundle().apply {
                    putString("ID", id)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            viewModel.id = it.getString("ID", "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedVoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initBar()
        initView()
        initData()
        initRefresh()
        initScroll()

        viewModel.feedData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isNew) {
                viewModel.isNew = false

                val feed = result.getOrNull()
                if (feed?.message != null) {
                    viewModel.errorMessage = feed.message
                    binding.indicator.isIndeterminate = false
                    binding.indicator.visibility = View.GONE
                    showErrorMessage()
                    return@observe
                } else if (feed?.data != null) {
                    viewModel.uid = feed.data.uid
                    viewModel.funame = feed.data.userInfo?.username.toString()
                    viewModel.avatar = feed.data.userAvatar
                    viewModel.device = feed.data.deviceTitle.toString()
                    viewModel.replyCount = feed.data.replynum
                    viewModel.dateLine = feed.data.dateline
                    viewModel.feedTypeName = feed.data.feedTypeName
                    viewModel.feedType = feed.data.feedType
                    viewModel.totalOptionNum = feed.data.vote!!.totalOptionNum
                    binding.toolBar.title = viewModel.feedTypeName
                    if (viewModel.isRefreshing) {
                        viewModel.feedContentList.clear()
                        viewModel.isRefreshing = true
                        viewModel.isNew = true

                        if (viewModel.totalOptionNum == 2) {
                            viewModel.extraKey = feed.data.vote.options[viewModel.currentOption].id
                            mAdapter.setExtraKey(feed.data.vote.options[viewModel.currentOption].id)
                        }
                        viewModel.getVoteComment()

                    }
                    if (viewModel.isRefreshing || viewModel.isLoadMore) {
                        viewModel.feedContentList.add(feed)
                    }
                } else {
                    viewModel.isEnd = true
                    viewModel.isLoadMore = false
                    viewModel.isRefreshing = false
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(activity, "加载失败", Toast.LENGTH_SHORT).show()
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

        viewModel.voteCommentData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isNew) {
                viewModel.isNew = false

                val voteComment = result.getOrNull()
                if (voteComment?.data != null) {
                    if (viewModel.isRefreshing) {
                        viewModel.isRefreshing = false
                        viewModel.leftVoteCommentList.clear()
                        viewModel.rightVoteCommentList.clear()
                        viewModel.voteCommentList.clear()
                    }
                    if (viewModel.isLoadMore && viewModel.currentOption == 0) {
                        viewModel.leftVoteCommentList.clear()
                        viewModel.rightVoteCommentList.clear()
                    }
                    if (viewModel.totalOptionNum == 2 && viewModel.currentOption == 0) {
                        if (voteComment.data.isEmpty())
                            viewModel.leftEnd = true
                        else
                            viewModel.leftVoteCommentList.addAll(voteComment.data)
                        viewModel.currentOption++
                        viewModel.extraKey =
                            viewModel.feedContentList[0].data!!.vote!!.options[viewModel.currentOption].id
                        viewModel.isNew = true
                        viewModel.getVoteComment()
                    } else if (viewModel.totalOptionNum == 2 && viewModel.currentOption == 1) {
                        if (voteComment.data.isEmpty())
                            viewModel.rightEnd = true
                        else
                            viewModel.rightVoteCommentList.addAll(voteComment.data)
                        viewModel.listSize = viewModel.voteCommentList.size
                        if (viewModel.leftVoteCommentList.isNotEmpty() && viewModel.rightVoteCommentList.isNotEmpty()) {
                            if (viewModel.leftVoteCommentList.size >= viewModel.rightVoteCommentList.size) {
                                for (index in 0 until viewModel.rightVoteCommentList.size) {
                                    viewModel.voteCommentList.add(viewModel.leftVoteCommentList[index])
                                    viewModel.voteCommentList.add(viewModel.rightVoteCommentList[index])
                                }
                                viewModel.voteCommentList.addAll(
                                    viewModel.leftVoteCommentList.subList(
                                        viewModel.rightVoteCommentList.size,
                                        viewModel.leftVoteCommentList.size
                                    )
                                )
                            } else {
                                for (index in 0 until viewModel.leftVoteCommentList.size) {
                                    viewModel.voteCommentList.add(viewModel.leftVoteCommentList[index])
                                    viewModel.voteCommentList.add(viewModel.rightVoteCommentList[index])
                                }
                                viewModel.voteCommentList.addAll(
                                    viewModel.rightVoteCommentList.subList(
                                        viewModel.leftVoteCommentList.size,
                                        viewModel.rightVoteCommentList.size
                                    )
                                )
                            }
                        } else if (viewModel.leftVoteCommentList.isNotEmpty())
                            viewModel.voteCommentList.addAll(viewModel.leftVoteCommentList)
                        else if (viewModel.rightVoteCommentList.isNotEmpty())
                            viewModel.voteCommentList.addAll(viewModel.rightVoteCommentList)
                        if (viewModel.leftEnd && viewModel.rightEnd) {
                            viewModel.isEnd = true
                            mAdapter.setLoadState(mAdapter.LOADING_END, null)
                        } else
                            mAdapter.setLoadState(mAdapter.LOADING_COMPLETE, null)
                        if (viewModel.isLoadMore)
                            mAdapter.notifyItemRangeChanged(
                                viewModel.listSize + 2,
                                viewModel.leftVoteCommentList.size + viewModel.rightVoteCommentList.size + 1
                            )
                        else {
                            mAdapter.notifyItemRangeChanged(
                                0,
                                viewModel.voteCommentList.size + 3
                            )
                        }
                        binding.indicator.isIndeterminate = false
                        binding.indicator.visibility = View.GONE
                        binding.contentLayout.visibility = View.VISIBLE
                        binding.swipeRefresh.isRefreshing = false
                        viewModel.isLoadMore = false
                        viewModel.isRefreshing = false
                    } else {
                        viewModel.listSize = viewModel.voteCommentList.size
                        if (voteComment.data.isEmpty()) {
                            viewModel.isEnd = true
                            mAdapter.setLoadState(mAdapter.LOADING_END, null)
                        } else {
                            viewModel.voteCommentList.addAll(voteComment.data)
                            mAdapter.setLoadState(mAdapter.LOADING_COMPLETE, null)
                        }
                        if (viewModel.isLoadMore) {
                            mAdapter.notifyItemRangeChanged(
                                viewModel.listSize + 2,
                                voteComment.data.size + 1
                            )
                        } else {
                            mAdapter.notifyItemRangeChanged(
                                0,
                                viewModel.voteCommentList.size + 3
                            )
                        }
                        binding.indicator.isIndeterminate = false
                        binding.indicator.visibility = View.GONE
                        binding.contentLayout.visibility = View.VISIBLE
                        binding.swipeRefresh.isRefreshing = false
                        viewModel.isLoadMore = false
                        viewModel.isRefreshing = false
                    }
                } else {
                    viewModel.isEnd = true
                    viewModel.isLoadMore = false
                    viewModel.isRefreshing = false
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

        viewModel.likeReplyData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isPostLikeReply) {
                viewModel.isPostLikeReply = false

                val response = result.getOrNull()
                if (response != null) {
                    if (response.data != null) {
                        viewModel.voteCommentList[viewModel.likeReplyPosition - 1].likenum =
                            response.data
                        viewModel.voteCommentList[viewModel.likeReplyPosition - 1].userAction?.like =
                            1
                        mAdapter.notifyItemChanged(viewModel.likeReplyPosition + 1, "like")
                    } else
                        Toast.makeText(activity, response.message, Toast.LENGTH_SHORT).show()
                } else {
                    result.exceptionOrNull()?.printStackTrace()
                }
            }
        }

        viewModel.unLikeReplyData.observe(viewLifecycleOwner) { result ->
            if (viewModel.isPostUnLikeReply) {
                viewModel.isPostUnLikeReply = false

                val response = result.getOrNull()
                if (response != null) {
                    if (response.data != null) {
                        viewModel.voteCommentList[viewModel.likeReplyPosition - 1].likenum =
                            response.data
                        viewModel.voteCommentList[viewModel.likeReplyPosition - 1].userAction?.like =
                            0
                        mAdapter.notifyItemChanged(viewModel.likeReplyPosition + 1, "like")
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
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    if (viewModel.lastVisibleItemPosition == viewModel.voteCommentList.size + 2
                        && !viewModel.isEnd && !viewModel.isRefreshing && !viewModel.isLoadMore
                    ) {
                        mAdapter.setLoadState(mAdapter.LOADING, null)
                        mAdapter.notifyItemChanged(viewModel.voteCommentList.size + 2)
                        viewModel.isLoadMore = true
                        viewModel.page++
                        viewModel.isNew = true
                        if (viewModel.totalOptionNum == 2) {
                            if (!viewModel.leftEnd) {
                                viewModel.currentOption = 0
                                viewModel.extraKey =
                                    viewModel.feedContentList[0].data!!.vote!!.options[viewModel.currentOption].id
                            } else {
                                viewModel.currentOption = 1
                                viewModel.extraKey =
                                    viewModel.feedContentList[0].data!!.vote!!.options[viewModel.currentOption].id
                            }
                        }
                        viewModel.getVoteComment()
                    }
                }
            }


            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (viewModel.feedContentList.isNotEmpty()) {

                    val result =
                        mCheckForGapMethod.invoke(binding.recyclerView.layoutManager) as Boolean
                    if (result)
                        mMarkItemDecorInsetsDirtyMethod.invoke(binding.recyclerView)

                    val positions = mLayoutManager.findLastVisibleItemPositions(null)
                    for (pos in positions) {
                        if (pos > viewModel.lastVisibleItemPosition) {
                            viewModel.lastVisibleItemPosition = pos
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

    private fun initBar() {
        binding.toolBar.apply {
            title = viewModel.feedTypeName
            setNavigationIcon(R.drawable.ic_back)
            setNavigationOnClickListener {
                requireActivity().finish()
            }
            setOnClickListener {
                binding.recyclerView.stopScroll()
                binding.titleProfile.visibility = View.GONE
                mLayoutManager.scrollToPositionWithOffset(0, 0)
            }
            inflateMenu(R.menu.feed_menu)
            val favorite = menu.findItem(R.id.favorite)
            CoroutineScope(Dispatchers.IO).launch {
                if (feedFavoriteDao.isFavorite(viewModel.id)) {
                    withContext(Dispatchers.Main) {
                        favorite.title = "取消收藏"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        favorite.title = "收藏"
                    }
                }
            }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.showReply -> {
                        binding.recyclerView.stopScroll()
                        if (viewModel.firstVisibleItemPosition <= 0)
                            mLayoutManager.scrollToPositionWithOffset(1, 0)
                        else {
                            binding.titleProfile.visibility = View.GONE
                            mLayoutManager.scrollToPositionWithOffset(0, 0)
                        }
                    }

                    R.id.block -> {
                        MaterialAlertDialogBuilder(requireContext()).apply {
                            setTitle("确定将 ${viewModel.funame} 加入黑名单？")
                            setNegativeButton(android.R.string.cancel, null)
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                BlackListUtil.saveUid(viewModel.uid)
                                //requireActivity().finish()
                            }
                            show()
                        }
                    }

                    R.id.share -> {
                        IntentUtil.shareText(
                            requireContext(),
                            "https://www.coolapk1s.com/feed/${viewModel.id}"
                        )
                    }

                    R.id.copyLink -> {
                        ClipboardUtil.copyText(
                            requireContext(),
                            "https://www.coolapk1s.com/feed/${viewModel.id}"
                        )
                    }

                    R.id.report -> {
                        val intent = Intent(requireContext(), WebViewActivity::class.java)
                        intent.putExtra(
                            "url",
                            "https://m.coolapk.com/mp/do?c=feed&m=report&type=feed&id=${viewModel.id}"
                        )
                        requireContext().startActivity(intent)
                    }


                    R.id.favorite -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (feedFavoriteDao.isFavorite(viewModel.id)) {
                                feedFavoriteDao.delete(viewModel.id)
                                withContext(Dispatchers.Main) {
                                    favorite.title = "收藏"
                                    ToastUtil.toast("已取消收藏")
                                }
                            } else {
                                try {
                                    val fav = FeedFavorite(
                                        viewModel.id,
                                        viewModel.uid,
                                        viewModel.funame,
                                        viewModel.avatar,
                                        viewModel.device,
                                        viewModel.feedContentList[0].data?.message.toString(), // 还未加载完会空指针
                                        viewModel.feedContentList[0].data?.dateline.toString()
                                    )
                                    feedFavoriteDao.insert(fav)
                                    withContext(Dispatchers.Main) {
                                        favorite.title = "取消收藏"
                                        ToastUtil.toast("已收藏")
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    ToastUtil.toast("请稍后再试")
                                }
                            }

                        }
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

    private fun showErrorMessage() {
        binding.errorMessage.visibility = View.VISIBLE
        binding.errorMessage.text = viewModel.errorMessage
    }

    private fun initData() {
        if (viewModel.feedContentList.isEmpty()) {
            binding.titleProfile.visibility = View.GONE
            binding.indicator.visibility = View.VISIBLE
            binding.indicator.isIndeterminate = true
            refreshData()
        } else {
            binding.contentLayout.visibility = View.VISIBLE
            if (viewModel.errorMessage != null) {
                mAdapter.setLoadState(mAdapter.LOADING_ERROR, viewModel.errorMessage)
                mAdapter.notifyItemChanged(2)
            } else if (viewModel.isEnd) {
                mAdapter.setLoadState(mAdapter.LOADING_END, null)
                mAdapter.notifyItemChanged(viewModel.feedReplyList.size + 2)
            }
        }
    }

    private fun refreshData() {
        viewModel.firstVisibleItemPosition = -1
        viewModel.lastVisibleItemPosition = -1
        viewModel.lastVisibleItemPosition = -1
        viewModel.currentOption = 0
        viewModel.page = 1
        viewModel.isEnd = false
        viewModel.leftEnd = false
        viewModel.rightEnd = false
        viewModel.isRefreshing = true
        viewModel.isLoadMore = false
        viewModel.isNew = true
        viewModel.getFeed()
    }

    private fun initView() {
        binding.tabLayout.visibility = View.GONE
        val space = requireContext().resources.getDimensionPixelSize(R.dimen.normal_space)
        mAdapter = FeedContentAdapter(
            requireContext(),
            viewModel.feedContentList,
            viewModel.voteCommentList
        )
        mAdapter.setAppListener(this)
        mLayoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

        // https://codeantenna.com/a/2NDTnG37Vg
        mCheckForGapMethod = checkForGaps
        mCheckForGapMethod.isAccessible = true
        mMarkItemDecorInsetsDirtyMethod = markItemDecorInsetsDirty
        mMarkItemDecorInsetsDirtyMethod.isAccessible = true

        binding.recyclerView.apply {
            adapter = mAdapter
            layoutManager = mLayoutManager
            if (itemDecorationCount == 0)
                addItemDecoration(VoteItemDecoration(space))
        }
    }

    override fun onDeleteFeedReply(id: String, position: Int, rPosition: Int?) {}

    override fun onShowCollection(id: String, title: String) {}

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
        val intent = Intent(requireContext(), FeedActivity::class.java)
        intent.putExtra("type", "feed")
        intent.putExtra("id", id)
        intent.putExtra("uid", uid)
        intent.putExtra("uname", uname)
        requireContext().startActivity(intent)
    }

    override fun onPostLike(type: String?, isLike: Boolean, id: String, position: Int?) {
        if (type == "reply") {
            viewModel.likeReplyPosition = position!!
            viewModel.likeReplyId = id
            if (isLike) {
                viewModel.isPostUnLikeReply = true
                viewModel.postUnLikeReply()
            } else {
                viewModel.isPostLikeReply = true
                viewModel.postLikeReply()
            }
        } else {
            viewModel.likeFeedId = id
            if (isLike) {
                viewModel.isPostUnLikeFeed = true
                viewModel.postUnLikeFeed()
            } else {
                viewModel.isPostLikeFeed = true
                viewModel.postLikeFeed()
            }
        }
    }

    override fun onRefreshReply(listType: String) {}

}