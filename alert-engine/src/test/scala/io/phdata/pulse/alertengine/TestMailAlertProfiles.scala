
/*
 * Copyright 2018 phData Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.phdata.pulse.alertengine

/**
  * Mother trait/interface for Mail Alert Profiles
  */

trait TestMailAlertProfiles

/**
  * Mother object for Mail Alert Profiles
  */
object TestMailAlertProfiles {

  def apply(category: String): MailAlertProfile = {
    category match {
      case "withOneAddress" => withOneAddress
      case "withTwoAddresses" => withTwoAddresses
      case "withThreeAddresses" => withThreeAddresses
      case "withAddressMailProfile1" => withAddressMailProfile1
    }
  }

  /*
  *Factory methods
   */
  def withOneAddress: MailAlertProfile = {
    MailAlertProfile("a", List("testing@phdata.io"))
  }

  def withTwoAddresses: MailAlertProfile = {
    MailAlertProfile("b", List("testing1@phdata.io", "testing@phdata.io"))
  }

  def withThreeAddresses: MailAlertProfile = {
    MailAlertProfile("b", List("testing1@phdata.io", "testing@phdata.io", "testing@phdata.io"))
  }

  def withAddressMailProfile1: MailAlertProfile = {
    MailAlertProfile("mailprofile1", List("person@phdata.io"))
  }

}

