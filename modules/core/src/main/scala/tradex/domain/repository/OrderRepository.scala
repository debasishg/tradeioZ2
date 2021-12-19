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
