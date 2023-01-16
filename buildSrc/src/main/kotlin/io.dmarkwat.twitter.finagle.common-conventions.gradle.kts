val otelVersion by extra("1.22.0")
val otelAlphaVersion by extra("${otelVersion}-alpha")
val otelJavaagentVersion by extra("1.22.1")
val otelJavaagentAlphaVersion by extra("${otelJavaagentVersion}-alpha")

val finagleVersion by extra("22.12.0")
val scalaVersion by extra(System.getenv("SCALA_VERSION") ?: "2.13.10")
val scalaMinor by extra(Regex("""^([0-9]+\.[0-9]+)\.?.*$""").find(scalaVersion)!!.run {
    val (minorVersion) = this.destructured
    minorVersion
})

val scalified by extra(fun(pack: String): String {
    return "${pack}_$scalaMinor"
})
