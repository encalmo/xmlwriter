package org.encalmo.writer.xml

import org.encalmo.utils.AnnotationUtils.*
import org.encalmo.utils.TypeNameUtils.*
import org.encalmo.utils.StringUtils
import org.encalmo.utils.StatementsCache.*
import org.encalmo.utils.StatementsCache
import org.encalmo.utils.*

import org.encalmo.writer.xml.XmlOutputBuilder
import scala.quoted.*

/** Macro parts of the XmlWriter toolkit. */
object XmlWriterMacro {

  // Set true to be able to debug the macro expansion
  // and see the the path taken by the algorithm together with the
  // annotations and the type information, and generated code.
  // This will show up in the console when the macro is expanded.
  transparent inline def shouldDebugMacroExpansion = false

  inline def write[A](inline tagName: String, expr: A)(using
      builder: XmlOutputBuilder
  ): Unit =
    ${ writeImpl[A]('{ tagName }, '{ expr }, '{ builder }, true) }

  inline def write[A](expr: A)(using
      builder: XmlOutputBuilder
  ): Unit =
    ${ writeImpl[A]('{ expr }, '{ builder }) }

  def writeImpl[A: Type](
      expr: Expr[A],
      builder: Expr[XmlOutputBuilder]
  )(using
      Quotes
  ): Expr[Unit] = {
    writeImpl(typeNameExpr[A], expr, builder, true)
  }

  def writeImpl[A: Type](
      tagName: Expr[String],
      expr: Expr[A],
      builder: Expr[XmlOutputBuilder],
      summonTypeclassInstance: Boolean
  )(using quotes: Quotes): Expr[Unit] = {
    given cache: StatementsCache = new StatementsCache
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*
    val tagName2 = tagName.value.map(TagName(_)).getOrElse(TagName(tagName.asTerm))
    writeUsingStatementsCache[A](tagName2, expr, builder, summonTypeclassInstance)
    cache.asTerm.asExprOf[Unit]
  }

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

  /** Entry method to write the value of any type to the XML output using the StatementsCache. */
  def writeUsingStatementsCache[A: Type](
      tagName: TagName,
      expr: Expr[A],
      builderExpr: Expr[XmlOutputBuilder],
      summonTypeclassInstance: Boolean
  )(using cache: StatementsCache): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*

    val builder = new Builder
    builder.initialize(builderExpr)

    val term = expr.asTerm match {
      case Inlined(_, _, t) => t
      case t                => t
    }

    val trace = scala.collection.mutable.Buffer.empty[String]
    val annotations = getValueAnnotations(term)

    writeType(
      tpe = TypeRepr.of[A],
      tagName = tagName,
      valueTerm = term,
      builder = builder,
      hasTag = false,
      isCollectionItem = false,
      currentAnnotations = annotations,
      trace = trace,
      debugIndent = 0,
      summonTypeclassInstance = summonTypeclassInstance
    )

    if shouldDebugMacroExpansion then {
      report.warning(
        trace.mkString("\n")
          + "\n\n--------------------------------\n\n"
          + cache.asTerm.show(using Printer.TreeCode)
      )
    }
  }

  /** Recursive algorithm to write the value of any type to the XML output. */
  def writeType(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      currentAnnotations: Set[AnnotationInfo],
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int,
      summonTypeclassInstance: Boolean = true
  ): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*

    val typeAnnotations = AnnotationUtils.annotationsOf(tpe)
    val valueAnnotations = getValueAnnotations(valueTerm)
    val allAnnotations = typeAnnotations ++ valueAnnotations ++ currentAnnotations

    val tagName2: TagName = tagName match {
      case string: String =>
        allAnnotations
          .getStringOrDefault[annotation.xmlTag](
            parameter = "name",
            defaultValue = string
          )
      case other => other
    }

    val isXmlContent: Boolean = allAnnotations.exists[annotation.xmlContent]
    val shouldTag: Boolean = !hasTag && !isXmlContent

    val maybeTagValue: Option[String] = allAnnotations
      .getString[annotation.xmlValue](parameter = "value")

    val maybeTagValueSelector: Option[String] = allAnnotations
      .getString[annotation.xmlValueSelector](parameter = "property")

    if shouldDebugMacroExpansion then
      trace.append(
        "  " * debugIndent
          + ":: "
          + tagName2
          + ": "
          + tpe.show(using Printer.TypeReprAnsiCode)
          + " " + allAnnotations.filter(_.name.contains("xml")).map(_.toString).mkString(", ")
      )

    def generateWriterExpressions: Unit = {
      if tpe.typeSymbol.isTypeParam then
        report.errorAndAbort(
          s"""${tpe.show} is an abstract type parameter and cannot be serialized to XML. 
  Possible solutions:
  - Add inline keyword to the method definition.
  - Add (using XmlWriter[${tpe.show}]) to the method definition
  - Define a given XmlWriter[${tpe.show}] in the current scope
  """
        )
      else
        tpe match {

          case TypeUtils.TypeReprIsPrimitiveOrStringOrBigDecimal() =>
            writeAsString(
              tpe = tpe,
              valueTerm = valueTerm,
              tagName = tagName2,
              builder = builder,
              hasTag = !shouldTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              debugIndent = debugIndent
            )

          case tpe if tpe.dealias =:= TypeRepr.of[BigInt] || tpe.dealias =:= TypeRepr.of[java.math.BigInteger] =>
            writeAsString(
              tpe = tpe,
              valueTerm = valueTerm.methodCall("toString", List(Literal(IntConstant(10)))).toTerm,
              tagName = tagName2,
              builder = builder,
              hasTag = !shouldTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              debugIndent = debugIndent
            )

          case TupleUtils.TypeReprIsTuple() =>
            writeTuple(
              tpe = tpe,
              valueTerm = valueTerm,
              tagName = tagName2,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )

          // Handle Option[T] special way, omit the element if the value is None
          case OptionUtils.TypeReprIsOption(tpe) =>
            writeOption(
              tpe = tpe,
              valueTerm = valueTerm,
              tagName = tagName2,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )

          case EitherUtils.TypeReprIsEither(leftTpe, rightTpe) =>
            writeEither(
              tpe = tpe,
              leftTpe = leftTpe,
              rightTpe = rightTpe,
              valueTerm = valueTerm,
              tagName = tagName2,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )

          case MapUtils.TypeReprIsMap(keyTpe, valueTpe) =>
            writeMap(
              tpe = tpe,
              keyTpe = keyTpe,
              valueTpe = valueTpe,
              valueTerm = valueTerm,
              tagName = tagName2,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )

          case IterableUtils.TypeReprIsIterable(itemTpe) =>
            writeCollection(
              tpe = tpe,
              itemTpe = itemTpe,
              tagName = tagName2,
              valueTerm = valueTerm,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )

          case ArrayUtils.TypeReprIsArray(itemTpe) =>
            writeArray(
              tpe = tpe,
              itemTpe = itemTpe,
              tagName = tagName2,
              valueTerm = valueTerm,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )

          case CaseClassUtils.TypeReprIsCaseClass() => {
            // prepare list of attributes for the xml opening tag
            val attributes: cache.quotes.reflect.Term = collectAttributesFromCaseClass(tpe, valueTerm)
            if shouldTag then builder.appendElementStartWithAttributes(tagName2, attributes)
            writeCaseClass(
              tpe = tpe,
              tagName = tagName2,
              valueTerm = valueTerm,
              builder = builder,
              hasTag = !hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )
            if shouldTag then builder.appendElementEnd(tagName2)
          }

          case EnumUtils.TypeReprIsEnum() =>
            writeEnum(
              tpe = tpe,
              tagName = tagName2,
              valueTerm = valueTerm,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )

          case UnionUtils.TypeReprIsUnion(_) =>
            if shouldTag then builder.appendElementStart(tagName2)
            writeUnion(
              tpe = tpe,
              tagName = tagName2,
              valueTerm = valueTerm,
              builder = builder,
              hasTag = !hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )
            if shouldTag then builder.appendElementEnd(tagName2)

          case SelectableUtils.TypeReprIsSelectable(fields) =>
            if shouldTag then builder.appendElementStart(tagName2)
            writeSelectable(
              tpe = tpe,
              fields = fields,
              tagName = tagName2,
              valueTerm = valueTerm,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )
            if shouldTag then builder.appendElementEnd(tagName2)

          case OpaqueTypeUtils.TypeReprIsOpaqueType(upperBoundTpe) =>
            writeOpaqueType(
              tpe = tpe,
              upperBoundTpe = upperBoundTpe,
              tagName = tagName2,
              valueTerm = valueTerm,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )

          case JavaRecordUtils.TypeReprIsJavaRecord() =>
            if shouldTag then builder.appendElementStart(tagName2)
            writeJavaRecord(
              tpe = tpe,
              tagName = tagName2,
              valueTerm = valueTerm,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )
            if shouldTag then builder.appendElementEnd(tagName2)

          case JavaMapUtils.TypeReprIsJavaMap(keyTpe, valueTpe) =>
            writeJavaMap(
              tpe = tpe,
              keyTpe = keyTpe,
              valueTpe = valueTpe,
              tagName = tagName2,
              valueTerm = valueTerm,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )

          case JavaIterableUtils.TypeReprIsJavaIterable(itemTpe) =>
            writeJavaIterable(
              tpe = tpe,
              itemTpe = itemTpe,
              tagName = tagName2,
              valueTerm = valueTerm,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              currentAnnotations = allAnnotations,
              debugIndent = debugIndent
            )

          case _ =>
            // default to writing the string representation of the value
            writeAsString(
              tpe = tpe,
              tagName = tagName2,
              valueTerm = valueTerm.methodCall("toString", List()).toTerm,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              trace = trace,
              debugIndent = debugIndent
            )
        }
    }

    def tryStaticValueOrSelectorExpression: Unit = {
      maybeTagValue match {
        case Some(value) =>
          debug(trace, debugIndent, tpe, tagName2, "writeXmlValue")
          if shouldTag then builder.appendElementStart(tagName2)
          builder.appendText(Literal(StringConstant(value)))
          if shouldTag then builder.appendElementEnd(tagName2)

        case None =>
          maybeTagValueSelector
            .map { selector =>
              MethodUtils.maybeSelectTerm(
                selector = selector,
                tpe = tpe,
                valueTerm = valueTerm,
                functionWhenSelected = { (tpe, value) =>
                  debug(
                    trace,
                    debugIndent,
                    tpe,
                    tagName,
                    "writeType selected by " + selector
                  )
                  writeType(
                    tpe = tpe,
                    tagName = tagName,
                    valueTerm = value,
                    builder = builder,
                    hasTag = hasTag,
                    isCollectionItem = isCollectionItem,
                    currentAnnotations = allAnnotations.remove[annotation.xmlValueSelector],
                    trace = trace,
                    debugIndent = debugIndent + 1
                  )
                }
              )
            }
            .getOrElse(generateWriterExpressions)
      }
    }

    if (summonTypeclassInstance) then
      tpe.asType match {
        case '[a] =>
          Expr.summon[XmlWriter[a]] match {
            case Some(writer) =>
              debug(
                trace,
                debugIndent,
                tpe,
                tagName2,
                s"use XmlWriter[${TypeRepr.of[a].show}] createTag=${shouldTag}"
              )

              cache.put(
                writer.asTerm.methodCall(
                  "write",
                  List(
                    tagName2.resolve,
                    valueTerm,
                    Literal(BooleanConstant(shouldTag))
                  ),
                  List(cache.getValueRef("builder"))
                )
              )

            case None => tryStaticValueOrSelectorExpression
          }
      }
    else tryStaticValueOrSelectorExpression

  }

  def writeAsString(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    debug(trace, debugIndent, tpe, tagName, "writePrimitive")
    if !hasTag then builder.appendElementStart(tagName)
    builder.appendText(valueTerm)
    if !hasTag then builder.appendElementEnd(tagName)
  }

  def collectAttributesFromCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term
  ): cache.quotes.reflect.Term = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*
    val list = collection.mutable.ListBuffer.empty[cache.quotes.reflect.Term]
    CaseClassUtils.visit(
      tpe,
      valueTerm,
      { (tpe, name, value, annotations) =>
        {
          val isAttribute = annotations.exists[annotation.xmlAttribute]
          if isAttribute then {

            val tagName2 = annotations
              .getStringOrDefault[annotation.xmlTag](parameter = "name", defaultValue = name)

            list.append(
              TupleUtils.createTuple2(
                Literal(StringConstant(tagName2)),
                StringUtils.applyToString(value)
              )
            )
          }
        }
      }
    )
    IterableUtils.createStaticList(TypeRepr.of[Tuple2[String, String]], list.toList)
  }

  def writeCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*
    debug(trace, debugIndent, tpe, tagName, "writeCaseClass")

    cache.putMethodCallOf[Unit](
      createMethodName("CaseClass", tpe, currentAnnotations),
      List(valueNameOf(tpe)),
      List(tpe),
      List(valueTerm),
      (nested: StatementsCache) ?=>
        (arguments: List[Tree]) =>
          given nested.quotes.type = nested.quotes
          val entity = nested.quotes.reflect.Ref(arguments.head.asInstanceOf[nested.quotes.reflect.Tree].symbol)

          CaseClassUtils.visit(
            tpe.toTypeRepr,
            entity,
            { (tpe, name, value, annotations) =>
              {
                val isAttribute = annotations.exists[annotation.xmlAttribute]
                // skip field if was aleady written as an attribute
                if !isAttribute then {
                  writeType(
                    tpe = tpe,
                    tagName = name,
                    valueTerm = value,
                    builder = builder,
                    hasTag = false,
                    isCollectionItem = false,
                    currentAnnotations = AnnotationUtils.annotationsOf(tpe.toTypeRepr) ++ annotations,
                    trace = trace,
                    debugIndent = debugIndent + 1
                  )
                }
              }
            }
          )
      ,
      scope = StatementsCache.Scope.TopLevel
    )
  }

  def writeEnum(using
      outer: StatementsCache
  )(
      tpe: outer.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: outer.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given outer.quotes.type = outer.quotes
    import outer.quotes.reflect.*

    val hasEnumCaseClasses = EnumUtils.hasEnumCaseClasses(tpe.toTypeRepr)
    // Shall we wrap the enum case values in a tag?
    val shouldTag = !hasTag

    debug(trace, debugIndent, tpe, tagName, "writeEnum")

    outer.putMethodCallOf[Unit](
      createMethodName("Enum", tpe, currentAnnotations),
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
              functionWhenCaseValue = { (tpe, name, value, annotations) =>
                {
                  val allAnnotations = currentAnnotations ++ annotations

                  debug(
                    trace,
                    debugIndent,
                    tpe,
                    name,
                    "writeEnum/functionWhenCaseValue"
                  )

                  val customTagValue: Option[cache.quotes.reflect.Term] =
                    allAnnotations.getTerm[annotation.xmlValue](parameter = "value")

                  val tagValue = customTagValue.getOrElse(value)

                  if shouldTag
                  then
                    block {
                      builder.appendElementStart(tagName)
                      builder.appendText(tagValue)
                      builder.appendElementEnd(tagName)
                    }
                  else if hasEnumCaseClasses
                  then
                    block {
                      builder.appendElementStart(TagName(tagValue.toTerm.applyToString))
                      builder.appendElementEnd(TagName(tagValue.toTerm.applyToString))
                    }
                  else
                    block {
                      builder.appendText(tagValue)
                    }
                }
              },
              functionWhenCaseClass = { (tpe, name, value, annotations) =>
                {
                  val allAnnotations = currentAnnotations ++ annotations

                  debug(
                    trace,
                    debugIndent,
                    tpe,
                    name,
                    "writeEnum/functionWhenCaseClass"
                  )

                  val body = block {
                    writeType(
                      tpe = tpe.toTypeRepr,
                      tagName = name,
                      valueTerm = value.toTerm,
                      builder = builder,
                      hasTag = false,
                      isCollectionItem = isCollectionItem,
                      currentAnnotations = allAnnotations,
                      trace = trace,
                      debugIndent = debugIndent + 1
                    )
                  }

                  if shouldTag
                  then {
                    block {
                      builder.appendElementStart(tagName)
                      put(body.toTerm)
                      builder.appendElementEnd(tagName)
                    }
                  } else {
                    body
                  }
                }
              }
            )
          )
      ,
      scope = StatementsCache.Scope.TopLevel
    )
  }

  def writeUnion(using
      outer: StatementsCache
  )(
      tpe: outer.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: outer.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given outer.quotes.type = outer.quotes
    import outer.quotes.reflect.*
    debug(trace, debugIndent, tpe, tagName, "writeUnion")

    outer.putMethodCallOf[Unit](
      createMethodName("Union", tpe, currentAnnotations),
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
              { (tpe, value) =>
                block(
                  writeType(
                    tpe = tpe.toTypeRepr,
                    tagName = tagName,
                    valueTerm = value.toTerm,
                    builder = builder,
                    hasTag = hasTag,
                    isCollectionItem = isCollectionItem,
                    currentAnnotations = AnnotationUtils.annotationsOf(tpe.toTypeRepr) ++ currentAnnotations,
                    trace = trace,
                    debugIndent = debugIndent + 1
                  )
                )
              }
            )
          )
        },
      scope = StatementsCache.Scope.TopLevel
    )
  }

  def writeOption(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    debug(trace, debugIndent, tpe, tagName, "writeOption")

    val itemLabel =
      if isCollectionItem && tagName == "Option"
      then typeNameOf(tpe)
      else tagName

    cache.put(
      OptionUtils.buildMatchTerm(
        tpe.toTypeRepr,
        target = valueTerm,
        functionOnSome = { (tpe, value) =>
          block {
            writeType(
              tpe = tpe.toTypeRepr,
              tagName = itemLabel,
              valueTerm = value.toTerm,
              builder = builder,
              hasTag = hasTag,
              isCollectionItem = isCollectionItem,
              currentAnnotations = currentAnnotations,
              trace = trace,
              debugIndent = debugIndent + 1
            )
          }
        },
        functionOnNone = unit
      )
    )
  }

  def writeEither(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      leftTpe: cache.quotes.reflect.TypeRepr,
      rightTpe: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    debug(trace, debugIndent, tpe, tagName, "writeEither")

    cache.put(
      EitherUtils.buildMatchTerm(
        leftTpe.toTypeRepr,
        rightTpe.toTypeRepr,
        target = valueTerm,
        functionOnLeft = { (tpe, value) =>
          {
            val itemLabel =
              if isCollectionItem && tagName == "Either"
              then typeNameOf(tpe)
              else tagName

            block(
              writeType(
                tpe = leftTpe.toTypeRepr,
                tagName = itemLabel,
                valueTerm = value.toTerm,
                builder = builder,
                hasTag = hasTag,
                isCollectionItem = isCollectionItem,
                currentAnnotations = currentAnnotations,
                trace = trace,
                debugIndent = debugIndent + 1
              )
            )
          }
        },
        functionOnRight = { (tpe, value) =>
          {
            val itemLabel =
              if isCollectionItem && tagName == "Either"
              then typeNameOf(tpe)
              else tagName

            block(
              writeType(
                tpe = rightTpe.toTypeRepr,
                tagName = itemLabel,
                valueTerm = value.toTerm,
                builder = builder,
                hasTag = hasTag,
                isCollectionItem = isCollectionItem,
                currentAnnotations = currentAnnotations,
                trace = trace,
                debugIndent = debugIndent + 1
              )
            )
          }
        }
      )
    )
  }

  def writeCollection(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    // get the name of the item tag from the annotation or default to the type name
    val itemAnnotations = currentAnnotations
      .find[annotation.xmlItemTag]
      .map(_.renameAs[annotation.xmlTag])
      .toSet
    // check if the item tags should be skipped
    val skipItemTags = currentAnnotations.exists[annotation.xmlNoItemTags]
    val isXmlContent = currentAnnotations.exists[annotation.xmlContent]
    val shouldTag = !hasTag && !isXmlContent

    debug(
      trace,
      debugIndent,
      tpe,
      tagName,
      "writeCollection"
    )

    if shouldTag then builder.appendElementStart(tagName)
    cache.put(
      IterableUtils.buildIterableLoop(
        toValueName(tagName.show),
        itemTpe.toTypeRepr,
        valueTerm,
        functionOnItem = { (tpe, value) =>
          block {
            writeType(
              tpe = tpe.toTypeRepr,
              tagName = typeNameOf(tpe),
              valueTerm = value.toTerm,
              builder = builder,
              hasTag = skipItemTags,
              isCollectionItem = true,
              currentAnnotations = itemAnnotations,
              trace = trace,
              debugIndent = debugIndent + 1
            )
          }
        }
      )
    )
    if shouldTag then builder.appendElementEnd(tagName)
  }

  def writeArray(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    // get the name of the item tag from the annotation or default to the type name
    val itemLabel =
      currentAnnotations.getStringOrDefault[annotation.xmlItemTag](
        parameter = "name",
        defaultValue = typeNameOf(itemTpe)
      )
    // check if the item tags should be skipped
    val skipItemTags = currentAnnotations.exists[annotation.xmlNoItemTags]
    val isXmlContent = currentAnnotations.exists[annotation.xmlContent]
    val shouldTag = !hasTag && !isXmlContent

    debug(trace, debugIndent, tpe, tagName, "writeArray")

    if shouldTag then builder.appendElementStart(tagName)
    cache.put(
      ArrayUtils.buildArrayLoop(
        valueNameOf(itemTpe),
        itemTpe.toTypeRepr,
        valueTerm.toTerm,
        functionOnItem = { (tpe, value) =>
          block {
            writeType(
              tpe = tpe.toTypeRepr,
              tagName = itemLabel,
              valueTerm = value.toTerm,
              builder = builder,
              hasTag = skipItemTags,
              isCollectionItem = true,
              currentAnnotations = Set.empty,
              trace = trace,
              debugIndent = debugIndent + 1
            )
          }
        }
      )
    )
    if shouldTag then builder.appendElementEnd(tagName)
  }

  def writeMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    debug(trace, debugIndent, tpe, tagName, "writeMap")

    val skipItemTags = currentAnnotations.exists[annotation.xmlNoItemTags]
    val isXmlContent = currentAnnotations.exists[annotation.xmlContent]
    val shouldTag = !hasTag && !isXmlContent

    if shouldTag then builder.appendElementStart(tagName)
    cache.put(
      MapUtils.buildMapLoop(
        toValueName(tagName.show),
        keyTpe.toTypeRepr,
        valueTpe.toTypeRepr,
        valueTerm,
        functionOnEntry = { (key, value) =>
          block {
            writeType(
              tpe = valueTpe.toTypeRepr,
              tagName = TagName(StringUtils.applyToString(key.toTerm)),
              valueTerm = value.toTerm,
              builder = builder,
              hasTag = skipItemTags,
              isCollectionItem = true,
              currentAnnotations = Set.empty,
              trace = trace,
              debugIndent = debugIndent + 1
            )
          }
        }
      )
    )
    if shouldTag then builder.appendElementEnd(tagName)
  }

  def writeJavaIterable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    // get the name of the item tag from the annotation or default to the type name
    val itemLabel =
      currentAnnotations.getStringOrDefault[annotation.xmlItemTag](
        parameter = "name",
        defaultValue = typeNameOf(itemTpe)
      )
    // check if the item tags should be skipped
    val skipItemTags = currentAnnotations.exists[annotation.xmlNoItemTags]
    val isXmlContent = currentAnnotations.exists[annotation.xmlContent]
    val shouldTag = !hasTag && !isXmlContent

    debug(trace, debugIndent, tpe, tagName, "writeJavaIterable")

    if shouldTag then builder.appendElementStart(tagName)
    cache.put(
      JavaIterableUtils.buildIterableLoop(
        toValueName(tagName.show),
        itemTpe.toTypeRepr,
        valueTerm,
        functionOnItem = { (tpe, value) =>
          block {
            writeType(
              tpe = tpe.toTypeRepr,
              tagName = itemLabel,
              valueTerm = value.toTerm,
              builder = builder,
              hasTag = skipItemTags,
              isCollectionItem = true,
              currentAnnotations = Set.empty,
              trace = trace,
              debugIndent = debugIndent + 1
            )
          }
        }
      )
    )
    if shouldTag then builder.appendElementEnd(tagName)
  }

  def writeJavaMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    debug(trace, debugIndent, tpe, tagName, "writeJavaMap")

    val skipItemTags = currentAnnotations.exists[annotation.xmlNoItemTags]
    val isXmlContent = currentAnnotations.exists[annotation.xmlContent]
    val shouldTag = !hasTag && !isXmlContent

    if shouldTag then builder.appendElementStart(tagName)
    cache.put(
      JavaMapUtils.buildMapLoop(
        toValueName(tagName.show),
        keyTpe.toTypeRepr,
        valueTpe.toTypeRepr,
        valueTerm,
        functionOnEntry = { (key, value) =>
          block {
            writeType(
              tpe = valueTpe.toTypeRepr,
              tagName = TagName(StringUtils.applyToString(key.toTerm)),
              valueTerm = value.toTerm,
              builder = builder,
              hasTag = skipItemTags,
              isCollectionItem = true,
              currentAnnotations = Set.empty,
              trace = trace,
              debugIndent = debugIndent + 1
            )
          }
        }
      )
    )
    if shouldTag then builder.appendElementEnd(tagName)
  }

  /** Write Scala tupes and named tuples. */
  def writeTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*
    debug(trace, debugIndent, tpe, tagName, "writeTuple")

    // check if the item tags should be skipped
    val skipItemTags = currentAnnotations.exists[annotation.xmlNoItemTags]
    val isXmlContent = currentAnnotations.exists[annotation.xmlContent]
    val shouldTag = !hasTag && !isXmlContent

    val itemTag: Option[String] = currentAnnotations
      .getString[annotation.xmlItemTag](parameter = "name")

    TupleUtils.visit(
      label = itemTag,
      tpe.toTypeRepr,
      valueTerm = valueTerm,
      onStart = {
        block(if shouldTag then builder.appendElementStart(tagName))
      },
      functionWhenTuple = { (tpe, name, value, index) =>
        debug(trace, debugIndent, tpe, name.getOrElse("unknown"), "writeTuple/functionWhenTuple")
        writeType(
          tpe = tpe.toTypeRepr,
          tagName = name.getOrElse(typeNameOf(tpe)),
          valueTerm = value,
          builder = builder,
          hasTag = skipItemTags,
          isCollectionItem = true,
          currentAnnotations = Set.empty,
          trace = trace,
          debugIndent = debugIndent + 1
        )
      },
      functionWhenNamedTuple = { (tpe, name, value, index) =>
        debug(trace, debugIndent, tpe, name.getOrElse("unknown"), "writeTuple/functionWhenNamedTuple")
        writeType(
          tpe = tpe.toTypeRepr,
          tagName = name.getOrElse(typeNameOf(tpe)),
          valueTerm = value,
          builder = builder,
          hasTag = skipItemTags,
          isCollectionItem = false,
          currentAnnotations = Set.empty,
          trace = trace,
          debugIndent = debugIndent + 1
        )
      },
      onEnd = {
        block(if shouldTag then builder.appendElementEnd(tagName))
      }
    )
  }

  /** Write Scala structural types and objects extending `Selectable` with a `Fields` member type. */
  def writeSelectable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      fields: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*
    debug(trace, debugIndent, tpe, tagName, "writeSelectable")

    cache.putMethodCallOf[Unit](
      createMethodName("Selectable", tpe, currentAnnotations),
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
            functionOnField = { (tpe, name, value) =>
              writeType(
                tpe = tpe.toTypeRepr,
                tagName = name,
                valueTerm = value,
                builder = builder,
                hasTag = hasTag,
                isCollectionItem = isCollectionItem,
                currentAnnotations = currentAnnotations,
                trace = trace,
                debugIndent = debugIndent + 1
              )
            }
          )
      ,
      scope = StatementsCache.Scope.TopLevel
    )
  }

  def writeJavaRecord(using
      cache: StatementsCache
  )(
      tagName: TagName,
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*
    debug(trace, debugIndent, tpe, tagName, "writeJavaRecord")

    cache.putMethodCallOf[Unit](
      createMethodName("Record", tpe, currentAnnotations),
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
            functionOnField = { (tpe, name, value) =>
              writeType(
                tpe = tpe.toTypeRepr,
                tagName = name,
                valueTerm = value,
                builder = builder,
                hasTag = hasTag,
                isCollectionItem = isCollectionItem,
                currentAnnotations = currentAnnotations,
                trace = trace,
                debugIndent = debugIndent + 1
              )
            }
          )
      ,
      scope = StatementsCache.Scope.TopLevel
    )
  }

  /** Write Scala opaque types, if there is an upper bound, write the upper bound, otherwise write the string
    * representation of the value.
    */
  def writeOpaqueType(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      upperBoundTpe: Option[cache.quotes.reflect.TypeRepr],
      tagName: TagName,
      valueTerm: cache.quotes.reflect.Term,
      builder: Builder,
      hasTag: Boolean,
      isCollectionItem: Boolean,
      trace: scala.collection.mutable.Buffer[String],
      currentAnnotations: Set[AnnotationInfo],
      debugIndent: Int
  ): Unit = {
    given cache.quotes.type = cache.quotes
    debug(trace, debugIndent, tpe, tagName, "writeOpaqueType")

    upperBoundTpe match {
      case Some(upperBoundTpe) =>
        writeType(
          tpe = upperBoundTpe,
          tagName = tagName,
          valueTerm = valueTerm,
          builder = builder,
          hasTag = hasTag,
          isCollectionItem = isCollectionItem,
          currentAnnotations = currentAnnotations,
          trace = trace,
          debugIndent = debugIndent + 1
        )

      case None =>
        writeAsString(
          tpe = tpe,
          tagName = tagName,
          valueTerm = valueTerm.methodCall("toString", List()).toTerm,
          builder = builder,
          hasTag = hasTag,
          isCollectionItem = isCollectionItem,
          trace = trace,
          debugIndent = debugIndent
        )
    }

  }

  inline def debug(using
      cache: StatementsCache
  )(
      trace: scala.collection.mutable.Buffer[String],
      debugIndent: Int,
      tpe: cache.quotes.reflect.TypeRepr,
      tagName: TagName,
      message: String
  ): Unit = {
    inline if shouldDebugMacroExpansion
    then trace.append("  " * debugIndent + " > " + message)
  }

  inline def createMethodName(using
      cache: StatementsCache
  )(inline name: String, tpe: cache.quotes.reflect.TypeRepr, annotations: Set[AnnotationInfo]): String = {
    "write" + name + "ToXml_" + underscored(tpe.show(using cache.quotes.reflect.Printer.TypeReprCode))
      + annotations.hash(_.contains(".xml")).replace("-", "")
  }

  /** Builder method names. */
  object Builder {
    val AppendElementStartSymbol = "appendElementStart"
    val AppendElementStartWithAttributesSymbol = "appendElementStartWithAttributes"
    val AppendTextSymbol = "appendText"
    val AppendElementEndSymbol = "appendElementEnd"
  }

  /** Helper class to build the XML output using the StatementsCache. */
  class Builder {

    def initialize(builderExpr: Expr[XmlOutputBuilder])(using cache: StatementsCache): Unit = {
      given cache.quotes.type = cache.quotes
      import cache.quotes.reflect.*

      cache.getValueRefOfExpr("builder", builderExpr)

      val builderTpe = TypeRepr.of[XmlOutputBuilder]
      cache.getSymbol(
        Builder.AppendElementStartSymbol,
        MethodUtils.findMethodByArity(builderTpe, "appendElementStart", 1)
      )
      cache.getSymbol(
        Builder.AppendElementStartWithAttributesSymbol,
        MethodUtils.findMethodByArity(builderTpe, "appendElementStart", 2)
      )
      cache.getSymbol(Builder.AppendTextSymbol, MethodUtils.findMethodByArity(builderTpe, "appendText", 1))
      cache.getSymbol(Builder.AppendElementEndSymbol, MethodUtils.findMethodByArity(builderTpe, "appendElementEnd", 1))
    }

    def appendElementStart(using
        cache: StatementsCache
    )(tagName: TagName): Unit =
      cache.put(
        MethodUtils.buildMethodCall(
          cache.getValueRef("builder"),
          cache.getSymbol(Builder.AppendElementStartSymbol),
          List(tagName.resolve)
        )
      )

    def appendElementStartWithAttributes(using
        cache: StatementsCache
    )(tagName: TagName, attributes: cache.quotes.reflect.Term): Unit =
      given cache.quotes.type = cache.quotes
      cache.put(
        MethodUtils.buildMethodCall(
          cache.getValueRef("builder"),
          cache.getSymbol(Builder.AppendElementStartWithAttributesSymbol),
          List(
            tagName.resolve,
            attributes
          )
        )
      )

    def appendElementEnd(using
        cache: StatementsCache
    )(tagName: TagName): Unit =
      cache.put(
        MethodUtils.buildMethodCall(
          cache.getValueRef("builder"),
          cache.getSymbol(Builder.AppendElementEndSymbol),
          List(tagName.resolve)
        )
      )

    def appendText(using
        cache: StatementsCache
    )(tagValue: Any): Unit =
      given cache.quotes.type = cache.quotes
      cache.put(
        MethodUtils.buildMethodCall(
          cache.getValueRef("builder"),
          cache.getSymbol(Builder.AppendTextSymbol),
          List(StringUtils.applyToString(tagValue.toTerm))
        )
      )
  }
}
