plugins {
    java
    `maven-publish`
    signing
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("org.owasp.dependencycheck") version "8.4.0"
}

group = "org.hl7.fhir"
version = "1.9.2-SNAPSHOT"

java {
    withJavadocJar()
    withSourcesJar()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    google()
    mavenLocal()
    mavenCentral()
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
    maven {
        url = uri("https://repo.eclipse.org/content/groups/releases/")
    }
}

dependencies {
    implementation("ca.uhn.hapi.fhir", "hapi-fhir-base", "6.4.1")
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.utilities", property("fhirCoreVersion").toString())
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.dstu2", property("fhirCoreVersion").toString())
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.dstu2016may", property("fhirCoreVersion").toString())
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.dstu3", property("fhirCoreVersion").toString())
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.r4", property("fhirCoreVersion").toString())
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.r4b", property("fhirCoreVersion").toString())
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.r5", property("fhirCoreVersion").toString())
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.convertors", property("fhirCoreVersion").toString())
    implementation("ca.uhn.hapi.fhir", "org.hl7.fhir.validation", property("fhirCoreVersion").toString())
    implementation("org.eclipse.jgit", "org.eclipse.jgit", "6.7.0.202309050840-r")
    implementation("ch.qos.logback", "logback-classic", property("logbackVersion").toString())
    implementation("com.google.code.gson", "gson", "2.8.9")
    implementation("commons-beanutils","commons-beanutils")

    implementation("commons-codec", "commons-codec", "1.9")
    implementation("commons-discovery", "commons-discovery", "0.2")
    implementation("org.apache.httpcomponents.client5", "httpclient5", "5.3")
    implementation("commons-io", "commons-io", "2.7")
    implementation("commons-logging", "commons-logging-api", "1.1")
    implementation("commons-logging", "commons-logging", "1.1.1")
    implementation("commons-net", "commons-net", "3.9")
    implementation("org.apache.commons", "commons-compress", "1.25.0")
    implementation("org.apache.commons", "commons-exec", "1.3")
    implementation("com.trilead", "trilead-ssh2", "1.0.0-build217")
    implementation("de.regnis.q.sequence", "sequence-library", "1.0.2")
    implementation("junit", "junit", "4.13.1")
    implementation("net.java.dev.jna", "jna", "3.5.2")
    implementation("org.antlr", "antlr-runtime", "3.5.2")
    implementation("org.antlr", "ST4", "4.0.7")
    implementation("org.apache.ant", "ant", "1.10.11")
    implementation("org.apache.commons", "commons-lang3", "3.3.2")
    implementation("org.apache.commons", "commons-collections4", "4.1")
    implementation("org.apache.httpcomponents", "fluent-hc", property("apacheHttpcomponentsVersion").toString())
    implementation("org.apache.httpcomponents.client5", "httpclient5", property("apacheHttpcomponents5Version").toString())
    implementation("org.apache.httpcomponents.core5", "httpcore5", property("apacheHttpcomponents5Version").toString())
    implementation("org.apache.httpcomponents", "httpmime", property("apacheHttpcomponentsVersion").toString())
    implementation("org.apache.httpcomponents", "httpclient-cache", property("apacheHttpcomponentsVersion").toString())

    /*These dependencies are interrelated, and do not share the same version. To evaluate the appropriate versions,
    https://mvnrepository.com/ was used, and the following dependencies pattern was observed.
     org.eclipse.emf.ecore depends on org.eclipse.emf.common
     org.eclipse.emf.ecore.xmi depends on org.eclipse.emf.ecore
    */
    implementation("org.eclipse.emf", "org.eclipse.emf.ecore", "2.35.0")
    implementation("org.eclipse.emf", "org.eclipse.emf.ecore.xmi", "2.36.0")
    implementation("org.eclipse.emf", "org.eclipse.emf.common", "2.29.0")

    implementation("org.apache.poi", "poi", property("apachePoiVersion").toString())
    implementation("org.apache.poi", "poi-ooxml", property("apachePoiVersion").toString())
    implementation("org.apache.xmlbeans", "xmlbeans", "3.1.0")
    implementation("org.mozilla", "rhino", "1.7R4")
    implementation("org.hamcrest", "hamcrest-core", "1.3")
    implementation("org.jdom", "jdom2", "2.0.6.1")
    implementation("org.tmatesoft.sqljet", "sqljet", "1.1.10")
    implementation("xpp3", "xpp3", "1.1.4c")
    implementation("xpp3", "xpp3_xpath", "1.1.4c")
    implementation("org.apache.jena", "jena-core", property("apacheJenaVersion").toString())
    implementation("org.apache.jena", "jena-arq", property("apacheJenaVersion").toString())
    implementation("org.apache.jena", "jena-iri", property("apacheJenaVersion").toString())
    implementation("org.apache.jena", "jena-base", property("apacheJenaVersion").toString())
    implementation("org.apache.jena", "jena-shaded-guava", "4.8.0")
    implementation("xerces", "xercesImpl", "2.12.2")
    implementation("com.fasterxml.jackson.core", "jackson-core", property("jacksonVersion").toString())
    implementation("com.fasterxml.jackson.core", "jackson-databind", property("jacksonVersion").toString())
    implementation("com.fasterxml.jackson.core", "jackson-annotations", property("jacksonVersion").toString())
    implementation("com.github.rjeschke", "txtmark", "0.11")
    implementation("net.sf.saxon", "Saxon-HE", "9.5.1-5")
    implementation("org.slf4s", "slf4s-api_2.11", "1.7.13")
    implementation("com.typesafe.scala-logging", "scala-logging_2.12", "3.5.0")
    implementation("com.github.everit-org.json-schema", "org.everit.json.schema", "1.14.3")
    implementation("org.json", "json", "20231013")
    implementation("com.google.code.javaparser", "javaparser", "1.0.11")
    implementation("com.google.guava", "guava", "32.1.3-jre")
    implementation("com.damnhandy", "handy-uri-templates", "2.1.6")
    implementation("es.weso", "schema_2.12", "0.2.2")
    implementation("es.weso", "shacl_2.12", "0.1.83")
    implementation("es.weso", "shex_2.12", "0.2.32")
    implementation("es.weso", "srdfjena_2.12", "0.1.124")
    implementation("es.weso", "srdf_2.12", "0.1.124")
    implementation("es.weso", "utils_2.12", "0.2.25")
    implementation("es.weso", "rbe_2.12", "0.2.32")
    implementation("es.weso", "typing_2.12", "0.2.25")
    implementation("es.weso", "validating_2.12", "0.2.25")
    implementation("org.antlr", "antlr4-runtime", "4.6")
    implementation("io.circe", "circe-core_2.11", "0.7.0-M2")
    implementation("com.atlassian.commonmark", "commonmark", "0.12.1")
    implementation("com.atlassian.commonmark", "commonmark-ext-gfm-tables", "0.12.1")
    implementation("org.fhir", "ucum", "1.0.3")
    implementation("commons-cli", "commons-cli", "1.4")
    implementation("javax.servlet", "javax.servlet-api", "3.1.0")
    implementation("org.eclipse.jetty", "jetty-http", property("jettyVersion").toString())
    implementation("org.eclipse.jetty", "jetty-io", property("jettyVersion").toString())
    implementation("org.eclipse.jetty", "jetty-security", property("jettyVersion").toString())
    implementation("org.eclipse.jetty", "jetty-server", property("jettyVersion").toString())
    implementation("org.eclipse.jetty", "jetty-servlet", property("jettyVersion").toString())
    implementation("org.eclipse.jetty", "jetty-util", property("jettyVersion").toString())
    implementation("ch.qos.logback", "logback-classic", property("logbackVersion").toString())
    implementation("ch.qos.logback", "logback-core", property("logbackVersion").toString())
    implementation("org.slf4j", "slf4j-log4j12", property("slf4jVersion").toString())
    implementation("org.slf4j", "slf4j-jdk14", property("slf4jVersion").toString())
    implementation("org.slf4j", "slf4j-api", property("slf4jVersion").toString())
    implementation("org.apache.logging.log4j", "log4j", property("log4jVersion").toString())
    implementation("org.apache.logging.log4j", "log4j-core", property("log4jVersion").toString())
    implementation("com.squareup.okhttp3", "okhttp", "4.11.0")
    implementation("com.squareup.okio", "okio")
    implementation("org.jetbrains.kotlin", "kotlin-stdlib", "1.6.10")

    testImplementation("org.junit.jupiter","junit-jupiter","5.8.2")

    configurations.implementation {
        exclude(group = "xml-apis")
    }

    constraints {
        implementation("commons-beanutils:commons-beanutils:1.9.4") {
            because("previous versions have a bug impacting this application")
        }
        implementation("org.apache.jena:jena-shex:4.9.0") {
            because("previous versions have a bug impacting this application")
        }
        implementation("org.apache.solr:solr-solrj:9.4.0") {
            because("previous versions have a bug impacting this application")
        }
        implementation("org.apache.jena:jena-shacl:4.9.0") {
            because("previous versions have a bug impacting this application")
        }
        implementation("com.squareup.okio:okio:3.4.0") {
            because("previous versions have a bug impacting this application")
        }
        implementation("com.squareup.okio:okio-jvm:3.4.0") {
            because("previous versions have a bug impacting this application")
        }
        implementation("org.apache.zookeeper:zookeeper:3.9.1") {
            because("previous versions have a bug impacting this application")
        }
        implementation("org.eclipse.jetty.http2:http2-client:11.0.17") {
            because("previous versions have a bug impacting this application")
        }
        implementation("org.eclipse.jetty.http2:http2-http-client-transport:11.0.17") {
            because("previous versions have a bug impacting this application")
        }
        implementation("io.netty:netty-handler:" + property("nettyConstrainedVersion").toString()) {
            because("previous versions have a bug impacting this application")
        }
        implementation("io.netty:netty-transport-classes-epoll:" + property("nettyConstrainedVersion").toString()) {
            because("previous versions have a bug impacting this application")
        }
        implementation("io.netty:netty-transport-native-epoll:" + property("nettyConstrainedVersion").toString()) {
            because("previous versions have a bug impacting this application")
        }
    }
}

configurations {
    implementation {
        resolutionStrategy.failOnVersionConflict()
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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
    setRequired({
        gradle.taskGraph.hasTask("publish")
    })
}

tasks {
    test {
        useJUnitPlatform()
        testLogging.showExceptions = true
    }
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("kindling")
        archiveFileName.set("kindling-${project.version}.jar")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "org.hl7.fhir.tools.publisher.Publisher"))
        }
        isZip64 = true
        doLast {
            println(" group:: ${project.group}\n version:: ${project.version}")
        }

    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}

tasks.javadoc {
    if (JavaVersion.current().isJava9Compatible) {
        (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
    }
}

dependencyCheck {
    formats = arrayListOf("SARIF", "HTML")
}