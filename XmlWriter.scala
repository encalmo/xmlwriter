package org.encalmo.writer.xml

import java.io.OutputStream
import org.encalmo.utils.TypeNameUtils

/** Typeclass for writing XML output. */
trait XmlWriter[A] {
  def write(name: Option[String], value: A, createTag: Boolean)(using XmlOutputBuilder): Unit
}

object XmlWriter {

  import scala.quoted.*

  inline def derived[T]: XmlWriter[T] = ${ derivedImpl[T] }
  private def derivedImpl[T: Type](using Quotes): Expr[XmlWriter[T]] = {
    val typeName = Expr(TypeNameUtils.typeNameOf[T])
    '{
      new XmlWriter[T] {
        def write(name: Option[String], value: T, createTag: Boolean)(using builder: XmlOutputBuilder): Unit =
          ${
            XmlWriterMacro.writeImpl[T](
              '{ name.getOrElse(${ typeName }) },
              '{ value },
              '{ builder },
              false
            )
          }
      }
    }
  }

  /** Serialize and append value of some type to the given xml output builder */
  inline def append[T](
      name: String,
      value: T
  )(using builder: XmlOutputBuilder): Unit = {
    XmlWriterMacro.write[T](name, value)
  }

  inline def writeIndented[T](
      value: T,
      addXmlDeclaration: Boolean = true
  ): String = {
    given builder: XmlOutputBuilder.IndentedXmlStringBuilder =
      XmlOutputBuilder.indented(
        indentation = 4,
        initialString =
          if addXmlDeclaration
          then "<?xml version='1.0' encoding='UTF-8'?>"
          else "" // No XML declaration if not requested
      )
    XmlWriterMacro.write[T](value)
    builder.result
  }

  inline def writeIndentedWithNameTransformation[T](
      value: T,
      tagNameTransformation: String => String,
      attributeNameTransformation: String => String,
      addXmlDeclaration: Boolean = true
  ): String = {
    given builder: XmlOutputBuilder.IndentedXmlStringBuilder =
      XmlOutputBuilder.indentedWithNameTransformation(
        indentation = 4,
        tagNameTransformation = tagNameTransformation,
        attributeNameTransformation = attributeNameTransformation,
        initialString =
          if addXmlDeclaration
          then "<?xml version='1.0' encoding='UTF-8'?>"
          else "" // No XML declaration if not requested
      )
    XmlWriterMacro.write[T](value)
    builder.result
  }

  inline def writeIndentedUsingRootTagName[T](
      rootTagName: String,
      value: T,
      addXmlDeclaration: Boolean = true
  ): String = {
    given builder: XmlOutputBuilder.IndentedXmlStringBuilder =
      XmlOutputBuilder.indented(
        indentation = 4,
        initialString =
          if addXmlDeclaration
          then "<?xml version='1.0' encoding='UTF-8'?>"
          else "" // No XML declaration if not requested
      )

    XmlWriterMacro.write[T](rootTagName, value)
    builder.result
  }

  inline def streamIndented[T](
      value: T,
      outputStream: OutputStream,
      addXmlDeclaration: Boolean = true
  ): Unit = {
    given builder: XmlOutputBuilder.IndentedXmlStreamOutputBuilder =
      XmlOutputBuilder.indentedToStream(
        indentation = 4,
        outputStream = outputStream,
        initialString =
          if addXmlDeclaration
          then "<?xml version='1.0' encoding='UTF-8'?>"
          else "" // No XML declaration if not requested
      )
    XmlWriterMacro.write[T](value)
    builder.result
  }

  inline def streamIndentedUsingRootTagName[T](
      rootTagName: String,
      value: T,
      outputStream: OutputStream,
      addXmlDeclaration: Boolean = true
  ): Unit = {
    given builder: XmlOutputBuilder.IndentedXmlStreamOutputBuilder =
      XmlOutputBuilder.indentedToStream(
        indentation = 4,
        outputStream = outputStream,
        initialString =
          if addXmlDeclaration
          then "<?xml version='1.0' encoding='UTF-8'?>"
          else "" // No XML declaration if not requested
      )

    XmlWriterMacro.write[T](rootTagName, value)
    builder.result
  }

  inline def writeCompact[T](
      value: T,
      addXmlDeclaration: Boolean = true
  ): String = {
    given builder: XmlOutputBuilder.CompactXmlStringBuilder =
      XmlOutputBuilder.compact(
        initialString =
          if addXmlDeclaration
          then "<?xml version='1.0' encoding='UTF-8'?>"
          else "" // No XML declaration if not requested
      )

    XmlWriterMacro.write[T](value)
    builder.result
  }

  inline def writeCompactWithNameTransformation[T](
      value: T,
      tagNameTransformation: String => String,
      attributeNameTransformation: String => String,
      addXmlDeclaration: Boolean = true
  ): String = {
    given builder: XmlOutputBuilder.CompactXmlStringBuilder =
      XmlOutputBuilder.compactWithNameTransformation(
        tagNameTransformation = tagNameTransformation,
        attributeNameTransformation = attributeNameTransformation,
        initialString =
          if addXmlDeclaration
          then "<?xml version='1.0' encoding='UTF-8'?>"
          else "" // No XML declaration if not requested
      )

    XmlWriterMacro.write[T](value)
    builder.result
  }

  inline def writeCompactUsingRootTagName[T](
      rootTagName: String,
      value: T,
      addXmlDeclaration: Boolean = true
  ): String = {
    given builder: XmlOutputBuilder.CompactXmlStringBuilder =
      XmlOutputBuilder.compact(
        initialString =
          if addXmlDeclaration
          then "<?xml version='1.0' encoding='UTF-8'?>"
          else "" // No XML declaration if not requested
      )

    XmlWriterMacro.write[T](rootTagName, value)
    builder.result
  }

  inline def streamCompact[T](
      value: T,
      outputStream: OutputStream,
      addXmlDeclaration: Boolean = true
  ): Unit = {
    given builder: XmlOutputBuilder.CompactXmlStreamOutputBuilder =
      XmlOutputBuilder.compactToStream(
        outputStream = outputStream,
        initialString =
          if addXmlDeclaration
          then "<?xml version='1.0' encoding='UTF-8'?>"
          else "" // No XML declaration if not requested
      )

    XmlWriterMacro.write[T](value)
    builder.result
  }

  inline def streamCompactUsingRootTagName[T](
      rootTagName: String,
      value: T,
      outputStream: OutputStream,
      addXmlDeclaration: Boolean = true
  ): Unit = {
    given builder: XmlOutputBuilder.CompactXmlStreamOutputBuilder =
      XmlOutputBuilder.compactToStream(
        outputStream = outputStream,
        initialString =
          if addXmlDeclaration
          then "<?xml version='1.0' encoding='UTF-8'?>"
          else "" // No XML declaration if not requested
      )

    XmlWriterMacro.write[T](rootTagName, value)
    builder.result
  }

  inline def writeDocument[T](
      value: T
  ): org.w3c.dom.Document = {
    given builder: XmlOutputBuilder.DocumentOutputBuilder =
      XmlOutputBuilder.document()

    XmlWriterMacro.write[T](value)
    builder.result
  }

  inline def writeDocumentUsingRootTagName[T](
      rootTagName: String,
      value: T
  ): org.w3c.dom.Document = {
    given builder: XmlOutputBuilder.DocumentOutputBuilder =
      XmlOutputBuilder.document()

    XmlWriterMacro.write[T](rootTagName, value)
    builder.result
  }

}
