import sbtassembly.AssemblyPlugin.autoImport.assembly
import sbtassembly.MergeStrategy

def first(slices: Seq[String]*): PartialFunction[String, MergeStrategy] = {
  case PathList(path@_*) if slices.exists(path.containsSlice) => MergeStrategy.first
}

def discard(slices: Seq[String]*): PartialFunction[String, MergeStrategy] = {
  case PathList(path@_*) if slices.exists(path.containsSlice) => MergeStrategy.discard
}

def merge(slices: Seq[String]*): PartialFunction[String, MergeStrategy] = {
  case PathList(path@_*) if slices.exists(path.containsSlice) => MergeStrategy.concat
}

def default: PartialFunction[String, MergeStrategy] = {
  case _ => MergeStrategy.deduplicate
}

val producerDuplicates = Seq(
  Seq("NOTICE"),
  Seq("LICENSE"),
  Seq("module-info.class"),
  Seq("META-INF", "MANIFEST.MF"),
  Seq("META-INF", "services", "com.fasterxml.jackson.databind.Module"))

val flinkDuplicates = Seq(Seq("LICENSE"),
  Seq("LICENSE.txt"),
  Seq("NOTICE"),
  Seq("NOTICE.txt"),
  Seq("module-info.class"),
  Seq("META-INF", "DEPENDENCIES"),
  Seq("META-INF", "MANIFEST.MF"),
  Seq("META-INF", "io.netty.versions.properties"),
  Seq("META-INF", "services", "com.fasterxml.jackson.databind.Module"),
  Seq("META-INF", "services", "reactor.blockhound.integration.BlockHoundIntegration"),
  Seq("rootdoc.txt"))

val kafkaDuplicates = Seq(
  Seq("NOTICE"),
  Seq("LICENSE"),
  Seq("META-INF", "services", "com.fasterxml.jackson.databind.Module"),
  Seq("META-INF", "MANIFEST.MF"),
  Seq("module-info.class"))

val sparkFirst = Seq(
  Seq("io", "netty", "util"),
  Seq("io", "netty", "handler"),
  Seq("io", "netty", "resolver"),
  Seq("io", "netty", "channel"),
  Seq("io", "netty", "buffer"),
  Seq("io", "netty", "bootstrap"),
  Seq("org", "apache", "commons", "logging"),
  Seq("javax", "inject"),
  Seq("org", "aopalliance", "aop"),
  Seq("org", "aopalliance", "intercept"))

val sparkMerge = Seq(Seq("META-INF", "services"))

val sparkDuplicates = Seq(
  Seq("LICENSE"),
  Seq("LICENSE.md"),
  Seq("LICENSE.txt"),
  Seq("NOTICE"),
  Seq("NOTICE.txt"),
  Seq("NOTICE.md"),
  Seq("NOTICE.md"),
  Seq("NOTICE.markdown"),
  Seq("META-INF", "DEPENDENCIES"),
  Seq("META-INF", "MANIFEST.MF"),
  Seq("META-INF", "INDEX.LIST"),
  Seq("META-INF", "DUMMY.DSA"),
  Seq("META-INF", "DUMMY.SF"),
  Seq("META-INF", "maven"),
  Seq("META-INF", "io.netty.versions.properties"),
  Seq("javax", "annotation"),
  Seq("spark", "unused"),
  Seq("git.properties"),
  Seq("native-image.properties"),
  Seq("reflection-config.json"),
  Seq("module-info.class"))

ThisBuild / scalaVersion := "2.12.12"

val confJavaOption = "-Dconfig.file=src/main/resources/test.conf"

Test / parallelExecution := false

ThisBuild / libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.4.2",
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "io.github.azhur" %% "kafka-serde-play-json" % "0.6.5",
  "net.codingwell" %% "scala-guice" % "5.1.0",
  "org.specs2" %% "specs2-core" % "4.16.0" % Test,
  "org.mockito" %% "mockito-scala" % "1.17.7" % Test)

lazy val producer = (project in file("producer"))
  .settings(
    name := "producer",
    assembly / mainClass := Some("Producer"),
    assembly / assemblyOutputPath := file("jars/producer.jar"),
    assembly / assemblyMergeStrategy := discard(producerDuplicates: _*) orElse default,
    libraryDependencies += "org.apache.kafka" % "kafka-clients" % "3.2.0"
  )

val streamFile = file("stream")

lazy val kafka = (project in (streamFile / "kafka"))
  .settings(
    name := "kafka",
    assembly / mainClass := Some("KafkaConsumer"),
    assembly / assemblyOutputPath := file("jars/kafka.jar"),
    assembly / assemblyMergeStrategy := discard(kafkaDuplicates: _*) orElse default,
    Test / fork := true,
    Test / javaOptions += confJavaOption,
    libraryDependencies ++= Seq(
      "org.apache.kafka" % "kafka-clients" % "3.2.0",
      "org.apache.kafka" % "kafka-streams" % "3.2.0",
      "org.apache.kafka" %% "kafka-streams-scala" % "3.2.0",
      "org.apache.kafka" % "kafka-streams-test-utils" % "3.2.0" % Test)
  )

lazy val flink = (project in (streamFile / "flink"))
  .settings(
    name := "flink",
    assembly / mainClass := Some("FlinkConsumer"),
    assembly / assemblyOutputPath := file("jars/flink.jar"),
    assembly / assemblyMergeStrategy := discard(flinkDuplicates: _*) orElse default,
    Test / fork := true,
    Test / javaOptions += confJavaOption,
    libraryDependencies ++= Seq(
      "org.apache.flink" % "flink-clients" % "1.15.1",
      "org.apache.flink" %% "flink-streaming-scala" % "1.15.1",
      "org.apache.flink" % "flink-connector-kafka" % "1.15.1",
      "org.apache.flink" % "flink-test-utils" % "1.15.1" % Test)
  )

lazy val spark = (project in (streamFile / "spark"))
  .settings(
    name := "spark",
    assembly / mainClass := Some("SparkConsumer"),
    assembly / assemblyOutputPath := file("jars/spark.jar"),
    assembly / assemblyMergeStrategy :=
      merge(sparkMerge: _*) orElse
        first(sparkFirst: _*) orElse
        discard(sparkDuplicates: _*) orElse
        default,
    Test / fork := true,
    Test / javaOptions += confJavaOption,
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.2.1",
      "org.apache.spark" %% "spark-sql" % "3.2.1",
      "org.apache.spark" %% "spark-streaming" % "3.2.1",
      "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.2.1")
  )

lazy val streams = (project in file("."))
  .disablePlugins(AssemblyPlugin)
  .aggregate(kafka, spark, flink)

