package tradex.domain
package repository.inmemory

import zio._
import zio.prelude.NonEmptyList
import java.time._
import model.order._
import repository.OrderRepository

final case class OrderRepositoryInMemory(state: Ref[Map[OrderNo, Order]]) extends OrderRepository {
  def queryByOrderNo(no: OrderNo): Task[Option[Order]] = state.get.map(_.get(no))

  def queryByOrderDate(date: LocalDate): Task[List[Order]] = state.get.map(_.values.filter(_.date == date).toList)

  def store(ord: Order): Task[Order] = state.update(m => m + ((ord.no, ord))).map(_ => ord)

  def store(orders: NonEmptyList[Order]): Task[Unit] = state.update(m => m ++ (orders.map(order => (order.no, order))))
}

object OrderRepositoryInMemory {
  val layer: ULayer[OrderRepository] =
    Ref.make(Map.empty[OrderNo, Order]).map(r => OrderRepositoryInMemory(r)).toLayer
}
