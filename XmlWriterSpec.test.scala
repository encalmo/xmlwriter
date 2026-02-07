package org.encalmo.writer.xml

import org.encalmo.writer.xml.annotation.*
import java.time.LocalDate
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class XmlWriterSpec extends munit.FunSuite {

  test("write simple case class") {

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
    assertEquals(
      xml,
      """|<?xml version='1.0' encoding='UTF-8'?>
         |<Employee>
         |    <name>John Doe</name>
         |    <age>30</age>
         |    <email>john.doe@example.com</email>
         |    <addresses>
         |        <Address>
         |            <street>123 Main St</street>
         |            <city>Anytown</city>
         |            <postcode>12345</postcode>
         |        </Address>
         |        <Address>
         |            <street>456 Back St</street>
         |            <city>Downtown</city>
         |            <postcode>78901</postcode>
         |        </Address>
         |    </addresses>
         |    <active>true</active>
         |</Employee>""".stripMargin
    )
  }

  test("write the same case class with different element tag names") {
    case class Tool(name: String, weight: Double)
    case class ToolBox(hammer: Tool, screwdriver: Tool)
    val entity =
      ToolBox(
        hammer = Tool(name = "Hammer", weight = 10.0),
        screwdriver = Tool(name = "Screwdriver", weight = 2.0)
      )
    val xml = XmlWriter.writeIndented(entity)
    println(xml)
    assertEquals(
      xml,
      """|<?xml version='1.0' encoding='UTF-8'?>
         |<ToolBox>
         |    <hammer>
         |        <name>Hammer</name>
         |        <weight>10.0</weight>
         |    </hammer>
         |    <screwdriver>
         |        <name>Screwdriver</name>
         |        <weight>2.0</weight>
         |    </screwdriver>
         |</ToolBox>""".stripMargin
    )
  }

  test("write a type alias") {
    type Name = String
    val entity: Name = "John Doe"
    val xml = XmlWriter.writeIndented(entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<Name>John Doe</Name>""".stripMargin
    )
  }

  test("write a list of type aliases") {
    type name = String
    val entity: List[name] = List("John Doe", "Jane Doe")
    val xml = XmlWriter.writeIndentedUsingRootTagName("names", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<names>
         |    <name>John Doe</name>
         |    <name>Jane Doe</name>
         |</names>""".stripMargin
    )
  }

  test("write an opaque type with an upper bound") {
    val entity = DriverLicense("1234567890", LocalDate.of(2026, 1, 1))
    val xml = XmlWriter.writeIndented(entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<DriverLicense expiryDate="2026-01-01">1234567890</DriverLicense>""".stripMargin
    )
  }

  test("write an opaque type without an upper bound") {
    val entity = PassportNumber("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
    val xml = XmlWriter.writeIndented(entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<PassportNumber>ABCDEFGHIJKLMNOPQRSTUVWXYZ</PassportNumber>""".stripMargin
    )
  }

  test("write a simple case class without selector annotation") {
    case class Ball(diameter: Double)
    val entity = Ball(1.2)
    val xml = XmlWriter.writeIndentedUsingRootTagName("ball", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<ball>
         |    <diameter>1.2</diameter>
         |</ball>""".stripMargin
    )
  }

  test("write a simple case class with a value selector annotation") {
    @xmlValueSelector("diameter") case class Ball(diameter: Double)
    val entity = Ball(1.2)
    val xml = XmlWriter.writeIndentedUsingRootTagName("ball", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<ball>1.2</ball>""".stripMargin
    )
  }

  test("write a list of values of case class with a value selector annotation") {
    @xmlValueSelector("diameter") case class Ball(diameter: Double)
    val entity = List(Ball(1.2), Ball(3.4))
    val xml = XmlWriter.writeIndentedUsingRootTagName("balls", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<balls>
         |    <Ball>1.2</Ball>
         |    <Ball>3.4</Ball>
         |</balls>""".stripMargin
    )
  }

  test("write Enum") {
    val entity = Citizenship.UK
    val xml = XmlWriter.writeIndentedUsingRootTagName("citizenship", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<citizenship>United Kingdom</citizenship>""".stripMargin
    )
  }

  test("write tuple of Enum with case values and case class values") {
    val entity =
      (
        MaritalStatus.Single,
        MaritalStatus.Married(partnerName = "Jane Doe", from = LocalDate.of(2025, 1, 1)),
        MaritalStatus.CivilPartnership(partnerName = "John Doe", from = LocalDate.of(2024, 1, 1)),
        MaritalStatus.Divorced(from = LocalDate.of(2023, 1, 1)),
        MaritalStatus.Widowed(from = LocalDate.of(2022, 1, 1))
      )

    val xml = XmlWriter.writeIndentedUsingRootTagName("maritalStatus", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<maritalStatus>
         |    <MaritalStatus>Single</MaritalStatus>
         |    <MaritalStatus>
         |        <Married>
         |            <partnerName>Jane Doe</partnerName>
         |            <from>2025-01-01</from>
         |        </Married>
         |    </MaritalStatus>
         |    <MaritalStatus>
         |        <CivilPartnership>
         |            <partnerName>John Doe</partnerName>
         |            <from>2024-01-01</from>
         |        </CivilPartnership>
         |    </MaritalStatus>
         |    <MaritalStatus>
         |        <Divorced>
         |            <from>2023-01-01</from>
         |        </Divorced>
         |    </MaritalStatus>
         |    <MaritalStatus>
         |        <Widowed>
         |            <from>2022-01-01</from>
         |        </Widowed>
         |    </MaritalStatus>
         |</maritalStatus>""".stripMargin
    )
  }

  test("write list of Enum with case values and case class values") {
    @xmlNoItemTags val entity =
      List(
        MaritalStatus.Single,
        MaritalStatus.Married(partnerName = "Jane Doe", from = LocalDate.of(2025, 1, 1)),
        MaritalStatus.CivilPartnership(partnerName = "John Doe", from = LocalDate.of(2024, 1, 1)),
        MaritalStatus.Divorced(from = LocalDate.of(2023, 1, 1)),
        MaritalStatus.Widowed(from = LocalDate.of(2022, 1, 1))
      )

    val xml = XmlWriter.writeIndentedUsingRootTagName("maritalStatus", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<maritalStatus>
         |    <Single></Single>
         |    <Married>
         |        <partnerName>Jane Doe</partnerName>
         |        <from>2025-01-01</from>
         |    </Married>
         |    <CivilPartnership>
         |        <partnerName>John Doe</partnerName>
         |        <from>2024-01-01</from>
         |    </CivilPartnership>
         |    <Divorced>
         |        <from>2023-01-01</from>
         |    </Divorced>
         |    <Widowed>
         |        <from>2022-01-01</from>
         |    </Widowed>
         |</maritalStatus>""".stripMargin
    )
  }

  test("write Enum with custom case value selector") {
    val entity = List(
      Hobby.Swimming,
      Hobby.Cooking,
      Hobby.Other(name = "Binge watching TV series")
    )
    val xml = XmlWriter.writeIndentedUsingRootTagName("hobbies", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<hobbies>
         |    <Hobby>Swimming</Hobby>
         |    <Hobby>Cooking</Hobby>
         |    <Hobby>Binge watching TV series</Hobby>
         |</hobbies>""".stripMargin
    )
  }

  test("write Java enum") {
    val entity = (Status.PENDING, Status.ACTIVE, Status.FAILED)
    val xml = XmlWriter.writeIndented(entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<Tuple3>
         |    <Status>PENDING</Status>
         |    <Status>ACTIVE</Status>
         |    <Status>FAILED</Status>
         |</Tuple3>""".stripMargin
    )
  }

  test("write Java enum tuple with custom tag name") {
    @xmlTag("Statuses") val entity = (Status.PENDING, Status.ACTIVE, Status.FAILED)
    val xml = XmlWriter.writeIndented(entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<Statuses>
         |    <Status>PENDING</Status>
         |    <Status>ACTIVE</Status>
         |    <Status>FAILED</Status>
         |</Statuses>""".stripMargin
    )
  }

  test("write Enum within a case class") {
    case class Foo(status: Status)
    val entity = Foo(status = Status.PENDING)
    val xml = XmlWriter.writeIndented(entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<Foo>
         |    <status>PENDING</status>
         |</Foo>""".stripMargin
    )
  }

  test("write case class Enum within a case class") {
    case class Foo(status: MaritalStatus, hobby: Hobby)
    val entity = Foo(
      status = MaritalStatus.Married(partnerName = "Jane Doe", from = LocalDate.of(2025, 1, 1)),
      hobby = Hobby.Other(name = "Binge watching TV series")
    )
    val xml = XmlWriter.writeIndented(entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<Foo>
         |    <status>
         |        <Married>
         |            <partnerName>Jane Doe</partnerName>
         |            <from>2025-01-01</from>
         |        </Married>
         |    </status>
         |    <hobby>Binge watching TV series</hobby>
         |</Foo>""".stripMargin
    )
  }

  test("write Option") {
    val entity = Option(Vector(1.2, 3.4))
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>
         |    <Double>1.2</Double>
         |    <Double>3.4</Double>
         |</foo>""".stripMargin
    )
  }

  test("write Either") {
    val entity: (Either[String, Double], Either[String, Double]) = (Left("fail"), Right(1.2))
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>
         |    <String>fail</String>
         |    <Double>1.2</Double>
         |</foo>""".stripMargin
    )
  }

  test("write List") {
    val entity = List(1, 2, 3)
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>
         |    <Int>1</Int>
         |    <Int>2</Int>
         |    <Int>3</Int>
         |</foo>""".stripMargin
    )
  }

  test("write List with item tag") {
    @xmlItemTag("pos") val entity = List(1, 2, 3)
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>
         |    <pos>1</pos>
         |    <pos>2</pos>
         |    <pos>3</pos>
         |</foo>""".stripMargin
    )
  }

  test("write List with item tag and skip item tags") {
    @xmlNoItemTags val entity = List(1, 2, 3)
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>123</foo>""".stripMargin
    )
  }

  test("write Array") {
    val entity = Array(1, 2, 3)
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>
         |    <Int>1</Int>
         |    <Int>2</Int>
         |    <Int>3</Int>
         |</foo>""".stripMargin
    )
  }

  test("write Array of Arrays") {
    val entity = Array(Array(1, 2, 3), Array(4, 5, 6))
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>
         |    <Array>
         |        <Int>1</Int>
         |        <Int>2</Int>
         |        <Int>3</Int>
         |    </Array>
         |    <Array>
         |        <Int>4</Int>
         |        <Int>5</Int>
         |        <Int>6</Int>
         |    </Array>
         |</foo>""".stripMargin
    )
  }

  test("write Array of Arrays using item tag") {
    @xmlItemTag("bar") val entity = Array(Array(1, 2, 3), Array(4, 5, 6))
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>
         |    <bar>
         |        <Int>1</Int>
         |        <Int>2</Int>
         |        <Int>3</Int>
         |    </bar>
         |    <bar>
         |        <Int>4</Int>
         |        <Int>5</Int>
         |        <Int>6</Int>
         |    </bar>
         |</foo>""".stripMargin
    )
  }

  test("write Array of Arrays of Arrays") {
    val entity = Array(Array(Array(1, 2, 3), Array(4, 5, 6)), Array(Array(10, 11, 12)))
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>
         |    <Array>
         |        <Array>
         |            <Int>1</Int>
         |            <Int>2</Int>
         |            <Int>3</Int>
         |        </Array>
         |        <Array>
         |            <Int>4</Int>
         |            <Int>5</Int>
         |            <Int>6</Int>
         |        </Array>
         |    </Array>
         |    <Array>
         |        <Array>
         |            <Int>10</Int>
         |            <Int>11</Int>
         |            <Int>12</Int>
         |        </Array>
         |    </Array>
         |</foo>""".stripMargin
    )
  }

  test("write Map") {
    val entity = Map("x" -> 1, "y" -> 2)
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>
         |    <x>1</x>
         |    <y>2</y>
         |</foo>""".stripMargin
    )
  }

  test("write Map inside the case class") {
    case class Foo(mappings: Map[String, Int])
    val entity = Foo(Map("x" -> 1, "y" -> 2))
    val xml = XmlWriter.writeIndented(entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<Foo>
         |    <mappings>
         |        <x>1</x>
         |        <y>2</y>
         |    </mappings>
         |</Foo>""".stripMargin
    )
  }

  test("write Tuple 2 elements") {
    val entity = (1, Option(Vector(1.2, 3.4)))
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>
         |    <Int>1</Int>
         |    <Vector>
         |        <Double>1.2</Double>
         |        <Double>3.4</Double>
         |    </Vector>
         |</foo>""".stripMargin
    )
  }

  test("write Union") {
    type Union = String | Int | Boolean
    val entity: Union = "a"
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>a</foo>""".stripMargin
    )
  }

  test("write List of Union") {
    type Union = String | Int | Boolean
    val entity: List[Union] = List("a", 1, true)
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>
         |    <Union>a</Union>
         |    <Union>1</Union>
         |    <Union>true</Union>
         |</foo>""".stripMargin
    )
  }

  test("write large Tuple") {
    val entity = (
      1, // Int
      "abc", // String
      3.14, // Double
      true, // Boolean
      'x', // Char
      2L, // Long
      1.23f, // Float
      100.toShort, // Short
      1.toByte, // Byte
      BigInt(42), // BigInt
      BigDecimal(3.1415), // BigDecimal
      Some("maybe"), // Option[String]
      None, // Option[Nothing]
      Left("fail"), // Either[String, Int]
      Right(7), // Either[String, Int]
      List(1, 2, 3), // List[Int]
      Vector("a", "b"), // Vector[String]
      Set(6, 7, 8), // Set[Int]
      Map("x" -> 1), // Map[String, Int]
      Array(2, 4, 6), // Array[Int]
      LocalDate.of(2024, 1, 1), // java.time.LocalDate
      BigDecimal("12345.67"), // java.math.BigDecimal
      None: Option[Int], // Option[Int] (None)
      Some(LocalDate.MAX), // Option[LocalDate]
      7.77d, // Double
      0x42, // Int (hex)
      List('a', 'b', 'c'), // List[Char]
      Map(1 -> "one", 2 -> "two"), // Map[Int, String]
      Option(Vector(1.2, 3.4)) // Option[Vector[Double]]
    )
    val xml = XmlWriter.writeIndentedUsingRootTagName("foo", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<foo>
         |    <Int>1</Int>
         |    <String>abc</String>
         |    <Double>3.14</Double>
         |    <Boolean>true</Boolean>
         |    <Char>x</Char>
         |    <Long>2</Long>
         |    <Float>1.23</Float>
         |    <Short>100</Short>
         |    <Byte>1</Byte>
         |    <BigInt>42</BigInt>
         |    <BigDecimal>3.1415</BigDecimal>
         |    <Some>maybe</Some>
         |    <Left>fail</Left>
         |    <Right>7</Right>
         |    <List>
         |        <Int>1</Int>
         |        <Int>2</Int>
         |        <Int>3</Int>
         |    </List>
         |    <Vector>
         |        <String>a</String>
         |        <String>b</String>
         |    </Vector>
         |    <Set>
         |        <Int>6</Int>
         |        <Int>7</Int>
         |        <Int>8</Int>
         |    </Set>
         |    <Map>
         |        <x>1</x>
         |    </Map>
         |    <Array>
         |        <Int>2</Int>
         |        <Int>4</Int>
         |        <Int>6</Int>
         |    </Array>
         |    <LocalDate>2024-01-01</LocalDate>
         |    <BigDecimal>12345.67</BigDecimal>
         |    <Some>+999999999-12-31</Some>
         |    <Double>7.77</Double>
         |    <Int>66</Int>
         |    <List>
         |        <Char>a</Char>
         |        <Char>b</Char>
         |        <Char>c</Char>
         |    </List>
         |    <Map>
         |        <1>one</1>
         |        <2>two</2>
         |    </Map>
         |    <Vector>
         |        <Double>1.2</Double>
         |        <Double>3.4</Double>
         |    </Vector>
         |</foo>""".stripMargin
    )
  }

  test("write Selectable") {
    val entity = new FactsRow
    val xml = XmlWriter.writeIndented(entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<FactsRow>
         |    <name>John Doe</name>
         |    <age>30</age>
         |    <email>john.doe@example.com</email>
         |</FactsRow>""".stripMargin
    )
  }

  test("write Java map") {
    val entity = new java.util.LinkedHashMap[String, Integer]()
    entity.put("UPS", 1)
    entity.put("FedEx", 2)
    val xml = XmlWriter.writeIndentedUsingRootTagName("suppliers", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<suppliers>
         |    <UPS>1</UPS>
         |    <FedEx>2</FedEx>
         |</suppliers>""".stripMargin
    )
  }

  test("write Java iterable") {
    @xmlItemTag("item") val entity = java.util.List.of(1, 2, 3)
    val xml = XmlWriter.writeIndentedUsingRootTagName("items", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<items>
         |    <item>1</item>
         |    <item>2</item>
         |    <item>3</item>
         |</items>""".stripMargin
    )
  }

  test("write Java record") {
    val deliveryMap = new java.util.LinkedHashMap[String, Integer]()
    deliveryMap.put("UPS", 1)
    deliveryMap.put("FedEx", 2)
    val entity =
      Order(
        "123",
        "John Doe",
        java.util.List.of(1, 2, 3),
        Array(1, 2, 3),
        java.math.BigDecimal("100.00"),
        deliveryMap
      )
    val xml = XmlWriter.writeIndented(entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<Order>
         |    <id>123</id>
         |    <customerId>John Doe</customerId>
         |    <items>
         |        <Integer>1</Integer>
         |        <Integer>2</Integer>
         |        <Integer>3</Integer>
         |    </items>
         |    <codes>
         |        <Int>1</Int>
         |        <Int>2</Int>
         |        <Int>3</Int>
         |    </codes>
         |    <total>100.00</total>
         |    <delivery>
         |        <UPS>1</UPS>
         |        <FedEx>2</FedEx>
         |    </delivery>
         |</Order>""".stripMargin
    )
  }

  test("write single Address") {
    val entity = Address(street = "123 <Main> St", city = "&Anytown", postcode = "12345")
    val xml = XmlWriter.writeIndented(entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<Address>
         |    <street>123 &lt;Main&gt; St</street>
         |    <city>&amp;Anytown</city>
         |    <zipcode>12345</zipcode>
         |</Address>""".stripMargin
    )
  }

  test("write single Airbus case class") {
    val xml = XmlWriter.writeIndented(Airbus("A380"), addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<Airbus>
         |    <model>A380</model>
         |</Airbus>""".stripMargin
    )
  }

  test("write single Airbus case class when typed as Planes trait") {
    val entity: Planes = Airbus("A380")
    val xml = XmlWriter.writeIndented(entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<Planes>
         |    <Airbus>
         |        <model>A380</model>
         |    </Airbus>
         |</Planes>""".stripMargin
    )
  }

  test("write list of Address using root tag name") {
    val entity = List(TestData.address1, TestData.address2)
    val xml = XmlWriter.writeIndentedUsingRootTagName("Addresses", entity)
    println(xml)
    assertEquals(
      xml,
      """|<?xml version='1.0' encoding='UTF-8'?>
         |<Addresses>
         |    <Address>
         |        <street>123 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </Address>
         |    <Address>
         |        <street>456 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </Address>
         |</Addresses>""".stripMargin
    )
  }

  test("write list of Address using type name") {
    val entity = List(TestData.address1, TestData.address2)
    val xml = XmlWriter.writeIndented(entity)
    println(xml)
    assertEquals(
      xml,
      """|<?xml version='1.0' encoding='UTF-8'?>
         |<List>
         |    <Address>
         |        <street>123 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </Address>
         |    <Address>
         |        <street>456 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </Address>
         |</List>""".stripMargin
    )
  }

  test("write list of Address using name provided by xmlTag annotation on a value") {
    @xmlTag("Addresses") val entity = List(TestData.address1, TestData.address2)
    val xml = XmlWriter.writeIndented(entity)
    println(xml)
    assertEquals(
      xml,
      """|<?xml version='1.0' encoding='UTF-8'?>
         |<Addresses>
         |    <Address>
         |        <street>123 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </Address>
         |    <Address>
         |        <street>456 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </Address>
         |</Addresses>""".stripMargin
    )
  }

  test("write list of Address using name provided by xmlTag and xmlItemTag annotations on a value") {
    @xmlTag("A")
    @xmlItemTag("B")
    val entity = List(TestData.address1, TestData.address2)

    val xml = XmlWriter.writeIndented(entity)
    println(xml)
    assertEquals(
      xml,
      """|<?xml version='1.0' encoding='UTF-8'?>
         |<A>
         |    <B>
         |        <street>123 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </B>
         |    <B>
         |        <street>456 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </B>
         |</A>""".stripMargin
    )
  }

  test("write list of Address using name provided by xmlTag and xmlNoItemTags annotations on a value") {
    @xmlTag("A")
    @xmlNoItemTags
    val entity = List(TestData.address1, TestData.address2)

    val xml = XmlWriter.writeIndented(entity)
    println(xml)
    assertEquals(
      xml,
      """|<?xml version='1.0' encoding='UTF-8'?>
         |<A>
         |    <street>123 &lt;Main&gt; St</street>
         |    <city>&amp;Anytown</city>
         |    <zipcode>12345</zipcode>
         |    <street>456 &lt;Main&gt; St</street>
         |    <city>&amp;Anytown</city>
         |    <zipcode>12345</zipcode>
         |</A>""".stripMargin
    )
  }

  test("write Person case class using indended writer") {
    val xml = XmlWriter.writeIndented(TestData.person, addXmlDeclaration = false)
    println(xml)

    assertEquals(
      xml,
      """|<Person ID="1234567890">
         |    <name>John Doe</name>
         |    <age>30</age>
         |    <stature>188.8</stature>
         |    <email>john.doe@example.com</email>
         |    <address>
         |        <street>123 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </address>
         |    <addresses>
         |        <home>
         |            <street>123 &lt;Main&gt; St</street>
         |            <city>&amp;Anytown</city>
         |            <zipcode>12345</zipcode>
         |        </home>
         |        <work>
         |            <street>456 &lt;Main&gt; St</street>
         |            <city>&amp;Anytown</city>
         |            <zipcode>12345</zipcode>
         |        </work>
         |    </addresses>
         |    <isStudent>false</isStudent>
         |    <tags>
         |        <tag name="tag1&quot;">value1</tag>
         |        <tag name="&lt;tag2&gt;">value2</tag>
         |    </tags>
         |    <citizenship>United Kingdom</citizenship>
         |    <current-immigration-status>permanent resident</current-immigration-status>
         |    <immigration-status-valid-until>2026-01-01</immigration-status-valid-until>
         |    <marital>
         |        <Single></Single>
         |        <Married>
         |            <partnerName>Jane Doe</partnerName>
         |            <from>2025-01-01</from>
         |        </Married>
         |    </marital>
         |    <hobbies>
         |        <Hobby>Swimming</Hobby>
         |        <Hobby>Cooking</Hobby>
         |        <Hobby>Binge watching TV series</Hobby>
         |    </hobbies>
         |    <hobby>Playing guitar</hobby>
         |    <passportNumber>1234567890</passportNumber>
         |    <driverLicense expiryDate="2026-12-31">abcdefghijklm</driverLicense>
         |    <disabilities>
         |        <Disability>Blindness</Disability>
         |        <Disability>Deafness</Disability>
         |    </disabilities>
         |    <disability>Blindness</disability>
         |    <benefits1>
         |        <Benefit>ChildBenefit</Benefit>
         |        <Benefit>UniversalCredit</Benefit>
         |    </benefits1>
         |    <benefits2>false</benefits2>
         |    <skills>
         |        <skill>Java</skill>
         |        <skill>Scala</skill>
         |        <skill>Python</skill>
         |    </skills>
         |    <wallet>
         |        <item>123</item>
         |        <item>John Doe</item>
         |        <item>2025-01-01</item>
         |    </wallet>
         |    <assets>
         |        <Cars>Ford</Cars>
         |        <Boats>
         |            <boat>Brave Wave</boat>
         |            <boat>Sharky</boat>
         |        </Boats>
         |        <Planes>
         |            <Airbus>
         |                <model>A380</model>
         |            </Airbus>
         |        </Planes>
         |    </assets>
         |    <books>
         |        <book>
         |            <author>Francis Scott Fitzgerald</author>
         |            <title>The Great Gatsby</title>
         |        </book>
         |        <book>
         |            <author>Harper Lee</author>
         |            <title>To Kill a Mockingbird</title>
         |        </book>
         |    </books>
         |    <bookAtDesk>
         |        <author>A.A. Milne</author>
         |        <title>Winnie the Pooh</title>
         |    </bookAtDesk>
         |    <hand1>Left hand</hand1>
         |    <hand2>Right hand</hand2>
         |    <status>PENDING</status>
         |    <active>yes</active>
         |</Person>""".stripMargin
    )
  }

  test("write Person case class using compact writer") {
    val entity = TestData.person
    val xml = XmlWriter.writeCompact(entity, addXmlDeclaration = false)
    println(xml)

    assertEquals(
      xml,
      """<Person ID="1234567890"><name>John Doe</name><age>30</age><stature>188.8</stature><email>john.doe@example.com</email><address><street>123 &lt;Main&gt; St</street><city>&amp;Anytown</city><zipcode>12345</zipcode></address><addresses><home><street>123 &lt;Main&gt; St</street><city>&amp;Anytown</city><zipcode>12345</zipcode></home><work><street>456 &lt;Main&gt; St</street><city>&amp;Anytown</city><zipcode>12345</zipcode></work></addresses><isStudent>false</isStudent><tags><tag name="tag1&quot;">value1</tag><tag name="&lt;tag2&gt;">value2</tag></tags><citizenship>United Kingdom</citizenship><current-immigration-status>permanent resident</current-immigration-status><immigration-status-valid-until>2026-01-01</immigration-status-valid-until><marital><Single></Single><Married><partnerName>Jane Doe</partnerName><from>2025-01-01</from></Married></marital><hobbies><Hobby>Swimming</Hobby><Hobby>Cooking</Hobby><Hobby>Binge watching TV series</Hobby></hobbies><hobby>Playing guitar</hobby><passportNumber>1234567890</passportNumber><driverLicense expiryDate="2026-12-31">abcdefghijklm</driverLicense><disabilities><Disability>Blindness</Disability><Disability>Deafness</Disability></disabilities><disability>Blindness</disability><benefits1><Benefit>ChildBenefit</Benefit><Benefit>UniversalCredit</Benefit></benefits1><benefits2>false</benefits2><skills><skill>Java</skill><skill>Scala</skill><skill>Python</skill></skills><wallet><item>123</item><item>John Doe</item><item>2025-01-01</item></wallet><assets><Cars>Ford</Cars><Boats><boat>Brave Wave</boat><boat>Sharky</boat></Boats><Planes><Airbus><model>A380</model></Airbus></Planes></assets><books><book><author>Francis Scott Fitzgerald</author><title>The Great Gatsby</title></book><book><author>Harper Lee</author><title>To Kill a Mockingbird</title></book></books><bookAtDesk><author>A.A. Milne</author><title>Winnie the Pooh</title></bookAtDesk><hand1>Left hand</hand1><hand2>Right hand</hand2><status>PENDING</status><active>yes</active></Person>""".stripMargin
    )
  }

  test("write Person case class using indended stream writer") {
    val outputStream = new ByteArrayOutputStream()
    XmlWriter.streamIndented(TestData.person, outputStream, addXmlDeclaration = false)
    val xml = new String(outputStream.toByteArray, StandardCharsets.UTF_8)
    outputStream.close()
    println(xml)
    assertEquals(
      xml,
      """|<Person ID="1234567890">
         |    <name>John Doe</name>
         |    <age>30</age>
         |    <stature>188.8</stature>
         |    <email>john.doe@example.com</email>
         |    <address>
         |        <street>123 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </address>
         |    <addresses>
         |        <home>
         |            <street>123 &lt;Main&gt; St</street>
         |            <city>&amp;Anytown</city>
         |            <zipcode>12345</zipcode>
         |        </home>
         |        <work>
         |            <street>456 &lt;Main&gt; St</street>
         |            <city>&amp;Anytown</city>
         |            <zipcode>12345</zipcode>
         |        </work>
         |    </addresses>
         |    <isStudent>false</isStudent>
         |    <tags>
         |        <tag name="tag1&quot;">value1</tag>
         |        <tag name="&lt;tag2&gt;">value2</tag>
         |    </tags>
         |    <citizenship>United Kingdom</citizenship>
         |    <current-immigration-status>permanent resident</current-immigration-status>
         |    <immigration-status-valid-until>2026-01-01</immigration-status-valid-until>
         |    <marital>
         |        <Single></Single>
         |        <Married>
         |            <partnerName>Jane Doe</partnerName>
         |            <from>2025-01-01</from>
         |        </Married>
         |    </marital>
         |    <hobbies>
         |        <Hobby>Swimming</Hobby>
         |        <Hobby>Cooking</Hobby>
         |        <Hobby>Binge watching TV series</Hobby>
         |    </hobbies>
         |    <hobby>Playing guitar</hobby>
         |    <passportNumber>1234567890</passportNumber>
         |    <driverLicense expiryDate="2026-12-31">abcdefghijklm</driverLicense>
         |    <disabilities>
         |        <Disability>Blindness</Disability>
         |        <Disability>Deafness</Disability>
         |    </disabilities>
         |    <disability>Blindness</disability>
         |    <benefits1>
         |        <Benefit>ChildBenefit</Benefit>
         |        <Benefit>UniversalCredit</Benefit>
         |    </benefits1>
         |    <benefits2>false</benefits2>
         |    <skills>
         |        <skill>Java</skill>
         |        <skill>Scala</skill>
         |        <skill>Python</skill>
         |    </skills>
         |    <wallet>
         |        <item>123</item>
         |        <item>John Doe</item>
         |        <item>2025-01-01</item>
         |    </wallet>
         |    <assets>
         |        <Cars>Ford</Cars>
         |        <Boats>
         |            <boat>Brave Wave</boat>
         |            <boat>Sharky</boat>
         |        </Boats>
         |        <Planes>
         |            <Airbus>
         |                <model>A380</model>
         |            </Airbus>
         |        </Planes>
         |    </assets>
         |    <books>
         |        <book>
         |            <author>Francis Scott Fitzgerald</author>
         |            <title>The Great Gatsby</title>
         |        </book>
         |        <book>
         |            <author>Harper Lee</author>
         |            <title>To Kill a Mockingbird</title>
         |        </book>
         |    </books>
         |    <bookAtDesk>
         |        <author>A.A. Milne</author>
         |        <title>Winnie the Pooh</title>
         |    </bookAtDesk>
         |    <hand1>Left hand</hand1>
         |    <hand2>Right hand</hand2>
         |    <status>PENDING</status>
         |    <active>yes</active>
         |</Person>""".stripMargin
    )
  }

  test("write Person case class using compact stream writer") {
    val entity = TestData.person
    val outputStream = new ByteArrayOutputStream()
    XmlWriter.streamCompact(entity, outputStream, addXmlDeclaration = false)
    val xml = new String(outputStream.toByteArray, StandardCharsets.UTF_8)
    outputStream.close()
    println(xml)

    assertEquals(
      xml,
      """<Person ID="1234567890"><name>John Doe</name><age>30</age><stature>188.8</stature><email>john.doe@example.com</email><address><street>123 &lt;Main&gt; St</street><city>&amp;Anytown</city><zipcode>12345</zipcode></address><addresses><home><street>123 &lt;Main&gt; St</street><city>&amp;Anytown</city><zipcode>12345</zipcode></home><work><street>456 &lt;Main&gt; St</street><city>&amp;Anytown</city><zipcode>12345</zipcode></work></addresses><isStudent>false</isStudent><tags><tag name="tag1&quot;">value1</tag><tag name="&lt;tag2&gt;">value2</tag></tags><citizenship>United Kingdom</citizenship><current-immigration-status>permanent resident</current-immigration-status><immigration-status-valid-until>2026-01-01</immigration-status-valid-until><marital><Single></Single><Married><partnerName>Jane Doe</partnerName><from>2025-01-01</from></Married></marital><hobbies><Hobby>Swimming</Hobby><Hobby>Cooking</Hobby><Hobby>Binge watching TV series</Hobby></hobbies><hobby>Playing guitar</hobby><passportNumber>1234567890</passportNumber><driverLicense expiryDate="2026-12-31">abcdefghijklm</driverLicense><disabilities><Disability>Blindness</Disability><Disability>Deafness</Disability></disabilities><disability>Blindness</disability><benefits1><Benefit>ChildBenefit</Benefit><Benefit>UniversalCredit</Benefit></benefits1><benefits2>false</benefits2><skills><skill>Java</skill><skill>Scala</skill><skill>Python</skill></skills><wallet><item>123</item><item>John Doe</item><item>2025-01-01</item></wallet><assets><Cars>Ford</Cars><Boats><boat>Brave Wave</boat><boat>Sharky</boat></Boats><Planes><Airbus><model>A380</model></Airbus></Planes></assets><books><book><author>Francis Scott Fitzgerald</author><title>The Great Gatsby</title></book><book><author>Harper Lee</author><title>To Kill a Mockingbird</title></book></books><bookAtDesk><author>A.A. Milne</author><title>Winnie the Pooh</title></bookAtDesk><hand1>Left hand</hand1><hand2>Right hand</hand2><status>PENDING</status><active>yes</active></Person>""".stripMargin
    )
  }

  test("write Person case class using indended writer with name transformation") {
    val xml = XmlWriter.writeIndentedWithNameTransformation(
      TestData.person,
      tagNameTransformation = name => name.toLowerCase,
      attributeNameTransformation = name => name.toLowerCase,
      addXmlDeclaration = false
    )
    println(xml)

    assertEquals(
      xml,
      """|<person id="1234567890">
         |    <name>John Doe</name>
         |    <age>30</age>
         |    <stature>188.8</stature>
         |    <email>john.doe@example.com</email>
         |    <address>
         |        <street>123 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </address>
         |    <addresses>
         |        <home>
         |            <street>123 &lt;Main&gt; St</street>
         |            <city>&amp;Anytown</city>
         |            <zipcode>12345</zipcode>
         |        </home>
         |        <work>
         |            <street>456 &lt;Main&gt; St</street>
         |            <city>&amp;Anytown</city>
         |            <zipcode>12345</zipcode>
         |        </work>
         |    </addresses>
         |    <isstudent>false</isstudent>
         |    <tags>
         |        <tag name="tag1&quot;">value1</tag>
         |        <tag name="&lt;tag2&gt;">value2</tag>
         |    </tags>
         |    <citizenship>United Kingdom</citizenship>
         |    <current-immigration-status>permanent resident</current-immigration-status>
         |    <immigration-status-valid-until>2026-01-01</immigration-status-valid-until>
         |    <marital>
         |        <single></single>
         |        <married>
         |            <partnername>Jane Doe</partnername>
         |            <from>2025-01-01</from>
         |        </married>
         |    </marital>
         |    <hobbies>
         |        <hobby>Swimming</hobby>
         |        <hobby>Cooking</hobby>
         |        <hobby>Binge watching TV series</hobby>
         |    </hobbies>
         |    <hobby>Playing guitar</hobby>
         |    <passportnumber>1234567890</passportnumber>
         |    <driverlicense expirydate="2026-12-31">abcdefghijklm</driverlicense>
         |    <disabilities>
         |        <disability>Blindness</disability>
         |        <disability>Deafness</disability>
         |    </disabilities>
         |    <disability>Blindness</disability>
         |    <benefits1>
         |        <benefit>ChildBenefit</benefit>
         |        <benefit>UniversalCredit</benefit>
         |    </benefits1>
         |    <benefits2>false</benefits2>
         |    <skills>
         |        <skill>Java</skill>
         |        <skill>Scala</skill>
         |        <skill>Python</skill>
         |    </skills>
         |    <wallet>
         |        <item>123</item>
         |        <item>John Doe</item>
         |        <item>2025-01-01</item>
         |    </wallet>
         |    <assets>
         |        <cars>Ford</cars>
         |        <boats>
         |            <boat>Brave Wave</boat>
         |            <boat>Sharky</boat>
         |        </boats>
         |        <planes>
         |            <airbus>
         |                <model>A380</model>
         |            </airbus>
         |        </planes>
         |    </assets>
         |    <books>
         |        <book>
         |            <author>Francis Scott Fitzgerald</author>
         |            <title>The Great Gatsby</title>
         |        </book>
         |        <book>
         |            <author>Harper Lee</author>
         |            <title>To Kill a Mockingbird</title>
         |        </book>
         |    </books>
         |    <bookatdesk>
         |        <author>A.A. Milne</author>
         |        <title>Winnie the Pooh</title>
         |    </bookatdesk>
         |    <hand1>Left hand</hand1>
         |    <hand2>Right hand</hand2>
         |    <status>PENDING</status>
         |    <active>yes</active>
         |</person>""".stripMargin
    )
  }

  test("write Person case class using compact writer with name transformation") {
    val entity = TestData.person
    val xml = XmlWriter.writeCompactWithNameTransformation(
      entity,
      tagNameTransformation = name => name.toUpperCase,
      attributeNameTransformation = name => name.toLowerCase,
      addXmlDeclaration = false
    )
    println(xml)

    assertEquals(
      xml,
      """<PERSON id="1234567890"><NAME>John Doe</NAME><AGE>30</AGE><STATURE>188.8</STATURE><EMAIL>john.doe@example.com</EMAIL><ADDRESS><STREET>123 &lt;Main&gt; St</STREET><CITY>&amp;Anytown</CITY><ZIPCODE>12345</ZIPCODE></ADDRESS><ADDRESSES><HOME><STREET>123 &lt;Main&gt; St</STREET><CITY>&amp;Anytown</CITY><ZIPCODE>12345</ZIPCODE></HOME><WORK><STREET>456 &lt;Main&gt; St</STREET><CITY>&amp;Anytown</CITY><ZIPCODE>12345</ZIPCODE></WORK></ADDRESSES><ISSTUDENT>false</ISSTUDENT><TAGS><TAG name="tag1&quot;">value1</TAG><TAG name="&lt;tag2&gt;">value2</TAG></TAGS><CITIZENSHIP>United Kingdom</CITIZENSHIP><CURRENT-IMMIGRATION-STATUS>permanent resident</CURRENT-IMMIGRATION-STATUS><IMMIGRATION-STATUS-VALID-UNTIL>2026-01-01</IMMIGRATION-STATUS-VALID-UNTIL><MARITAL><SINGLE></SINGLE><MARRIED><PARTNERNAME>Jane Doe</PARTNERNAME><FROM>2025-01-01</FROM></MARRIED></MARITAL><HOBBIES><HOBBY>Swimming</HOBBY><HOBBY>Cooking</HOBBY><HOBBY>Binge watching TV series</HOBBY></HOBBIES><HOBBY>Playing guitar</HOBBY><PASSPORTNUMBER>1234567890</PASSPORTNUMBER><DRIVERLICENSE expirydate="2026-12-31">abcdefghijklm</DRIVERLICENSE><DISABILITIES><DISABILITY>Blindness</DISABILITY><DISABILITY>Deafness</DISABILITY></DISABILITIES><DISABILITY>Blindness</DISABILITY><BENEFITS1><BENEFIT>ChildBenefit</BENEFIT><BENEFIT>UniversalCredit</BENEFIT></BENEFITS1><BENEFITS2>false</BENEFITS2><SKILLS><SKILL>Java</SKILL><SKILL>Scala</SKILL><SKILL>Python</SKILL></SKILLS><WALLET><ITEM>123</ITEM><ITEM>John Doe</ITEM><ITEM>2025-01-01</ITEM></WALLET><ASSETS><CARS>Ford</CARS><BOATS><BOAT>Brave Wave</BOAT><BOAT>Sharky</BOAT></BOATS><PLANES><AIRBUS><MODEL>A380</MODEL></AIRBUS></PLANES></ASSETS><BOOKS><BOOK><AUTHOR>Francis Scott Fitzgerald</AUTHOR><TITLE>The Great Gatsby</TITLE></BOOK><BOOK><AUTHOR>Harper Lee</AUTHOR><TITLE>To Kill a Mockingbird</TITLE></BOOK></BOOKS><BOOKATDESK><AUTHOR>A.A. Milne</AUTHOR><TITLE>Winnie the Pooh</TITLE></BOOKATDESK><HAND1>Left hand</HAND1><HAND2>Right hand</HAND2><STATUS>PENDING</STATUS><ACTIVE>yes</ACTIVE></PERSON>""".stripMargin
    )
  }

  test("write ExampleLargeCaseClass case class using derived writer") {
    val entity = ExampleLargeCaseClass(
      field1 = "value1",
      field2 = 1,
      field3 = 2.0,
      field4 = true,
      field5 = Some("value5"),
      field6 = 1L,
      field7 = 2.0f,
      field8 = 'c',
      field9 = 1.toShort,
      field10 = 1.toByte,
      field11 = BigDecimal(1.0),
      field12 = List(1, 2, 3),
      field13 = Map("key1" -> 1, "key2" -> 2),
      field14 = Set(1.0, 2.0, 3.0),
      field15 = Vector(true, false, true),
      field16 = Array('a', 'b', 'c'),
      field17 = Seq("value1", "value2", "value3"),
      field18 = Left("value1"),
      field19 = Some(1.0),
      field20 = ("value1", 1),
      field21 = List("value1", "value2", "value3"),
      field22 = Some(List(1.0, 2.0, 3.0)),
      field23 = Map(1 -> "value1", 2 -> "value2"),
      field24 = Set("value1", "value2", "value3"),
      field25 = Some(true),
      field26 = BigInt(1),
      field27 = Some(BigDecimal(1.0)),
      field28 = Array(1.toByte, 2.toByte, 3.toByte),
      field29 = Some('c'),
      field30 = "value30"
    )

    val writer = summon[XmlWriter[ExampleLargeCaseClass]]
    given xmlOutputBuilder: XmlOutputBuilder.IndentedXmlStringBuilder =
      XmlOutputBuilder.indented(indentation = 4, initialString = "")
    writer.write("ExampleLargeCaseClass", entity, createTag = true)
    val xml = xmlOutputBuilder.result
    println(xml)
    assertEquals(
      xml,
      """|<ExampleLargeCaseClass>
         |    <field1>value1</field1>
         |    <field2>1</field2>
         |    <field3>2.0</field3>
         |    <field4>true</field4>
         |    <field5>value5</field5>
         |    <field6>1</field6>
         |    <field7>2.0</field7>
         |    <field8>c</field8>
         |    <field9>1</field9>
         |    <field10>1</field10>
         |    <field11>1.0</field11>
         |    <field12>
         |        <Int>1</Int>
         |        <Int>2</Int>
         |        <Int>3</Int>
         |    </field12>
         |    <field13>
         |        <key1>1</key1>
         |        <key2>2</key2>
         |    </field13>
         |    <field14>
         |        <Double>1.0</Double>
         |        <Double>2.0</Double>
         |        <Double>3.0</Double>
         |    </field14>
         |    <field15>
         |        <Boolean>true</Boolean>
         |        <Boolean>false</Boolean>
         |        <Boolean>true</Boolean>
         |    </field15>
         |    <field16>
         |        <Char>a</Char>
         |        <Char>b</Char>
         |        <Char>c</Char>
         |    </field16>
         |    <field17>
         |        <String>value1</String>
         |        <String>value2</String>
         |        <String>value3</String>
         |    </field17>
         |    <field18>value1</field18>
         |    <field19>1.0</field19>
         |    <field20>
         |        <String>value1</String>
         |        <Int>1</Int>
         |    </field20>
         |    <field21>
         |        <String>value1</String>
         |        <String>value2</String>
         |        <String>value3</String>
         |    </field21>
         |    <field22>
         |        <Double>1.0</Double>
         |        <Double>2.0</Double>
         |        <Double>3.0</Double>
         |    </field22>
         |    <field23>
         |        <1>value1</1>
         |        <2>value2</2>
         |    </field23>
         |    <field24>
         |        <String>value1</String>
         |        <String>value2</String>
         |        <String>value3</String>
         |    </field24>
         |    <field25>true</field25>
         |    <field26>1</field26>
         |    <field27>1.0</field27>
         |    <field28>
         |        <Byte>1</Byte>
         |        <Byte>2</Byte>
         |        <Byte>3</Byte>
         |    </field28>
         |    <field29>c</field29>
         |    <field30>value30</field30>
         |</ExampleLargeCaseClass>""".stripMargin
    )
  }

  test("write Employee case class") {
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

    val xml: String = XmlWriter.writeIndented(employee)
    println(xml)
    assertEquals(
      xml,
      """|<?xml version='1.0' encoding='UTF-8'?>
         |<Employee>
         |    <name>Alice Smith</name>
         |    <age>29</age>
         |    <email>alice.smith@company.com</email>
         |    <address>
         |        <street>456 Market Ave</street>
         |        <city>Metropolis</city>
         |        <postcode>90210</postcode>
         |    </address>
         |    <company>
         |        <name>Acme Widgets Inc.</name>
         |        <address>
         |            <street>123 Corporate Plaza</street>
         |            <city>Metropolis</city>
         |            <postcode>90211</postcode>
         |            <country>USA</country>
         |        </address>
         |    </company>
         |</Employee>""".stripMargin
    )
  }

  test("write Library case class with XML annotations") {
    case class Tag(
        @xmlAttribute name: String,
        @xmlContent value: String
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
    assertEquals(
      xml,
      """|<?xml version='1.0' encoding='UTF-8'?>
         |<Bookshelf libraryId="lib123">
         |    <name>City Library</name>
         |    <books>
         |        <Book isbn="978-3-16-148410-0">
         |            <title>Programming Scala</title>
         |            <author>Dean Wampler</author>
         |            <tags>
         |                <Tag name="Scala">Functional</Tag>
         |                <Tag name="Programming">JVM</Tag>
         |            </tags>
         |        </Book>
         |        <Book isbn="978-1-61729-065-7">
         |            <title>Functional Programming in Scala</title>
         |            <author>Paul Chiusano</author>
         |            <tags>
         |                <Tag name="Scala">FP</Tag>
         |                <Tag name="Education">Advanced</Tag>
         |            </tags>
         |        </Book>
         |    </books>
         |</Bookshelf>""".stripMargin
    )
  }

}
