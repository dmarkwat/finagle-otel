package io.dmarkwat.twitter.finagle.otel.example

import com.google.cloud.Timestamp
import com.google.cloud.spanner._
import com.twitter.app
import com.twitter.app.Flag
import com.twitter.finagle.http.{Request, Response, Status}
import com.twitter.finagle.tracing.TraceInitializerFilter
import com.twitter.finagle.{Http, Service, http}
import com.twitter.io.{Buf, Reader}
import com.twitter.util.logging.Logging
import com.twitter.util.{Await, Future}
import io.dmarkwat.twitter.finagle.otel.SdkBootstrap
import io.dmarkwat.twitter.finagle.tracing.otel.FinagleContextStorage.ContextExternalizer
import io.dmarkwat.twitter.finagle.tracing.otel.{ContextStorageProvider, HttpServerTraceSpanInitializer, HttpServerTracer}
import io.opentelemetry.context.Context

import java.time.{OffsetDateTime, ZoneOffset}
import scala.jdk.CollectionConverters.IterableHasAsScala

object App extends app.App with SdkBootstrap.Auto with ContextStorageProvider.WrappingContextStorage with Logging {

  val port: Flag[Int] = flag("p", 9999, "port")

  import com.google.cloud.spanner.{DatabaseClient, KeySet, Mutation, Statement}
  import com.google.common.collect.ImmutableList

  import java.util.Collections

  private val DDL_STATEMENTS = ImmutableList.of(
    "CREATE TABLE AccessLog (IpAddr String(64) NOT NULL, Access TIMESTAMP) PRIMARY KEY (IpAddr, Access)"
  )

  def createDatabase(databaseAdminClient: DatabaseAdminClient, instanceId: String, databaseId: String): Unit = {
    if (databaseExists(databaseAdminClient, instanceId, databaseId))
      databaseAdminClient.dropDatabase(instanceId, databaseId)
    databaseAdminClient.createDatabase(instanceId, databaseId, DDL_STATEMENTS)
  }

  def databaseExists(databaseAdminClient: DatabaseAdminClient, instanceId: String, databaseId: String): Boolean = {
    for (database <- databaseAdminClient.listDatabases(instanceId).iterateAll.asScala) {
      if (databaseId == database.getId.getDatabase) return true
    }
    false
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

  def performRead(dbClient: DatabaseClient, ipAddr: String): ResultSet =
    dbClient.singleUse.executeQuery(
      Statement
        .of("SELECT Access FROM AccessLog WHERE IpAddr = @ipAddr order by Access DESC limit 1")
        .toBuilder
        .bind("ipAddr")
        .to(ipAddr)
        .build()
    )

  def deleteDatabase(dbClient: DatabaseClient): Unit = {
    dbClient.write(Collections.singletonList(Mutation.delete("Singers", KeySet.all)))
    System.out.println("Records deleted.")
  }

  import com.google.cloud.spanner.{InstanceConfigId, InstanceId, InstanceInfo}

  def createTestInstance(instanceAdminClient: InstanceAdminClient, projectId: String, instanceId: String): Unit = {
    if (instanceExists(instanceAdminClient, instanceId)) instanceAdminClient.deleteInstance(instanceId)
    val instanceInfo = InstanceInfo
      .newBuilder(InstanceId.of(projectId, instanceId))
      .setInstanceConfigId(InstanceConfigId.of(projectId, "regional-us-central1"))
      .setNodeCount(1)
      .setDisplayName(instanceId)
      .build
    try instanceAdminClient.createInstance(instanceInfo).get
    catch {
      case e: Exception =>
        throw new Exception("Failed to create Spanner instance.", e)
    }
  }

  def instanceExists(instanceAdminClient: InstanceAdminClient, instanceName: String): Boolean = {
    for (instance <- instanceAdminClient.listInstances().iterateAll().asScala) {
      if (instanceName == instance.getId.getInstance) return true
    }
    false
  }

  def main(): Unit = {
    trace(Context.current())

    val options = SpannerOptions.newBuilder
      .setProjectId("test-project")
//      .setChannelProvider()
      .build
    val spanner = options.getService

    val instanceAdminClient = spanner.getInstanceAdminClient
    val databaseAdminClient = spanner.getDatabaseAdminClient

    createTestInstance(instanceAdminClient, options.getProjectId, "my-instance")
    createDatabase(databaseAdminClient, "my-instance", "my-db")

    val server = Http.server
      .withTracer(new HttpServerTracer)
      .withStack(stack =>
        stack
          .insertAfter(
            TraceInitializerFilter.role,
            ContextExternalizer.module[http.Request, http.Response]
          )
          .insertAfter(
            TraceInitializerFilter.role,
            new HttpServerTraceSpanInitializer[http.Request, http.Response](otelTracer)
          )
      )
      .serve(
        s"localhost:${port()}",
        new Service[Request, Response] {
          private val dbClient = spanner.getDatabaseClient(DatabaseId.of(options.getProjectId, "my-instance", "my-db"))

          override def apply(req: Request): Future[Response] = {
            val rs = performRead(dbClient, req.remoteAddress.toString)
            var lastOpt: Option[Timestamp] = None
            if (rs.next()) {
              Some(rs.getCurrentRowAsStruct.getTimestamp("Access"))
            }
            rs.close()

            insertUsingMutation(dbClient, req.remoteAddress.toString, OffsetDateTime.now(ZoneOffset.UTC))

            info("logged")
            Future.value(
              Response(
                req.version,
                Status.Ok,
                Reader.fromBuf(Buf.Utf8(lastOpt.map(_.toString).getOrElse("never before")))
              )
            )
          }
        }
      )

    Await.ready(server)
  }
}
