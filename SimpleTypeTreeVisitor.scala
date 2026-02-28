package org.encalmo.utils

import org.encalmo.utils.StatementsCache
import org.encalmo.utils.AnnotationUtils.AnnotationInfo
import org.encalmo.utils.TagName

/** Simplified version of TypeTreeVisitor leaving only seven methods to be implemented. */
trait SimpleTypeTreeVisitor extends TypeTreeVisitor {

  /** Before visiting a product type node in the type tree. */
  def beforeProduct(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    context
  }

  /** Visit a field of a product type node in the type tree. Default implementation just visits the node without any
    * special processing.
    */
  def visitProductField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: TagName,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = false,
      context = context
    )

  /** After visiting a product type node in the type tree. */
  def afterProduct(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {}

  /** Before visiting a sum type node in the type tree. */
  def beforeSum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    context
  }

  /** Visit a case of a sum type node in the type tree. Default implementation just visits the node without any special
    * processing.
    */
  def visitSumCase(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = isCollectionItem,
      context = context
    )

  /** After visiting a sum type node in the type tree. */
  def afterSum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {}

  /** After visiting a node in the type tree. Default implementation does nothing. */
  inline override def afterNode(using
      cache: StatementsCache
  )(annotations: Set[AnnotationInfo], context: Context): Unit = {}

  /** Visit a node represented by a primitive-like values, including BigDecimal. Default implementation does nothing. */
  inline override def visitPrimitive(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: Context
  ): Unit = {}

  /** Visit a node represented by a string value. Default implementation does nothing. */
  inline override def visitAsString(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: Context
  ): Unit = {}

  /** Before visiting a collection node in the type tree. Default implementation does nothing. */
  inline override def beforeCollection(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    context
  }

  /** Visit an item of a collection node in the type tree. Default implementation just visits the node without any
    * special processing.
    */
  inline override def visitCollectionItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = true,
      context = context
    )

  /** After visiting a collection node in the type tree. Default implementation does nothing. */
  inline override def afterCollection(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {}

  /** Create a prefix to use for some intermediate variables. Default implementation returns "it".
    */
  inline override def createVariableNamePrefix(using cache: StatementsCache)(context: Context): String =
    "it"

  /** Maybe process the node directly without walking the tree further. Default implementation returns None, which means
    * the node will be visited by the default implementation of the visit method.
    */
  inline override def maybeProcessNodeDirectly(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Option[Unit] = None

  /** Maybe summon an existing typeclass instance to process the node instead of walking down the tree further. Default
    * implementation returns None, which means no typeclass instance will be summoned.
    */
  inline override def maybeSummonTypeclassInstance(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: Context
  ): Option[Unit] = None

  /** Visit a node holding Some value. Default implementation just visits the node without any special processing. */
  inline override def visitOptionSome(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = isCollectionItem,
      context = context
    )
  }

  /** Visit a node holding None value. Default implementation does nothing. */
  inline override def visitOptionNone(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context
  ): Unit = {}

  /** Visit a node holding Either Left value. Default implementation just visits the node without any special
    * processing.
    */
  inline override def visitEitherLeft(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = isCollectionItem,
      context = context
    )
  }

  /** Visit a node holding Either Right value. Default implementation just visits the node without any special
    * processing.
    */
  inline override def visitEitherRight(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = isCollectionItem,
      context = context
    )
  }

  /** Visit a node holding an opaque type value. Depending on the availabilty of the upper bound type, the default
    * implementation will either visit respective upper bound type or default to processing string representation.
    */
  inline override def visitOpaqueType(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      upperBoundTpe: Option[cache.quotes.reflect.TypeRepr],
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes
    upperBoundTpe match {
      case Some(upperBoundTpe) =>
        visitNode(using cache, this)(
          tpe = upperBoundTpe,
          valueTerm = valueTerm,
          annotations = annotations,
          isCollectionItem = isCollectionItem,
          context = context
        )

      case None =>
        visitAsString(
          tpe = tpe,
          valueTerm = valueTerm.methodCall("toString", List()).toTerm,
          context = context
        )
    }
  }

  /** Before visiting a case class node in the type tree. */
  inline override def beforeCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, valueTerm, annotations, context)

  /** Visit a field of a case class node in the type tree. */
  inline override def visitCaseClassField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitProductField(tpe, name, valueTerm, annotations, context, visitNode)

  /** After visiting a case class node in the type tree. */
  inline override def afterCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)

  /** Before visiting an enum node in the type tree. */
  inline override def beforeEnum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeSum(tpe, valueTerm, annotations, context)

  /** Visit a case value of an enum node in the type tree. */
  inline override def visitEnumCaseValue(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitSumCase(tpe, name, valueTerm, annotations, isCollectionItem, context, visitNode)

  /** Visit a case class of an enum node in the type tree. */
  inline override def visitEnumCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitSumCase(tpe, name, valueTerm, annotations, isCollectionItem, context, visitNode)

  /** After visiting an enum node in the type tree. */
  inline override def afterEnum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = afterSum(tpe, context)

  /** Before visiting an array node in the type tree. */
  inline override def beforeArray(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeCollection(tpe, itemTpe, valueTerm, annotations, context)

  /** Visit an item of an array node in the type tree. */
  inline override def visitArrayItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitCollectionItem(tpe, valueTerm, annotations, context, visitNode)

  /** After visiting an array node in the type tree. */
  inline override def afterArray(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterCollection(tpe, context)

  /** Before visiting a tuple node in the type tree. */
  inline override def beforeTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeCollection(tpe, tpe, valueTerm, annotations, context)

  /** Visit an item of a tuple node in the type tree. */
  inline override def visitTupleItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitCollectionItem(tpe, valueTerm, annotations, context, visitNode)

  /** After visiting a tuple node in the type tree. */
  inline override def afterTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterCollection(tpe, context)

  /** Before visiting a named tuple node in the type tree. */
  inline override def beforeNamedTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, valueTerm, annotations, context)

  /** Visit a field of a named tuple node in the type tree. */
  inline override def visitNamedTupleItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitProductField(tpe, name, valueTerm, annotations, context, visitNode)

  /** After visiting a named tuple node in the type tree. */
  inline override def afterNamedTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)

  /** Before visiting a Selectable node in the type tree. */
  inline override def beforeSelectable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      fields: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, valueTerm, annotations, context)

  /** Visit a field of a Selectable node in the type tree. */
  inline override def visitSelectableField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitProductField(tpe, name, valueTerm, annotations, context, visitNode)

  /** After visiting a Selectable node in the type tree. */
  inline override def afterSelectable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)

  /** Before visiting a union type node in the type tree. */
  inline override def beforeUnion(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeSum(tpe, valueTerm, annotations, context)

  /** Visit a member type of a union type node in the type tree. */
  inline override def visitUnionMember(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes
    visitSumCase(tpe, TypeNameUtils.typeNameOf(tpe), valueTerm, annotations, isCollectionItem, context, visitNode)
  }

  /** After visiting a union type node in the type tree. */
  inline override def afterUnion(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = afterSum(tpe, context)

  /** Before visiting a Java Iterable node in the type tree. */
  inline override def beforeJavaIterable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeCollection(tpe, itemTpe, valueTerm, annotations, context)

  /** Visit an item of a Java Iterable node in the type tree. */
  inline override def visitJavaIterableItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitCollectionItem(tpe, valueTerm, annotations, context, visitNode)

  /** After visiting a Java Iterable node in the type tree. */
  inline override def afterJavaIterable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = afterCollection(tpe, context)

  /** Before visiting a Map node in the type tree. */
  inline override def beforeMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, valueTerm, annotations, context)

  /** Visit an entry of a Map node in the type tree. */
  inline override def visitMapEntry(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTerm: cache.quotes.reflect.Term,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitProductField(tpe, TagName(keyTerm), valueTerm, annotations, context, visitNode)

  /** After visiting a Map node in the type tree. */
  inline override def afterMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)

  /** Before visiting a Java Map node in the type tree. */
  inline override def beforeJavaMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeMap(tpe, keyTpe, valueTpe, valueTerm, annotations, context)

  /** Visit an entry of a Java Map node in the type tree. */
  inline override def visitJavaMapEntry(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTerm: cache.quotes.reflect.Term,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitMapEntry(tpe, keyTerm, valueTerm, annotations, context, visitNode)

  /** After visiting a Java Map node in the type tree. */
  inline override def afterJavaMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = afterMap(tpe, context)

  /** Before visiting a Java Record node in the type tree. */
  inline override def beforeJavaRecord(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, valueTerm, annotations, context)

  /** Visit a field of a Java Record node in the type tree. */
  inline override def visitJavaRecordField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitProductField(tpe, name, valueTerm, annotations, context, visitNode)

  /** After visiting a Java Record node in the type tree. */
  inline override def afterJavaRecord(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = afterProduct(tpe, context)
}
