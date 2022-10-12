package io.dmarkwat.twitter.finagle.otel.example

import com.google.cloud.spanner._
import com.twitter.app.Flag

import scala.jdk.CollectionConverters.IterableHasAsScala

trait BaseApp {
  self: com.twitter.app.App =>

  val port: Flag[Int] = self.flag("p", 9999, "port")

  import com.google.cloud.spanner.{DatabaseClient, KeySet, Mutation}
  import com.google.common.collect.ImmutableList

  import java.util.Collections

  private val DDL_STATEMENTS = ImmutableList.of(
    "CREATE TABLE AccessLog (IpAddr String(64) NOT NULL, Access TIMESTAMP) PRIMARY KEY (IpAddr, Access)"
  )

  lazy val options: SpannerOptions = SpannerOptions.newBuilder
    .setProjectId("test-project")
    //      .setChannelProvider()
    .build
  lazy val spanner: Spanner = options.getService

  lazy val instanceAdminClient: InstanceAdminClient = spanner.getInstanceAdminClient
  lazy val databaseAdminClient: DatabaseAdminClient = spanner.getDatabaseAdminClient

  def init(): Unit = {
    createTestInstance(instanceAdminClient, options.getProjectId, "my-instance")
    createDatabase(databaseAdminClient, "my-instance", "my-db")
  }

  def mkDbClient(): DatabaseClient =
    spanner.getDatabaseClient(DatabaseId.of(options.getProjectId, "my-instance", "my-db"))

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
}
