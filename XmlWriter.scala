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

  /** Writes indented XML output. */
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

  /** Writes indented XML output with tag name and attribute name transformation. */
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

  /** Writes indented XML output using root tag name. */
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

  /** Streams indented XML output. */
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

  /** Streams indented XML output using root tag name. */
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

  /** Writes compact XML output. */
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

  /** Writes compact XML output with tag name and attribute name transformation. */
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

  /** Writes compact XML output using root tag name. */
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

  /** Streams compact XML output. */
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

  /** Streams compact XML output using root tag name. */
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

  /** Writes org.w3c.dom.Document output. */
  inline def writeDocument[T](
      value: T
  ): org.w3c.dom.Document = {
    given builder: XmlOutputBuilder.DocumentOutputBuilder =
      XmlOutputBuilder.document()

    XmlWriterMacro.write[T](value)
    builder.result
  }

  /** Writes org.w3c.dom.Document output using root tag name. */
  inline def writeDocumentUsingRootTagName[T](
      rootTagName: String,
      value: T
  ): org.w3c.dom.Document = {
    given builder: XmlOutputBuilder.DocumentOutputBuilder =
      XmlOutputBuilder.document()

    XmlWriterMacro.write[T](rootTagName, value)
    builder.result
  }

  /** Writes org.w3c.dom.Document output with given namespace. */
  inline def writeDocumentWithNamespace[T](
      value: T,
      namespace: String
  ): org.w3c.dom.Document = {
    given builder: XmlOutputBuilder.DocumentWithNamespaceOutputBuilder =
      XmlOutputBuilder.documentWithNamespace(namespace)

    XmlWriterMacro.write[T](value)
    builder.result
  }

  /** Writes org.w3c.dom.Document output with given namespace using root tag name. */
  inline def writeDocumentWithNamespaceUsingRootTagName[T](
      rootTagName: String,
      value: T,
      namespace: String
  ): org.w3c.dom.Document = {
    given builder: XmlOutputBuilder.DocumentWithNamespaceOutputBuilder =
      XmlOutputBuilder.documentWithNamespace(namespace)

    XmlWriterMacro.write[T](rootTagName, value)
    builder.result
  }

  /** Writes org.w3c.dom.Document output with namespaces derived from 'xmlns' attributes. */
  inline def writeDocumentWithNamespaceFromAttributes[T](
      value: T
  ): org.w3c.dom.Document = {
    given builder: XmlOutputBuilder.DocumentWithNamespaceFromAttrributesOutputBuilder =
      XmlOutputBuilder.documentWithNamespaceFromAttributes()

    XmlWriterMacro.write[T](value)
    builder.result
  }

  /** Writes org.w3c.dom.Document output with namespaces derived from 'xmlns' attributes using root tag name. */
  inline def writeDocumentWithNamespaceFromAttributesUsingRootTagName[T](
      rootTagName: String,
      value: T
  ): org.w3c.dom.Document = {
    given builder: XmlOutputBuilder.DocumentWithNamespaceFromAttrributesOutputBuilder =
      XmlOutputBuilder.documentWithNamespaceFromAttributes()

    XmlWriterMacro.write[T](rootTagName, value)
    builder.result
  }

  /** Writes org.w3c.dom.Document output with namespaces derived from the given mapping between leading element names
    * and their namespaces.
    */
  inline def writeDocumentWithNamespaceMapping[T](
      value: T,
      namespaces: Map[String, (String, String)]
  ): org.w3c.dom.Document = {
    given builder: XmlOutputBuilder.DocumentWithNamespaceMappingOutputBuilder =
      XmlOutputBuilder.documentWithNamespaceMapping(namespaces)

    XmlWriterMacro.write[T](value)
    builder.result
  }

  /** Writes org.w3c.dom.Document output with namespaces derived from the given mapping between leading element names
    * and their namespaces using root tag name.
    */
  inline def writeDocumentWithNamespaceMappingUsingRootTagName[T](
      rootTagName: String,
      value: T,
      namespaces: Map[String, (String, String)]
  ): org.w3c.dom.Document = {
    given builder: XmlOutputBuilder.DocumentWithNamespaceMappingOutputBuilder =
      XmlOutputBuilder.documentWithNamespaceMapping(namespaces)

    XmlWriterMacro.write[T](rootTagName, value)
    builder.result
  }
}
