import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}
import scalanative.sbtplugin.ScalaNativePluginInternal.NativeTest
import scala.xml.{Node => XmlNode, NodeSeq => XmlNodeSeq, _}
import scala.xml.transform.{RewriteRule, RuleTransformer}

lazy val versionSuffix = Def.setting {
  val sv = scalaVersion.value
  val isDottyNightly = isDotty.value && sv.length > 10 // // "0.xx.0-bin".length
  if (isDottyNightly) "-dotty" + sv.substring(10)
  else ""
}
lazy val dottyMinorV = Def.setting {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((0, n)) => Some(n)
    case _            => None
  }
}

val sharedSettings = Seq(
  name := "scalacheck-1.14",
  organization := (
    if(dottyMinorV.value.exists(_ > 22)) "com.sandinh"
    else "org.scalatestplus"),
  version := "3.1.1.1" + versionSuffix.value,
  scmInfo := Some(ScmInfo(
    url("https://github.com/scalatest/scalatestplus-scalacheck"),
    "scm:git@github.com:scalatest/scalatestplus-scalacheck.git"
  )),
  homepage := Some(url("https://github.com/scalatest/scalatestplus-scalacheck")),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  developers := List(
    Developer(
      "bvenners",
      "Bill Venners",
      "bill@artima.com",
      url("https://github.com/bvenners")
    ),
    Developer(
      "cheeseng",
      "Chua Chee Seng",
      "cheeseng@amaseng.com",
      url("https://github.com/cheeseng")
    )
  ),
  resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
  libraryDependencies ++= Seq(
    ( if(dottyMinorV.value.exists(_ > 22)) "com.sandinh"
      else "org.scalatest") %%% "scalatest" % ("3.1.1" + versionSuffix.value),
    ("org.scalacheck" %%% "scalacheck" % "1.14.3").withDottyCompat(scalaVersion.value)
  ),
  scalacOptions += "-language:implicitConversions",
  // skip dependency elements with a scope
  pomPostProcess := { (node: XmlNode) =>
    new RuleTransformer(new RewriteRule {
      override def transform(node: XmlNode): XmlNodeSeq = node match {
        case e: Elem if e.label == "dependency"
            && e.child.exists(child => child.label == "scope") =>
          def txt(label: String): String = "\"" + e.child.filter(_.label == label).flatMap(_.text).mkString + "\""
          Comment(s""" scoped dependency ${txt("groupId")} % ${txt("artifactId")} % ${txt("version")} % ${txt("scope")} has been omitted """)
        case _ => node
      }
    }).transform(node).head
  }, 
  sourceGenerators in Compile += {
    Def.task {
      GenScalaCheckGen.genMain((sourceManaged in Compile).value / "org" / "scalatest" / "check", version.value, scalaVersion.value)
    }
  },
  sourceGenerators in Test += {
    Def.task {
      GenScalaCheckGen.genTest((sourceManaged in Test).value / "org" / "scalatest" / "check", version.value, scalaVersion.value)
    }
  },
  publishTo := sonatypePublishToBundle.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
)

lazy val scalatestPlusScalaCheck =
  // select supported platforms
  crossProject(JSPlatform, JVMPlatform, NativePlatform)
    .crossType(CrossType.Pure) // [Pure, Full, Dummy], default: CrossType.Full
    .settings(sharedSettings)
    .enablePlugins(SbtOsgi)
    .settings(osgiSettings: _*).settings(
      libraryDependencies ++= Seq(
        "org.scalacheck" %%% "scalacheck" % "1.14.3"
      ).map(_.withDottyCompat(scalaVersion.value)),
      OsgiKeys.exportPackage := Seq(
        "org.scalatestplus.scalacheck.*"
      ),
      OsgiKeys.importPackage := Seq(
        "org.scalatest.*",
        "org.scalactic.*", 
        "scala.*;version=\"$<range;[==,=+);$<replace;"+scalaBinaryVersion.value+";-;.>>\"",
        "*;resolution:=optional"
      ),
      OsgiKeys.additionalHeaders:= Map(
        "Bundle-Name" -> "ScalaTestPlusScalaCheck",
        "Bundle-Description" -> "ScalaTest+ScalaCheck is an open-source integration library between ScalaTest and ScalaCheck for Scala projects.",
        "Bundle-DocURL" -> "http://www.scalacheck.org/",
        "Bundle-Vendor" -> "Artima, Inc."
      )
    )
    .jsSettings(
      crossScalaVersions := List("2.10.7", "2.11.12", "2.12.10", "2.13.1"),
      sourceGenerators in Compile += {
        Def.task {
          GenResourcesJSVM.genResources((sourceManaged in Compile).value / "org" / "scalatestplus" / "scalacheck", version.value, scalaVersion.value) ++
          GenResourcesJSVM.genFailureMessages((sourceManaged in Compile).value / "org" / "scalatestplus" / "scalacheck", version.value, scalaVersion.value)
        }
      }
    )
    .jvmSettings(
      crossScalaVersions := List("2.10.7", "2.11.12", "2.12.10", "2.13.1", "0.23.0-RC1", "0.24.0-bin-20200408-4cc224b-NIGHTLY"),
      sourceGenerators in Compile += {
        Def.task {
          GenResourcesJVM.genResources((sourceManaged in Compile).value / "org" / "scalatestplus" / "scalacheck", version.value, scalaVersion.value) ++
          GenResourcesJVM.genFailureMessages((sourceManaged in Compile).value / "org" / "scalatestplus" / "scalacheck", version.value, scalaVersion.value)
        }
      }
    )
    .nativeSettings(
      scalaVersion := "2.11.12", 
      nativeLinkStubs in NativeTest := true, 
      sourceGenerators in Compile += {
        Def.task {
          GenResourcesJSVM.genResources((sourceManaged in Compile).value / "org" / "scalatestplus" / "scalacheck", version.value, scalaVersion.value) ++
          GenResourcesJSVM.genFailureMessages((sourceManaged in Compile).value / "org" / "scalatestplus" / "scalacheck", version.value, scalaVersion.value)
        }
      }
    )

lazy val scalatestPlusScalaCheckJS     = scalatestPlusScalaCheck.js
lazy val scalatestPlusScalaCheckJVM    = scalatestPlusScalaCheck.jvm
lazy val scalatestPlusScalaCheckNative = scalatestPlusScalaCheck.native
