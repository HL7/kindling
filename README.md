# kindling

| CI Status (master) | Latest SNAPSHOT |
| :---: | :---: |
| [![Build Status][Badge-BuildPipeline]][Link-BuildPipeline] | [![Snapshot Artifact][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots] |

This is the core publishing code for the HL7 FHIR specification. The jar produced from this repository is used within the publisher [here][Link-PublisherProject].

## Building this Project

This project uses the [gradle build tool][Link-GradleWebpage] to build, and includes pre-build gradlew wrappers for common build tasks. 

### To build:

On Mac or Linux:

```
gradlew build
```

On Windows:

```
gradlew.bat build
```

### Maintenance
This project is maintained by [Grahame Grieve][Link-grahameGithub] and [Mark Iantorno][Link-markGithub] on behalf of the FHIR community.

[Link-AzureProject]: https://dev.azure.com/fhir-pipelines/kindling
[Link-BuildPipeline]: https://dev.azure.com/fhir-pipelines/kindling/_build/latest?definitionId=41&branchName=main
[Link-SonatypeSnapshots]: https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=org.hl7.fhir&a=kindling&v=LATEST "Sonatype Snapshots"
[Link-PublisherProject]: https://github.com/HL7/fhir
[Link-grahameGithub]: https://github.com/grahamegrieve
[Link-markGithub]: https://github.com/markiantorno

[Link-GradleWebpage]: https://gradle.org/

[Badge-BuildPipeline]: https://dev.azure.com/fhir-pipelines/kindling/_apis/build/status/Main%20Branch%20Pipeline?branchName=main
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/org.hl7.fhir/kindling.svg "Sonatype Snapshots"

