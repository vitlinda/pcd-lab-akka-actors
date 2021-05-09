package it.unibo.pcd.akka.e02oopstyle

import akka.actor.PoisonPill
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}

import scala.concurrent.duration.DurationInt

sealed trait PingPong
case class Pong(replyTo: ActorRef[Ping]) extends PingPong
case class Ping(replyTo: ActorRef[Pong]) extends PingPong

class PingPonger(context: ActorContext[PingPong], var bounces: Int = 10) extends AbstractBehavior(context) {
  context.log.info(s"Hello. My path is: ${context.self.path}")

  override def onMessage(msg: PingPong): Behavior[PingPong] = {
    bounces -= 1
    if(bounces < 0) {
      context.log.info("I got tired of pingpong-ing. Bye bye.")
      Behaviors.stopped
    } else {
      msg match {
        case Pong(replyTo) =>
          context.log.info("Pong")
          context.scheduleOnce(1.second, replyTo, Ping(context.self))
        case Ping(replyTo) =>
          context.log.info("Ping")
          context.scheduleOnce(1.second, replyTo, Pong(context.self))
      }
      this
    }
  }
}

object PingPongMainSimple extends App {
  val system = ActorSystem[PingPong](Behaviors.setup(new PingPonger(_)), "ping-pong")
  system ! Ping(system)
}

/**
 * Concepts:
 * - actor hierarchy
 * - watching children for termination (through signals)
 */
object PingPongMain extends App {
  val system = ActorSystem[PingPong](Behaviors.setup { ctx =>
    // Child actor creation
    val pingponger = ctx.spawn(Behaviors.setup[PingPong](ctx => new PingPonger(ctx, 5)), "ping-ponger")
    // Watching child
    ctx.watch(pingponger)
    Behaviors.receiveMessage[PingPong](msg => {
      pingponger ! msg
      Behaviors.same
    }).receiveSignal {
      case (ctx, t@Terminated(_)) => {
        ctx.log.info("PingPonger terminated. Shutting down")
        Behaviors.stopped // Or Behaviors.same to continue
      }
    }
  }, "ping-pong")
  system.log.info(s"System root path: ${system.path.root}")
  system.log.info(s"Top-level user guardian path: ${system.path}")
  system ! Ping(system)
}