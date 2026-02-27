package org.encalmo.writer.xml

import org.encalmo.utils.AnnotationUtils
import org.encalmo.utils.AnnotationUtils.AnnotationInfo
import org.encalmo.utils.CaseClassUtils
import org.encalmo.utils.EnumUtils
import org.encalmo.utils.IterableUtils
import org.encalmo.utils.MethodUtils
import org.encalmo.utils.StatementsCache
import org.encalmo.utils.StatementsCache.*
import org.encalmo.utils.StringUtils
import org.encalmo.utils.TupleUtils
import org.encalmo.utils.TypeTreeVisitor
import org.encalmo.utils.VisitNodeFunction

import scala.quoted.Expr
import org.encalmo.utils.TypeNameUtils
import org.encalmo.utils.TagName
import org.encalmo.utils.{show, resolve}

case class XmlWriterMacroContext(tagName: TagName, builder: XmlWriterMacroVisitor.Builder, hasTag: Boolean) {
  override def toString: String = s"$tagName hasTag=$hasTag"
}

class XmlWriterMacroVisitor extends TypeTreeVisitor {

  type Context = XmlWriterMacroContext

  override def createVariableNamePrefix(using cache: StatementsCache)(context: XmlWriterMacroContext): String =
    TypeNameUtils.toValueName(context.tagName.show)

  inline override def beforeNode(using
      cache: StatementsCache
  )(annotations: Set[AnnotationInfo], context: XmlWriterMacroContext): XmlWriterMacroContext = {
    given cache.quotes.type = cache.quotes

    val tagName2: TagName =
      context.tagName match {
        case string: String =>
          annotations
            .getStringOrDefault[annotation.xmlTag](
              parameter = "name",
              defaultValue = string
            )
        case other => other
      }

    val isXmlContent: Boolean = annotations.exists[annotation.xmlContent]
    val shouldTag: Boolean = !context.hasTag && !isXmlContent

    context.copy(tagName = tagName2, hasTag = !shouldTag)
  }

  /** After visiting a node in the type tree. */
  inline def afterNode(using cache: StatementsCache)(context: Context): Unit = {}

  override def maybeProcessNodeDirectly(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: XmlWriterMacroContext,
      visitNode: VisitNodeFunction
  ): Option[Unit] = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*

    val maybeTagValue: Option[String] = annotations
      .getString[annotation.xmlValue](parameter = "value")

    val maybeTagValueSelector: Option[String] = annotations
      .getString[annotation.xmlValueSelector](parameter = "property")

    maybeTagValue
      .map { valueTerm =>
        // debug(trace, debugIndent, tpe, "writeXmlValue")
        if !context.hasTag then context.builder.appendElementStart(context.tagName)
        context.builder.appendText(Literal(StringConstant(valueTerm)))
        if !context.hasTag then context.builder.appendElementEnd(context.tagName)

      }
      .orElse {
        maybeTagValueSelector
          .map { selector =>
            MethodUtils.maybeSelectTerm(
              selector = selector,
              tpe = tpe,
              valueTerm = valueTerm,
              functionWhenSelected = { (tpe, valueTerm) =>
                visitNode(using cache, this)(
                  tpe = tpe,
                  valueTerm = valueTerm,
                  context = context,
                  annotations = annotations.remove[annotation.xmlValueSelector],
                  isCollectionItem = isCollectionItem
                )
              }
            )
          }
      }
  }

  override def maybeSummonTypeclassInstance(using
      cache: StatementsCache
  )(tpe: cache.quotes.reflect.TypeRepr, valueTerm: cache.quotes.reflect.Term, context: Context): Option[Unit] = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*

    tpe.asType match {
      case '[a] =>
        Expr.summon[XmlWriter[a]].map { writer =>
          cache.put(
            writer.asTerm.methodCall(
              "write",
              List(
                context.tagName.resolve,
                valueTerm,
                Literal(BooleanConstant(!context.hasTag))
              ),
              List(cache.getValueRef("builder"))
            )
          )
        }
    }
  }

  override def visitAsString(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementStart(context.tagName)
    context.builder.appendText(valueTerm)
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }

  override def beforeCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    if !context.hasTag
    then {
      val attributes: cache.quotes.reflect.Term = collectAttributesFromCaseClass(tpe, valueTerm)
      context.builder.appendElementStartWithAttributes(context.tagName, attributes)
    }
    context
  }

  private def collectAttributesFromCaseClass(using
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
      { (tpe, name, valueTerm, annotations) =>
        {
          val isAttribute = annotations.exists[annotation.xmlAttribute]
          if isAttribute then {

            val tagName2 = annotations
              .getStringOrDefault[annotation.xmlTag](parameter = "name", defaultValue = name)

            list.append(
              TupleUtils.createTuple2(
                Literal(StringConstant(tagName2)),
                StringUtils.applyToString(valueTerm)
              )
            )
          }
        }
      }
    )
    IterableUtils.createStaticList(TypeRepr.of[Tuple2[String, String]], list.toList)
  }

  override def visitCaseClassField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes

    val isAttribute = annotations.exists[annotation.xmlAttribute]
    // skip field if was aleady written as an attribute
    if !isAttribute then {
      visitNode(using cache, this)(
        tpe = tpe,
        valueTerm = valueTerm,
        context = context.copy(tagName = TagName(name), hasTag = false),
        isCollectionItem = false,
        annotations = AnnotationUtils.annotationsOf(tpe.toTypeRepr) ++ annotations
      )
    }
  }

  inline override def afterCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }

  override def beforeEnum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    context
  }

  override def visitEnumCaseValue(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes

    val customTagValue: Option[cache.quotes.reflect.Term] =
      annotations.getTerm[annotation.xmlValue](parameter = "value")

    val tagValue = customTagValue.getOrElse(valueTerm)
    if !context.hasTag
    then {
      context.builder.appendElementStart(context.tagName)
      context.builder.appendText(tagValue)
      context.builder.appendElementEnd(context.tagName)
    } else if EnumUtils.hasEnumCaseClasses(tpe.toTypeRepr)
    then {
      context.builder.appendElementStart(TagName(tagValue.toTerm.applyToString))
      context.builder.appendElementEnd(TagName(tagValue.toTerm.applyToString))
    } else {
      context.builder.appendText(tagValue)
    }
  }

  override def visitEnumCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    if !context.hasTag then context.builder.appendElementStart(context.tagName)
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = isCollectionItem,
      context = context.copy(tagName = TagName(name), hasTag = false)
    )
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }

  inline override def afterEnum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {}

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
    given cache.quotes.type = cache.quotes

    val itemLabel =
      if isCollectionItem && context.tagName == "Option"
      then TypeNameUtils.typeNameOf(tpe)
      else context.tagName

    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = isCollectionItem,
      context = context.copy(tagName = itemLabel)
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
    given cache.quotes.type = cache.quotes
    val itemLabel =
      if isCollectionItem && context.tagName == "Either"
      then TypeNameUtils.typeNameOf(tpe)
      else context.tagName

    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = isCollectionItem,
      context = context.copy(tagName = itemLabel)
    )
  }

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
    given cache.quotes.type = cache.quotes
    val itemLabel =
      if isCollectionItem && context.tagName == "Either"
      then TypeNameUtils.typeNameOf(tpe)
      else context.tagName

    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = isCollectionItem,
      context = context.copy(tagName = itemLabel)
    )
  }

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

  override def beforeCollection(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    given cache.quotes.type = cache.quotes
    // check if the item tags should be skipped
    val skipItemTags = annotations.exists[annotation.xmlNoItemTags]

    if !context.hasTag then context.builder.appendElementStart(context.tagName)
    context.copy(tagName = TagName(TypeNameUtils.typeNameOf(itemTpe)), hasTag = skipItemTags)
  }

  override def visitCollectionItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes
    // get the name of the item tag from the annotation or default to the type name
    val itemAnnotations = annotations
      .find[annotation.xmlItemTag]
      .map(_.renameAs[annotation.xmlTag])
      .toSet

    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = itemAnnotations,
      isCollectionItem = true,
      context = context
    )
  }

  inline override def afterCollection(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }

  override def beforeArray(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    given cache.quotes.type = cache.quotes
    // check if the item tags should be skipped
    val skipItemTags = annotations.exists[annotation.xmlNoItemTags]

    if !context.hasTag then context.builder.appendElementStart(context.tagName)
    context.copy(tagName = TagName(TypeNameUtils.typeNameOf(itemTpe)), hasTag = skipItemTags)
  }

  override def visitArrayItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes
    // get the name of the item tag from the annotation or default to the type name
    val itemAnnotations = annotations
      .find[annotation.xmlItemTag]
      .map(_.renameAs[annotation.xmlTag])
      .toSet

    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = itemAnnotations,
      isCollectionItem = true,
      context = context
    )
  }

  inline override def afterArray(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }

  override def beforeTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    given cache.quotes.type = cache.quotes
    // check if the item tags should be skipped
    val skipItemTags = annotations.exists[annotation.xmlNoItemTags]

    if !context.hasTag then context.builder.appendElementStart(context.tagName)
    context.copy(hasTag = skipItemTags)
  }

  override def visitTupleItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes
    val itemTag: Option[String] = annotations.getString[annotation.xmlItemTag](parameter = "name")
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = Set.empty,
      isCollectionItem = true,
      context = context.copy(tagName = itemTag.getOrElse(TypeNameUtils.typeNameOf(tpe)))
    )
  }

  inline override def afterTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }

  override def beforeNamedTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    given cache.quotes.type = cache.quotes
    // check if the item tags should be skipped
    val skipItemTags = annotations.exists[annotation.xmlNoItemTags]

    if !context.hasTag then context.builder.appendElementStart(context.tagName)
    context.copy(hasTag = skipItemTags)
  }

  override def visitNamedTupleItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = Set.empty,
      isCollectionItem = false,
      context = context.copy(tagName = name)
    )
  }

  inline override def afterNamedTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }

  override def beforeSelectable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      fields: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    if !context.hasTag then context.builder.appendElementStart(context.tagName)
    context.copy(hasTag = false)
  }

  override def visitSelectableField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = false,
      context = context.copy(tagName = TagName(name))
    )
  }

  inline override def afterSelectable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }

  override def beforeUnion(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    if !context.hasTag then context.builder.appendElementStart(context.tagName)
    context.copy(hasTag = true)
  }

  override def visitUnionMember(using
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

  inline override def afterUnion(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }

  override def beforeJavaIterable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      itemTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    given cache.quotes.type = cache.quotes
    // check if the item tags should be skipped
    val skipItemTags = annotations.exists[annotation.xmlNoItemTags]

    if !context.hasTag then context.builder.appendElementStart(context.tagName)
    context.copy(tagName = TagName(TypeNameUtils.typeNameOf(itemTpe)), hasTag = skipItemTags)
  }

  override def visitJavaIterableItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes
    // get the name of the item tag from the annotation or default to the type name
    val itemAnnotations = annotations
      .find[annotation.xmlItemTag]
      .map(_.renameAs[annotation.xmlTag])
      .toSet

    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = itemAnnotations,
      isCollectionItem = true,
      context = context
    )
  }

  inline override def afterJavaIterable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }

  override def beforeMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    given cache.quotes.type = cache.quotes
    // check if the item tags should be skipped
    val skipItemTags = annotations.exists[annotation.xmlNoItemTags]

    if !context.hasTag then context.builder.appendElementStart(context.tagName)
    context.copy(tagName = TagName(TypeNameUtils.typeNameOf(valueTpe)), hasTag = skipItemTags)
  }

  override def visitMapEntry(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTerm: cache.quotes.reflect.Term,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes
    visitNode(using cache, this)(
      tpe = tpe.toTypeRepr,
      valueTerm = valueTerm.toTerm,
      annotations = Set.empty,
      isCollectionItem = true,
      context = context.copy(tagName = TagName(StringUtils.applyToString(keyTerm)))
    )
  }

  inline override def afterMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }

  override def beforeJavaMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTpe: cache.quotes.reflect.TypeRepr,
      valueTpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    given cache.quotes.type = cache.quotes
    // check if the item tags should be skipped
    val skipItemTags = annotations.exists[annotation.xmlNoItemTags]

    if !context.hasTag then context.builder.appendElementStart(context.tagName)
    context.copy(tagName = TagName(TypeNameUtils.typeNameOf(valueTpe)), hasTag = skipItemTags)
  }

  override def visitJavaMapEntry(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      keyTerm: cache.quotes.reflect.Term,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    visitNode(using cache, this)(
      tpe = tpe.toTypeRepr,
      valueTerm = valueTerm.toTerm,
      annotations = Set.empty,
      isCollectionItem = true,
      context = context.copy(tagName = TagName(StringUtils.applyToString(keyTerm)))
    )
  }

  inline override def afterJavaMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }

  override def beforeJavaRecord(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    if !context.hasTag
    then {
      val attributes: cache.quotes.reflect.Term = collectAttributesFromCaseClass(tpe, valueTerm)
      context.builder.appendElementStartWithAttributes(context.tagName, attributes)
    }
    context
  }

  override def visitJavaRecordField(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      name: String,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      context = context.copy(tagName = TagName(name), hasTag = false),
      isCollectionItem = false,
      annotations = AnnotationUtils.annotationsOf(tpe.toTypeRepr)
    )
  }

  inline override def afterJavaRecord(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagName)
  }
}

object XmlWriterMacroVisitor {

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

    inline def appendElementStart(using
        cache: StatementsCache
    )(tagName: TagName): Unit =
      cache.put(
        MethodUtils.buildMethodCall(
          cache.getValueRef("builder"),
          cache.getSymbol(Builder.AppendElementStartSymbol),
          List(tagName.resolve)
        )
      )

    inline def appendElementStartWithAttributes(using
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

    inline def appendElementEnd(using
        cache: StatementsCache
    )(tagName: TagName): Unit =
      cache.put(
        MethodUtils.buildMethodCall(
          cache.getValueRef("builder"),
          cache.getSymbol(Builder.AppendElementEndSymbol),
          List(tagName.resolve)
        )
      )

    inline def appendText(using
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
