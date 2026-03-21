<a href="https://github.com/encalmo/xmlwriter">![GitHub](https://img.shields.io/badge/github-%23121011.svg?style=for-the-badge&logo=github&logoColor=white)</a> <a href="https://central.sonatype.com/artifact/org.encalmo/xmlwriter_3" target="_blank">![Maven Central Version](https://img.shields.io/maven-central/v/org.encalmo/xmlwriter_3?style=for-the-badge)</a> <a href="https://encalmo.github.io/xmlwriter/scaladoc/org/encalmo/writer/xml.html" target="_blank"><img alt="Scaladoc" src="https://img.shields.io/badge/docs-scaladoc-red?style=for-the-badge"></a>

# xmlwriter

Macro-powered fast and easy XML serialization library for Scala 3.

## Table of contents

- [Example usage](#example-usage)
- [Outstanding features](#outstanding-features)
- [Scala types supported directly without the need for typeclass derivation](#scala-types-supported-directly-without-the-need-for-typeclass-derivation)
- [Supported Java types](#supported-java-types)
- [Supported annotations](#supported-annotations)
- [Key abstractions](#key-abstractions)
- [How do we tag elements?](#how-do-we-tag-elements?)
   - [Root element tag](#root-element-tag)
- [Nested elements](#nested-elements)
- [Output types: String, Streaming, and Document](#output-types:-string,-streaming,-and-document)
   - [1. String output](#1.-string-output)
   - [2. Streaming output](#2.-streaming-output)
   - [3. Document output (DOM) without namespace](#3.-document-output-(dom)-without-namespace)
   - [4. Document output (DOM) with default namespace](#4.-document-output-(dom)-with-default-namespace)
   - [5. Document output (DOM) with multiple naamespaces](#5.-document-output-(dom)-with-multiple-naamespaces)
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

The example above produces the following code after macro expansion:
```scala
{
  val builder: org.encalmo.writer.xml.XmlOutputBuilder = ...
  builder.appendElementStart("Employee", immutable.Nil)

  def writeCaseClassToXml_Address(address: Address): scala.Unit = {
    builder.appendElementStart("street")
    builder.appendText(address.street)
    builder.appendElementEnd("street")
    builder.appendElementStart("city")
    builder.appendText(address.city)
    builder.appendElementEnd("city")
    builder.appendElementStart("postcode")
    builder.appendText(address.postcode)
    builder.appendElementEnd("postcode")
  }

  def writeCaseClassToXml_Employee(employee: Employee): scala.Unit = {
    builder.appendElementStart("name")
    builder.appendText(employee.name)
    builder.appendElementEnd("name")
    builder.appendElementStart("age")
    builder.appendText(employee.age.toString())
    builder.appendElementEnd("age")

    employee.email match {
      case string: scala.Some[scala.Predef.String] =>
        builder.appendElementStart("email")
        builder.appendText(string.value)
        builder.appendElementEnd("email")
      case scala.None =>
        ()
    }

    builder.appendElementStart("addresses")
    val addressesIterator: scala.collection.Iterator[Address] = (employee.addresses: scala.collection.Iterable[Address]).iterator
    while (addressesIterator.hasNext) {
      val addressItem: Address = addressesIterator.next()
      builder.appendElementStart("Address", immutable.Nil)
      writeCaseClassToXml_Address(addressItem)
      builder.appendElementEnd("Address")
      ()
    }
    builder.appendElementEnd("addresses")

    builder.appendElementStart("active")
    builder.appendText(employee.active.toString())
    builder.appendElementEnd("active")
  }
  
  writeCaseClassToXml_Employee(entity)
  builder.appendElementEnd("Employee")
}
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
| `@xmlAdditionalTag` | Annotation to wrap value in and additional XML element |
| `@xmlTagLabelAndType` | Annotation to mandate nested tag elements for a field: &lt;field&gt;&lt;type&gt; ... &lt;/type&gt;&lt;/field&gt; |
| `@xmlItemTag`         | Annotation to define the name of the XML element wrapping each item in an array or collection. This will override custom names of the items in the collection.                              |
| `@xmlAdditionalItemTag`         | Annotation to define the name of the XML element additionally wrapping each item in an array or collection. This will NOT override custom names of the items in the collection.                              |
| `@xmlNoItemTags`      | Prevents wrapping each collection element in an extra XML tag; all items are added directly.          |
| `@xmlValue`           | Defines a static value for an element, useful for enum cases     |
| `@xmlValueSelector`   | Selects which member/field/property from a nested type is used as the value/text for this element.    |
| `@xmlEnumCaseValuePlain`   | Annotation to force writing the enum case value as plain text, without wrapping it in a tag.   |

## Key abstractions

- object [`XmlWriter`](XmlWriter.scala) provides the main user-facing API, a host of methods to serialize data types to XML,
- trait `XmlWriter[T]` defines typeclass interface,
- trait [`XmlOutputBuilder`](XmlOutputBuilder.scala) defines low-level API for constructing XML output,
- object `XmlOutputBuilder` provides a set of default implementations of `XmlOutputBuilder` trait producing indented or compact format, building a `String` or writing directly to the `java.io.OutputStream`

## How do we tag elements?

### Root element tag

Root tag can be either provided by the user or derived from the type name.
```scala
case class Foo(bar: String)
val entity = Foo("HELLO")

// <Foo><bar>HELLO</bar></Foo>
val xml1 = XmlWriter.writeIndented(entity) 

// <Example><bar>HELLO</bar></Example>
val xml2 = XmlWriter.writeIndentedUsingRootTagName("Example", entity, addXmlDeclaration = false)
```

## Nested elements

Nested elements borrow tag name either from:
- field name of case classes, selectables or records
- enum case name or value
- declared type name (including type aliases and opaque types)
- keys of the map
- @xmlTag and @xmlItemTag annotations

```scala
case class Tool(name: String, weight: Double)
case class ToolBox(hammer: Tool, screwdriver: Tool)
val entity =
  ToolBox(
    hammer = Tool(name = "Hammer", weight = 10.0),
    screwdriver = Tool(name = "Screwdriver", weight = 2.0)
  )
val xml = XmlWriter.writeIndented(entity)
println(xml)
```
```xml
<?xml version='1.0' encoding='UTF-8'?>
<ToolBox>
    <hammer>
        <name>Hammer</name>
        <weight>10.0</weight>
    </hammer>
    <screwdriver>
        <name>Screwdriver</name>
        <weight>2.0</weight>
    </screwdriver>
</ToolBox>
```

## Output types: String, Streaming, and Document

| Output Type | Use Case                                         | Example API                          |
|:------------|:-------------------------------------------------|:-------------------------------------|
| `String`    | Quick serialization, logs, tests, small data     | `XmlWriter.writeIndented(entity)`    |
| Stream      | Large data, file/network/stream, low memory      | `XmlWriter.streamIndented(...)`      |
| Document    | Java/Scala XML interop, DOM manipulation         | `XmlWriter.writeToDocument(entity)`  |

Choose the output option that matches your workflow—converting between them is possible, but choosing the most direct is typically more efficient.

### 1. String output

The default for most APIs. Methods like `XmlWriter.writeIndented` and `XmlWriter.writeCompact` return the XML as a `String` for easy inspection, logging, or further in-memory processing.

```scala
val xml: String = XmlWriter.writeIndented(entity)
```

### 2. Streaming output

For efficient and memory-safe writing of large or unknown-size documents, you can direct output straight to an `OutputStream` (e.g., file, network socket):

```scala
import java.io.FileOutputStream

val out = new FileOutputStream("output.xml")
XmlWriter.streamIndented(entity, out, addXmlDeclaration = true)
out.close()
```
or for compact (single-line) XML:

```scala
XmlWriter.streamCompact(entity, out, addXmlDeclaration = false)
```

### 3. Document output (DOM) without namespace

For integration with Java XML tools or advanced in-memory XML manipulation, you can serialize to a `org.w3c.dom.Document`:

```scala
val document: org.w3c.dom.Document = XmlWriter.writeToDocument(entity)
```

This DOM-based output allows you to use the rich Java XML ecosystem for further processing, validation, or transformation (for example, using XPath or XSLT).

### 4. Document output (DOM) with default namespace

If you need to generate an XML document with a specific default namespace (e.g., for standards compliance or interoperability), use `XmlWriter.writeDocumentWithNamespace`. This creates a DOM document with the namespace applied to the root element and all descendants where appropriate.

```scala
import org.encalmo.writer.xml.XmlWriter

case class Person(
  name: String,
  age: Int
  // ... other fields ...
)

val person = Person(
  name = "John Doe",
  age = 30
  // ... other values ...
)

val namespace = "http://example.com/person"
// Produce a DOM Document (org.w3c.dom.Document) with namespace
val doc: org.w3c.dom.Document =
  XmlWriter.writeDocumentWithNamespace(person, namespace)

// To convert the document to a String for output or inspection:
val xmlString = org.encalmo.writer.xml.XmlUtils.toXmlString(doc)
println(xmlString)
```

This produces output like:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<Person xmlns="http://example.com/person">
  <name>John Doe</name>
  <age>30</age>
  <!-- ... other fields ... -->
</Person>
```

### 5. Document output (DOM) with multiple naamespaces

If you need to produce XML with multiple namespaces mapped to different prefixes, you can use `XmlWriter.writeDocumentWithNamespaceMapping`. This method allows you to specify a default namespace and any number of additional namespace prefixes and URIs. All child elements will use the namespace of the parent element, unless the child element gets a new namespace from the mapping.

```scala
import org.encalmo.writer.xml.XmlWriter
import org.encalmo.writer.xml.annotation.*

case class Book(
  title: String,
  author: String
)

case class Library(
  @xmlAttribute libraryId: String,
  name: String,
  @xmlItemTag("Book") books: List[Book]
)

val library = Library(
  libraryId = "lib123",
  name = "City Library",
  books = List(
    Book("Programming Scala", "Dean Wampler"),
    Book("Functional Programming in Scala", "Paul Chiusano")
  )
)

val nsMapping = Map(
  "Library" -> ("","http://example.com/library"),    // default namespace
  "Book" -> ("bk","http://example.com/book")
)

val doc: org.w3c.dom.Document =
  XmlWriter.writeDocumentWithNamespaceMapping(library, nsMapping)

// Convert the document to a String for display or output:
val xmlString = org.encalmo.writer.xml.XmlUtils.toXmlString(doc)
println(xmlString)
```

This will produce output similar to:

```xml
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<Library xmlns="http://example.com/library" xmlns:bk="http://example.com/book" libraryId="lib123">
  <name>City Library</name>
  <books>
    <bk:Book>
      <bk:title>Programming Scala</bk:title>
      <bk:author>Dean Wampler</bk:author>
    </bk:Book>
    <bk:Book>
      <bk:title>Functional Programming in Scala</bk:title>
      <bk:author>Paul Chiusano</bk:author>
    </bk:Book>
  </books>
</Library>
```

This approach is useful for generating XML that integrates with schemas or APIs requiring multiple namespaces, allowing you to fully control the output format.

## Dependencies

   - [Scala](https://www.scala-lang.org) >= 3.7.4
   - org.encalmo [**type-tree-visitor** 0.10.0](https://central.sonatype.com/artifact/org.encalmo/type-tree-visitor_3)

## Usage

Use with SBT

    libraryDependencies += "org.encalmo" %% "xmlwriter" % "0.17.0"

or with SCALA-CLI

    //> using dep org.encalmo::xmlwriter:0.17.0

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
├── ExampleModel.test.scala
├── ExampleModelSpec.test.scala
├── LICENSE
├── Order.java
├── project.scala
├── README.md
├── Status.java
├── test.sh
├── TestData.test.scala
├── TestModel.test.scala
├── XmlOutputBuilder.scala
├── XmlUtils.scala
├── XmlWriter.scala
├── XmlWriterMacro.scala
├── XmlWriterMacroVisitor.scala
└── XmlWriterSpec.test.scala
```

