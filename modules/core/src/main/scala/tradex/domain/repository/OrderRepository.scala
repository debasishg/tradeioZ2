package tradex.domain
package repository

import java.time.LocalDate
import zio._
import zio.prelude.NonEmptyList
import model.order._

trait OrderRepository {

  /** query by unique key order no, account number and date */
  def queryByOrderNo(no: OrderNo): Task[Option[Order]]

  /** query by order date */
  def queryByOrderDate(date: LocalDate): Task[List[Order]]

  /** store */
  def store(ord: Order): Task[Order]

  /** store many orders */
  def store(orders: NonEmptyList[Order]): Task[Unit]
}

object OrderRepository {
  def queryByOrderNo(no: OrderNo): RIO[OrderRepository, Option[Order]] =
    ZIO.serviceWithZIO(_.queryByOrderNo(no))

  def queryByOrderDate(date: LocalDate): RIO[OrderRepository, List[Order]] =
    ZIO.serviceWithZIO(_.queryByOrderDate(date))

  def store(ord: Order): RIO[OrderRepository, Order] =
    ZIO.serviceWithZIO(_.store(ord))

  def store(orders: NonEmptyList[Order]): RIO[OrderRepository, Unit] =
    ZIO.serviceWithZIO(_.store(orders))
}
