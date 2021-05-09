package it.unibo.pcd.akka.e04actorlifecycle

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Props, Scheduler, SpawnProtocol}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.DurationInt

object SpawningProtocol extends App {
  case object Ping; case object Pong

  val system = ActorSystem[SpawnProtocol.Command](Behaviors.setup(_ => SpawnProtocol()), "myroot")

  import akka.actor.typed.scaladsl.AskPattern._
  implicit val ec: ExecutionContext = system.executionContext
  implicit val sched: Scheduler = system.scheduler // for ask pattern
  implicit val timeout: Timeout = Timeout(3.seconds) // for ask pattern

  def pongerBehavior: Behavior[Ping.type] = Behaviors.receive[Ping.type] { (ctx, _) => { ctx.log.info("pong"); Behaviors.stopped } }
  val ponger: Future[ActorRef[Ping.type]] = system.ask(SpawnProtocol.Spawn(pongerBehavior, "ponger", Props.empty, _))
  for(pongerRef <- ponger) pongerRef ! Ping

}
