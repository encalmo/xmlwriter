package org.encalmo.utils

import org.encalmo.utils.AnnotationUtils.*
import org.encalmo.utils.StatementsCache
import org.encalmo.utils.StatementsCache.*
import org.encalmo.utils.TypeNameUtils.*

import scala.quoted.*

/** Function to visit a structure (tree) of some Scala type and value. */
type VisitNodeFunction = (cache: StatementsCache, visitor: TypeTreeVisitor) ?=> (
    tpe: cache.quotes.reflect.TypeRepr,
    valueTerm: cache.quotes.reflect.Term,
    annotations: Set[AnnotationInfo],
    isCollectionItem: Boolean,
    context: visitor.Context
) => Unit

/** Iterator over the structure (tree) of some Scala type and value. Uses TypeTreeVisitor instance to visit the tree. */
object TypeTreeIterator {

  /** Create delayed function to visit a node of some type and value. */
  private def visitNodeFunction(
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int,
      summonTypeclassInstance: Boolean = true
  ): VisitNodeFunction =
    (cache: StatementsCache, visitor: TypeTreeVisitor) ?=>
      (
          tpe: cache.quotes.reflect.TypeRepr,
          valueTerm: cache.quotes.reflect.Term,
          annotations: Set[AnnotationInfo],
          isCollectionItem: Boolean,
          context: visitor.Context
      ) =>
        visitNode(using cache, visitor)(
          tpe = tpe,
          valueTerm = valueTerm,
          context = context,
          isCollectionItem = isCollectionItem,
          annotations = annotations,
          trace = trace,
          debugIndent = debugIndent,
          summonTypeclassInstance = summonTypeclassInstance
        )

  /** Recursive algorithm to visit value of some type and value */
  def visitNode(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      annotations: Set[AnnotationInfo],
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int,
      summonTypeclassInstance: Boolean = true
  ): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*

    val typeAnnotations = AnnotationUtils.annotationsOf(tpe)
    val valueAnnotations = getValueAnnotations(valueTerm)
    val allAnnotations = typeAnnotations ++ valueAnnotations ++ annotations

    val (context2, currentAnnotations) =
      visitor.beforeNode(
        tpe = tpe,
        valueTerm = valueTerm,
        annotations = allAnnotations,
        isCollectionItem = isCollectionItem,
        context = context
      )

    if (debugIndent >= 0) then
      trace.append(
        "  " * debugIndent
          + ":: "
          + context2
          + ": "
          + tpe.show(using Printer.TypeReprAnsiCode)
          + " " + allAnnotations.filter(_.name.contains("xml")).map(_.toString).mkString(", ")
      )

    def generateWriterExpressions: Unit = {
      tpe match {

        case TypeUtils.TypeReprIsPrimitiveOrStringOrBigDecimal() =>
          visitPrimitive(
            tpe = tpe,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            debugIndent = debugIndent
          )

        case tpe if tpe.dealias =:= TypeRepr.of[BigInt] || tpe.dealias =:= TypeRepr.of[java.math.BigInteger] =>
          visitAsString(
            tpe = tpe,
            valueTerm = valueTerm.methodCall("toString", List(Literal(IntConstant(10)))).toTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            debugIndent = debugIndent
          )

        case NamedTupleUtils.TypeReprIsNamedTuple() =>
          visitNamedTuple(
            tpe = tpe,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case TupleUtils.TypeReprIsTuple() =>
          visitTuple(
            tpe = tpe,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        // Handle Option[T] special way, omit the element if the value is None
        case OptionUtils.TypeReprIsOption(tpe) =>
          visitOption(
            tpe = tpe,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case EitherUtils.TypeReprIsEither(leftTpe, rightTpe) =>
          visitEither(
            tpe = tpe,
            leftTpe = leftTpe,
            rightTpe = rightTpe,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case MapUtils.TypeReprIsMap(keyTpe, valueTpe) =>
          visitMap(
            tpe = tpe,
            valueTerm = valueTerm,
            keyTpe = keyTpe,
            valueTpe = valueTpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case IterableUtils.TypeReprIsIterable(itemTpe) =>
          visitCollection(
            tpe = tpe,
            valueTerm = valueTerm,
            itemTpe = itemTpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case ArrayUtils.TypeReprIsArray(itemTpe) =>
          visitArray(
            tpe = tpe,
            valueTerm = valueTerm,
            itemTpe = itemTpe,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case CaseClassUtils.TypeReprIsCaseClass() => {
          visitCaseClass(
            tpe = tpe,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )
        }

        case EnumUtils.TypeReprIsEnum() =>
          visitEnum(
            tpe = tpe,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case UnionUtils.TypeReprIsUnion(_) =>
          visitUnion(
            tpe = tpe,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case SelectableUtils.TypeReprIsSelectable(fields) =>
          visitSelectable(
            tpe = tpe,
            fields = fields,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case OpaqueTypeUtils.TypeReprIsOpaqueType(upperBoundTpe) =>
          visitOpaqueType(
            tpe = tpe,
            upperBoundTpe = upperBoundTpe,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case JavaRecordUtils.TypeReprIsJavaRecord() =>
          visitJavaRecord(
            tpe = tpe,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case JavaMapUtils.TypeReprIsJavaMap(keyTpe, valueTpe) =>
          visitJavaMap(
            tpe = tpe,
            keyTpe = keyTpe,
            valueTpe = valueTpe,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case JavaIterableUtils.TypeReprIsJavaIterable(itemTpe) =>
          visitJavaIterable(
            tpe = tpe,
            itemTpe = itemTpe,
            valueTerm = valueTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            annotations = currentAnnotations,
            debugIndent = debugIndent
          )

        case _ =>
          // default to the string representation of the value
          visitAsString(
            tpe = tpe,
            valueTerm = valueTerm.methodCall("toString", List()).toTerm,
            context = context2,
            isCollectionItem = isCollectionItem,
            trace = trace,
            debugIndent = debugIndent
          )
      }
    }

    def maybeProcessNodeDirectly: Unit = {
      visitor
        .maybeProcessNodeDirectly(
          tpe = tpe,
          valueTerm = valueTerm,
          annotations = currentAnnotations,
          isCollectionItem = isCollectionItem,
          context = context2,
          visitNode = visitNodeFunction(trace, debugIndent + 1)
        )
        .getOrElse {
          generateWriterExpressions
        }
    }

    if (summonTypeclassInstance)
    then
      visitor
        .maybeSummonTypeclassInstance(tpe, valueTerm, context2)
        .getOrElse(maybeProcessNodeDirectly)
    else maybeProcessNodeDirectly

    visitor.afterNode(allAnnotations, context)
  }

  def visitPrimitive(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitPrimitive")
    visitor.visitPrimitive(tpe, valueTerm, context)
  }

  def visitAsString(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitAsString")
    visitor.visitAsString(tpe, valueTerm, context)
  }

  def visitCaseClass(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int,
      context: visitor.Context
  ): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*
    debug(trace, debugIndent, tpe, "visitCaseClass")

    val context2 = visitor.beforeCaseClass(tpe, valueTerm, annotations, context)

    cache.putMethodCallOf[Unit](
      createMethodName("CaseClass", tpe, annotations),
      List(valueNameOf(tpe)),
      List(tpe),
      List(valueTerm),
      (nested: StatementsCache) ?=>
        (arguments: List[Tree]) =>
          given nested.quotes.type = nested.quotes
          val entity = nested.quotes.reflect.Ref(arguments.head.asInstanceOf[nested.quotes.reflect.Tree].symbol)

          CaseClassUtils.visit(
            tpe = tpe.toTypeRepr,
            valueTerm = entity,
            functionOnField = { (tpe, name, valueTerm, annotations) =>
              visitor
                .visitCaseClassField(
                  tpe = tpe,
                  name = name,
                  valueTerm = valueTerm,
                  annotations = annotations,
                  context = context2,
                  visitNode = visitNodeFunction(trace, debugIndent + 1)
                )
            }
          )
      ,
      scope = StatementsCache.Scope.TopLevel
    )

    visitor.afterCaseClass(tpe, context)
  }

  def visitEnum(using
      outer: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: outer.quotes.reflect.TypeRepr,
      valueTerm: outer.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitEnum")
    given outer.quotes.type = outer.quotes
    import outer.quotes.reflect.*

    val context2 = visitor.beforeEnum(tpe, valueTerm, annotations, context)

    outer.putMethodCallOf[Unit](
      createMethodName("Enum", tpe, annotations),
      List(valueNameOf(tpe)),
      List(tpe),
      List(valueTerm),
      (cache: StatementsCache) ?=>
        (arguments: List[Tree]) =>
          given cache.quotes.type = cache.quotes
          val entity = cache.quotes.reflect.Ref(arguments.head.asInstanceOf[cache.quotes.reflect.Tree].symbol)

          cache.put(
            EnumUtils.transformToMatchTerm(
              tpe.toTypeRepr,
              valueTerm = entity,
              functionWhenCaseValue = { (tpe, name, valueTerm, caseAnnotations) =>
                block {
                  visitor.visitEnumCaseValue(
                    tpe = tpe.toTypeRepr,
                    name = name,
                    valueTerm = valueTerm.toTerm,
                    annotations = annotations ++ caseAnnotations,
                    isCollectionItem = isCollectionItem,
                    context = context2,
                    visitNode = visitNodeFunction(trace, debugIndent + 1)
                  )
                }
              },
              functionWhenCaseClass = { (tpe, name, valueTerm, caseAnnotations) =>
                block {
                  visitor.visitEnumCaseClass(
                    tpe = tpe.toTypeRepr,
                    name = name,
                    valueTerm = valueTerm.toTerm,
                    annotations = annotations ++ caseAnnotations,
                    isCollectionItem = isCollectionItem,
                    context = context2,
                    visitNode = visitNodeFunction(trace, debugIndent + 1)
                  )
                }
              }
            )
          )
      ,
      scope = StatementsCache.Scope.TopLevel
    )

    visitor.afterEnum(tpe, context)
  }

  def visitUnion(using
      outer: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: outer.quotes.reflect.TypeRepr,
      valueTerm: outer.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitUnion")
    given outer.quotes.type = outer.quotes
    import outer.quotes.reflect.*

    val context2 = visitor.beforeUnion(tpe, valueTerm, annotations, context)
    outer.putMethodCallOf[Unit](
      createMethodName("Union", tpe, annotations),
      List(valueNameOf(tpe)),
      List(tpe),
      List(valueTerm),
      (cache: StatementsCache) ?=>
        (arguments: List[Tree]) => {
          val entity = cache.quotes.reflect
            .Ref(arguments.head.asInstanceOf[cache.quotes.reflect.Tree].symbol)

          cache.put(
            UnionUtils.transformToMatchTerm(
              tpe.toTypeRepr,
              entity,
              { (tpe, valueTerm) =>
                block(
                  visitor.visitUnionMember(
                    tpe = tpe.toTypeRepr,
                    valueTerm = valueTerm.toTerm,
                    annotations = AnnotationUtils.annotationsOf(tpe.toTypeRepr) ++ annotations,
                    isCollectionItem = isCollectionItem,
                    context = context2,
                    visitNode = visitNodeFunction(trace, debugIndent + 1)
                  )
                )
              }
            )
          )
        },
      scope = StatementsCache.Scope.TopLevel
    )
    visitor.afterUnion(tpe, context)
  }

  def visitOption(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitOption")
    given cache.quotes.type = cache.quotes

    cache.put(
      OptionUtils.buildMatchTerm(
        tpe.toTypeRepr,
        target = valueTerm,
        functionOnSome = { (tpe, valueTerm) =>
          block {
            visitor.visitOptionSome(
              tpe = tpe.toTypeRepr,
              valueTerm = valueTerm.toTerm,
              annotations = annotations,
              isCollectionItem = isCollectionItem,
              context = context,
              visitNode = visitNodeFunction(trace, debugIndent + 1)
            )
          }
        },
        functionOnNone = {
          block {
            visitor.visitOptionNone(
              tpe = tpe.toTypeRepr,
              annotations = annotations,
              isCollectionItem = isCollectionItem,
              context = context
            )
          }
        }
      )
    )
  }

  def visitEither(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      leftTpe: cache.quotes.reflect.TypeRepr,
      rightTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitEither")
    given cache.quotes.type = cache.quotes

    cache.put(
      EitherUtils.buildMatchTerm(
        leftTpe.toTypeRepr,
        rightTpe.toTypeRepr,
        target = valueTerm,
        functionOnLeft = { (tpe, valueTerm) =>
          block(
            visitor.visitEitherLeft(
              tpe = tpe.toTypeRepr,
              valueTerm = valueTerm.toTerm,
              annotations = annotations,
              isCollectionItem = isCollectionItem,
              context = context,
              visitNode = visitNodeFunction(trace, debugIndent + 1)
            )
          )

        },
        functionOnRight = { (tpe, valueTerm) =>
          block(
            visitor.visitEitherRight(
              tpe = tpe.toTypeRepr,
              valueTerm = valueTerm.toTerm,
              annotations = annotations,
              isCollectionItem = isCollectionItem,
              context = context,
              visitNode = visitNodeFunction(trace, debugIndent + 1)
            )
          )
        }
      )
    )
  }

  def visitCollection(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitCollection")
    val context2 = visitor.beforeCollection(tpe, itemTpe, valueTerm, annotations, context)
    cache.put(
      IterableUtils.buildIterableLoop(
        visitor.createVariableNamePrefix(context),
        itemTpe.toTypeRepr,
        valueTerm,
        functionOnItem = { (tpe, valueTerm) =>
          block {
            visitor.visitCollectionItem(
              tpe = tpe.toTypeRepr,
              valueTerm = valueTerm.toTerm,
              annotations = annotations,
              context = context2,
              visitNode = visitNodeFunction(trace, debugIndent + 1)
            )
          }
        }
      )
    )
    visitor.afterCollection(tpe, context)
  }

  def visitArray(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitArray")
    given cache.quotes.type = cache.quotes

    val context2 = visitor.beforeArray(tpe, itemTpe, valueTerm, annotations, context)
    cache.put(
      ArrayUtils.buildArrayLoop(
        visitor.createVariableNamePrefix(context),
        itemTpe.toTypeRepr,
        valueTerm.toTerm,
        functionOnItem = { (tpe, valueTerm) =>
          block {
            visitor.visitArrayItem(
              tpe = tpe.toTypeRepr,
              valueTerm = valueTerm.toTerm,
              annotations = annotations,
              context = context2,
              visitNode = visitNodeFunction(trace, debugIndent + 1)
            )
          }
        }
      )
    )
    visitor.afterArray(tpe, context)
  }

  def visitMap(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitMap")
    given cache.quotes.type = cache.quotes

    val context2 = visitor.beforeMap(tpe, keyTpe, valueTpe, valueTerm, annotations, context)
    cache.put(
      MapUtils.buildMapLoop(
        visitor.createVariableNamePrefix(context),
        keyTpe.toTypeRepr,
        valueTpe.toTypeRepr,
        valueTerm,
        functionOnEntry = { (keyTerm, valueTerm) =>
          block {
            visitor.visitMapEntry(
              tpe = valueTpe.toTypeRepr,
              keyTerm = keyTerm.toTerm,
              valueTerm = valueTerm.toTerm,
              annotations = annotations,
              context = context2,
              visitNode = visitNodeFunction(trace, debugIndent + 1)
            )
          }
        }
      )
    )
    visitor.afterMap(tpe, context)
  }

  def visitJavaIterable(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitJavaIterable")
    given cache.quotes.type = cache.quotes
    val context2 = visitor.beforeJavaIterable(tpe, itemTpe, valueTerm, annotations, context)
    cache.put(
      JavaIterableUtils.buildIterableLoop(
        visitor.createVariableNamePrefix(context),
        itemTpe.toTypeRepr,
        valueTerm,
        functionOnItem = { (tpe, valueTerm) =>
          block {
            visitor.visitJavaIterableItem(
              tpe = tpe.toTypeRepr,
              valueTerm = valueTerm.toTerm,
              annotations = annotations,
              context = context2,
              visitNode = visitNodeFunction(trace, debugIndent + 1)
            )
          }
        }
      )
    )
    visitor.afterJavaIterable(tpe, context)
  }

  def visitJavaMap(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitJavaMap")
    given cache.quotes.type = cache.quotes

    val context2 = visitor.beforeJavaMap(tpe, keyTpe, valueTpe, valueTerm, annotations, context)
    cache.put(
      JavaMapUtils.buildMapLoop(
        visitor.createVariableNamePrefix(context),
        keyTpe.toTypeRepr,
        valueTpe.toTypeRepr,
        valueTerm,
        functionOnEntry = { (keyTerm, valueTerm) =>
          block {
            visitor.visitJavaMapEntry(
              tpe = valueTpe.toTypeRepr,
              keyTerm = keyTerm.toTerm,
              valueTerm = valueTerm.toTerm,
              annotations = annotations,
              context = context2,
              visitNode = visitNodeFunction(trace, debugIndent + 1)
            )
          }
        }
      )
    )
    visitor.afterJavaMap(tpe, context)
  }

  /** Write Scala tupes */
  def visitTuple(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitTuple")

    val context2 = visitor.beforeTuple(tpe, valueTerm, annotations, context)
    TupleUtils.visit(
      tpe.toTypeRepr,
      valueTerm = valueTerm,
      functionOnItem = { (tpe, valueTerm, index) =>
        visitor.visitTupleItem(
          tpe = tpe.toTypeRepr,
          valueTerm = valueTerm,
          annotations = annotations,
          context = context2,
          visitNode = visitNodeFunction(trace, debugIndent + 1)
        )
      }
    )
    visitor.afterTuple(tpe, context)
  }

  /** Write Scala named tuples */
  def visitNamedTuple(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitNamedTuple")

    val context2 = visitor.beforeNamedTuple(tpe, valueTerm, annotations, context)
    NamedTupleUtils.visit(
      label = None,
      tpe.toTypeRepr,
      valueTerm = valueTerm,
      functionOnField = { (tpe, name, valueTerm, index) =>
        visitor.visitNamedTupleItem(
          tpe = tpe.toTypeRepr,
          name = name,
          valueTerm = valueTerm,
          annotations = annotations,
          context = context2,
          visitNode = visitNodeFunction(trace, debugIndent + 1)
        )
      }
    )
    visitor.afterNamedTuple(tpe, context)
  }

  /** Write Scala structural types and objects extending `Selectable` with a `Fields` member type. */
  def visitSelectable(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      fields: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitSelectable")
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*

    val context2 = visitor.beforeSelectable(tpe, fields, valueTerm, annotations, context)
    cache.putMethodCallOf[Unit](
      createMethodName("Selectable", tpe, annotations),
      List(valueNameOf(tpe)),
      List(tpe),
      List(valueTerm),
      (nested: StatementsCache) ?=>
        (arguments: List[Tree]) =>
          given nested.quotes.type = nested.quotes
          val entity = nested.quotes.reflect
            .Ref(arguments.head.asInstanceOf[nested.quotes.reflect.Tree].symbol)

          SelectableUtils.visitFields(
            fieldsTpe = fields.toTypeRepr,
            valueTerm = entity.toTerm,
            functionOnField = { (tpe, name, valueTerm) =>
              visitor.visitSelectableField(
                tpe = tpe.toTypeRepr,
                name = name,
                valueTerm = valueTerm.toTerm,
                annotations = annotations,
                context = context2,
                visitNode = visitNodeFunction(trace, debugIndent + 1)
              )
            }
          )
      ,
      scope = StatementsCache.Scope.TopLevel
    )
    visitor.afterSelectable(tpe, context)
  }

  def visitJavaRecord(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*
    debug(trace, debugIndent, tpe, "visitJavaRecord")

    val context2 = visitor.beforeJavaRecord(tpe, valueTerm, annotations, context)
    cache.putMethodCallOf[Unit](
      createMethodName("Record", tpe, annotations),
      List(valueNameOf(tpe)),
      List(tpe),
      List(valueTerm),
      (nested: StatementsCache) ?=>
        (arguments: List[Tree]) =>
          given nested.quotes.type = nested.quotes
          val entity = nested.quotes.reflect
            .Ref(arguments.head.asInstanceOf[nested.quotes.reflect.Tree].symbol)

          JavaRecordUtils.visit(
            tpe = tpe.toTypeRepr,
            valueTerm = entity,
            functionOnField = { (tpe, name, valueTerm) =>
              visitor.visitJavaRecordField(
                tpe = tpe,
                name = name,
                valueTerm = valueTerm,
                annotations = annotations,
                context = context2,
                visitNode = visitNodeFunction(trace, debugIndent + 1)
              )
            }
          )
      ,
      scope = StatementsCache.Scope.TopLevel
    )
    visitor.afterJavaRecord(tpe, context)
  }

  /** Write Scala opaque types, if there is an upper bound, write the upper bound, otherwise write the string
    * representation of the value.
    */
  def visitOpaqueType(using
      cache: StatementsCache,
      visitor: TypeTreeVisitor
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      upperBoundTpe: Option[cache.quotes.reflect.TypeRepr],
      valueTerm: cache.quotes.reflect.Term,
      context: visitor.Context,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      annotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    debug(trace, debugIndent, tpe, "visitOpaqueType")
    given cache.quotes.type = cache.quotes

    visitor.visitOpaqueType(
      tpe = tpe,
      upperBoundTpe = upperBoundTpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = isCollectionItem,
      context = context,
      visitNode = visitNodeFunction(trace, debugIndent + 1)
    )
  }

  inline def debug(using
      cache: StatementsCache
  )(
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int,
      tpe: cache.quotes.reflect.TypeRepr,
      message: String
  ): Unit = {
    if (debugIndent >= 0) then trace.append("  " * debugIndent + " > " + message)
  }

  inline def createMethodName(using
      cache: StatementsCache
  )(inline name: String, tpe: cache.quotes.reflect.TypeRepr, annotations: Set[AnnotationInfo]): String = {
    "visit" + name + "_" + underscored(tpe.show(using cache.quotes.reflect.Printer.TypeReprCode))
      + annotations.hash(_.contains(".xml")).replace("-", "")
  }
}
