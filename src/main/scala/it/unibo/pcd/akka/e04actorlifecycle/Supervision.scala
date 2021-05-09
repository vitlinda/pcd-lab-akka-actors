package it.unibo.pcd.akka.e04actorlifecycle

import akka.actor.typed.{ActorSystem, Behavior, SupervisorStrategy, Terminated}
import akka.actor.typed.scaladsl.Behaviors

object SupervisedActor {
  def apply(supervisorStrategy: SupervisorStrategy): Behavior[String] = Behaviors.supervise[String]{ actor("") }
    .onFailure[RuntimeException](supervisorStrategy)

  def actor(prefix: String): Behavior[String] = Behaviors.receive {
    case (ctx, "fail") => throw new RuntimeException("Just fail")
    case (ctx, "quit") => ctx.log.info("Quitting")
      Behaviors.stopped
    case (ctx, s) => ctx.log.info(s"Got ${prefix+s}")
      actor(prefix + s)
  }
}

object SupervisionExampleRestart extends App {
  val system = ActorSystem[String](SupervisedActor(SupervisorStrategy.restart), "supervision")
  for(cmd <- List("foo","bar","fail","!!!","fail","quit")) system ! cmd
}

object SupervisionExampleResume extends App {
  val system = ActorSystem[String](SupervisedActor(SupervisorStrategy.resume), "supervision")
  for(cmd <- List("foo","bar","fail","!!!","fail","quit")) system ! cmd
}

object SupervisionExampleStop extends App {
  val system = ActorSystem[String](SupervisedActor(SupervisorStrategy.stop), "supervision")
  for(cmd <- List("foo","bar","fail","!!!","fail","quit")) system ! cmd
}

object SupervisionExampleParent extends App {
  val system = ActorSystem[String](Behaviors.setup { ctx =>
    val child = ctx.spawn(SupervisedActor(SupervisorStrategy.stop), "fallibleChild")
    Behaviors.receiveMessage { msg =>
      child ! msg
      Behaviors.same
    }
  }, "supervision")
  for(cmd <- List("foo","bar","fail","!!!","fail","quit")) system ! cmd
}

object SupervisionExampleParentWatching extends App {
  val system = ActorSystem[String](Behaviors.setup { ctx =>
    val child = ctx.spawn(SupervisedActor(SupervisorStrategy.stop), "fallibleChild")
    ctx.watch(child) // watching child (if Terminated not handled => dead pact)
    Behaviors.receiveMessage[String] { msg =>
      child ! msg
      Behaviors.same
    }
  }, "supervision")
  for(cmd <- List("foo","bar","fail","!!!","fail","quit")) system ! cmd
}

object SupervisionExampleParentWatchingHandled extends App {
  val system = ActorSystem[String](Behaviors.setup { ctx =>
    val child = ctx.spawn(SupervisedActor(SupervisorStrategy.stop), "fallibleChild")
    ctx.watch(child)
    Behaviors.receiveMessage[String] { msg =>
      child ! msg
      Behaviors.same
    }.receiveSignal {
      case (ctx, Terminated(ref)) =>
        ctx.log.info(s"Child ${ref.path} terminated")
        Behaviors.ignore
    }
  }, "supervision")
  for(cmd <- List("foo","bar","fail","!!!","fail","quit")) system ! cmd
}