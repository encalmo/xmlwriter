package org.encalmo.writer.xml

import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/** XmlOutputBuilder is a base trait for output builders that produce XML output. */
trait XmlOutputBuilder {

  type Result
  def result: Result

  def appendElementStart(name: String): Unit
  def appendElementStart(name: String, attributes: Iterable[(String, String)]): Unit
  def appendElementEnd(name: String): Unit
  def appendText(text: String): Unit

  def transformElementName(name: String): String = name
  def transformAttributeName(name: String): String = name

  def escapeTextForAttribute(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&apos;")

  def escapeTextForElement(text: String): String = text
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
}

object XmlOutputBuilder {

  /** Creates builder of indented XML output. */
  def indented(
      indentation: Int,
      initialString: String
  ): IndentedXmlStringBuilder =
    new IndentedXmlStringBuilder(indentation, initialString)

  /** Creates builder of indented XML output. */
  def indentedWithNameTransformation(
      indentation: Int,
      initialString: String,
      tagNameTransformation: String => String,
      attributeNameTransformation: String => String
  ): IndentedXmlStringBuilder =
    new IndentedXmlStringBuilder(indentation, initialString) {
      override def transformElementName(name: String): String = tagNameTransformation(name)
      override def transformAttributeName(name: String): String = attributeNameTransformation(name)
    }

  /** Creates builder of indented XML output. */
  def indentedToStream(
      indentation: Int,
      outputStream: OutputStream,
      initialString: String
  ): IndentedXmlStreamOutputBuilder =
    new IndentedXmlStreamOutputBuilder(indentation, initialString, outputStream)

  /** Creates builder of indented XML output. */
  def indentedToStreamWithNameTransformation(
      indentation: Int,
      outputStream: OutputStream,
      initialString: String,
      tagNameTransformation: String => String,
      attributeNameTransformation: String => String
  ): IndentedXmlStreamOutputBuilder =
    new IndentedXmlStreamOutputBuilder(indentation, initialString, outputStream) {
      override def transformElementName(name: String): String = tagNameTransformation(name)
      override def transformAttributeName(name: String): String = attributeNameTransformation(name)
    }

  /** Creates builder of compact XML output. */
  def compact(initialString: String): CompactXmlStringBuilder =
    new CompactXmlStringBuilder(initialString)

  /** Creates builder of compact XML output. */
  def compactWithNameTransformation(
      initialString: String,
      tagNameTransformation: String => String,
      attributeNameTransformation: String => String
  ): CompactXmlStringBuilder =
    new CompactXmlStringBuilder(initialString) {
      override def transformElementName(name: String): String = tagNameTransformation(name)
      override def transformAttributeName(name: String): String = attributeNameTransformation(name)
    }

  /** Creates builder of compact XML output. */
  def compactToStream(outputStream: OutputStream, initialString: String): CompactXmlStreamOutputBuilder =
    new CompactXmlStreamOutputBuilder(outputStream, initialString)

  /** Creates builder of compact XML output. */
  def compactToStreamWithNameTransformation(
      outputStream: OutputStream,
      initialString: String,
      tagNameTransformation: String => String,
      attributeNameTransformation: String => String
  ): CompactXmlStreamOutputBuilder =
    new CompactXmlStreamOutputBuilder(outputStream, initialString) {
      override def transformElementName(name: String): String = tagNameTransformation(name)
      override def transformAttributeName(name: String): String = attributeNameTransformation(name)
    }

  /** Builder of indented XML output. */
  class IndentedXmlStringBuilder(indentation: Int, initialString: String) extends XmlOutputBuilder {

    type Result = String

    private val sb = new StringBuilder(initialString)

    private val indentationString = " " * indentation
    private var indentationLevel = 0
    private var previous = '-' // 's' for start, 'e' for end, 't' for text

    private inline def indent(): Unit =
      sb.append(indentationString * indentationLevel)

    private inline def newline(): Unit =
      sb.append("\n")

    final def appendElementStart(
        name: String
    ): Unit = {
      if !sb.isEmpty then {
        newline()
        indent()
      }
      sb.append("<")
      sb.append(transformElementName(name))
      sb.append(">")
      indentationLevel = indentationLevel + 1
      previous = 's'
    }

    final def appendElementStart(
        name: String,
        attributes: Iterable[(String, String)]
    ): Unit = {
      if !sb.isEmpty then {
        newline()
        indent()
      }
      sb.append("<")
      sb.append(transformElementName(name))
      attributes.foreach { case (k, v) =>
        sb.append(" ")
        sb.append(transformAttributeName(k))
        sb.append("=")
        sb.append("\"")
        sb.append(escapeTextForAttribute(v))
        sb.append("\"")
      }
      sb.append(">")
      indentationLevel = indentationLevel + 1
      previous = 's'
    }

    final def appendElementEnd(name: String): Unit = {
      indentationLevel = indentationLevel - 1
      if (previous == 'e') {
        newline()
        indent()
      }
      sb.append("</")
      sb.append(transformElementName(name))
      sb.append(">")
      previous = 'e'
    }

    final def appendText(text: String): Unit = {
      if (previous == 'e') {
        newline()
        indent()
      }
      sb.append(escapeTextForElement(text))
      previous = 't'
    }

    final def result: String = sb.toString()
  }

  /** Builder of compact XML output. */
  class CompactXmlStringBuilder(initialString: String) extends XmlOutputBuilder {

    type Result = String

    private val sb = new StringBuilder(initialString)

    final def appendElementStart(
        name: String
    ): Unit = {
      sb.append("<")
      sb.append(transformElementName(name))
      sb.append(">")
    }

    final def appendElementStart(
        name: String,
        attributes: Iterable[(String, String)]
    ): Unit = {
      sb.append("<")
      sb.append(transformElementName(name))
      attributes.foreach { case (k, v) =>
        sb.append(" ")
        sb.append(transformAttributeName(k))
        sb.append("=")
        sb.append("\"")
        sb.append(escapeTextForAttribute(v))
        sb.append("\"")
      }
      sb.append(">")
    }

    final def appendElementEnd(name: String): Unit =
      sb.append("</")
      sb.append(transformElementName(name))
      sb.append(">")

    final def appendText(text: String): Unit =
      sb.append(escapeTextForElement(text))

    final def result: String = sb.toString()
  }

  /** Builder of indented XML output. */
  class IndentedXmlStreamOutputBuilder(indentation: Int, initialString: String, outputStream: OutputStream)
      extends XmlOutputBuilder {

    type Result = Unit

    private val writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

    private val indentationString = " " * indentation
    private var indentationLevel = 0
    private var previous = '-'
    private var isEmpty = true

    private inline def write(text: String): Unit = {
      writer.write(text)
      isEmpty = false
    }

    private inline def indent(): Unit =
      write(indentationString * indentationLevel)

    private inline def newline(): Unit =
      write("\n")

    final def appendElementStart(
        name: String
    ): Unit = {
      if !isEmpty then {
        newline()
        indent()
      }
      write("<")
      write(transformElementName(name))
      write(">")
      indentationLevel = indentationLevel + 1
      previous = 's'
    }

    final def appendElementStart(
        name: String,
        attributes: Iterable[(String, String)]
    ): Unit = {
      if !isEmpty then {
        newline()
        indent()
      }
      write("<")
      write(transformElementName(name))
      attributes.foreach { case (k, v) =>
        write(" ")
        write(transformAttributeName(k))
        write("=")
        write("\"")
        write(escapeTextForAttribute(v))
        write("\"")
      }
      write(">")
      indentationLevel = indentationLevel + 1
      previous = 's'
    }

    final def appendElementEnd(name: String): Unit = {
      indentationLevel = indentationLevel - 1
      if (previous == 'e') {
        newline()
        indent()
      }
      write("</")
      write(transformElementName(name))
      write(">")
      previous = 'e'
    }

    final def appendText(text: String): Unit = {
      if (previous == 'e') {
        newline()
        indent()
      }
      write(escapeTextForElement(text))
      previous = 't'
    }

    final def result: Unit = writer.flush()
  }

  /** Builder of compact XML output. */
  class CompactXmlStreamOutputBuilder(outputStream: OutputStream, initialString: String) extends XmlOutputBuilder {

    type Result = Unit

    private val writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)

    final def appendElementStart(
        name: String
    ): Unit = {
      writer.write("<")
      writer.write(transformElementName(name))
      writer.write(">")
    }

    final def appendElementStart(
        name: String,
        attributes: Iterable[(String, String)]
    ): Unit = {
      writer.write("<")
      writer.write(transformElementName(name))
      attributes.foreach { case (k, v) =>
        writer.write(" ")
        writer.write(transformAttributeName(k))
        writer.write("=")
        writer.write("\"")
        writer.write(escapeTextForAttribute(v))
        writer.write("\"")
      }
      writer.write(">")
    }

    final def appendElementEnd(name: String): Unit =
      writer.write("</")
      writer.write(transformElementName(name))
      writer.write(">")

    final def appendText(text: String): Unit =
      writer.write(escapeTextForElement(text))

    final def result: Unit = writer.flush()
  }

}
