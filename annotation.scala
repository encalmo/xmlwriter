package org.encalmo.writer.xml

object annotation {

  /** Annotation to mark a field as an XML attribute. */
  class xmlAttribute extends scala.annotation.StaticAnnotation

  /** Annotation to mark a field as an XML element content. */
  class xmlContent extends scala.annotation.StaticAnnotation

  /** Annotation to define the name of the XML element. */
  case class xmlTag(val name: String & Singleton) extends scala.annotation.StaticAnnotation

  /** Annotation to omit this element tag when inside a collection or array. */
  class xmlNoTagInsideCollection extends scala.annotation.StaticAnnotation

  /** Annotation to define the name of the XML element wrapping each item in an array or collection.
    */
  case class xmlItemTag(val name: String & Singleton) extends scala.annotation.StaticAnnotation

  /** Annotation to switch off wrapping each item in an array or collection in a separate XML element.
    */
  class xmlNoItemTags extends scala.annotation.StaticAnnotation

  /** Annotation to define static value of the XML element, usefull for enums and case objects. */
  case class xmlValue(val value: String & Singleton) extends scala.annotation.StaticAnnotation

  /** Annotation to extract value of the XML element from the instance field or parameterless method, including
    * Java-style getters.
    */
  case class xmlValueSelector(val property: String & Singleton) extends scala.annotation.StaticAnnotation

  /** Annotation to use the case name of the enum value as the XML element tag. */
  class xmlUseEnumCaseNames extends scala.annotation.StaticAnnotation

}
