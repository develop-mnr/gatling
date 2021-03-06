/**
 * Copyright 2011-2012 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.excilys.ebi.gatling.core.scenario.configuration.Simulation
import com.excilys.ebi.gatling.core.Predef._
import com.excilys.ebi.gatling.http.Predef._
import com.thoughtworks.gatling.ping.Predef._

class MyFirstSimulation extends Simulation {
  def apply = {
    val scn = scenario("My first scenario")
      .exec(
        ping("Google")
          .url("www.google.com")
          .timeout(1)
      )
      .exec(
        ping("ThoughtWorks")
          .url("www.thoughtworks.com")
      )

    List(scn.configure.users(1))
  }
}