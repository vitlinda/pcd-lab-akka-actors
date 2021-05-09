package it.unibo.pcd.akka.e06interaction

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Scheduler}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Success

object HelloBehavior {
  final case class Greet(whom: String, replyTo: ActorRef[Greeted])
  final case class Greeted(whom: String, from: ActorRef[Greet])

  def apply(): Behavior[Greet] = Behaviors.receive { (context, message) =>
    context.log.info("Hello {}!", message.whom)
    message.replyTo ! Greeted(message.whom, context.self)
    Behaviors.same
  }
}

object InteractionPatternsAsk extends App {
  import HelloBehavior._

  val system = ActorSystem[Greeted](Behaviors.setup{ ctx =>
    val greeter = ctx.spawnAnonymous(HelloBehavior())
    implicit val timeout: Timeout = 2.seconds
    implicit val scheduler = ctx.system.scheduler
    val f: Future[Greeted] = greeter ? (replyTo => Greet("Bob", replyTo))
    implicit val ec = ctx.executionContext
    f.onComplete {
      case Success(Greeted(who,from)) => println(s"${who} has been greeted by ${from.path}!")
      case _ => println("No greet")
    }
    Behaviors.empty
  }, name = "hello-world")
}

object InteractionPatternsPipeToSelf extends App {
  import HelloBehavior._

  val system = ActorSystem[Greeted](Behaviors.setup{ ctx =>
    val greeter = ctx.spawn(HelloBehavior(), "greeter")
    implicit val timeout: Timeout = 2.seconds
    implicit val scheduler = ctx.system.scheduler
    val f: Future[Greeted] = greeter ? (replyTo => Greet("Bob", replyTo))
    ctx.pipeToSelf(f)(_.getOrElse(Greeted("nobody", ctx.system.ignoreRef)))
    Behaviors.receive {
      case (ctx, Greeted(whom, from)) =>
        ctx.log.info(s"${whom} has been greeted by ${from.path.name}")
        Behaviors.stopped
    }
  }, name = "hello-world")
}

object InteractionPatternsSelfMessage extends App {
  val system = ActorSystem[String](Behaviors.setup { ctx =>
    Behaviors.withTimers { timers =>
      Behaviors.receiveMessage {
        case "" => Behaviors.stopped
        case s =>
          ctx.log.info(""+s.head)
          timers.startSingleTimer(s.tail, 300.millis)
          Behaviors.same
      }
    }
  }, name = "hello-world")

  system ! "hello akka"
}