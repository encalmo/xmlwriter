package org.encalmo.writer.xml

import java.util.UUID
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.ZoneId

import org.encalmo.writer.xml.annotation.*
import org.encalmo.writer.xml.XmlWriter
import org.encalmo.writer.xml.XmlOutputBuilder

final case class GovTalkMessage(
    @xmlAttribute xmlns: String = "http://www.govtalk.gov.uk/CM/envelope",
    EnvelopeVersion: String = "2.0",
    Header: Header = Header(),
    GovTalkDetails: GovTalkDetails,
    Body: Body
)

final case class Header(
    MessageDetails: MessageDetails = MessageDetails(),
    SenderDetails: SenderDetails =
      SenderDetails() // This is intentionally set to be empty (<SenderDetails></SenderDetails>)
)

val gatewayTimestampFormat: DateTimeFormatter =
  DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneId.of("UTC"))

final case class MessageDetails(
    Class: String = "HMRC-CHAR-CLM",
    Qualifier: String = "request",
    Function: String = "submit",
    CorrelationID: String = UUID
      .randomUUID()
      .toString()
      .replace("-", "")
      .toUpperCase, // All messages coming into ChRIS must have this ID for tracing purposes.
    GatewayTimestamp: String =
      gatewayTimestampFormat.format(Instant.now()) // Current system datetime in format "yyyy-MM-ddTHH:mm:ss.SSS"
)

final case class SenderDetails()

final case class GovTalkDetails(
    Keys: List[Key],
    ChannelRouting: ChannelRouting = ChannelRouting()
)

// need to check
final case class Key(
    @xmlAttribute Type: String,
    @xmlContent Value: String
)

final case class ChannelRouting(
    Channel: Channel = Channel()
)

final case class Channel(
    URI: String = "9998",
    Product: String = "Charities portal",
    Version: String = "1.0"
)

final case class Body(
    IRenvelope: IRenvelope
)

final case class IRenvelope(
    @xmlAttribute xmlns: String = "http://www.govtalk.gov.uk/taxation/charities/r68/2",
    IRheader: IRheader,
    R68: R68
)

final case class IRheader(
    Keys: List[Key],
    PeriodEnd: String,
    IRmark: Option[IRmark] = None,
    Sender: String
)

final case class IRmark(
    @xmlAttribute Type: String,
    @xmlContent Content: String
)

final case class R68(
    WelshSubmission: Option[YesNo] =
      None, // If the claim has been completed in Welsh (language selector is "cym), then set to "yes" else omit this element.
    AuthOfficial: Option[AuthOfficial] =
      None, // If user has an affinity group of "Organisation", then set to AuthOfficial else omit this element.
    AgtOrNom: Option[AgtOrNom] =
      None, // If user has an affinity group of "Agent", then set to AgtOrNom else omit this element.
    Declaration: YesNo,
    Claim: Claim
)

final case class AuthOfficial(
    Trustee: Option[String] =
      None, // If "Are you a Corporate Trustee" is "Yes", then set to the value of "Name of Corporate Trustee" Else omit this element.
    OffName: Option[OffName] = None,
    ClaimNo: Option[String] = None,
    OffID: Option[OffID] =
      None, // If a "Claim Reference Number" is given, then set to this value Else omit this element.
    Phone: Option[String] = None
)

final case class OffName(
    Ttl: Option[String] =
      None, // If "Are you a Corporate Trustee" is "No", and a "Title of Authorised Official" is given, then set to this value Else omit this element.
    Fore: Option[String] =
      None, // If "Are you a Corporate Trustee" is "No", then set to the value of "First name of Authorised Official" Else omit this element.
    Sur: Option[String] =
      None // If "Are you a Corporate Trustee" is "No", then set to the value of "Last name of Authorised Official" Else omit this element.
)

final case class OffID(
    Overseas: Option[YesNo] =
      None, // If "Are you a Corporate Trustee" is "Yes", and "Is the Corporate Trustee's Address In The UK" is "No", then set to the value of "yes"
    // Else If "Are you a Corporate Trustee" is "No", and "Is the Authorised Official's Address In The UK" is "No", then set to the value of "yes"
    // Else omit this element.
    Postcode: Option[String] =
      None // If "Are you a Corporate Trustee" is "Yes", and "Is the Corporate Trustee's Address In The UK is "Yes", then set to the value of "Postcode of Corporate Trustee"
      // Else If "Are you a Corporate Trustee" is "No", and "Is the Authorised Official's Address In The UK" is "Yes", then set to the value of "Postcode of Authorised Official"
      // Else omit this element.
)

// to check
final case class AgtOrNom(
    OrgName: String,
    RefNo: String,
    ClaimNo: Option[String] =
      None, // If a "Claim Reference Number" is given, then set to this value Else omit this element.
    PayToAoN: Option[YesNo] =
      None, // If "Who should HMRC send payment to" is "Tax Agent", then set to the value of "yes" Else omit this element.
    AoNID: Option[AoNin] = None,
    Phone: String
)

// to check
final case class AoNin(
    Overseas: Option[YesNo] =
      None, // If "Is your Address In The UK" is "No", then set to the value of "yes" Else omit this element.
    Postcode: Option[String] =
      None // If "Is your Address In The UK" is "Yes", then set to the value of "Agent Postcode" Else omit this element.
)

// to check
final case class Claim(
    OrgName: String,
    HMRCref: String,
    Regulator: Option[Regulator] = None,
    Repayment: Option[Repayment] = None,
    // If "Are you claiming Gift Aid" is "Yes", and the prevOverclaimedGiftAid is > 0, then set to this value
    // If "Are you claiming Other Income" is "Yes", and the "Previously Overclaimed Other Income Amount" is > 0, then set to this value
    // Else omit this element.
    GiftAidSmallDonationsScheme: Option[GiftAidSmallDonationsScheme] = None,
    OtherInfo: Option[String] =
      None // If a "Adjustments Detail" is given, then set to this value Else omit this element.
)

// to check
final case class Regulator(
    RegName: Option[RegulatorName] = None,
    // If "Name Of Charity Regulator" is "EnglandAndWales", then set to "CCEW"
    // If "Name Of Charity Regulator" is "NorthernIreland", then set to "CCNI"
    // If "Name Of Charity Regulator" is "Scottish", then set to "OSCR"
    // Else omit this element.
    NoReg: Option[YesNo] = None, // If "Name Of Charity Regulator" is "None", then set to "yes" Else omit this element.
    RegNo: Option[String] =
      None // If a "Charity Registration Number" is given, then set to this value Else omit this element.
)

final case class Repayment(
    @xmlContent GAD: Option[List[GAD]] = None,
    // If a "Adjustments Detail" is given, then set to this value   Else omit this element.
    EarliestGAdate: String,
    @xmlContent OtherInc: Option[List[OtherInc]] = None,
    Adjustment: Option[BigDecimal] = None
)

// to check
final case class GAD(
    AggDonation: Option[String] =
      None, // If aggregatedDonations is not blank, then set to this value Else omit this element.
    Donor: Option[Donor] = None,
    Sponsored: Option[YesNo] = None, // If sponsoredEvent is "Yes", then set to value of "yes" Else omit this element.
    Date: String,
    Total: String
)

final case class Donor(
    Ttl: Option[String] =
      None, // If aggregatedDonations is blank, then set to value of donorTitle  Else omit this element.
    Fore: Option[String] =
      None, // If aggregatedDonations is blank, then set to value of donorFirstName  Else omit this element.
    Sur: Option[String] =
      None, // If aggregatedDonations is blank, then set to value of donorLastName  Else omit this element.
    House: Option[String] =
      None, // If aggregatedDonations is blank, then set to value of donorHouse  Else omit this element.
    Overseas: Option[YesNo] =
      None, // If aggregatedDonations is blank, and donorPostcode is "X", then set to "yes" Else omit this element.
    Postcode: Option[String] =
      None // If aggregatedDonations is blank, and donorPostcode is not "X", then set to value of donorPostcode Else omit this element.
)

// to check
final case class OtherInc(
    Payer: String,
    OIDate: String, // not sure if Date type is required
    Gross: BigDecimal,
    Tax: BigDecimal
)

// to check
final case class GiftAidSmallDonationsScheme(
    ConnectedCharities: YesNo,
    @xmlContent Charity: Option[List[Charity]] = None,
    @xmlContent GiftAidSmallDonationsSchemeClaim: Option[List[GiftAidSmallDonationsSchemeClaim]] = None,
    CommBldgs: Option[YesNo] = None,
    @xmlContent Building: Option[List[Building]] = None,
    Adj: Option[String]
)

final case class Charity(
    Name: String,
    HMRCref: String
)

// to check
final case class GiftAidSmallDonationsSchemeClaim(
    Year: Option[String] =
      None, // If "Donations received by organisation" is "Yes", then set to the value of "Tax Year 1"   Else omit this element.
    // If "Donations received by organisation" is "Yes", and "Do you want to claim for a second tax year" is "Yes", then set to the value of "Tax Year 2"
    // If "Donations received by organisation" is "Yes", and "Do you want to claim for a third tax year" is "Yes", then set to the value of "Tax Year 3"
    Amount: Option[BigDecimal] =
      None // If "Donations received by organisation" is "Yes", then set to the value of "Tax Year 1 Amount of Donations Received"  Else omit this element.
      // If "Donations received by organisation" is "Yes", then set to the value of "Tax Year 2 Amount of Donations Received"  Else omit this element.
      // If "Donations received by organisation" is "Yes", then set to the value of "Tax Year 2 Amount of Donations Received"  Else omit this element.
)

final case class Building(
    BldgName: String,
    Address: String,
    Postcode: String,
    @xmlContent BldgClaim: List[BldgClaim]
)

// to check
// this is option for Year 2 and Year 3 but mandatory for year 1
final case class BldgClaim(
    Year: String,
    Amount: BigDecimal
)

opaque type YesNo = Boolean

object YesNo {

  given Conversion[Boolean, YesNo] = {
    case true  => true
    case false => false
  }

  given XmlWriter[YesNo] = new XmlWriter[YesNo] {
    def label: String = "YesNo"

    def write(name: String, value: YesNo, createTag: Boolean)(using builder: XmlOutputBuilder): Unit =
      if createTag then builder.appendElementStart(name)
      builder.appendText(value match {
        case true  => "yes"
        case false => "no"
      })
      if createTag then builder.appendElementEnd(name)
  }
}

enum RegulatorName {
  case CCEW
  case CCNI
  case OSCR
  case None
}
