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
import v1.models.request.deletePensionsReliefs.DeletePensionsReliefsRequest

import scala.concurrent.Future

class DeletePensionsReliefsConnectorSpec extends ConnectorSpec {

  val nino = Nino("AA123456A")
  val taxYear = "2019-20"


  class Test extends MockHttpClient with MockAppConfig {
    val connector: DeletePensionsReliefsConnector = new DeletePensionsReliefsConnector(http = mockHttpClient, appConfig = mockAppConfig)
    val desRequestHeaders: Seq[(String, String)] = Seq("Environment" -> "des-environment", "Authorization" -> s"Bearer des-token")
    MockedAppConfig.desBaseUrl returns baseUrl
    MockedAppConfig.desToken returns "des-token"
    MockedAppConfig.desEnv returns "des-environment"
  }

  "delete" should {
    val request = DeletePensionsReliefsRequest(nino, taxYear)

    "return a result" when {
      "the downstream call is successful" in new Test {
        val outcome = Right(ResponseWrapper(correlationId, ()))

        MockedHttpClient.
          delete(
            url = s"$baseUrl/income-tax/reliefs/pensions/${request.nino}/${request.taxYear}",
            requiredHeaders = "Environment" -> "des-environment", "Authorization" -> s"Bearer des-token"
          ).returns(Future.successful(outcome))


        await(connector.delete(request)) shouldBe outcome
      }
    }
  }
}
