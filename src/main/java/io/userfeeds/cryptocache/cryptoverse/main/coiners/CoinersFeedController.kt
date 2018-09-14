package io.userfeeds.cryptocache.cryptoverse.main.coiners

import io.userfeeds.cryptocache.cryptoverse.main.common.Controller
import io.userfeeds.cryptocache.cryptoverse.main.common.Page
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class CoinersFeedController(private val repository: CoinersFeedRepository) {

    @RequestMapping("/cryptoverse_coiners_feed")
    fun getFeed(
            @RequestParam("oldestKnown", required = false) oldestKnown: String?,
            @RequestParam("lastVersion", required = false) lastVersion: Long?,
            @RequestParam("size", required = false) size: Int?): Page {
        return Controller.getFeed(repository, oldestKnown, lastVersion, size)
    }
}
