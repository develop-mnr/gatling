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
package com.thoughtworks.gatling.socket.async

import com.excilys.ebi.gatling.core.session.Session
import java.lang.System._
import akka.actor.{ActorRef, Actor}
import com.thoughtworks.gatling.socket.check.SocketCheck
import com.excilys.ebi.gatling.core.check.Check._
import com.excilys.ebi.gatling.core.check.Failure
import com.thoughtworks.gatling.socket.async.OurWebSocketListener.Messages

class AsyncSocketHandlerActor(val requestName : String, val session : Session, val next : ActorRef, val messagesToSend : List[String], val checks : List[SocketCheck]) extends Actor {
  def receive = {
    case Messages.Open(websocket) => {
      System.out.println("Socket Connected")
      if (messagesToSend == null) {
        System.out.println("error")
      }
      messagesToSend.foreach(msg => {
        websocket.sendTextMessage(msg)
        System.out.println("Sent " + msg)
      })
    }
    case Messages.Close(websocket) => {
      System.out.println("Socket Disconnected")
      executeNext(session)
    }
    case Messages.Fragment(message : String, last : Boolean) => {
      System.out.println("Message fragment received: "+message)
    }
    case Messages.Message(message : String) => {
      System.out.println("Message received: "+message)
      val (newSessionWithSavedValues, checkResult) = applyChecks(session, message, checks)

      checkResult match {
        case Failure(errorMessage) =>
          System.out.println("ERROR: check on request '"+requestName+"' failed: "+errorMessage+", response was: "+message)
          executeNext(newSessionWithSavedValues)
        case _ =>
      }
    }
    case Messages.Error(t : Throwable) => {
      System.out.println("ERROR: "+t)
    }
    case m => {
      System.out.println("I don't know what this is: "+ m)
    }
  }

  private def executeNext(newSession: Session) {
    next ! newSession.setAttribute(Session.LAST_ACTION_DURATION_KEY, currentTimeMillis) // - responseEndDate)
    context.stop(self)
  }
}