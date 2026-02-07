package org.encalmo.writer.xml

import org.encalmo.writer.xml.XmlWriter

class ChRISSubmissionSpec extends munit.FunSuite {

  test("write GovTalkMessage") {

    val govTalkMessage = GovTalkMessage(
      Header = Header(
        MessageDetails = MessageDetails(
          Class = "HMRC-CHAR-CLM",
          Qualifier = "request",
          Function = "submit",
          CorrelationID = "E5E33CCB3A0241BCB38252F2B0B6A7DC",
          GatewayTimestamp = "2025-12-04T12:53:29.124"
        ),
        SenderDetails = SenderDetails()
      ),
      GovTalkDetails = GovTalkDetails(
        Keys = List(
          Key(Type = "CredentialID", Value = "authorityOrg1"),
          Key(Type = "CHARID", Value = "XR4010"),
          Key(Type = "SessionID", Value = "session-7f2f616c-c50d-461e-93c9-57b8d60e04a6")
        ),
        ChannelRouting = ChannelRouting(
          Channel = Channel(
            URI = "9998",
            Product = "Charities portal",
            Version = "1.0"
          )
        )
      ),
      Body = Body(
        IRenvelope = IRenvelope(
          IRheader = IRheader(
            Keys = List(
              Key(Type = "CHARID", Value = "XR4010")
            ),
            PeriodEnd = "2012-01-01",
            IRmark = Some(IRmark(Type = "generic", Content = "ecYRGN8K5yfiZSK5RDXoskrwbJE=")),
            Sender = "Other"
          ),
          R68 = R68(
            AuthOfficial = Some(
              AuthOfficial(
                Trustee = Some("Test Corp"),
                ClaimNo = Some("CHR-DS-029"),
                OffID = Some(
                  OffID(
                    Postcode = Some("AB12 3YZ")
                  )
                ),
                Phone = Some("07777777777")
              )
            ),
            Declaration = true,
            Claim = Claim(
              OrgName = "CHARITY TC088",
              HMRCref = "XR4010",
              Regulator = Some(
                Regulator(
                  RegName = Some(RegulatorName.CCEW),
                  RegNo = Some("1137948")
                )
              ),
              Repayment = Some(
                Repayment(
                  GAD = Some(
                    List(
                      GAD(
                        Donor = Some(
                          Donor(
                            Ttl = Some("Prof"),
                            Fore = Some("Henry"),
                            Sur = Some("House Martin"),
                            House = Some("152A"),
                            Postcode = Some("M99 2QD")
                          )
                        ),
                        Date = "2025-03-24",
                        Total = "240.00"
                      ),
                      GAD(
                        Donor = Some(
                          Donor(
                            Ttl = Some("Mr"),
                            Fore = Some("John"),
                            Sur = Some("Smith"),
                            House = Some("100 Champs Elysees, Paris"),
                            Overseas = Some(true)
                          )
                        ),
                        Date = "2025-06-24",
                        Total = "250.00"
                      ),
                      GAD(
                        AggDonation = Some("One off Gift Aid donations"),
                        Date = "2025-03-31",
                        Total = "880.00"
                      ),
                      GAD(
                        Donor = Some(
                          Donor(
                            Ttl = Some("Miss"),
                            Fore = Some("B"),
                            Sur = Some("Chaudry"),
                            House = Some("21"),
                            Postcode = Some("L43 4FB")
                          )
                        ),
                        Sponsored = Some(true),
                        Date = "2025-04-26",
                        Total = "4000.00"
                      )
                    )
                  ),
                  OtherInc = Some(
                    List(
                      OtherInc(
                        Payer = "Joe Bloggs",
                        OIDate = "2025-01-01",
                        Gross = BigDecimal("100.00"),
                        Tax = BigDecimal("20.00")
                      ),
                      OtherInc(
                        Payer = "Imogen Smith",
                        OIDate = "2025-02-02",
                        Gross = BigDecimal("157.66"),
                        Tax = BigDecimal("31.53")
                      ),
                      OtherInc(
                        Payer = "Paul Robinson",
                        OIDate = "2025-03-03",
                        Gross = BigDecimal("45222.78"),
                        Tax = BigDecimal("9044.56")
                      )
                    )
                  ),
                  Adjustment = Some(BigDecimal("123.45")),
                  EarliestGAdate = "2025-01-01"
                )
              ),
              GiftAidSmallDonationsScheme = Some(
                GiftAidSmallDonationsScheme(
                  ConnectedCharities = true,
                  Charity = Some(
                    List(
                      Charity(Name = "Charity One", HMRCref = "X95442"),
                      Charity(Name = "Charity Two", HMRCref = "X95443"),
                      Charity(Name = "Charity Three", HMRCref = "X95444"),
                      Charity(Name = "Charity Four", HMRCref = "X95445"),
                      Charity(Name = "Charity Five", HMRCref = "X95446")
                    )
                  ),
                  GiftAidSmallDonationsSchemeClaim = Some(
                    List(
                      GiftAidSmallDonationsSchemeClaim(Year = Some("2024"), Amount = Some(BigDecimal("67.09"))),
                      GiftAidSmallDonationsSchemeClaim(Year = Some("2023"), Amount = Some(BigDecimal("460.34")))
                    )
                  ),
                  CommBldgs = Some(true),
                  Building = Some(
                    List(
                      Building(
                        BldgName = "YMCA",
                        Address = "123 New Street",
                        Postcode = "AB12 3CD",
                        BldgClaim = List(
                          BldgClaim(Year = "2024", Amount = BigDecimal("1257.21"))
                        )
                      ),
                      Building(
                        BldgName = "The Vault",
                        Address = "22 Liberty Place",
                        Postcode = "L20 3UD",
                        BldgClaim = List(
                          BldgClaim(Year = "2023", Amount = BigDecimal("1500.00")),
                          BldgClaim(Year = "2024", Amount = BigDecimal("2500.00")),
                          BldgClaim(Year = "2025", Amount = BigDecimal("2000.00"))
                        )
                      ),
                      Building(
                        BldgName = "Bootle Village Hall",
                        Address = "11A Grange Road",
                        Postcode = "L20 1KL",
                        BldgClaim = List(
                          BldgClaim(Year = "2023", Amount = BigDecimal("1750.00"))
                        )
                      )
                    )
                  ),
                  Adj = Some("56.89")
                )
              ),
              OtherInfo = Some("This is my other info about my adjustments")
            )
          )
        )
      )
    )

    val xml = XmlWriter.writeIndented(govTalkMessage)
    println(xml)

    assertEquals(
      xml,
      """|<?xml version='1.0' encoding='UTF-8'?>
         |<GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
         |    <EnvelopeVersion>2.0</EnvelopeVersion>
         |    <Header>
         |        <MessageDetails>
         |            <Class>HMRC-CHAR-CLM</Class>
         |            <Qualifier>request</Qualifier>
         |            <Function>submit</Function>
         |            <CorrelationID>E5E33CCB3A0241BCB38252F2B0B6A7DC</CorrelationID>
         |            <GatewayTimestamp>2025-12-04T12:53:29.124</GatewayTimestamp>
         |        </MessageDetails>
         |        <SenderDetails></SenderDetails>
         |    </Header>
         |    <GovTalkDetails>
         |        <Keys>
         |            <Key Type="CredentialID">authorityOrg1</Key>
         |            <Key Type="CHARID">XR4010</Key>
         |            <Key Type="SessionID">session-7f2f616c-c50d-461e-93c9-57b8d60e04a6</Key>
         |        </Keys>
         |        <ChannelRouting>
         |            <Channel>
         |                <URI>9998</URI>
         |                <Product>Charities portal</Product>
         |                <Version>1.0</Version>
         |            </Channel>
         |        </ChannelRouting>
         |    </GovTalkDetails>
         |    <Body>
         |        <IRenvelope xmlns="http://www.govtalk.gov.uk/taxation/charities/r68/2">
         |            <IRheader>
         |                <Keys>
         |                    <Key Type="CHARID">XR4010</Key>
         |                </Keys>
         |                <PeriodEnd>2012-01-01</PeriodEnd>
         |                <IRmark Type="generic">ecYRGN8K5yfiZSK5RDXoskrwbJE=</IRmark>
         |                <Sender>Other</Sender>
         |            </IRheader>
         |            <R68>
         |                <AuthOfficial>
         |                    <Trustee>Test Corp</Trustee>
         |                    <ClaimNo>CHR-DS-029</ClaimNo>
         |                    <OffID>
         |                        <Postcode>AB12 3YZ</Postcode>
         |                    </OffID>
         |                    <Phone>07777777777</Phone>
         |                </AuthOfficial>
         |                <Declaration>yes</Declaration>
         |                <Claim>
         |                    <OrgName>CHARITY TC088</OrgName>
         |                    <HMRCref>XR4010</HMRCref>
         |                    <Regulator>
         |                        <RegName>CCEW</RegName>
         |                        <RegNo>1137948</RegNo>
         |                    </Regulator>
         |                    <Repayment>
         |                        <GAD>
         |                            <Donor>
         |                                <Ttl>Prof</Ttl>
         |                                <Fore>Henry</Fore>
         |                                <Sur>House Martin</Sur>
         |                                <House>152A</House>
         |                                <Postcode>M99 2QD</Postcode>
         |                            </Donor>
         |                            <Date>2025-03-24</Date>
         |                            <Total>240.00</Total>
         |                        </GAD>
         |                        <GAD>
         |                            <Donor>
         |                                <Ttl>Mr</Ttl>
         |                                <Fore>John</Fore>
         |                                <Sur>Smith</Sur>
         |                                <House>100 Champs Elysees, Paris</House>
         |                                <Overseas>yes</Overseas>
         |                            </Donor>
         |                            <Date>2025-06-24</Date>
         |                            <Total>250.00</Total>
         |                        </GAD>
         |                        <GAD>
         |                            <AggDonation>One off Gift Aid donations</AggDonation>
         |                            <Date>2025-03-31</Date>
         |                            <Total>880.00</Total>
         |                        </GAD>
         |                        <GAD>
         |                            <Donor>
         |                                <Ttl>Miss</Ttl>
         |                                <Fore>B</Fore>
         |                                <Sur>Chaudry</Sur>
         |                                <House>21</House>
         |                                <Postcode>L43 4FB</Postcode>
         |                            </Donor>
         |                            <Sponsored>yes</Sponsored>
         |                            <Date>2025-04-26</Date>
         |                            <Total>4000.00</Total>
         |                        </GAD>
         |                        <EarliestGAdate>2025-01-01</EarliestGAdate>
         |                        <OtherInc>
         |                            <Payer>Joe Bloggs</Payer>
         |                            <OIDate>2025-01-01</OIDate>
         |                            <Gross>100.00</Gross>
         |                            <Tax>20.00</Tax>
         |                        </OtherInc>
         |                        <OtherInc>
         |                            <Payer>Imogen Smith</Payer>
         |                            <OIDate>2025-02-02</OIDate>
         |                            <Gross>157.66</Gross>
         |                            <Tax>31.53</Tax>
         |                        </OtherInc>
         |                        <OtherInc>
         |                            <Payer>Paul Robinson</Payer>
         |                            <OIDate>2025-03-03</OIDate>
         |                            <Gross>45222.78</Gross>
         |                            <Tax>9044.56</Tax>
         |                        </OtherInc>
         |                        <Adjustment>123.45</Adjustment>
         |                    </Repayment>
         |                    <GiftAidSmallDonationsScheme>
         |                        <ConnectedCharities>yes</ConnectedCharities>
         |                        <Charity>
         |                            <Name>Charity One</Name>
         |                            <HMRCref>X95442</HMRCref>
         |                        </Charity>
         |                        <Charity>
         |                            <Name>Charity Two</Name>
         |                            <HMRCref>X95443</HMRCref>
         |                        </Charity>
         |                        <Charity>
         |                            <Name>Charity Three</Name>
         |                            <HMRCref>X95444</HMRCref>
         |                        </Charity>
         |                        <Charity>
         |                            <Name>Charity Four</Name>
         |                            <HMRCref>X95445</HMRCref>
         |                        </Charity>
         |                        <Charity>
         |                            <Name>Charity Five</Name>
         |                            <HMRCref>X95446</HMRCref>
         |                        </Charity>
         |                        <GiftAidSmallDonationsSchemeClaim>
         |                            <Year>2024</Year>
         |                            <Amount>67.09</Amount>
         |                        </GiftAidSmallDonationsSchemeClaim>
         |                        <GiftAidSmallDonationsSchemeClaim>
         |                            <Year>2023</Year>
         |                            <Amount>460.34</Amount>
         |                        </GiftAidSmallDonationsSchemeClaim>
         |                        <CommBldgs>yes</CommBldgs>
         |                        <Building>
         |                            <BldgName>YMCA</BldgName>
         |                            <Address>123 New Street</Address>
         |                            <Postcode>AB12 3CD</Postcode>
         |                            <BldgClaim>
         |                                <Year>2024</Year>
         |                                <Amount>1257.21</Amount>
         |                            </BldgClaim>
         |                        </Building>
         |                        <Building>
         |                            <BldgName>The Vault</BldgName>
         |                            <Address>22 Liberty Place</Address>
         |                            <Postcode>L20 3UD</Postcode>
         |                            <BldgClaim>
         |                                <Year>2023</Year>
         |                                <Amount>1500.00</Amount>
         |                            </BldgClaim>
         |                            <BldgClaim>
         |                                <Year>2024</Year>
         |                                <Amount>2500.00</Amount>
         |                            </BldgClaim>
         |                            <BldgClaim>
         |                                <Year>2025</Year>
         |                                <Amount>2000.00</Amount>
         |                            </BldgClaim>
         |                        </Building>
         |                        <Building>
         |                            <BldgName>Bootle Village Hall</BldgName>
         |                            <Address>11A Grange Road</Address>
         |                            <Postcode>L20 1KL</Postcode>
         |                            <BldgClaim>
         |                                <Year>2023</Year>
         |                                <Amount>1750.00</Amount>
         |                            </BldgClaim>
         |                        </Building>
         |                        <Adj>56.89</Adj>
         |                    </GiftAidSmallDonationsScheme>
         |                    <OtherInfo>This is my other info about my adjustments</OtherInfo>
         |                </Claim>
         |            </R68>
         |        </IRenvelope>
         |    </Body>
         |</GovTalkMessage>""".stripMargin
    )
  }
}
