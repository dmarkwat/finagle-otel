package io.dmarkwat.twitter.finagle.otel.example

import com.google.cloud.Timestamp
import com.google.cloud.spanner._
import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.Future

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.Collections

class SimpleService(dbClient: () => DatabaseClient) extends Service[Request, Response] {

  def performRead(dbClient: DatabaseClient, ipAddr: String): ResultSet = {
    dbClient.singleUse.executeQuery(
      Statement
        .of("SELECT Access FROM AccessLog WHERE IpAddr = @ipAddr order by Access DESC limit 1")
        .toBuilder
        .bind("ipAddr")
        .to(ipAddr)
        .build()
    )
  }

  def insertUsingMutation(dbClient: DatabaseClient, ipAddr: String, time: OffsetDateTime): Unit = {
    val mutation = Mutation
      .newInsertBuilder("AccessLog")
      .set("IpAddr")
      .to(ipAddr)
      .set("Access")
      .to(time.toString)
      .build
    dbClient.write(Collections.singletonList(mutation))
  }

  def insertUsingDml(dbClient: DatabaseClient): Unit = {
    dbClient
      .readWriteTransaction()
      .run((transaction: TransactionContext) => {
        def foo(transaction: TransactionContext) = {
          val sql = "INSERT INTO AccessLog (SingerId, FirstName, LastName) " + " VALUES (10, 'Virginia', 'Watson')"
          transaction.executeUpdate(Statement.of(sql))
          null
        }

        foo(transaction)
      })
  }

  override def apply(req: Request): Future[Response] = {
    var lastOpt: Option[Timestamp] = None

    val dbClient = this.dbClient()
    val rs = performRead(dbClient, req.remoteAddress.toString)
    if (rs.next()) {
      // todo clean up
      lastOpt = Some(rs.getCurrentRowAsStruct.getTimestamp("Access"))
    }
    rs.close()

    insertUsingMutation(dbClient, req.remoteAddress.toString, OffsetDateTime.now(ZoneOffset.UTC))

    Future.value(
      Response(
        req.version,
        Status.Ok,
        Reader.fromBuf(Buf.Utf8(lastOpt.map(_.toString).getOrElse("never before")))
      )
    )
  }
}
