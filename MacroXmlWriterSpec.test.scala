package org.encalmo.writer.xml

import org.encalmo.writer.xml.annotation.*
import java.time.LocalDate
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class MacroXmlWriterSpec extends munit.FunSuite {

  test("write Enum") {
    val entity = Citizenship.UK
    val xml = XmlWriter.writeIndentedUsingRootTagName("citizenship", entity, addXmlDeclaration = false)
    println(xml)
    assertEquals(
      xml,
      """|<citizenship>United Kingdom</citizenship>""".stripMargin
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

  test("write Tuple 30 elements") {
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
      new java.util.Date(0L), // java.util.Date
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
         |    <x>1</x>
         |    <Array>
         |        <Int>2</Int>
         |        <Int>4</Int>
         |        <Int>6</Int>
         |    </Array>
         |    <Date>Thu Jan 01 01:00:00 GMT 1970</Date>
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
         |    <1>one</1>
         |    <2>two</2>
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
      """|<Airbus>
         |    <model>A380</model>
         |</Airbus>""".stripMargin
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
         |    <home>
         |        <street>123 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </home>
         |    <work>
         |        <street>456 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </work>
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
         |</Person>""".stripMargin
    )
  }

  test("write Person case class using compact writer") {
    val entity = TestData.person
    val xml = XmlWriter.writeCompact(entity, addXmlDeclaration = false)
    println(xml)

    assertEquals(
      xml,
      """<Person ID="1234567890"><name>John Doe</name><age>30</age><stature>188.8</stature><email>john.doe@example.com</email><address><street>123 &lt;Main&gt; St</street><city>&amp;Anytown</city><zipcode>12345</zipcode></address><home><street>123 &lt;Main&gt; St</street><city>&amp;Anytown</city><zipcode>12345</zipcode></home><work><street>456 &lt;Main&gt; St</street><city>&amp;Anytown</city><zipcode>12345</zipcode></work><isStudent>false</isStudent><tags><tag name="tag1&quot;">value1</tag><tag name="&lt;tag2&gt;">value2</tag></tags><citizenship>United Kingdom</citizenship><current-immigration-status>permanent resident</current-immigration-status><immigration-status-valid-until>2026-01-01</immigration-status-valid-until><marital><Single></Single><Married><partnerName>Jane Doe</partnerName><from>2025-01-01</from></Married></marital><hobbies><Hobby>Swimming</Hobby><Hobby>Cooking</Hobby><Hobby>Binge watching TV series</Hobby></hobbies><hobby>Playing guitar</hobby><passportNumber>1234567890</passportNumber><driverLicense expiryDate="2026-12-31">abcdefghijklm</driverLicense><disabilities><Disability>Blindness</Disability><Disability>Deafness</Disability></disabilities><disability>Blindness</disability><benefits1><Benefit>ChildBenefit</Benefit><Benefit>UniversalCredit</Benefit></benefits1><benefits2>false</benefits2><skills><skill>Java</skill><skill>Scala</skill><skill>Python</skill></skills><wallet><item>123</item><item>John Doe</item><item>2025-01-01</item></wallet><assets><Cars>Ford</Cars><Boats><boat>Brave Wave</boat><boat>Sharky</boat></Boats><Planes><Airbus><model>A380</model></Airbus></Planes></assets><books><book><author>Francis Scott Fitzgerald</author><title>The Great Gatsby</title></book><book><author>Harper Lee</author><title>To Kill a Mockingbird</title></book></books><bookAtDesk><author>A.A. Milne</author><title>Winnie the Pooh</title></bookAtDesk><hand1>Left hand</hand1><hand2>Right hand</hand2><status>PENDING</status></Person>""".stripMargin
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
         |    <home>
         |        <street>123 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </home>
         |    <work>
         |        <street>456 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </work>
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
      """<Person ID="1234567890"><name>John Doe</name><age>30</age><stature>188.8</stature><email>john.doe@example.com</email><address><street>123 &lt;Main&gt; St</street><city>&amp;Anytown</city><zipcode>12345</zipcode></address><home><street>123 &lt;Main&gt; St</street><city>&amp;Anytown</city><zipcode>12345</zipcode></home><work><street>456 &lt;Main&gt; St</street><city>&amp;Anytown</city><zipcode>12345</zipcode></work><isStudent>false</isStudent><tags><tag name="tag1&quot;">value1</tag><tag name="&lt;tag2&gt;">value2</tag></tags><citizenship>United Kingdom</citizenship><current-immigration-status>permanent resident</current-immigration-status><immigration-status-valid-until>2026-01-01</immigration-status-valid-until><marital><Single></Single><Married><partnerName>Jane Doe</partnerName><from>2025-01-01</from></Married></marital><hobbies><Hobby>Swimming</Hobby><Hobby>Cooking</Hobby><Hobby>Binge watching TV series</Hobby></hobbies><hobby>Playing guitar</hobby><passportNumber>1234567890</passportNumber><driverLicense expiryDate="2026-12-31">abcdefghijklm</driverLicense><disabilities><Disability>Blindness</Disability><Disability>Deafness</Disability></disabilities><disability>Blindness</disability><benefits1><Benefit>ChildBenefit</Benefit><Benefit>UniversalCredit</Benefit></benefits1><benefits2>false</benefits2><skills><skill>Java</skill><skill>Scala</skill><skill>Python</skill></skills><wallet><item>123</item><item>John Doe</item><item>2025-01-01</item></wallet><assets><Cars>Ford</Cars><Boats><boat>Brave Wave</boat><boat>Sharky</boat></Boats><Planes><Airbus><model>A380</model></Airbus></Planes></assets><books><book><author>Francis Scott Fitzgerald</author><title>The Great Gatsby</title></book><book><author>Harper Lee</author><title>To Kill a Mockingbird</title></book></books><bookAtDesk><author>A.A. Milne</author><title>Winnie the Pooh</title></bookAtDesk><hand1>Left hand</hand1><hand2>Right hand</hand2><status>PENDING</status></Person>""".stripMargin
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
         |    <home>
         |        <street>123 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </home>
         |    <work>
         |        <street>456 &lt;Main&gt; St</street>
         |        <city>&amp;Anytown</city>
         |        <zipcode>12345</zipcode>
         |    </work>
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
      """<PERSON id="1234567890"><NAME>John Doe</NAME><AGE>30</AGE><STATURE>188.8</STATURE><EMAIL>john.doe@example.com</EMAIL><ADDRESS><STREET>123 &lt;Main&gt; St</STREET><CITY>&amp;Anytown</CITY><ZIPCODE>12345</ZIPCODE></ADDRESS><HOME><STREET>123 &lt;Main&gt; St</STREET><CITY>&amp;Anytown</CITY><ZIPCODE>12345</ZIPCODE></HOME><WORK><STREET>456 &lt;Main&gt; St</STREET><CITY>&amp;Anytown</CITY><ZIPCODE>12345</ZIPCODE></WORK><ISSTUDENT>false</ISSTUDENT><TAGS><TAG name="tag1&quot;">value1</TAG><TAG name="&lt;tag2&gt;">value2</TAG></TAGS><CITIZENSHIP>United Kingdom</CITIZENSHIP><CURRENT-IMMIGRATION-STATUS>permanent resident</CURRENT-IMMIGRATION-STATUS><IMMIGRATION-STATUS-VALID-UNTIL>2026-01-01</IMMIGRATION-STATUS-VALID-UNTIL><MARITAL><SINGLE></SINGLE><MARRIED><PARTNERNAME>Jane Doe</PARTNERNAME><FROM>2025-01-01</FROM></MARRIED></MARITAL><HOBBIES><HOBBY>Swimming</HOBBY><HOBBY>Cooking</HOBBY><HOBBY>Binge watching TV series</HOBBY></HOBBIES><HOBBY>Playing guitar</HOBBY><PASSPORTNUMBER>1234567890</PASSPORTNUMBER><DRIVERLICENSE expirydate="2026-12-31">abcdefghijklm</DRIVERLICENSE><DISABILITIES><DISABILITY>Blindness</DISABILITY><DISABILITY>Deafness</DISABILITY></DISABILITIES><DISABILITY>Blindness</DISABILITY><BENEFITS1><BENEFIT>ChildBenefit</BENEFIT><BENEFIT>UniversalCredit</BENEFIT></BENEFITS1><BENEFITS2>false</BENEFITS2><SKILLS><SKILL>Java</SKILL><SKILL>Scala</SKILL><SKILL>Python</SKILL></SKILLS><WALLET><ITEM>123</ITEM><ITEM>John Doe</ITEM><ITEM>2025-01-01</ITEM></WALLET><ASSETS><CARS>Ford</CARS><BOATS><BOAT>Brave Wave</BOAT><BOAT>Sharky</BOAT></BOATS><PLANES><AIRBUS><MODEL>A380</MODEL></AIRBUS></PLANES></ASSETS><BOOKS><BOOK><AUTHOR>Francis Scott Fitzgerald</AUTHOR><TITLE>The Great Gatsby</TITLE></BOOK><BOOK><AUTHOR>Harper Lee</AUTHOR><TITLE>To Kill a Mockingbird</TITLE></BOOK></BOOKS><BOOKATDESK><AUTHOR>A.A. Milne</AUTHOR><TITLE>Winnie the Pooh</TITLE></BOOKATDESK><HAND1>Left hand</HAND1><HAND2>Right hand</HAND2><STATUS>PENDING</STATUS></PERSON>""".stripMargin
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
         |    <key1>1</key1>
         |    <key2>2</key2>
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
         |    <1>value1</1>
         |    <2>value2</2>
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
         |<Library libraryId="lib123">
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
         |</Library>""".stripMargin
    )
  }

}
