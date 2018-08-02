package io.userfeeds.cryptocache.opensea

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.springframework.stereotype.Component
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

@Component
class OpenSeaFacade(private val openSeaRepository: OpenSeaRepository) {

    private val cache = ConcurrentHashMap<Asset, Observable<OpenSeaData>>()
    private val api = Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.from(Executors.newFixedThreadPool(10))))
            .client(OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }).build())
            .baseUrl("https://opensea-api.herokuapp.com/")
            .build()
            .create(OpenSeaApi::class.java)

    fun asset(context: String): Observable<OpenSeaData> {
        val asset: Asset = context.substringAfter(":")
                .split(":")
                .let { (address, token) -> address to token }
        return cache.getOrPut(asset) {
            getAssetFromPersistentCache(asset).orElse(getAssetFromApi(asset))
        }
    }

    private fun getAssetFromPersistentCache(asset: Asset): Optional<Observable<OpenSeaData>> {
        return openSeaRepository.findById(asset.toString())
                .map { Observable.just(it) }
    }

    private fun getAssetFromApi(asset: Asset): Observable<OpenSeaData> {
        return api.asset(asset.address, asset.token)
                .map {
                    OpenSeaData(
                            asset = asset.toString(),
                            backgroundColor = it.backgroundColor,
                            imageUrl = it.imageUrl,
                            name = it.name
                    )
                }
                .doOnNext {
                    cache[asset] = Observable.just(it)
                    openSeaRepository.save(it)
                }
                .share()
    }
}

typealias Asset = Pair<String, String>

val Asset.address
    get() = this.first

val Asset.token
    get() = this.second