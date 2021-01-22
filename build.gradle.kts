plugins {
    java
    `java-library`
    `maven-publish`
    signing
}

group = "org.hl7.fhir.tools.core"
version = "1.0-SNAPSHOT"

java {
    withJavadocJar()
    withSourcesJar()
}

repositories {
    google()
    jcenter()
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://dl.bintray.com/labra/maven")
    }
    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
    maven {
        url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
    }
}

dependencies {
    implementation("ca.uhn.hapi.fhir", "hapi-fhir-base", "4.0.0")
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.utilities", "5.2.18-SNAPSHOT")
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.dstu2", "5.2.18-SNAPSHOT")
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.dstu2016may", "5.2.18-SNAPSHOT")
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.dstu3", "5.2.18-SNAPSHOT")
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.r4", "5.2.18-SNAPSHOT")
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.r5", "5.2.18-SNAPSHOT")
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.convertors", "5.2.18-SNAPSHOT")
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.validation", "5.2.18-SNAPSHOT")
    implementation("ch.qos.logback", "logback-classic", "1.2.3")
    implementation("com.google.code.gson", "gson", "2.8.5")
    implementation("commons-codec", "commons-codec", "1.9")
    implementation("commons-discovery", "commons-discovery", "0.2")
    implementation("commons-httpclient", "commons-httpclient", "3.0.1")
    implementation("commons-io", "commons-io", "1.2")
    implementation("commons-logging", "commons-logging-api", "1.1")
    implementation("commons-logging", "commons-logging", "1.1.1")
    implementation("commons-net", "commons-net", "3.6")
    implementation("org.apache.commons", "commons-compress", "1.16.1")
    implementation("org.apache.commons", "commons-exec", "1.3")
    implementation("com.trilead", "trilead-ssh2", "1.0.0-build217")
    implementation("de.regnis.q.sequence", "sequence-library", "1.0.2")
    implementation("junit", "junit", "4.11")
    implementation("net.java.dev.jna", "jna", "3.5.2")
    implementation("org.antlr", "antlr-runtime", "3.5.2")
    implementation("org.antlr", "ST4", "4.0.7")
    implementation("org.apache.ant", "ant", "1.10.3")
    implementation("org.apache.commons", "commons-lang3", "3.3.2")
    implementation("org.apache.commons", "commons-collections4", "4.1")
    implementation("org.apache.httpcomponents", "fluent-hc", "4.2.3")
    implementation("org.apache.httpcomponents", "httpclient", "4.2.3")
    implementation("org.apache.httpcomponents", "httpcore", "4.2.2")
    implementation("org.apache.httpcomponents", "httpmime", "4.2.3")
    implementation("org.apache.httpcomponents", "httpclient-cache", "4.2.3")
    implementation("org.eclipse.emf", "org.eclipse.emf.ecore", "2.9.2-v20131212-0545")
    implementation("org.eclipse.emf", "org.eclipse.emf.ecore.xmi", "2.9.1-v20131212-0545")
    implementation("org.eclipse.emf", "org.eclipse.emf.common", "2.9.2-v20131212-0545")
    implementation("org.apache.poi", "poi", "4.0.1")
    implementation("org.apache.poi", "poi-ooxml", "4.0.1")
    implementation("org.apache.poi", "poi-ooxml-schemas", "4.0.1")
    implementation("org.apache.xmlbeans", "xmlbeans", "3.1.0")
    implementation("org.mozilla", "rhino", "1.7R4")
    implementation("org.hamcrest", "hamcrest-core", "1.3")
    implementation("org.jdom", "jdom", "1.1.3")
    implementation("org.tmatesoft.sqljet", "sqljet", "1.1.10")
    implementation("xpp3", "xpp3", "1.1.4c")
    implementation("xpp3", "xpp3_xpath", "1.1.4c")
    implementation("org.apache.jena", "jena-core", "3.1.0")
    implementation("org.apache.jena", "jena-arq", "3.1.0")
    implementation("org.apache.jena", "jena-iri", "3.1.0")
    implementation("org.apache.jena", "jena-base", "3.1.0")
    implementation("org.apache.jena", "jena-shaded-guava", "3.1.0")
    implementation("xerces", "xercesImpl", "2.11.0")
    implementation("xml-apis", "xml-apis", "1.4.01")
    implementation("com.fasterxml.jackson.core", "jackson-core", "2.5.2")
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.5.2")
    implementation("com.fasterxml.jackson.core", "jackson-annotations", "2.5.2")
    implementation("com.github.rjeschke", "txtmark", "0.11")
    implementation("net.sf.saxon", "Saxon-HE", "9.5.1-5")
    implementation("org.slf4s", "slf4s-api_2.11", "1.7.13")
    implementation("com.typesafe.scala-logging", "scala-logging_2.12", "3.5.0")
    implementation("com.github.everit-org.json-schema", "org.everit.json.schema", "1.9.1")
    implementation("org.json", "json", "20160212")
    implementation("com.github.jsonld-java", "jsonld-java", "0.8.3")
    implementation("com.google.code.javaparser", "javaparser", "1.0.11")
    implementation("com.github.jsonld-java", "jsonld-java", "0.9.0")
    implementation("com.github.jsonld-java", "jsonld-java-jena", "0.4.1")
    implementation("com.google.guava", "guava", "23.6-jre")
    implementation("org.json", "json", "20171018")
    implementation("com.damnhandy", "handy-uri-templates", "2.1.6")
    implementation("es.weso", "schema_2.12", "0.0.60")
    implementation("es.weso", "shacl_2.12", "0.0.60")
    implementation("es.weso", "shex_2.12", "0.0.60")
    implementation("es.weso", "manifest_2.12", "0.0.60")
    implementation("es.weso", "srdfjena_2.12", "0.0.60")
    implementation("es.weso", "srdf_2.12", "0.0.60")
    implementation("es.weso", "utils_2.12", "0.0.60")
    implementation("es.weso", "converter_2.12", "0.0.60")
    implementation("es.weso", "rbe_2.12", "0.0.60")
    implementation("es.weso", "typing_2.12", "0.0.60")
    implementation("es.weso", "validating_2.12", "0.0.60")
    implementation("es.weso", "server_2.12", "0.0.60")
    implementation("org.scalactic", "scalactic_2.12", "3.0.1")
    implementation("org.scalatest", "scalatest_2.12", "3.0.1")
    implementation("com.typesafe.scala-logging", "scala-logging_2.12", "3.5.0")
    implementation("org.rogach", "scallop_2.12", "2.0.6")
    implementation("org.typelevel", "cats-core_2.12", "0.9.0")
    implementation("org.typelevel", "cats-kernel_2.12", "0.9.0")
    implementation("org.antlr", "antlr4-runtime", "4.6")
    implementation("io.circe", "circe-core_2.11", "0.7.0-M2")
    implementation("com.atlassian.commonmark", "commonmark", "0.12.1")
    implementation("com.atlassian.commonmark", "commonmark-ext-gfm-tables", "0.12.1")
    implementation("org.fhir", "ucum", "1.0.3")
    implementation("commons-cli", "commons-cli", "1.4")
    implementation("javax.servlet", "javax.servlet-api", "3.1.0")
    implementation("org.eclipse.jetty", "jetty-http", "9.4.5.v20170502")
    implementation("org.eclipse.jetty", "jetty-io", "9.4.5.v20170502")
    implementation("org.eclipse.jetty", "jetty-security", "9.4.5.v20170502")
    implementation("org.eclipse.jetty", "jetty-server", "9.4.5.v20170502")
    implementation("org.eclipse.jetty", "jetty-servlet", "9.4.5.v20170502")
    implementation("org.eclipse.jetty", "jetty-util", "9.4.5.v20170502")
    implementation("ch.qos.logback", "logback-classic", "1.1.8")
    implementation("ch.qos.logback", "logback-core", "1.1.8")
    implementation("org.slf4j", "slf4j-log4j12", "1.7.22")
    implementation("org.slf4j", "slf4j-jdk14", "1.7.22")
    implementation("org.slf4j", "slf4j-api", "1.7.22")
    implementation("org.apache.logging.log4j", "log4j", "2.11.1")
    implementation("com.squareup.okhttp3", "okhttp", "4.9.0")
    implementation("com.squareup.okio", "okio", "2.9.0")
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.4.21")

    testImplementation("junit", "junit", "4.12")
}

configurations {
    implementation {
        resolutionStrategy.failOnVersionConflict()
    }
}

sourceSets {
    main {
        java.srcDir("src/main/java")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    test {
        testLogging.showExceptions = true
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "kindling"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("kindling")
                description.set("FHIR Core Publisher")
                url.set("http://hl7.org/fhir/")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("markiantorno")
                        name.set("Mark Iantorno")
                        email.set("markiantorno@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/HL7/kindling.git")
                    developerConnection.set("scm:git:ssh://github.com/HL7/kindling.git")
                    url.set("https://github.com/HL7/kindling.git")
                }
            }
        }
    }
    repositories {
        maven {
            credentials {
                val nexusUsername: String? by project
                val nexusPassword: String? by project
                username = nexusUsername
                password = nexusPassword
            }
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
        }
    }
}

signing {
    useGpgCmd()
    sign(configurations.archives.get())
    sign(publishing.publications["mavenJava"])
    setRequired({
        gradle.taskGraph.hasTask("publish")
    })
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}
