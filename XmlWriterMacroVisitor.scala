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

case class XmlWriterMacroContext(
    tagNameCandidate: Option[TagName],
    builder: XmlWriterMacroVisitor.Builder,
    hasTag: Boolean
) {

  inline def tagNameOr(using cache: StatementsCache)(tpe: cache.quotes.reflect.TypeRepr): TagName =
    given cache.quotes.type = cache.quotes
    tagNameCandidate.getOrElse(TagName(TypeNameUtils.typeNameOf(tpe)))

  inline def maybeSetTagNameCandidate(using
      cache: StatementsCache
  )(tpe: cache.quotes.reflect.TypeRepr): XmlWriterMacroContext =
    given cache.quotes.type = cache.quotes
    tagNameCandidate match {
      case Some(tagName) => this
      case None          => copy(tagNameCandidate = Some(TagName(TypeNameUtils.typeNameOf(tpe))))
    }

  inline override def toString: String = s"${tagNameCandidate.map(_.toString).getOrElse("<undefined>")} hasTag=$hasTag"
}

object XmlWriterMacroVisitor extends TypeTreeVisitor {

  type Context = XmlWriterMacroContext

  inline override def createVariableNamePrefix(using cache: StatementsCache)(context: XmlWriterMacroContext): String =
    TypeNameUtils.toValueName(context.tagNameCandidate.map(_.show).getOrElse("it"))

  inline override def beforeNode(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      isCollectionItem: Boolean,
      context: Context
  ): (XmlWriterMacroContext, Set[AnnotationInfo]) = {
    given cache.quotes.type = cache.quotes

    val tagNameCandidate2: Option[TagName] =
      context.tagNameCandidate
        .map { tagName =>
          annotations
            .getString[annotation.xmlTag](parameter = "name")
            .getOrElse(tagName)
        }
        .orElse(
          annotations
            .getString[annotation.xmlTag](parameter = "name")
        )

    val isXmlContent: Boolean = annotations.exists[annotation.xmlContent]
    val additionalTag = annotations.getString[annotation.xmlAdditionalTag](parameter = "name")
    if !context.hasTag then additionalTag.foreach(tag => context.builder.appendElementStart(TagName(tag)))

    (
      context.copy(tagNameCandidate = tagNameCandidate2, hasTag = context.hasTag || isXmlContent),
      annotations
        .remove[annotation.xmlContent]
        .remove[annotation.xmlTag]
        .remove[annotation.xmlAdditionalTag]
    )
  }

  /** After visiting a node in the type tree. */
  inline def afterNode(using cache: StatementsCache)(annotations: Set[AnnotationInfo], context: Context): Unit = {
    given cache.quotes.type = cache.quotes
    val additionalTag = annotations.getString[annotation.xmlAdditionalTag](parameter = "name")
    if !context.hasTag then additionalTag.foreach(tag => context.builder.appendElementEnd(TagName(tag)))
  }

  inline override def maybeProcessNodeDirectly(using
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
        if !context.hasTag then context.builder.appendElementStart(context.tagNameOr(tpe))
        context.builder.appendText(Literal(StringConstant(valueTerm)))
        if !context.hasTag then context.builder.appendElementEnd(context.tagNameOr(tpe))

      }
      .orElse {
        maybeTagValueSelector
          .map { selector =>
            MethodUtils.maybeSelectTerm(
              selector = selector,
              tpe = tpe,
              valueTerm = valueTerm,
              functionWhenSelected = { (valueTpe, valueTerm) =>
                visitNode(using cache, this)(
                  tpe = valueTpe,
                  valueTerm = valueTerm,
                  context = context.maybeSetTagNameCandidate(tpe),
                  annotations = annotations.remove[annotation.xmlValueSelector],
                  isCollectionItem = isCollectionItem
                )
              }
            )
          }
      }
  }

  inline override def maybeSummonTypeclassInstance(using
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
                context.tagNameCandidate.resolve,
                valueTerm,
                Literal(BooleanConstant(!context.hasTag))
              ),
              List(cache.getValueRef("builder"))
            )
          )
        }
    }
  }

  inline override def visitPrimitive(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementStart(context.tagNameOr(tpe))
    context.builder.appendText(valueTerm)
    if !context.hasTag then context.builder.appendElementEnd(context.tagNameOr(tpe))
  }

  inline override def visitAsString(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementStart(context.tagNameOr(tpe))
    context.builder.appendText(valueTerm)
    if !context.hasTag then context.builder.appendElementEnd(context.tagNameOr(tpe))
  }

  inline override def beforeCaseClass(using
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
      context.builder.appendElementStartWithAttributes(context.tagNameOr(tpe), attributes)
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

  inline override def visitCaseClassField(using
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
      val isTagLabelAndType = annotations.exists[annotation.xmlTagLabelAndType]
      if isTagLabelAndType then context.builder.appendElementStart(TagName(name))
      visitNode(using cache, this)(
        tpe = tpe,
        valueTerm = valueTerm,
        context = context.copy(
          tagNameCandidate = if isTagLabelAndType then None else Some(TagName(name)),
          hasTag = false
        ),
        isCollectionItem = false,
        annotations = AnnotationUtils.annotationsOf(tpe.toTypeRepr) ++ annotations
      )
      if isTagLabelAndType then context.builder.appendElementEnd(TagName(name))
    }
  }

  inline override def afterCaseClass(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagNameOr(tpe))
  }

  inline override def beforeEnum(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    context
  }

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
  ): Unit = {
    given cache.quotes.type = cache.quotes

    val isEnumCaseValuePlain = annotations.exists[annotation.xmlEnumCaseValuePlain]

    val customTagValue: Option[cache.quotes.reflect.Term] =
      annotations.getTerm[annotation.xmlValue](parameter = "value")

    val tagValue = customTagValue.getOrElse(valueTerm)
    val hasEnumCaseClasses = EnumUtils.hasEnumCaseClasses(tpe.toTypeRepr)

    if context.hasTag
    then {
      if hasEnumCaseClasses && !isEnumCaseValuePlain
      then {
        context.builder.appendElementStart(TypeNameUtils.typeNameOf(tpe))
        context.builder.appendElementEnd(TypeNameUtils.typeNameOf(tpe))
      } else {
        context.builder.appendText(tagValue)
      }
    } else
      context.tagNameCandidate match {
        case Some(tagName) =>
          context.builder.appendElementStart(tagName)
          context.builder.appendText(tagValue)
          context.builder.appendElementEnd(tagName)

        case None =>
          if hasEnumCaseClasses && !isEnumCaseValuePlain
          then {
            context.builder.appendElementStart(TypeNameUtils.typeNameOf(tpe))
            context.builder.appendElementEnd(TypeNameUtils.typeNameOf(tpe))
          } else {
            context.builder.appendText(tagValue)
          }
      }
  }

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
  ): Unit = {
    if !context.hasTag then context.builder.appendElementStart(context.tagNameOr(tpe))
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = annotations,
      isCollectionItem = isCollectionItem,
      context = context.copy(tagNameCandidate = None, hasTag = true)
    )
    if !context.hasTag then context.builder.appendElementEnd(context.tagNameOr(tpe))
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
          context = context.maybeSetTagNameCandidate(tpe)
        )

      case None =>
        visitAsString(
          tpe = tpe,
          valueTerm = valueTerm.methodCall("toString", List()).toTerm,
          context = context
        )
    }
  }

  inline override def beforeCollection(using
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
    if !context.hasTag then context.builder.appendElementStart(context.tagNameOr(tpe))
    context.copy(tagNameCandidate = None, hasTag = skipItemTags)
  }

  inline override def visitCollectionItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      indexTerm: cache.quotes.reflect.Term,
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
      ++ annotations
        .remove[annotation.xmlTag]
        .remove[annotation.xmlAdditionalItemTag]

    val additionalItemTag = annotations.getString[annotation.xmlAdditionalItemTag](parameter = "name")
    additionalItemTag.foreach(tag => context.builder.appendElementStart(TagName(tag)))
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = itemAnnotations,
      isCollectionItem = true,
      context = context
    )
    additionalItemTag.foreach(tag => context.builder.appendElementEnd(TagName(tag)))
  }

  inline override def afterCollection(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagNameOr(tpe))
  }

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

  inline override def visitArrayItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      indexTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitCollectionItem(tpe, valueTerm, indexTerm, annotations, context, visitNode)

  inline override def afterArray(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterCollection(tpe, context)

  inline override def beforeTuple(using
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

    if !context.hasTag then context.builder.appendElementStart(context.tagNameOr(tpe))
    context.copy(tagNameCandidate = None, hasTag = skipItemTags)
  }

  inline override def visitTupleItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      indexTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit = {
    given cache.quotes.type = cache.quotes
    val additionalItemTag = annotations.getString[annotation.xmlAdditionalItemTag](parameter = "name")
    // get the name of the item tag from the annotation or default to the type name
    val itemAnnotations = annotations
      .find[annotation.xmlItemTag]
      .map(_.renameAs[annotation.xmlTag])
      .toSet
      ++ annotations
        .remove[annotation.xmlTag]
        .remove[annotation.xmlAdditionalItemTag]

    additionalItemTag.foreach(tag => context.builder.appendElementStart(TagName(tag)))
    visitNode(using cache, this)(
      tpe = tpe,
      valueTerm = valueTerm,
      annotations = itemAnnotations,
      isCollectionItem = true,
      context = context
    )
    additionalItemTag.foreach(tag => context.builder.appendElementEnd(TagName(tag)))
  }

  inline override def afterTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagNameOr(tpe))
  }

  inline override def beforeNamedTuple(using
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

    if !context.hasTag then context.builder.appendElementStart(context.tagNameOr(tpe))
    context.copy(hasTag = skipItemTags)
  }

  inline override def visitNamedTupleItem(using
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
      context = context.copy(tagNameCandidate = Some(TagName(name)))
    )
  }

  inline override def afterNamedTuple(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagNameOr(tpe))
  }

  inline override def beforeSelectable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      fields: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    if !context.hasTag then context.builder.appendElementStart(context.tagNameOr(tpe))
    context.copy(hasTag = false)
  }

  inline override def visitSelectableField(using
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
      context = context.copy(tagNameCandidate = Some(TagName(name)))
    )
  }

  inline override def afterSelectable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagNameOr(tpe))
  }

  inline override def beforeUnion(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context
  ): Context = {
    given cache.quotes.type = cache.quotes
    if !context.hasTag && context.tagNameCandidate.isDefined
    then {
      context.builder.appendElementStart(context.tagNameOr(tpe))
      context.copy(tagNameCandidate = Some(TagName(TypeNameUtils.typeNameOf(tpe))), hasTag = true)
    } else context
  }

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
    if !context.hasTag && context.tagNameCandidate.isDefined
    then {
      context.builder.appendElementEnd(context.tagNameOr(tpe))
    }
  }

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

  inline override def visitJavaIterableItem(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      valueTerm: cache.quotes.reflect.Term,
      indexTerm: cache.quotes.reflect.Term,
      annotations: Set[AnnotationInfo],
      context: Context,
      visitNode: VisitNodeFunction
  ): Unit =
    visitCollectionItem(tpe, valueTerm, indexTerm, annotations, context, visitNode)

  inline override def afterJavaIterable(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit =
    afterCollection(tpe, context)

  inline override def beforeMap(using
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

    if !context.hasTag then context.builder.appendElementStart(context.tagNameOr(tpe))
    context.copy(hasTag = skipItemTags)
  }

  inline override def visitMapEntry(using
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
      context = context.copy(tagNameCandidate = Some(TagName(StringUtils.applyToString(keyTerm))))
    )
  }

  inline override def afterMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagNameOr(tpe))
  }

  inline override def beforeJavaMap(using
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

    if !context.hasTag then context.builder.appendElementStart(context.tagNameOr(tpe))
    context.copy(hasTag = skipItemTags)
  }

  inline override def visitJavaMapEntry(using
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
      context = context.copy(tagNameCandidate = Some(TagName(StringUtils.applyToString(keyTerm))))
    )
  }

  inline override def afterJavaMap(using
      cache: StatementsCache
  )(
      tpe: cache.quotes.reflect.TypeRepr,
      context: Context
  ): Unit = {
    if !context.hasTag then context.builder.appendElementEnd(context.tagNameOr(tpe))
  }

  inline override def beforeJavaRecord(using
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
      context.builder.appendElementStartWithAttributes(context.tagNameOr(tpe), attributes)
    }
    context
  }

  inline override def visitJavaRecordField(using
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
      context = context.copy(tagNameCandidate = Some(TagName(name)), hasTag = false),
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
    if !context.hasTag then context.builder.appendElementEnd(context.tagNameOr(tpe))
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
