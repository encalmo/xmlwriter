package org.encalmo.writer.xml

import org.encalmo.utils.AnnotationUtils.*
import org.encalmo.utils.StatementsCache
import org.encalmo.utils.StatementsCache.*
import org.encalmo.utils.TagName
import org.encalmo.utils.TypeTreeIterator
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
    given cache: StatementsCache = new StatementsCache
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*
    writeUsingTypeTreeIterator[A](
      tagNameCandidate = None,
      expr = expr,
      builderExpr = builder,
      summonTypeclassInstance = true
    )
    cache.asTerm.asExprOf[Unit]
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
    val tagNameCandidate = Some(tagName.value.map(TagName(_)).getOrElse(TagName(tagName.asTerm)))
    writeUsingTypeTreeIterator[A](tagNameCandidate, expr, builder, summonTypeclassInstance)
    cache.asTerm.asExprOf[Unit]
  }

  /** Entry method to write the value of any type to the XML output using TypeTreeIterator and StatementsCache. */
  def writeUsingTypeTreeIterator[A: Type](
      tagNameCandidate: Option[TagName],
      expr: Expr[A],
      builderExpr: Expr[XmlOutputBuilder],
      summonTypeclassInstance: Boolean
  )(using cache: StatementsCache): Unit = {
    given cache.quotes.type = cache.quotes
    import cache.quotes.reflect.*

    val builder = new XmlWriterMacroVisitor.Builder
    builder.initialize(builderExpr)

    val valueTerm = expr.asTerm match {
      case Inlined(_, _, t) => t
      case t                => t
    }

    val trace = scala.collection.mutable.Buffer.empty[String]
    val annotations = getValueAnnotations(valueTerm)

    val tpe = TypeRepr.of[A]

    if tpe.typeSymbol.isTypeParam then
      report.errorAndAbort(
        s"""${tpe.show} is an abstract type parameter and cannot be serialized to XML.
      Possible solutions:
      - Add inline keyword to the method definition.
      - Add (using XmlWriter[${tpe.show}]) to the method definition
      - Define a given XmlWriter[${tpe.show}] in the current scope
      """
      )
    else {
      TypeTreeIterator.visitNode(using cache, XmlWriterMacroVisitor)(
        tpe = TypeRepr.of[A],
        valueTerm = valueTerm,
        context = XmlWriterMacroContext(tagNameCandidate = tagNameCandidate, builder = builder, hasTag = false),
        isCollectionItem = false,
        annotations = annotations,
        trace = trace,
        debugIndent = if shouldDebugMacroExpansion then 0 else Int.MinValue,
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
  }

}
