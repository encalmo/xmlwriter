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

  /** Builder of org.w3c.dom.Document output. */
  def document(): DocumentOutputBuilder =
    new DocumentOutputBuilder()

  /** Builder of org.w3c.dom.Document output with given namespace. All elements will get that namespace. */
  def documentWithNamespace(namespace: String): DocumentWithNamespaceOutputBuilder =
    new DocumentWithNamespaceOutputBuilder(namespace)

  /** Builder of org.w3c.dom.Document output with namespaces derived from 'xmlns' attributes. All child elements will
    * use the namespace of the parent element, unless the child element has a 'xmlns' attribute.
    */
  def documentWithNamespaceFromAttributes(): DocumentWithNamespaceFromAttrributesOutputBuilder =
    new DocumentWithNamespaceFromAttrributesOutputBuilder()

  /** Builder of org.w3c.dom.Document output with namespaces derived from the given mapping between leading element
    * names and their namespaces. All child elements will use the namespace of the parent element, unless the child
    * element gets a new namespace from the mapping.
    */
  def documentWithNamespaceMapping(
      namespaces: Map[String, (String, String)]
  ): DocumentWithNamespaceMappingOutputBuilder =
    new DocumentWithNamespaceMappingOutputBuilder(namespaces)

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

  import javax.xml.parsers.DocumentBuilderFactory

  class DocumentOutputBuilder extends XmlOutputBuilder {

    type Result = org.w3c.dom.Document

    private val factory = DocumentBuilderFactory.newInstance();

    private val document = {
      val builder = factory.newDocumentBuilder();
      builder.newDocument();
    }

    val stack = scala.collection.mutable.Stack.empty[org.w3c.dom.Node]
    stack.push(document)

    final override def appendElementStart(name: String): Unit = {
      appendElementStart(name, Iterable.empty)
    }

    final override def appendElementStart(name: String, attributes: Iterable[(String, String)]): Unit = {
      val node = document.createElement(name)
      attributes.foreach { case (key, value) =>
        val attribute = document.createAttribute(key)
        attribute.setValue(value)
        node.setAttributeNode(attribute)
      }
      stack.head.appendChild(node)
      stack.push(node)
    }

    final override def appendElementEnd(name: String): Unit =
      stack.pop()

    final override def appendText(text: String): Unit = {
      val node = document.createTextNode(text)
      stack.head.appendChild(node)
    }

    final override def result: org.w3c.dom.Document = document
  }

  class DocumentWithNamespaceOutputBuilder(namespace: String) extends XmlOutputBuilder {

    type Result = org.w3c.dom.Document

    private val factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true)

    private val document = {
      val builder = factory.newDocumentBuilder();
      builder.newDocument();
    }

    val stack = scala.collection.mutable.Stack.empty[org.w3c.dom.Node]
    stack.push(document)

    final override def appendElementStart(name: String): Unit = {
      appendElementStart(name, Iterable.empty)
    }

    final override def appendElementStart(name: String, attributes: Iterable[(String, String)]): Unit = {
      val node = document.createElementNS(namespace, name)
      if (stack.head == document) {
        node.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", namespace)
      }
      attributes.foreach { case (key, value) =>
        val attribute = document.createAttribute(key)
        attribute.setValue(value)
        node.setAttributeNode(attribute)
      }
      stack.head.appendChild(node)
      stack.push(node)
    }

    final override def appendElementEnd(name: String): Unit =
      stack.pop()

    final override def appendText(text: String): Unit = {
      val node = document.createTextNode(text)
      stack.head.appendChild(node)
    }

    final override def result: org.w3c.dom.Document = document
  }

  class DocumentWithNamespaceFromAttrributesOutputBuilder extends XmlOutputBuilder {

    type Result = org.w3c.dom.Document

    private val factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true)

    private val document = {
      val builder = factory.newDocumentBuilder()
      builder.newDocument()
    }

    val stack = scala.collection.mutable.Stack.empty[org.w3c.dom.Node]
    stack.push(document)

    final override def appendElementStart(name: String): Unit = {
      appendElementStart(name, Iterable.empty)
    }

    final override def appendElementStart(name: String, attributes: Iterable[(String, String)]): Unit = {
      val namespace = attributes.find((name, _) => name == "xmlns").map((_, ns) => ns)
      val node =
        namespace match {
          case Some(ns) =>
            val node = document.createElementNS(ns, name)
            node.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns", ns)
            node
          case None =>
            Option(stack.head.getNamespaceURI()) match {
              case Some(ns) => document.createElementNS(ns, name)
              case None     => document.createElement(name)
            }
        }

      attributes.foreach { case (key, value) =>
        if key != "xmlns" then {
          val attribute = document.createAttribute(key)
          attribute.setValue(value)
          node.setAttributeNode(attribute)
        }
      }

      stack.head.appendChild(node)
      stack.push(node)
    }

    final override def appendElementEnd(name: String): Unit =
      stack.pop()

    final override def appendText(text: String): Unit = {
      val node = document.createTextNode(text)
      stack.head.appendChild(node)
    }

    final override def result: org.w3c.dom.Document = document
  }

  class DocumentWithNamespaceMappingOutputBuilder(namespaces: Map[String, (String, String)]) extends XmlOutputBuilder {

    type Result = org.w3c.dom.Document

    private val factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true)

    private val document = {
      val builder = factory.newDocumentBuilder()
      builder.newDocument()
    }

    val stack = scala.collection.mutable.Stack.empty[org.w3c.dom.Node]
    stack.push(document)

    final override def appendElementStart(name: String): Unit = {
      appendElementStart(name, Iterable.empty)
    }

    final override def appendElementStart(name: String, attributes: Iterable[(String, String)]): Unit = {
      val namespace = namespaces.get(name)
      val node =
        namespace match {
          case Some((prefix, namespace)) =>
            val node = document.createElementNS(
              namespace,
              if prefix.isEmpty then name else prefix + ":" + name
            )
            node

          case None =>
            Option(stack.head.getNamespaceURI()) match {
              case Some(ns) =>
                Option(stack.head.getPrefix()) match {
                  case Some(prefix) => document.createElementNS(ns, prefix + ":" + name)
                  case None         => document.createElementNS(ns, name)
                }
              case None => document.createElement(name)
            }
        }

      if stack.head == document then {
        namespaces.map(_._2).toSeq.distinct.foreach { (prefix, namespace) =>
          node.setAttributeNS(
            "http://www.w3.org/2000/xmlns/",
            if prefix.isEmpty then "xmlns" else "xmlns:" + prefix,
            namespace
          )
        }
      }

      attributes.foreach { case (key, value) =>
        val attribute = document.createAttribute(key)
        attribute.setValue(value)
        node.setAttributeNode(attribute)
      }

      stack.head.appendChild(node)
      stack.push(node)
    }

    final override def appendElementEnd(name: String): Unit =
      stack.pop()

    final override def appendText(text: String): Unit = {
      val node = document.createTextNode(text)
      stack.head.appendChild(node)
    }

    final override def result: org.w3c.dom.Document = document
  }

}
