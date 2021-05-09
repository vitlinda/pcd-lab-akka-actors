package it.unibo.pcd.akka.e01hello

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}

object Counter {
  sealed trait Command
  case object Tick extends Command

  def apply(from: Int, to: Int): Behavior[Command] = Behaviors.receive { (context: ActorContext[_], msg) => msg match {
    case Tick if from != to => {
      context.log.info(s"Count: ${from}")
      Counter(from - from.compareTo(to), to)
    }
    case _ => Behaviors.stopped
  } }

  def apply(to: Int): Behavior[Command] =
    Behaviors.setup(new Counter(_, 0, to))
}

class Counter(context: ActorContext[Counter.Command], var from: Int, val to: Int) extends AbstractBehavior[Counter.Command](context) {
  override def onMessage(msg: Counter.Command): Behavior[Counter.Command] = msg match {
    case Counter.Tick if from != to => {
      context.log.info(s"Count: ${from}")
      from -= from.compareTo(to)
      this
    }
    case _ => Behaviors.stopped
  }
}

object CounterAppFunctional extends App {
  import Counter._

  val system = ActorSystem[Command](Counter(0,2), "counter")
  for(i <- 0 to 2) system ! Tick
}

object CounterAppOOP extends App {
  val system = ActorSystem[Counter.Command](Counter(2), "counter")
  for(i <- 0 to 2) system ! Counter.Tick
}