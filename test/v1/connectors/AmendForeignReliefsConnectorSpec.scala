/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package v1.connectors

import mocks.MockAppConfig
import uk.gov.hmrc.domain.Nino
import v1.mocks.MockHttpClient
import v1.models.outcomes.ResponseWrapper
import v1.models.request.amendForeignReliefs._

import scala.concurrent.Future

class AmendForeignReliefsConnectorSpec extends ConnectorSpec {

  val taxYear = "2017-18"
  val nino = Nino("AA123456A")
  val amount: BigDecimal = 1234.56
  val body = AmendForeignReliefsBody(
    foreignTaxCreditRelief = Some(ForeignTaxCreditRelief(
      amount = amount
    )),
    foreignIncomeTaxCreditRelief = Some(Seq(ForeignIncomeTaxCreditRelief(
      countryCode = "FRA",
      foreignTaxPaid = Some(amount),
      taxableAmount = amount,
      employmentLumpSum = true
    ))),
    foreignTaxForFtcrNotClaimed = Some(ForeignTaxForFtcrNotClaimed(
      amount = amount
    ))
  )

  class Test extends MockHttpClient with MockAppConfig {
    val connector: AmendForeignReliefsConnector = new AmendForeignReliefsConnector(http = mockHttpClient, appConfig = mockAppConfig)

    MockedAppConfig.ifsBaseUrl returns baseUrl
    MockedAppConfig.ifsToken returns "ifs-token"
    MockedAppConfig.ifsEnv returns "ifs-environment"
  }

  "doConnector" must {
    val request = AmendForeignReliefsRequest(nino, taxYear, body)

    "put a body and return 204 no body" in new Test {
      val outcome = Right(ResponseWrapper(correlationId, ()))
      MockedHttpClient
        .put(
          url = s"$baseUrl/income-tax/reliefs/foreign/$nino/$taxYear",
          body = body,
          requiredHeaders = "Environment" -> "ifs-environment", "Authorization" -> s"Bearer ifs-token"
        )
        .returns(Future.successful(outcome))

      await(connector.amend(request)) shouldBe outcome
    }
  }
}
