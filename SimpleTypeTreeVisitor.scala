package org.encalmo.utils

import org.encalmo.utils.StatementsCache
import org.encalmo.utils.AnnotationUtils.AnnotationInfo
import org.encalmo.utils.TagName

/** Instance of this trait visits the structure (tree) of some Scala type and value
  * @see
  *   https://refactoring.guru/design-patterns/visitor
  */
trait SimpleTypeTreeVisitor extends TypeTreeVisitor {

  /** Before visiting a product type node in the type tree. */
  def beforeProduct(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit a field of a product type node in the type tree. */
  def visitProductField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: TagName,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** After visiting a product type node in the type tree. */
  def afterProduct(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  /** Before visiting a sum type node in the type tree. */
  def beforeSum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit a case of a sum type node in the type tree. */
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
  ): Unit

  /** After visiting a sum type node in the type tree. */
  def afterSum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  // ---------------------------------------------------
  // Concrete implementations of the visitor methods
  // ---------------------------------------------------

  /** Maybe process the node directly without walking the tree further. */
  def maybeProcessNodeDirectly(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Option[Unit] = None

  /** Maybe summon an existing typeclass instance to process the node instead of walking the tree further. */
  def maybeSummonTypeclassInstance(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: Context
  ): Option[Unit] = None

  /** Visit a node holding Some value. */
  inline def visitOptionSome(using
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

  inline def visitOptionNone(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context
  ): Unit = {}

  /** Visit a node holding Either Left value. */
  inline def visitEitherLeft(using
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

  /** Visit a node holding Either Right value. */
  inline def visitEitherRight(using
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

  /** Visit a node holding an opaque type value. */
  inline def visitOpaqueType(using
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
  inline def beforeCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, valueTerm, annotations, context)

  /** Visit a field of a case class node in the type tree. */
  inline def visitCaseClassField(using
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
  inline def afterCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)

  /** Before visiting an enum node in the type tree. */
  inline def beforeEnum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeSum(tpe, valueTerm, annotations, context)

  /** Visit a case value of an enum node in the type tree. */
  inline def visitEnumCaseValue(using
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
  inline def visitEnumCaseClass(using
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
  inline def afterEnum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = afterSum(tpe, context)

  /** Before visiting an array node in the type tree. */
  inline def beforeArray(using
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
  inline def visitArrayItem(using
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
  inline def afterArray(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterCollection(tpe, context)

  /** Before visiting a tuple node in the type tree. */
  inline def beforeTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeCollection(tpe, tpe, valueTerm, annotations, context)

  /** Visit an item of a tuple node in the type tree. */
  inline def visitTupleItem(using
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
  inline def afterTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterCollection(tpe, context)

  /** Before visiting a named tuple node in the type tree. */
  inline def beforeNamedTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, valueTerm, annotations, context)

  /** Visit a field of a named tuple node in the type tree. */
  inline def visitNamedTupleItem(using
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
  inline def afterNamedTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)

  /** Before visiting a Selectable node in the type tree. */
  inline def beforeSelectable(using
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
  inline def visitSelectableField(using
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
  inline def afterSelectable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)

  /** Before visiting a union type node in the type tree. */
  inline def beforeUnion(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeSum(tpe, valueTerm, annotations, context)

  /** Visit a member type of a union type node in the type tree. */
  inline def visitUnionMember(using
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
  inline def afterUnion(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = afterSum(tpe, context)

  /** Before visiting a Java Iterable node in the type tree. */
  inline def beforeJavaIterable(using
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
  inline def visitJavaIterableItem(using
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
  inline def afterJavaIterable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = afterCollection(tpe, context)

  /** Before visiting a Map node in the type tree. */
  inline def beforeMap(using
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
  inline def visitMapEntry(using
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
  inline def afterMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterProduct(tpe, context)

  /** Before visiting a Java Map node in the type tree. */
  inline def beforeJavaMap(using
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
  inline def visitJavaMapEntry(using
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
  inline def afterJavaMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = afterMap(tpe, context)

  /** Before visiting a Java Record node in the type tree. */
  inline def beforeJavaRecord(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context =
    beforeProduct(tpe, valueTerm, annotations, context)

  /** Visit a field of a Java Record node in the type tree. */
  inline def visitJavaRecordField(using
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
  inline def afterJavaRecord(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = afterProduct(tpe, context)
}
