package it.unibo.pcd.akka.basics.e05blockingissue

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector, LogOptions}
import org.slf4j.event.Level

import scala.concurrent.{ExecutionContext, Future}

object Actors {
  def blockingActor(): Behavior[Int] = Behaviors.receive((ctx,i) => {
    Thread.sleep(5000)
    ctx.log.info(s"Blocking op finished: $i")
    Behaviors.same
  })

  def nonblockingActorBad(): Behavior[Int] = Behaviors.receive((ctx, i) => {
    implicit val executionContext: ExecutionContext = ctx.executionContext // issue (!)
    ctx.log.info(s"Handling op: ${i}")
    Future {
      // ctx.log.info(s"Truly starting ${i}") // NB: THIS WOULD BLOCK (ctx.log is not thread-safe: see docs)
      print(s"Starting op: $i.\n")
      Thread.sleep(5000)
      print(s"Blocking op finished: $i\n")
    }
    ctx.log.info(s"Done handling ${i}")
    Behaviors.same
  })

  def nonblockingActor(): Behavior[Int] = Behaviors.receive((ctx,i) => {
    implicit val executionContext: ExecutionContext =
      ctx.system.dispatchers.lookup(DispatcherSelector.fromConfig("my-blocking-dispatcher"))
    ctx.log.info(s"Handling op: ${i}")
    Future {
      // ctx.log.info(s"Truly starting ${i}") // NB: THIS WOULD BLOCK (ctx.log is not thread-safe: see docs)
      print(s"Starting op: $i.\n")
      Thread.sleep(5000)
      print(s"Blocking op finished: $i.\n")
    }
    ctx.log.info(s"Done handling ${i}")
    Behaviors.same
  })

  def printActor(): Behavior[Int] = Behaviors.receive((ctx,i) => {
    ctx.log.info(s"Just printing: $i")
    Behaviors.same
  })

  def startSystem(b: () => Behavior[Int]): Unit = {
    val system = ActorSystem[Int](Behaviors.setup(ctx => {
      for(i <- 1 to 20) {
        ctx.log.info(s"Spawned actors for $i")
        ctx.spawn(b(), s"blocking-actor-${i}") ! i
        ctx.spawn(Actors.printActor(), s"print-actor-${i}") ! i
      }
      Behaviors.empty
    }), "myroot")
  }
}

object BlockingIssue extends App {
  Actors.startSystem(Actors.blockingActor)
}

object BlockingIssueNotSolved extends App {
  Actors.startSystem(Actors.nonblockingActorBad)
}

object BlockingIssueSolved extends App {
  Actors.startSystem(Actors.nonblockingActor)
}
