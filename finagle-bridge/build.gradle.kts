plugins {
    id("io.dmarkwat.twitter.finagle.scala-library-conventions")
    `java-test-fixtures`
}

dependencies {
    compileOnly("com.twitter:finagle-core_2.13")
}
