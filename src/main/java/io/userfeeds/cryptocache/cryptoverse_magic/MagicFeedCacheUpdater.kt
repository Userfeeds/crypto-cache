package io.userfeeds.cryptocache.cryptoverse_magic

import io.userfeeds.cryptocache.logger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

@Component
class MagicFeedCacheUpdater(private val repository: MagicFeedRepository) {

    private val api = Retrofit.Builder()
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create())
            .baseUrl("https://api.userfeeds.io/ranking/")
            .client(OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor())
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build())
            .build()
            .create(MagicFeedApi::class.java)

    @Scheduled(fixedDelay = 1_000)
    fun updateCache() {
        val oldCache = repository.cache
        val idToOldRoot = oldCache.allItems.associateBy { it["id"] }
        val newAllItems = api.getFeed().blockingFirst().items
        val version = System.currentTimeMillis()
        (listOf(null) + newAllItems).zipWithNext().forEach { (prev, current) ->
            val oldItem = idToOldRoot[current!!["id"]]
            current["version"] = if (equalByAmountOfRepliesAndLikes(current, oldItem)) (oldItem!!["version"] as Long) else version
            prev?.get("id")?.let { current["after"] = it }
        }
        repository.cache = Cache(newAllItems, version)
        logger.info("Update cache ${javaClass.simpleName}")
    }

    private fun equalByAmountOfRepliesAndLikes(newItem: MutableMap<String, Any>, oldItem: MutableMap<String, Any>?): Boolean {
        if (oldItem == null) {
            return false
        }
        if ((newItem["likes"] as List<*>).size != (oldItem["likes"] as List<*>).size) {
            return false
        }
        if ((newItem["replies"] as List<*>).size != (oldItem["replies"] as List<*>).size) {
            return false
        }
        val idToOldReply = (oldItem["replies"] as List<Map<String, Any>>).associateBy { it["id"] }
        (newItem["replies"] as List<Map<String, Any>>).forEach {
            val oldReply = idToOldReply[it["id"]] ?: return false
            if ((it["likes"] as List<*>).size != (oldReply["likes"] as List<*>).size) {
                return false
            }
        }
        return true
    }
}