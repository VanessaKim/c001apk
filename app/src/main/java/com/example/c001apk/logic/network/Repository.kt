package com.example.c001apk.logic.network

import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import okhttp3.MultipartBody
import kotlin.coroutines.CoroutineContext

object Repository {

    fun getHomeFeed(
        page: Int,
        firstLaunch: Int,
        installTime: String,
        firstItem: String?,
        lastItem: String?
    ) =
        fire(Dispatchers.IO) {
            val homeFeedResponse =
                Network.getHomeFeed(page, firstLaunch, installTime, firstItem, lastItem)
            if (homeFeedResponse.data.isNotEmpty())
                Result.success(homeFeedResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getFeedContent(id: String, rid: String?) = fire(Dispatchers.IO) {
        val feedResponse = Network.getFeedContent(id, rid)
        if (feedResponse != null)
            Result.success(feedResponse)
        else
            Result.failure(RuntimeException("response status is null"))
    }

    fun getFeedContentReply(
        id: String,
        listType: String,
        page: Int,
        firstItem: String?,
        lastItem: String?,
        discussMode: Int,
        feedType: String,
        blockStatus: Int,
        fromFeedAuthor: Int
    ) =
        fire(Dispatchers.IO) {
            val feedReplyResponse = Network.getFeedContentReply(
                id,
                listType,
                page,
                firstItem,
                lastItem,
                discussMode,
                feedType,
                blockStatus,
                fromFeedAuthor
            )
            if (feedReplyResponse != null)
                Result.success(feedReplyResponse)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getSearch(
        type: String,
        feedType: String,
        sort: String,
        keyWord: String,
        pageType: String,
        pageParam: String,
        page: Int,
        showAnonymous: Int
    ) =
        fire(Dispatchers.IO) {
            val searchResponse = Network.getSearch(
                type,
                feedType,
                sort,
                keyWord,
                pageType,
                pageParam,
                page,
                showAnonymous
            )
            if (searchResponse.data.isNotEmpty())
                Result.success(searchResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getReply2Reply(id: String, page: Int) =
        fire(Dispatchers.IO) {
            val searchResponse = Network.getReply2Reply(id, page)
            if (searchResponse != null)
                Result.success(searchResponse)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getTopicLayout(tag: String) =
        fire(Dispatchers.IO) {
            val topicLayoutResponse = Network.getTopicLayout(tag)
            if (topicLayoutResponse.data != null)
                Result.success(topicLayoutResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getProductLayout(id: String) =
        fire(Dispatchers.IO) {
            val topicLayoutResponse = Network.getProductLayout(id)
            if (topicLayoutResponse.data != null)
                Result.success(topicLayoutResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getUserSpace(uid: String) =
        fire(Dispatchers.IO) {
            val userResponse = Network.getUserSpace(uid)
            if (userResponse != null)
                Result.success(userResponse)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getUserFeed(uid: String, page: Int) =
        fire(Dispatchers.IO) {
            val userResponse = Network.getUserFeed(uid, page)
            if (userResponse.data.isNotEmpty())
                Result.success(userResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getAppInfo(id: String) =
        fire(Dispatchers.IO) {
            val appResponse = Network.getAppInfo(id)
            if (appResponse != null)
                Result.success(appResponse)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getAppDownloadLink(pn: String, aid: String, vc: String) =
        fire(Dispatchers.IO) {
            val appResponse = Network.getAppDownloadLink(pn, aid, vc)
            if (appResponse != null) {
                Result.success(appResponse.headers()["Location"])
            } else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getAppsUpdate(pkgs: String) =
        fire(Dispatchers.IO) {
            val multipartBody =
                MultipartBody.Part.createFormData("pkgs", pkgs)
            val appResponse = Network.getAppsUpdate(multipartBody)
            if (appResponse != null)
                Result.success(appResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getProfile(uid: String) =
        fire(Dispatchers.IO) {
            val profileResponse = Network.getProfile(uid)
            if (profileResponse.data != null)
                Result.success(profileResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getFollowList(url: String, uid: String, page: Int) =
        fire(Dispatchers.IO) {
            val feedResponse = Network.getFollowList(url, uid, page)
            if (feedResponse.data.isNotEmpty())
                Result.success(feedResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun postLikeFeed(id: String) =
        fire(Dispatchers.IO) {
            val response = Network.postLikeFeed(id)
            if (response != null)
                Result.success(response)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun postUnLikeFeed(id: String) =
        fire(Dispatchers.IO) {
            val response = Network.postUnLikeFeed(id)
            if (response != null)
                Result.success(response)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun postLikeReply(id: String) =
        fire(Dispatchers.IO) {
            val response = Network.postLikeReply(id)
            if (response != null)
                Result.success(response)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun postUnLikeReply(id: String) =
        fire(Dispatchers.IO) {
            val response = Network.postUnLikeReply(id)
            if (response != null)
                Result.success(response)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun checkLoginInfo() =
        fire(Dispatchers.IO) {
            val response = Network.checkLoginInfo()
            if (response != null)
                Result.success(response)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun preGetLoginParam() =
        fire(Dispatchers.IO) {
            val response = Network.preGetLoginParam()
            if (response != null)
                Result.success(response)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getLoginParam() =
        fire(Dispatchers.IO) {
            val response = Network.getLoginParam()
            if (response != null)
                Result.success(response)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun tryLogin(data: HashMap<String, String?>) =
        fire(Dispatchers.IO) {
            val response = Network.tryLogin(data)
            if (response != null)
                Result.success(response)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getCaptcha(url: String) = fire(Dispatchers.IO) {
        val response = Network.getCaptcha(url)
        if (response != null)
            Result.success(response)
        else
            Result.failure(RuntimeException("response status is null"))
    }

    fun getValidateCaptcha(url: String) = fire(Dispatchers.IO) {
        val response = Network.getValidateCaptcha(url)
        if (response != null)
            Result.success(response)
        else
            Result.failure(RuntimeException("response status is null"))
    }

    fun postReply(data: HashMap<String, String>, id: String, type: String) = fire(Dispatchers.IO) {
        val replyResponse = Network.postReply(data, id, type)
        if (replyResponse != null)
            Result.success(replyResponse)
        else
            Result.failure(RuntimeException("response status is null"))
    }

    fun getDataList(url: String, title: String, subTitle: String?, lastItem: String?, page: Int) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.getDataList(url, title, subTitle, lastItem, page)
            if (dataResponse.data.isNotEmpty())
                Result.success(dataResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getDyhDetail(dyhId: String, type: String, page: Int) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.getDyhDetail(dyhId, type, page)
            if (dataResponse.data.isNotEmpty())
                Result.success(dataResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getSmsToken(type: String, data: HashMap<String, String?>) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.getSmsToken(type, data)
            if (dataResponse != null)
                Result.success(dataResponse)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getSmsLoginParam(type: String) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.getSmsLoginParam(type)
            if (dataResponse != null)
                Result.success(dataResponse)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getMessage(url: String, page: Int) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.getMessage(url, page)
            if (dataResponse.data.isNotEmpty())
                Result.success(dataResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun postFollowUnFollow(url: String, uid: String) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.postFollowUnFollow(url, uid)
            if (dataResponse.data != null)
                Result.success(dataResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun postCreateFeed(data: HashMap<String, String?>) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.postCreateFeed(data)
            if (dataResponse != null)
                Result.success(dataResponse)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun postRequestValidate(data: HashMap<String, String?>) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.postRequestValidate(data)
            if (dataResponse != null)
                Result.success(dataResponse)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getVoteComment(
        fid: String, extraKey: String, page: Int, firstItem: String?, lastItem: String?,
    ) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.getVoteComment(fid, extraKey, page, firstItem, lastItem)
            if (dataResponse != null)
                Result.success(dataResponse)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getProductList() =
        fire(Dispatchers.IO) {
            val dataResponse = Network.getProductList()
            if (dataResponse.data != null)
                Result.success(dataResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getCollectionList(url: String, uid: String?, id: String?, showDefault: Int, page: Int) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.getCollectionList(url, uid, id, showDefault, page)
            if (dataResponse.data != null)
                Result.success(dataResponse.data)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun postDelete(url: String, id: String) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.postDelete(url, id)
            if (dataResponse != null)
                Result.success(dataResponse)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun postFollow(data: HashMap<String, String>) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.postFollow(data)
            if (dataResponse != null)
                Result.success(dataResponse)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    fun getFollow(url: String, tag: String?, id: String?) =
        fire(Dispatchers.IO) {
            val dataResponse = Network.getFollow(url, tag, id)
            if (dataResponse != null)
                Result.success(dataResponse)
            else
                Result.failure(RuntimeException("response status is null"))
        }

    private fun <T> fire(context: CoroutineContext, block: suspend () -> Result<T>) =
        liveData<Result<T>>(context) {
            val result = try {
                block()
            } catch (e: Exception) {
                Result.failure<T>(e)
            }
            emit(result)
        }

}