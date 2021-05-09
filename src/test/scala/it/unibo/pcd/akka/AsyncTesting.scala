package it.unibo.pcd.akka

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import it.unibo.pcd.akka.e03state.GuessGame.PlayerMessage
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AsyncTesting
  extends AnyWordSpec with Matchers with BeforeAndAfterAll {
  val testKit = ActorTestKit() // creates an actor system (one per a set of tests)

  // Shutdown the actor system after all the tests
  override def afterAll(): Unit = testKit.shutdownTestKit()

  "A testkit" must {
    "support verifying a response" in {
      import e02oopstyle._
      val pinger = testKit.spawn[PingPong](Behaviors.setup(new PingPonger(_)), "ping")
      val probe = testKit.createTestProbe[PingPong]()

      testKit.system.log.info("Sending ping")
      pinger ! Ping(probe.ref)
      probe.expectMessage(Pong(pinger.ref))

      testKit.system.log.info("Sending pong")
      pinger ! Pong(probe.ref)
      probe.expectMessage(Ping(pinger.ref))
    }

    "support mocking" in {
      import e03state._
      import GuessGame._
      val BAD_GUESS = -1
      val SECRET = 50
      val NUM_ATTEMPTS = 3

      val gameRef = testKit.spawn(game(SECRET, NUM_ATTEMPTS))
      val playerProbe = testKit.createTestProbe[PlayerMessage]()
      val playerMockBehavior = Behaviors.receiveMessage[PlayerMessage]{
        case NewInput =>
          gameRef ! Guess(BAD_GUESS, playerProbe.ref)
          Behaviors.same
        case _ => Behaviors.ignore
      }
      val playerMock = testKit.spawn(Behaviors.monitor(playerProbe.ref, playerMockBehavior))
      for(i <- 0 until NUM_ATTEMPTS - 1) {
        playerMock ! NewInput
        playerProbe.expectMessage(NewInput)
        playerProbe.expectMessage(NotGuessed(TooSmall(BAD_GUESS), NUM_ATTEMPTS - 1 - i))
      }
      playerMock ! NewInput
      playerProbe.expectMessage(NewInput)
      playerProbe.expectMessageType[Loss.type]
    }
  }
}