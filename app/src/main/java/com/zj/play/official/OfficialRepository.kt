package com.zj.play.official

import android.app.Application
import com.zj.core.util.DataStoreUtils
import com.zj.model.pojo.QueryArticle
import com.zj.model.room.PlayDatabase
import com.zj.model.room.entity.OFFICIAL
import com.zj.network.base.PlayAndroidNetwork
import com.zj.network.repository.DOWN_OFFICIAL_ARTICLE_TIME
import com.zj.network.repository.FOUR_HOUR
import com.zj.network.repository.fire
import kotlinx.coroutines.flow.first

/**
 * 版权：Zhujiang 个人版权
 *
 * @author zhujiang
 * 创建日期：2020/9/10
 * 描述：PlayAndroid
 *
 */
class OfficialRepository(private val application: Application) {

    private val projectClassifyDao = PlayDatabase.getDatabase(application).projectClassifyDao()
    private val articleListDao = PlayDatabase.getDatabase(application).browseHistoryDao()

    /**
     * 获取公众号标题列表
     */
    fun getWxArticleTree(isRefresh: Boolean) = fire {
        val projectClassifyLists = projectClassifyDao.getAllOfficial()
        if (projectClassifyLists.isNotEmpty() && !isRefresh) {
            Result.success(projectClassifyLists)
        } else {
            val projectTree = PlayAndroidNetwork.getWxArticleTree()
            if (projectTree.errorCode == 0) {
                val projectList = projectTree.data
                projectClassifyDao.insertList(projectList)
                Result.success(projectList)
            } else {
                Result.failure(RuntimeException("response status is ${projectTree.errorCode}  msg is ${projectTree.errorMsg}"))
            }
        }

    }

    /**
     * 获取具体公众号文章列表
     * @param page 页码
     * @param cid 公众号id
     */
    fun getWxArticle(query: QueryArticle) = fire {
        if (query.page == 1) {
            val dataStore = DataStoreUtils.getInstance(application)
            val articleListForChapterId =
                articleListDao.getArticleListForChapterId(OFFICIAL, query.cid)
            var downArticleTime : Long = System.currentTimeMillis()
            dataStore.readLongFlow(DOWN_OFFICIAL_ARTICLE_TIME).first {
                downArticleTime = it
                true
            }
            if (articleListForChapterId.isNotEmpty() && downArticleTime > 0 && downArticleTime - System.currentTimeMillis() < FOUR_HOUR && !query.isRefresh) {
                Result.success(articleListForChapterId)
            } else {
                val projectTree = PlayAndroidNetwork.getWxArticle(query.page, query.cid)
                if (projectTree.errorCode == 0) {
                    if (articleListForChapterId.isNotEmpty() && articleListForChapterId[0].link == projectTree.data.datas[0].link && !query.isRefresh) {
                        Result.success(articleListForChapterId)
                    } else {
                        projectTree.data.datas.forEach {
                            it.localType = OFFICIAL
                        }
                        DataStoreUtils.getInstance(application).saveLongData(DOWN_OFFICIAL_ARTICLE_TIME,System.currentTimeMillis())
                        if (query.isRefresh) {
                            articleListDao.deleteAll(OFFICIAL, query.cid)
                        }
                        articleListDao.insertList(projectTree.data.datas)
                        Result.success(projectTree.data.datas)
                    }
                } else {
                    Result.failure(RuntimeException("response status is ${projectTree.errorCode}  msg is ${projectTree.errorMsg}"))
                }
            }
        } else {
            val projectTree = PlayAndroidNetwork.getWxArticle(query.page, query.cid)
            if (projectTree.errorCode == 0) {
                Result.success(projectTree.data.datas)
            } else {
                Result.failure(RuntimeException("response status is ${projectTree.errorCode}  msg is ${projectTree.errorMsg}"))
            }
        }

    }


}