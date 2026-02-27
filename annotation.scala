package org.encalmo.writer.xml

object annotation {

  /** Annotation to mark a field as an XML attribute. */
  class xmlAttribute extends scala.annotation.StaticAnnotation

  /** Annotation to mark a field as an XML element content, and skip wrapping it in opening/closing tags. */
  class xmlContent extends scala.annotation.StaticAnnotation

  /** Annotation to define the name of the XML element. */
  case class xmlTag(val name: String & Singleton) extends scala.annotation.StaticAnnotation

  /** Annotation to wrap value in and additional XML element. */
  case class xmlAdditionalTag(val name: String & Singleton) extends scala.annotation.StaticAnnotation

  /** Annotation to mandate nested tag elements: <label><type> ... </type></label> */
  class xmlTagLabelAndType extends scala.annotation.StaticAnnotation

  /** Annotation to define the name of the XML element wrapping each item in an array or collection. This will override
    * custom names of the items in the collection.
    */
  case class xmlItemTag(val name: String & Singleton) extends scala.annotation.StaticAnnotation

  /** Annotation to define the name of the XML element additionally wrapping each item in an array or collection. This
    * will NOT override custom names of the items in the collection.
    */
  case class xmlAdditionalItemTag(val name: String & Singleton) extends scala.annotation.StaticAnnotation

  /** Annotation to skip wrapping each item in an array or collection in its own tag. */
  class xmlNoItemTags extends scala.annotation.StaticAnnotation

  /** Annotation to define static value of the XML element, usefull for enums and case objects. */
  case class xmlValue(val value: String & Singleton) extends scala.annotation.StaticAnnotation

  /** Annotation to extract value of the XML element from the instance field or parameterless method, including
    * Java-style getters.
    */
  case class xmlValueSelector(val property: String & Singleton) extends scala.annotation.StaticAnnotation

  /** Annotation to force writing the enum case value as plain text, without wrapping it in a tag. */
  class xmlEnumCaseValuePlain extends scala.annotation.StaticAnnotation

}
