package org.encalmo.utils

import org.encalmo.utils.StatementsCache
import org.encalmo.utils.AnnotationUtils.AnnotationInfo

/** Instance of this trait visits the structure (tree) of some Scala type and value
  * @see
  *   https://refactoring.guru/design-patterns/visitor
  */
trait TypeTreeVisitor {

  /** Custom context passed between visitor methods during the walk of the type tree structure. */
  type Context

  /** Create a prefix to use for some intermediate variables */
  def createVariableNamePrefix(using cache: StatementsCache)(context: Context): String

  /** Before visiting a node in the type tree. */
  def beforeNode(using cache: StatementsCache)(annotations: Set[AnnotationInfo], context: Context): Context

  /** After visiting a node in the type tree. */
  def afterNode(using cache: StatementsCache)(annotations: Set[AnnotationInfo], context: Context): Unit

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
  ): Option[Unit]

  /** Maybe summon an existing typeclass instance to process the node instead of walking the tree further. */
  def maybeSummonTypeclassInstance(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: Context
  ): Option[Unit]

  /** Visit a node represented by a string value. */
  def visitAsString(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: Context
  ): Unit

  /** Visit a node holding Some value. */
  def visitOptionSome(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  def visitOptionNone(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context
  ): Unit

  /** Visit a node holding Either Left value. */
  def visitEitherLeft(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** Visit a node holding Either Right value. */
  def visitEitherRight(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** Visit a node holding an opaque type value. */
  def visitOpaqueType(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      upperBoundTpe: Option[cache.quotes.reflect.TypeRepr],
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** Before visiting a case class node in the type tree. */
  def beforeCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit a field of a case class node in the type tree. */
  def visitCaseClassField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** After visiting a case class node in the type tree. */
  def afterCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  /** Before visiting an enum node in the type tree. */
  def beforeEnum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit a case value of an enum node in the type tree. */
  def visitEnumCaseValue(using
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

  /** Visit a case class of an enum node in the type tree. */
  def visitEnumCaseClass(using
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

  /** After visiting an enum node in the type tree. */
  def afterEnum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  /** Before visiting a collection node in the type tree. */
  def beforeCollection(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit an item of a collection node in the type tree. */
  def visitCollectionItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** After visiting a collection node in the type tree. */
  def afterCollection(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  /** Before visiting an array node in the type tree. */
  def beforeArray(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit an item of an array node in the type tree. */
  def visitArrayItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** After visiting an array node in the type tree. */
  def afterArray(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  /** Before visiting a tuple node in the type tree. */
  def beforeTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit an item of a tuple node in the type tree. */
  def visitTupleItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** After visiting a tuple node in the type tree. */
  def afterTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  /** Before visiting a named tuple node in the type tree. */
  def beforeNamedTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit a field of a named tuple node in the type tree. */
  def visitNamedTupleItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** After visiting a namedtuple node in the type tree. */
  def afterNamedTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  /** Before visiting a Selectable node in the type tree. */
  def beforeSelectable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      fields: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit a field of a Selectable node in the type tree. */
  def visitSelectableField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** After visiting a Selectable node in the type tree. */
  def afterSelectable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  /** Visit a member type of a union type node in the type tree. */
  def visitUnionMember(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** Before visiting a union type node in the type tree. */
  def beforeUnion(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** After visiting a union type node in the type tree. */
  def afterUnion(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  /** Before visiting a Java Iterable node in the type tree. */
  def beforeJavaIterable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit an item of a Java Iterable node in the type tree. */
  def visitJavaIterableItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** After visiting a Java Iterable node in the type tree. */
  def afterJavaIterable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  /** Before visiting a Map node in the type tree. */
  def beforeMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit an entry of a Map node in the type tree. */
  def visitMapEntry(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTerm: cache.quotes.reflect.Term,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** After visiting a Map node in the type tree. */
  def afterMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  /** Before visiting a Java Map node in the type tree. */
  def beforeJavaMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit an entry of a Java Map node in the type tree. */
  def visitJavaMapEntry(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTerm: cache.quotes.reflect.Term,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** After visiting a Java Map node in the type tree. */
  def afterJavaMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit

  /** Before visiting a Java Record node in the type tree. */
  def beforeJavaRecord(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context

  /** Visit a field of a Java Record node in the type tree. */
  def visitJavaRecordField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit

  /** After visiting a Java Record node in the type tree. */
  def afterJavaRecord(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit
}
