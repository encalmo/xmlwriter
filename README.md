<a href="https://github.com/encalmo/xmlwriter">![GitHub](https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white)</a> <a href="https://central.sonatype.com/artifact/org.encalmo/xmlwriter_3" target="_blank">![Maven Central Version](https://img.shields.io/maven-central/v/org.encalmo/xmlwriter_3?style=for-the-badge)</a> <a href="https://encalmo.github.io/xmlwriter/scaladoc/org/encalmo/writer/xml.html" target="_blank"><img alt="Scaladoc" src="https://img.shields.io/badge/docs-scaladoc-red?style=for-the-badge"></a>

# xmlwriter

Macro-powered fast and easy XML serialization library for Scala 3.

## Table of contents

- [Example usage](#example-usage)
- [Oustanding features](#oustanding-features)
- [Scala types supported directly without the need for typeclass derivation](#scala-types-supported-directly-without-the-need-for-typeclass-derivation)
- [Supported Java types](#supported-java-types)
- [Supported annotations](#supported-annotations)
- [Key abstractions](#key-abstractions)
- [Dependencies](#dependencies)
- [Usage](#usage)
- [More examples](#more-examples)
- [Project content](#project-content)

## Example usage

```scala
import org.encalmo.writer.xml.XmlWriter

case class Address(
    street: String,
    city: String,
    postcode: String
)

case class Employee(
    name: String,
    age: Int,
    email: Option[String],
    addresses: List[Address],
    active: Boolean
)

val entity = Employee(
    name = "John Doe",
    age = 30,
    email = Some("john.doe@example.com"),
    addresses = List(
    Address(street = "123 Main St", city = "Anytown", postcode = "12345"),
    Address(street = "456 Back St", city = "Downtown", postcode = "78901")
    ),
    active = true
)

val xml = XmlWriter.writeIndented(entity)
println(xml)
```
Output:
```xml
<?xml version='1.0' encoding='UTF-8'?>
<Employee>
    <name>John Doe</name>
    <age>30</age>
    <email>john.doe@example.com</email>
    <addresses>
        <Address>
            <street>123 Main St</street>
            <city>Anytown</city>
            <postcode>12345</postcode>
        </Address>
        <Address>
            <street>456 Back St</street>
            <city>Downtown</city>
            <postcode>78901</postcode>
        </Address>
    </addresses>
    <active>true</active>
</Employee>
```

The example above produces the following inlined code:
```scala
val xml: String = {

    org.encalmo.writer.xml.XmlWriter.writeIndented[Employee]{
      final lazy given val builder:
        org.encalmo.writer.xml.XmlOutputBuilder.IndentedXmlStringBuilder =
        org.encalmo.writer.xml.XmlOutputBuilder.indented(
          indentation = 4,
          initialString = if addXmlDeclaration then
              "<?xml version=\'1.0\' encoding=\'UTF-8\'?>" else ""
        )
      
        builder.appendElementStart("Employee", Nil)
        
        def writeCaseClassToXml_Employee(): Unit = {

            builder.appendElementStart("name")
            builder.appendText(entity.name.toString())
            builder.appendElementEnd("name")

            builder.appendElementStart("age")
            builder.appendText(entity.age.toString())
            builder.appendElementEnd("age")
              
            entity.email match {
                  case Some(value) =>
                      builder.appendElementStart("email")
                      builder.appendText(value.toString())
                      builder.appendElementEnd("email")

                  case None =>
                    ()
            }
            
            builder.appendElementStart("addresses")

            entity.addresses.iterator.foreach( (value: Address) => {
                    builder.appendElementStart("Address", Nil)
                    
                    def writeCaseClassToXml_Address(): Unit ={
                        builder.appendElementStart("street")
                        builder.appendText(value.street.toString()
                        builder.appendElementEnd("street")

                        builder.appendElementStart("city")
                        builder.appendText(value.city.toString())
                        builder.appendElementEnd("city")

                        builder.appendElementStart("postcode")
                        builder.appendText(value.postcode.toString())
                        builder.appendElementEnd("postcode")
                    }

                    writeCaseClassToXml_Address()
                    builder.appendElementEnd("Address")
                }
            )
            builder.appendElementEnd("addresses")
              
            builder.appendElementStart("active")
            builder.appendText(entity.active.toString())
            builder.appendElementEnd("active")
        }

        writeCaseClassToXml_Employee()
        builder.appendElementEnd("Employee")
    }
    builder.result
}
println(xml)
```

## Outstanding features
- **Generates highly performant low-level code** 
- Supports **field, value, case, and type annotations** enabling fine-tuning of the resulting XML,
- Supports **custom tag and attribute name transformation** (e.g., snake_case, kebab-case, upper/lower case, etc),
- **Indented or compact XML output** with pluggable output builders (including streaming),
- Automatic **escaping of text** (element and attribute content) to produce well-formed XML.
- Extensible to custom types via **typeclass** instances,
- Can automatically **derive** `XmlWriter` typeclass if requested,
- Invokes `toString()` as a **fallback** strategy when type is not supported directly or does not have an XmlWriter instance in scope.
- Decouples data structure traversal (`XmlWriter`) from output assembly (`XmlOutputBuilder`)

## Scala types supported directly without the need for typeclass derivation
- **Case classes** and nested case classes (including recursive, deeply nested types)
- **Enums and sealed trait hierarchies**
- **Tuples**: e.g. `(A, B)`, `(A, B, C)` etc.
- **Named tuples**: `(a: A, b: B)`
- **Instances of `Selectable` with a `Fields` type**: serialization for structural types and objects extending `Selectable` with a `Fields` member type
- **Opaque types with an upper bound**
- **Iterable[T]** collections and **Array[T]**
- **Option[T]**: (properly serializes presence or absence) 
- **Either[T]**
- All standard **Scala primitive types**: `Int`, `Long`, `Double`, `Float`, `Boolean`, `Char`, `Short`, `Byte` and **`String`**
- **Big number types**: `BigInt`, `BigDecimal`

## Supported Java types
- **Java boxed primitives:** `java.lang.Integer`, `java.lang.Long`, `java.lang.Double`, etc.
- **Java records**
- **Java enums**
- **Java iterables:** support for `java.util.List`, `java.util.Set`, and other iterables
- **Java maps:** support for `java.util.Map` and subclasses

## Supported annotations

- All annotations are defined in `org.encalmo.writer.xml.annotation`.
- Annotations can be placed on types, fields, values and enum cases, on case class fields or sealed trait members.
- Custom tag and attribute names are only required when you want to override defaults.

| Annotation            | Description                                                                                           |
|-----------------------|-------------------------------------------------------------------------------------------------------|
| `@xmlAttribute`       | Marks the target to be serialized as an XML attribute of the enclosing element rather than as a child.   |
| `@xmlContent`         | Marks target as the content (text value) of the XML element instead of a tag or attribute.           |
| `@xmlTag`             | Sets a custom XML tag or attribute name for this target (overrides the target name in serialization).   |
| `@xmlItemTag`         | Specifies the tag name to use for each element in a collection or array.                              |
| `@xmlNoItemTags`      | Prevents wrapping each collection element in an extra XML tag; all items are added directly.          |
| `@xmlNoTagInsideCollection` | Omits the tags of the target when inside a collection or an array.                |
| `@xmlUseEnumCaseNames`      | Sets the case name of an enum as the XML element tag when serializing the enum value instead of field name or enum type name. 
| `@xmlValue`           | Defines a static value for an element, useful for enum cases     |
| `@xmlValueSelector`   | Selects which member/field/property from a nested type is used as the value/text for this element.    |              |

## Key abstractions

- object [`XmlWriter`](XmlWriter.scala) provides the main user-facing API, a host of methods to serialize data types to XML,
- trait `XmlWriter[T]` defines typeclass interface,
- trait [`XmlOutputBuilder`](XmlOutputBuilder.scala) defines low-level API for constructing XML output,
- object `XmlOutputBuilder` provides a set of default implementations of `XmlOutputBuilder` trait producing indented or compact format, building a `String` or writing directly to the `java.io.OutputStream`

## Dependencies

   - [Scala](https://www.scala-lang.org) >= 3.7.4
   - org.encalmo [**macro-utils** 0.9.2](https://central.sonatype.com/artifact/org.encalmo/macro-utils_3)

## Usage

Use with SBT

    libraryDependencies += "org.encalmo" %% "xmlwriter" % "0.9.3"

or with SCALA-CLI

    //> using dep org.encalmo::xmlwriter:0.9.3

## More examples

Example with nested case classes and optional fields:

```scala
import org.encalmo.writer.xml.XmlWriter

case class Address(
  street: String,
  city: String,
  postcode: String,
  country: Option[String] = None
)

case class Company(
  name: String,
  address: Address
)

case class Employee(
  name: String,
  age: Int,
  email: Option[String],
  address: Option[Address],
  company: Option[Company]
)

val employee = Employee(
  name = "Alice Smith",
  age = 29,
  email = Some("alice.smith@company.com"),
  address = Some(
    Address(
      street = "456 Market Ave",
      city = "Metropolis",
      postcode = "90210",
      country = None
    )
  ),
  company = Some(
    Company(
      name = "Acme Widgets Inc.",
      address = Address(
        street = "123 Corporate Plaza",
        city = "Metropolis",
        postcode = "90211",
        country = Some("USA")
      )
    )
  )
)

// Serialize as indented XML (with XML declaration)
val xml: String = XmlWriter.writeIndented(employee)
println(xml)
```
Output:
```xml
<?xml version='1.0' encoding='UTF-8'?>
<Employee>
    <name>Alice Smith</name>
    <age>29</age>
    <email>alice.smith@company.com</email>
    <address>
        <street>456 Market Ave</street>
        <city>Metropolis</city>
        <postcode>90210</postcode>
    </address>
    <company>
        <name>Acme Widgets Inc.</name>
        <address>
            <street>123 Corporate Plaza</street>
            <city>Metropolis</city>
            <postcode>90211</postcode>
            <country>USA</country>
        </address>
    </company>
</Employee>
```

```scala
// Example: Serialize a case class with collections and XML annotations

import org.encalmo.writer.xml.XmlWriter
import org.encalmo.writer.xml.annotation.{xmlAttribute, xmlItemTag, xmlTag}

case class Tag(
  @xmlAttribute name: String,
  value: String
)

@xmlTag("Bookshelf")
case class Library(
  @xmlAttribute libraryId: String,
  name: String,
  @xmlItemTag("Book") books: List[Book]
)

case class Book(
  @xmlAttribute isbn: String,
  title: String,
  author: String,
  tags: List[Tag]
)

val library = Library(
  libraryId = "lib123",
  name = "City Library",
  books = List(
    Book(
      isbn = "978-3-16-148410-0",
      title = "Programming Scala",
      author = "Dean Wampler",
      tags = List(
        Tag(name = "Scala", value = "Functional"),
        Tag(name = "Programming", value = "JVM")
      )
    ),
    Book(
      isbn = "978-1-61729-065-7",
      title = "Functional Programming in Scala",
      author = "Paul Chiusano",
      tags = List(
        Tag(name = "Scala", value = "FP"),
        Tag(name = "Education", value = "Advanced")
      )
    )
  )
)

val xml: String = XmlWriter.writeIndented(library)
println(xml)
```
Output:
```xml
<?xml version='1.0' encoding='UTF-8'?>
<Bookshelf libraryId="lib123">
    <name>City Library</name>
    <books>
        <Book isbn="978-3-16-148410-0">
            <title>Programming Scala</title>
            <author>Dean Wampler</author>
            <tags>
                <Tag name="Scala">Functional</Tag>
                <Tag name="Programming">JVM</Tag>
            </tags>
        </Book>
        <Book isbn="978-1-61729-065-7">
            <title>Functional Programming in Scala</title>
            <author>Paul Chiusano</author>
            <tags>
                <Tag name="Scala">FP</Tag>
                <Tag name="Education">Advanced</Tag>
            </tags>
        </Book>
    </books>
</Bookshelf>
```


## Project content

```
├── .github
│   └── workflows
│       ├── pages.yaml
│       ├── release.yaml
│       └── test.yaml
│
├── .gitignore
├── .scalafmt.conf
├── annotation.scala
├── LICENSE
├── MacroXmlWriter.scala
├── MacroXmlWriterSpec.test.scala
├── Order.java
├── project.scala
├── README.md
├── Status.java
├── test.sh
├── TestData.test.scala
├── TestModel.test.scala
├── XmlOutputBuilder.scala
└── XmlWriter.scala
```

