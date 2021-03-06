package io.userfeeds.cryptocache

import io.userfeeds.cryptocache.opensea.OpenSeaItemInterceptor


typealias ContextItem = MutableMap<String, Any>

val ContextItem.target
    get() = this["target"] as? String

val ContextItem.context
    get() = this["context"] as String?

val ContextItem.about
    get() = this["about"] as String?

class ContextItemVisitor : OpenSeaItemInterceptor.Visitor<ContextItem> {
    override fun visit(item: ContextItem, accept: (ContextItem) -> Unit) = accept(item)
}