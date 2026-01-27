<a href="https://central.sonatype.com/artifact/org.encalmo/xmlwriter_3" target="_blank">![Maven Central Version](https://img.shields.io/maven-central/v/org.encalmo/xmlwriter_3?style=for-the-badge)</a> <a href="https://encalmo.github.io/xmlwriter/scaladoc/org/encalmo/writer/xml.html" target="_blank"><img alt="Scaladoc" src="https://img.shields.io/badge/docs-scaladoc-red?style=for-the-badge"></a>

# xmlwriter

Macro-powered XML fast serialization library for Scala 3.

## Table of contents

- [Oustanding features](#oustanding-features)
- [Scala types supported directly without the need for typeclass derivation](#scala-types-supported-directly-without-the-need-for-typeclass-derivation)
- [Supported Java types (when imported into Scala):](#supported-java-types-(when-imported-into-scala):)
- [Annotations](#annotations)
   - [Notes](#notes)
- [Dependencies](#dependencies)
- [Usage](#usage)
- [Examples](#examples)

## Oustanding features
- **Highly performant low-level code avoiding typeclass instance allocations** 
- Support for **field annotations** enabling fine-tuning of the resulting XML,
- Support for **custom tag and attribute name transformation** (e.g., snake_case, kebab-case, upper/lower case, etc),
- **Indented or compact XML output** with pluggable output builders (including streaming),
- Automatic **escaping of text** (element and attribute content) to produce well-formed XML.
- Extensible to custom types via **typeclass** instances,
- Can automatically derive of the `XmlWriter` typeclass if needed,
- Use of `toString()` representation of the instance valuea as a fallback strategy.

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

## Supported Java types (when imported into Scala):
- **Java boxed primitives:** `java.lang.Integer`, `java.lang.Long`, `java.lang.Double`, etc.
- **Java records**
- **Java enums**
- **Java iterables:** Support for `java.util.List`, `java.util.Set`, and other iterables
- **Java maps:** Support for `java.util.Map` and subclasses

> **Note:**  
> You can derive XML serialization for **any product (`case class`) or sum (`sealed trait`/Scala 3 enum) type** recursively composed of the above types.  

> You can provide your own custom `XmlWriter` for arbitrary types as well.

## Annotations

| Annotation                                   | Usage Example                                            | Description                                                                                           |
|-----------------------------------------------|----------------------------------------------------------|-------------------------------------------------------------------------------------------------------|
| `@xmlAttribute`                              | `@xmlAttribute id: String`                               | Marks a field to be serialized as an XML attribute of the enclosing element rather than as a child.   |
| `@xmlContent`                                | `@xmlContent text: String`                               | Marks a field as the content (text value) of the XML element instead of a tag or attribute.           |
| `@xmlName("tagName")`                        | `@xmlName("person") name: String`                        | Sets a custom XML tag or attribute name for this field (overrides the field name in serialization).   |
| `@xmlItemTag("itemTagName")`                 | `@xmlItemTag("entry") entries: List[String]`             | Specifies the tag name to use for each element in a collection or array.                              |
| `@xmlNoItemTags`                             | `@xmlNoItemTags values: List[String]`                    | Prevents wrapping each collection element in an extra XML tag; all items are added directly.          |

### Notes
- All annotations are defined in `org.encalmo.writer.xml.annotation`.
- Annotations can be used in any combination on case class fields or sealed trait members.
- Custom tag and attribute names are only required when you want to override defaults.


## Dependencies

   - [Scala](https://www.scala-lang.org) >= 3.7.4
   - org.encalmo [**macro-utils** 0.9.0](https://central.sonatype.com/artifact/org.encalmo/macro-utils_3)

## Usage

Use with SBT

    libraryDependencies += "org.encalmo" %% "xmlwriter" % "0.9.0"

or with SCALA-CLI

    //> using dep org.encalmo::xmlwriter:0.9.0

## Examples

Simple example of the case class serialization:
```scala
import org.encalmo.writer.xml.XmlWriter

case class Person(name: String, age: Int)

val person = Person("John Doe", 42)

// Serialize as indented XML (with XML declaration)
val xml: String = XmlWriter.writeIndented(person)
println(xml)
```
Output:
```xml
<?xml version='1.0' encoding='UTF-8'?>
<Person>
     <name>John Doe</name>
     <age>42</age>
</Person>
```

More advanced example with nested case classes and optional fields:

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
import org.encalmo.writer.xml.annotation.{xmlAttribute, xmlItemTag, xmlName}

case class Tag(
  @xmlAttribute name: String,
  value: String
)

@xmlName("Bookshelf")
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
<Library libraryId="lib123">
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
</Library>
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