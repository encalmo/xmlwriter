package org.encalmo.utils

import org.encalmo.utils.StatementsCache

type DynamicTagName = (cache: StatementsCache) ?=> cache.quotes.reflect.Term
type TagName = String | DynamicTagName

object TagName {
  def apply(value: String): TagName = value
  def apply(using cache: StatementsCache)(value: cache.quotes.reflect.Term): TagName =
    (cache2: StatementsCache) ?=> value.asInstanceOf[cache2.quotes.reflect.Term]
}

extension (tagName: TagName) {
  def resolve(using cache: StatementsCache): cache.quotes.reflect.Term =
    import cache.quotes.reflect.*
    tagName match {
      case string: String => Literal(StringConstant(string))
      case _              => tagName.asInstanceOf[DynamicTagName](using cache)
    }

  def show(using cache: StatementsCache): String =
    import cache.quotes.reflect.*
    tagName match {
      case string: String => string
      case _              => tagName.asInstanceOf[DynamicTagName](using cache).show(using Printer.TreeCode)
    }
}
