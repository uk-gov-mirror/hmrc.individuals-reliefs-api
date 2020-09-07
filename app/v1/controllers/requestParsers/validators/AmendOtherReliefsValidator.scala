/*
 * Copyright 2020 HM Revenue & Customs
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

package v1.controllers.requestParsers.validators

import config.FixedConfig
import v1.controllers.requestParsers.validators.validations._
import v1.models.errors._
import v1.models.request.amendOtherReliefs._

class AmendOtherReliefsValidator extends Validator[AmendOtherReliefsRawData] with FixedConfig {

  private val validationSet = List(
    parameterFormatValidation,
    parameterRuleValidation,
    bodyFormatValidation,
    incorrectOrEmptyBodySubmittedValidation,
    bodyFieldValidation
  )

  private def parameterFormatValidation: AmendOtherReliefsRawData => List[List[MtdError]] = (data: AmendOtherReliefsRawData) => {
    List(
      NinoValidation.validate(data.nino),
      TaxYearValidation.validate(data.taxYear)
    )
  }

  private def parameterRuleValidation: AmendOtherReliefsRawData => List[List[MtdError]] = (data: AmendOtherReliefsRawData) => {
    List(
      MtdTaxYearValidation.validate(data.taxYear, reliefsMinimumTaxYear)
    )
  }

  private def bodyFormatValidation: AmendOtherReliefsRawData => List[List[MtdError]] = { data =>
    List(
      JsonFormatValidation.validate[AmendOtherReliefsBody](data.body, RuleIncorrectOrEmptyBodyError)
    )
  }

  private def incorrectOrEmptyBodySubmittedValidation: AmendOtherReliefsRawData => List[List[MtdError]] = { data =>
    val body = data.body.as[AmendOtherReliefsBody]
    if (body.isIncorrectOrEmptyBody) List(List(RuleIncorrectOrEmptyBodyError)) else NoValidationErrors
  }

  private def bodyFieldValidation: AmendOtherReliefsRawData => List[List[MtdError]] = { data =>
    val body = data.body.as[AmendOtherReliefsBody]

    val errorsO: Option[List[List[MtdError]]] = for {
      nonDeductableLoanInterestErrors <- body.nonDeductableLoanInterest.map(validateNonDeductableLoanInterest)
      payrollGivingErrors <- body.payrollGiving.map(validatePayrollGiving)
      qualifyingDistributionRedemptionOfSharesAndSecuritiesErrors <- {
        body.qualifyingDistributionRedemptionOfSharesAndSecurities.map(validateQualifyingDistributionRedemptionOfSharesAndSecurities)
      }
      maintenancePaymentsErrors <- {
        body.maintenancePayments.map(_.zipWithIndex.flatMap {
          case (item, i) => validateMaintenancePayments(item, i)
        })
      }
      postCessationTradeReliefAndCertainOtherLossesErrors <- {
        body.postCessationTradeReliefAndCertainOtherLosses.map(_.zipWithIndex.flatMap {
          case (item, i) => validatePostCessationTradeReliefAndCertainOtherLosses(item, i)
        })
      }
      annualPaymentsMadeErrors <- body.annualPaymentsMade.map(validateAnnualPayments)
      qualifyingLoanInterestPayments <- {
        body.qualifyingLoanInterestPayments.map(_.zipWithIndex.flatMap {
          case (item, i) => validateQualifyingLoanInterestPayments(item, i)
        })
      }
    } yield {
      List(
        nonDeductableLoanInterestErrors,
        payrollGivingErrors,
        qualifyingDistributionRedemptionOfSharesAndSecuritiesErrors,
        maintenancePaymentsErrors,
        postCessationTradeReliefAndCertainOtherLossesErrors,
        annualPaymentsMadeErrors,
        qualifyingLoanInterestPayments
      ).map(_.toList)
    }

    List(errorsO.map(flattenErrors)).flatten
  }

  private def validateNonDeductableLoanInterest(nonDeductableLoanInterest: NonDeductableLoanInterest): List[MtdError] = {
    List(
      ReferenceRegexValidation.validateOptional(
        field = nonDeductableLoanInterest.customerReference,
        path = s"/nonDeductableLoanInterest/customerReference",
        error = CustomerReferenceFormatError
      ),
      NumberValidation.validateOptional(
        field = Some(nonDeductableLoanInterest.reliefClaimed),
        path = s"/nonDeductableLoanInterest/reliefClaimed"
      )
    ).flatten
  }

  private def validatePayrollGiving(payrollGiving: PayrollGiving): List[MtdError] = {
    List(
      ReferenceRegexValidation.validateOptional(
        field = payrollGiving.customerReference,
        path = s"/payrollGiving/customerReference",
        error = CustomerReferenceFormatError
      ),
      NumberValidation.validateOptional(
        field = Some(payrollGiving.reliefClaimed),
        path = s"/payrollGiving/reliefClaimed"
      )
    ).flatten
  }

  private def validateQualifyingDistributionRedemptionOfSharesAndSecurities
  (qualifyingDistributionRedemptionOfSharesAndSecurities: QualifyingDistributionRedemptionOfSharesAndSecurities): List[MtdError] = {
    List(
      ReferenceRegexValidation.validateOptional(
        field = qualifyingDistributionRedemptionOfSharesAndSecurities.customerReference,
        path = s"/qualifyingDistributionRedemptionOfSharesAndSecurities/customerReference",
        error = CustomerReferenceFormatError
      ),
      NumberValidation.validateOptional(
        field = Some(qualifyingDistributionRedemptionOfSharesAndSecurities.amount),
        path = s"/qualifyingDistributionRedemptionOfSharesAndSecurities/amount"
      )
    ).flatten
  }


  private def validateMaintenancePayments(maintenancePayments: MaintenancePayments, arrayIndex: Int): List[MtdError] = {
    List(
      ReferenceRegexValidation.validateOptional(
        field = maintenancePayments.customerReference,
        path = s"/maintenancePayments/$arrayIndex/customerReference",
        error = CustomerReferenceFormatError
      ),
      FieldLengthValidation.validateOptional(
        field = maintenancePayments.exSpouseName,
        path = s"/maintenancePayments/$arrayIndex/exSpouseName",
        error = ExSpouseNameFormatError
      ),
      DateValidation.validateOptional(
        date = maintenancePayments.exSpouseDateOfBirth,
        path = s"/maintenancePayments/$arrayIndex/exSpouseDateOfBirth",
        error = DateFormatError
      ),
      NumberValidation.validateOptional(
        field = Some(maintenancePayments.amount),
        path = s"/maintenancePayments/$arrayIndex/amount"
      )
    ).flatten
  }

  private def validatePostCessationTradeReliefAndCertainOtherLosses(
                                                                     postCessationTradeReliefAndCertainOtherLosses: PostCessationTradeReliefAndCertainOtherLosses,
                                                                     arrayIndex: Int
                                                                   ): List[MtdError] = {
    List(
      ReferenceRegexValidation.validateOptional(
        field = postCessationTradeReliefAndCertainOtherLosses.customerReference,
        path = s"/postCessationTradeReliefAndCertainOtherLosses/$arrayIndex/customerReference",
        error = CustomerReferenceFormatError
      ),
      FieldLengthValidation.validateOptional(
        field = postCessationTradeReliefAndCertainOtherLosses.businessName,
        path = s"/postCessationTradeReliefAndCertainOtherLosses/$arrayIndex/businessName",
        error = BusinessNameFormatError
      ),
      DateValidation.validateOptional(
        date = postCessationTradeReliefAndCertainOtherLosses.dateBusinessCeased,
        path = s"/postCessationTradeReliefAndCertainOtherLosses/$arrayIndex/dateBusinessCeased",
        error = DateFormatError
      ),
      ReferenceRegexValidation.validateOptional(
        field = postCessationTradeReliefAndCertainOtherLosses.natureOfTrade,
        path = s"/postCessationTradeReliefAndCertainOtherLosses/$arrayIndex/natureOfTrade",
        error = NatureOfTradeFormatError
      ),
      FieldLengthValidation.validateOptional(
        field = postCessationTradeReliefAndCertainOtherLosses.incomeSource,
        path = s"/postCessationTradeReliefAndCertainOtherLosses/$arrayIndex/incomeSource",
        error = IncomeSourceFormatError
      ),
      NumberValidation.validateOptional(
        field = Some(postCessationTradeReliefAndCertainOtherLosses.amount),
        path = s"/postCessationTradeReliefAndCertainOtherLosses/$arrayIndex/amount"
      )
    ).flatten
  }


  private def validateAnnualPayments(annualPaymentsMade: AnnualPaymentsMade): List[MtdError] = {
    List(
      ReferenceRegexValidation.validateOptional(
        field = annualPaymentsMade.customerReference,
        path = s"/annualPaymentsMade/customerReference",
        error = CustomerReferenceFormatError
      ),
      NumberValidation.validateOptional(
        field = Some(annualPaymentsMade.reliefClaimed),
        path = s"/annualPaymentsMade/reliefClaimed"
      )
    ).flatten
  }

  private def validateQualifyingLoanInterestPayments(qualifyingLoanInterestPayments: QualifyingLoanInterestPayments, arrayIndex: Int): List[MtdError] = {
    List(
      ReferenceRegexValidation.validateOptional(
        field = qualifyingLoanInterestPayments.customerReference,
        path = s"/qualifyingLoanInterestPayments/$arrayIndex/customerReference",
        error = CustomerReferenceFormatError
      ),
      FieldLengthValidation.validateOptional(
        field = qualifyingLoanInterestPayments.lenderName,
        path = s"/qualifyingLoanInterestPayments/$arrayIndex/lenderName",
        error = LenderNameFormatError
      ),
      NumberValidation.validateOptional(
        field = Some(qualifyingLoanInterestPayments.reliefClaimed),
        path = s"/qualifyingLoanInterestPayments/$arrayIndex/reliefClaimed"
      )
    ).flatten
  }


  override def validate(data: AmendOtherReliefsRawData): List[MtdError] = {
    run(validationSet, data).distinct
  }
}
