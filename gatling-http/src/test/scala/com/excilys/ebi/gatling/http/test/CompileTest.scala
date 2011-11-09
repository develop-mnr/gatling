package com.excilys.ebi.gatling.http.test

import com.excilys.ebi.gatling.core.action.builder.SimpleActionBuilder._
import com.excilys.ebi.gatling.core.feeder._
import com.excilys.ebi.gatling.core.context._
import com.excilys.ebi.gatling.core.context.handler.CounterBasedIterationHandler._
import com.excilys.ebi.gatling.core.context.handler.TimerBasedIterationHandler._
import com.excilys.ebi.gatling.core.util.StringHelper._
import com.excilys.ebi.gatling.core.runner.Runner._
import com.excilys.ebi.gatling.core.structure.ScenarioBuilder._
import com.excilys.ebi.gatling.core.structure.ChainBuilder._
import com.excilys.ebi.gatling.http.action.HttpRequestActionBuilder._
import com.excilys.ebi.gatling.http.config.HttpProtocolConfigurationBuilder._
import com.excilys.ebi.gatling.http.check.body.HttpBodyRegExpCheckBuilder._
import com.excilys.ebi.gatling.http.check.body.HttpBodyXPathCheckBuilder._
import com.excilys.ebi.gatling.http.check.header.HttpHeaderCheckBuilder._
import com.excilys.ebi.gatling.http.check.status.HttpStatusCheckBuilder._
import org.jboss.netty.handler.codec.http.HttpHeaders.Names._
import java.util.concurrent.TimeUnit
import org.joda.time.DateTime

object CompileTest {

	def runSimulations = runSim(DateTime.now)_

	val iterations = 10
	val pause1 = 1
	val pause2 = 2
	val pause3 = 3

	val baseUrl = "http://localhost:3000"

	val httpConf = httpConfig.baseURL(baseUrl)

	val usersInformation = new TSVFeeder("user_information", List("login", "password", "firstname", "lastname"))

	val loginChain = chain.exec(http("First Request Chain").get("/")).pause(1, 2)

	val loginGroup = "Login"
	val doStuffGroup = "Do Stuff"

	val testData = new TSVFeeder("test-data", List("omg", "socool"))

	val lambdaUser = scenario("Standard User")
		.insertChain(loginChain)
		// First request outside iteration
		.loop(chain.exec(http("Catégorie Poney").get("/").queryParam("omg").queryParam("socool").capture(xpath("//input[@id='text1']/@value") in "aaaa_value").feeder(testData)))
		.times(2)
		.pause(pause2, pause3)
		// Loop
		.loop(
			// What will be repeated ?
			chain
				// First request to be repeated
				.exec((c: Context) => println("iterate: " + getCounterValue(c, "titi")))
				.exec(
					http("Page accueil").get("http://localhost:3000")
						.check(
							xpathExists(interpolate("//input[@value='{}']/@id", "aaaa_value")) in "ctxParam",
							xpathNotExists(interpolate("//input[@id='{}']/@value", "aaaa_value")),
							regexpExists("""<input id="text1" type="text" value="aaaa" />"""),
							regexpNotExists("""<input id="text1" type="test" value="aaaa" />"""),
							statusInRange(200 to 210) in "blablaParam",
							xpathNotEquals("//input[@value='aaaa']/@id", "omg"),
							xpathEquals("//input[@id='text1']/@value", "aaaa") in "test2"))
				.loop(chain
					.exec(http("In During 1").get("http://localhost:3000/aaaa"))
					.pause(2)
					.loop(chain.exec((c: Context) => println("--nested loop: " + getCounterValue(c, "tutu")))).counterName("tutu").times(2)
					.exec((c: Context) => println("-loopDuring: " + getCounterValue(c, "toto")))
					.exec(http("In During 2").get("/"))
					.pause(2))
				.counterName("toto").during(12000, TimeUnit.MILLISECONDS)
				.pause(pause2)
				.loop(
					chain
						.exec(http("In During 1").get("/"))
						.pause(2)
						.exec((c: Context) => println("-iterate1: " + getCounterValue(c, "titi") + ", doFor: " + getCounterValue(c, "hehe")))
						.loop(
							chain
								.exec((c: Context) => println("--iterate1: " + getCounterValue(c, "titi") + ", doFor: " + getCounterValue(c, "hehe") + ", iterate2: " + getCounterValue(c, "hoho"))))
						.counterName("hoho").times(2)
						.exec(http("In During 2").get("/"))
						.pause(2))
				.counterName("hehe").during(12000, TimeUnit.MILLISECONDS)
				.startGroup(loginGroup)
				.exec((c: Context) => c.setAttribute("test2", "bbbb"))
				.doIf("test2", "aaaa",
					chain.exec(http("IF=TRUE Request").get("/")), chain.exec(http("IF=FALSE Request").get("/")))
				.pause(pause2)
				.exec(http("Url from context").get("/aaaa"))
				.pause(1000, 3000, TimeUnit.MILLISECONDS)
				// Second request to be repeated
				.exec(http("Create Thing blabla").post("/things").queryParam("login").queryParam("password").withTemplateBody("create_thing", Map("name" -> "blabla")).asJSON)
				.pause(pause1)
				.endGroup(loginGroup)
				// Third request to be repeated
				.exec(http("Liste Articles") get ("/things") queryParam "firstname" queryParam "lastname")
				.pause(pause1)
				.exec(http("Test Page") get ("/tests") check (headerEquals(CONTENT_TYPE, "text/html; charset=utf-8") in "ctxParam"))
				// Fourth request to be repeated
				.exec(http("Create Thing omgomg")
					.post("/things").queryParam("postTest", FromContext("ctxParam")).withTemplateBody("create_thing", Map("name" -> FromContext("ctxParam"))).asJSON
					.check(status(201) in "status"))).counterName("titi").times(iterations)
		// Second request outside iteration
		.startGroup(doStuffGroup)
		.exec(http("Ajout au panier") get ("/") capture (regexp("""<input id="text1" type="text" value="(.*)" />""") in "input"))
		.pause(pause1)
		.endGroup(doStuffGroup)

	runSimulations(
		lambdaUser.configure.users(5).ramp(10).feeder(usersInformation).protocolConfig(httpConf))
}