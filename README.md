<!---
 ____________________
|                    |
|  N  O  T  I  C  E  |
|____________________|

Please maintain this README.md as a linkable document, as other documentation may link back to it. The following sections should appear consistently in all updates to this document to maintain linkability:

## Building this Project
## CI/CD
## Maintenance

--->

# kindling

| CI Status (master) | Latest SNAPSHOT |
| :---: | :---: |
| [![Build Status][Badge-BuildPipeline]][Link-AzureMainPipeline] | [![Snapshot Artifact][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots] |

This is the core publishing code for the HL7 FHIR specification. The jar produced from this repository is used within the publisher [here][Link-PublisherProject].

## Building this Project

### Prerequisites

This project uses the [gradle build tool][Link-GradleWebpage] to build, and includes pre-build gradlew wrappers for common build tasks. 

### Build Commands

To build the project use the following:

On Mac or Linux:

```
gradlew build
```

On Windows:

```
gradlew.bat build
```

## CI/CD

This project has pipelines hosted on [Azure Pipelines][Link-AzureProject]. 

* **Pull Request Pipeline** is automatically run for every Pull Request to ensure that the project can be built via gradle. [[Azure Pipeline]][Link-AzurePullRequestPipeline] [[source]](pull-request-pipeline.yml)
* **Main Branch Pipeline** is automatically run whenever code is merged to the master branch and builds the SNAPSHOT binaries distributed to OSSRH [[Azure Pipeline]][Link-AzureMainPipeline][[source]](main-branch-pipeline.yml)

### Maintenance

Have you found an issue? Do you have a feature request? Great! Submit it [here][Link-GithubIssues] and we'll try to fix it as soon as possible.

This project is maintained by [Grahame Grieve][Link-grahameGithub], [David Otasek][Link-davidGithub] and [Mark Iantorno][Link-markGithub] on behalf of the FHIR community.

[Link-AzureProject]: https://dev.azure.com/fhir-pipelines/kindling
[Link-AzureMainPipeline]: https://dev.azure.com/fhir-pipelines/kindling/_build/latest?definitionId=41&branchName=main
[Link-AzurePullRequestPipeline]: https://dev.azure.com/fhir-pipelines/kindling/_build?definitionId=43
[Link-SonatypeSnapshots]: https://oss.sonatype.org/service/local/artifact/maven/redirect?r=snapshots&g=org.hl7.fhir&a=kindling&v=LATEST "Sonatype Snapshots"
[Link-PublisherProject]: https://github.com/HL7/fhir

[Link-GithubIssues]: https://github.com/HL7/kindling/issues

[Link-davidGithub]: https://github.com/dotasek
[Link-grahameGithub]: https://github.com/grahamegrieve
[Link-markGithub]: https://github.com/markiantorno

[Link-GradleWebpage]: https://gradle.org/

[Badge-BuildPipeline]: https://dev.azure.com/fhir-pipelines/kindling/_apis/build/status/Main%20Branch%20Pipeline?branchName=main
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/oss.sonatype.org/org.hl7.fhir/kindling.svg "Sonatype Snapshots"

