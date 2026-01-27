package org.encalmo.writer.xml

import org.encalmo.writer.xml.annotation.*
import java.time.LocalDate

case class Person(
    @xmlAttribute @xmlName("ID") id: String,
    name: Name,
    age: Int,
    stature: Double,
    email: Option[String],
    address: Option[Address],
    addresses: Option[Map[String, Address]],
    isStudent: Boolean,
    tags: List[tag],
    citizenship: Citizenship,
    immigrationStatus: Option[ImmigrationStatus],
    @xmlName("marital") @xmlNoItemTags maritalStatus: Array[
      MaritalStatus
    ],
    hobbies: List[Hobby],
    hobby: Hobby,
    passportNumber: Option[PassportNumber],
    driverLicense: Option[DriverLicense],
    disabilities: List[Disability],
    disability: Disability,
    benefits1: List[Benefit] | false,
    benefits2: List[Benefit] | false,
    @xmlItemTag("skill") skills: Skills,
    @xmlItemTag("item") wallet: (Int, String, LocalDate),
    assets: (Cars, Boats, Planes),
    @xmlItemTag("book") books: List[(author: String, title: String)],
    bookAtDesk: (author: String, title: String),
    hand1: Either[String, String],
    hand2: Either[String, String],
    status: Status
)

enum Citizenship {
  case UK
  case other
}

case class tag(
    @xmlAttribute name: String,
    @xmlContent value: String
)

case class Address(
    street: String,
    city: String,
    @xmlName("zipcode") postcode: String
)

trait ImmigrationStatus {
  def status: String
  def validUntil: LocalDate
}

enum MaritalStatus {
  case Single
  case CivilPartnership(partnerName: LocalDate, from: LocalDate)
  case Married(partnerName: String, from: LocalDate)
  case Divorced(from: LocalDate)
  case Widowed(from: LocalDate)
}

enum Hobby {
  case Reading
  case Swimming
  case Cycling
  case Cooking
  case Other(name: String)
}

object Hobby {
  given XmlWriter[Hobby.Other] =
    new XmlWriter[Hobby.Other] {
      def write(name: String, value: Hobby.Other, createTag: Boolean)(using
          builder: XmlOutputBuilder
      ): Unit = {
        if createTag then builder.appendElementStart("OtherHobby")
        builder.appendText(value.name)
        if createTag then builder.appendElementEnd("OtherHobby")
      }
    }
}

object ImmigrationStatus {
  given XmlWriter[ImmigrationStatus] =
    new XmlWriter[ImmigrationStatus] {
      def write(name: String, value: ImmigrationStatus, createTag: Boolean)(using
          builder: XmlOutputBuilder
      ): Unit = {
        builder.appendElementStart("current-immigration-status")
        builder.appendText(value.status)
        builder.appendElementEnd("current-immigration-status")
        builder.appendElementStart("immigration-status-valid-until")
        builder.appendText(value.validUntil.toString)
        builder.appendElementEnd("immigration-status-valid-until")
      }
    }
}

opaque type PassportNumber = String
object PassportNumber {
  def apply(value: String): PassportNumber = value
}

case class SensitiveData[T](value: T)

opaque type Disability = SensitiveData[String]
object Disability {
  def apply(value: String): Disability = SensitiveData(value)

  given XmlWriter[Disability] =
    new XmlWriter[Disability] {
      def write(name: String, value: Disability, createTag: Boolean)(using
          builder: XmlOutputBuilder
      ): Unit = {
        if createTag then builder.appendElementStart(name)
        builder.appendText(value.value)
        if createTag then builder.appendElementEnd(name)
      }
    }
}

opaque type DriverLicense <: Document = Document
object DriverLicense {
  def apply(number: String, expiryDate: LocalDate): DriverLicense = Document(number, expiryDate)
}

case class Document(
    @xmlContent number: String,
    @xmlAttribute expiryDate: LocalDate
)

opaque type Skills <: Set[String] = Set[String]
object Skills {
  def apply(values: String*): Skills = values.toSet
}

type Name = String

enum TestEnum {
  case Foo
  case Bar(name: String)
}

enum Benefit {
  case ChildBenefit
  case UniversalCredit
  case JobSeekersAllowance
  case EmploymentSupportAllowance
  case HousingBenefit
  case PensionCredit
  case Other(name: String)
}

enum Cars {
  case Ford
  case Toyota
  case Honda
  case Nissan
  case Other(name: String)
}

@xmlItemTag("boat") opaque type Boats <: Set[String] = Set[String]
object Boats {
  def apply(values: String*): Boats = values.toSet
}

sealed trait Planes
case class Boeing(model: String) extends Planes
case class Airbus(model: String) extends Planes

class Row extends Selectable {
  type Fields <: Any
}

class FactsRow extends Row {
  type Fields = (name: String, age: Int, email: String)

  transparent inline def selectDynamic(name: String): Any =
    name match {
      case "name"  => "John Doe"
      case "age"   => 30
      case "email" => "john.doe@example.com"
      case _       => throw new NoSuchElementException(s"Field $name not found")
    }
}

case class ExampleLargeCaseClass(
    field1: String,
    field2: Int,
    field3: Double,
    field4: Boolean,
    field5: Option[String],
    field6: Long,
    field7: Float,
    field8: Char,
    field9: Short,
    field10: Byte,
    field11: BigDecimal,
    field12: List[Int],
    field13: Map[String, Int],
    field14: Set[Double],
    field15: Vector[Boolean],
    field16: Array[Char],
    field17: Seq[String],
    field18: Either[String, Int],
    field19: Option[Double],
    field20: (String, Int),
    field21: List[String],
    field22: Option[List[Double]],
    field23: Map[Int, String],
    field24: Set[String],
    field25: Option[Boolean],
    field26: BigInt,
    field27: Option[BigDecimal],
    field28: Array[Byte],
    field29: Option[Char],
    field30: String
) derives XmlWriter
