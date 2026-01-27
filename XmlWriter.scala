package org.encalmo.writer.xml

import java.io.OutputStream

/** Typeclass for writing XML output. */
trait XmlWriter[A] {
  def write(name: String, value: A, createTag: Boolean)(using XmlOutputBuilder): Unit
}

object XmlWriter {

  import scala.quoted.*

  @scala.annotation.nowarn
  inline def derived[T]: XmlWriter[T] = ${ derivedImpl[T] }
  private def derivedImpl[T: Type](using Quotes): Expr[XmlWriter[T]] = {
    '{
      new XmlWriter[T] {
        def write(name: String, value: T, createTag: Boolean)(using builder: XmlOutputBuilder): Unit =
          ${ MacroXmlWriter.writeImpl[T]('{ name }, '{ value }, '{ builder }, false) }
      }
    }
  }

  /** Serialize and append value of some type to the given xml output builder */
  inline def append[T](
      name: String,
      value: T
  )(using builder: XmlOutputBuilder): Unit = {
    MacroXmlWriter.write[T](name, value)
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
    MacroXmlWriter.write[T](value)
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
    MacroXmlWriter.write[T](value)
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

    MacroXmlWriter.write[T](rootTagName, value)
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
    MacroXmlWriter.write[T](value)
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

    MacroXmlWriter.write[T](rootTagName, value)
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

    MacroXmlWriter.write[T](value)
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

    MacroXmlWriter.write[T](value)
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

    MacroXmlWriter.write[T](rootTagName, value)
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

    MacroXmlWriter.write[T](value)
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

    MacroXmlWriter.write[T](rootTagName, value)
    builder.result
  }

}
