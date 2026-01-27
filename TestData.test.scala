package org.encalmo.writer.xml

import java.time.LocalDate

object TestData {

  val address1 = Address(street = "123 <Main> St", city = "&Anytown", postcode = "12345")
  val address2 = Address(street = "456 <Main> St", city = "&Anytown", postcode = "12345")

  val person = Person(
    id = "1234567890",
    name = "John Doe",
    age = 30,
    stature = 188.8,
    email = Some("john.doe@example.com"),
    address = Some(
      Address(street = "123 <Main> St", city = "&Anytown", postcode = "12345")
    ),
    addresses = Some(
      Map(
        "home" -> address1,
        "work" -> address2
      )
    ),
    isStudent = false,
    tags = List(
      tag(name = "tag1\"", value = "value1"),
      tag(name = "<tag2>", value = "value2")
    ),
    citizenship = Citizenship.UK,
    immigrationStatus = Some(new ImmigrationStatus {
      val status = "permanent resident"
      val validUntil = LocalDate.of(2026, 1, 1)
    }),
    maritalStatus = Array(
      MaritalStatus.Single,
      MaritalStatus.Married(
        partnerName = "Jane Doe",
        from = LocalDate.of(2025, 1, 1)
      )
    ),
    hobbies = List(
      Hobby.Swimming,
      Hobby.Cooking,
      Hobby.Other(name = "Binge watching TV series")
    ),
    hobby = Hobby.Other(name = "Playing guitar"),
    passportNumber = Some(PassportNumber("1234567890")),
    driverLicense = Some(DriverLicense(number = "abcdefghijklm", expiryDate = LocalDate.of(2026, 12, 31))),
    disabilities = List(Disability("Blindness"), Disability("Deafness")),
    disability = Disability("Blindness"),
    benefits1 = List(Benefit.ChildBenefit, Benefit.UniversalCredit),
    benefits2 = false,
    skills = Skills("Java", "Scala", "Python"),
    wallet = (123, "John Doe", LocalDate.of(2025, 1, 1)),
    assets = (Cars.Ford, Boats("Brave Wave", "Sharky"), Airbus("A380")),
    books = List(
      (author = "Francis Scott Fitzgerald", title = "The Great Gatsby"),
      (author = "Harper Lee", title = "To Kill a Mockingbird")
    ),
    bookAtDesk = (author = "A.A. Milne", title = "Winnie the Pooh"),
    hand1 = Left("Left hand"),
    hand2 = Right("Right hand"),
    status = Status.PENDING,
    active = true
  )

}
