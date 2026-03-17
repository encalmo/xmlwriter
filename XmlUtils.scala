package org.encalmo.writer.xml

import javax.xml.transform.TransformerFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.io.StringWriter

object XmlUtils {
  def toXmlString(document: org.w3c.dom.Document): String = {
    val transformer = TransformerFactory.newInstance().newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
    val stringWriter = new StringWriter()
    transformer.transform(new DOMSource(document), new StreamResult(stringWriter))
    stringWriter.toString()
  }
}
