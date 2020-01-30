package com.lightbend.akka_oled

import akka.actor.{ActorRef, Props}
import akka.persistence.PersistentActor
import com.lightbend.akka_oled.ClusterShardingStatus.{Notification, TeamNotification}
import com.lightbend.akka_oled.Team.{Get, PointsAdded, PostPoints, Stop}

object Team {
  trait Message

  case class PostPoints(name: String, amount: Int) extends Message

  case object Stop extends Message

  case class Get(name: String) extends Message

  final case class PointsAdded(name: String, points: Int) extends Message

  def props(ref: ActorRef) = Props(new Team(ref))
}

class Team(ref: ActorRef) extends PersistentActor {
  override def persistenceId: String = self.path.name

  def updateState(event: PointsAdded): Unit = {
    name = name.orElse(Some(event.name))
    total += event.points
    ref ! TeamNotification(persistenceId, total)
  }


  var name: Option[String] = None
  var total: Int = 0

  override def receiveRecover: Receive = {
    case evt: PointsAdded => updateState(evt)
  }

  override def receiveCommand: Receive = {
    case PostPoints(name, amount) =>

      persist(PointsAdded(name, amount)) {
        a =>
          updateState(a)
          sender() ! "Ok\n"
      }
    case Get(name) =>
      sender() ! total
      ref ! TeamNotification(name, total)
    case Stop => context.stop(self)
  }
}

