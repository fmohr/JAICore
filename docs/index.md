---
layout: main
---
[![Build Status](https://travis-ci.com/starlibs/AILibs.svg?branch=dev)](https://travis-ci.com/starlibs/AILibs)
[![Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=starlibs.ailibs&metric=alert_status)](https://sonarcloud.io/dashboard/index/starlibs.ailibs)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=starlibs.ailibs&metric=coverage)](https://sonarcloud.io/component_measures?id=starlibs.ailibs&metric=coverage&view=list)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ai.libs/jaicore-basic/badge.svg)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A"ai.libs)
[![Javadocs](https://javadoc.io/badge/ai.libs/jaicore-basic.svg)](https://javadoc.io/doc/ai.libs/jaicore-basic)


# AILibs
AILibs is a collection of Java libraries related to automated decision making. It currently consists of two building blocks. It is also home of the [current version](https://github.com/starlibs/AILibs/tree/master/softwareconfiguration/mlplan) of the AutoML-tool [ML-Plan](https://starlibs.github.io/AILibs/projects/mlplan/).

* **JAICore** (Java AI Core) is a collection of projects with basic general purpose AI algorithms mainly in the area of logic reasoning, heuristic search, and machine learning
* **softwareconfiguration** is a collection of projects related to automatically configuring software systems. Here we also maintain the code for our AutoML flagship **[ML-Plan](https://starlibs.github.io/AILibs/projects/mlplan)**

## Using AILibs in your project
You can resolve each of our projects via a Maven dependency (using Maven central as repository).
### Maven
```
<dependency>
  <groupId>ai.libs</groupId>
  <artifactId>jaicore-ml</artifactId>
  <version>{{ page.version }}</version>
</dependency>
```

### Gradle 
```gradle
dependencies {
    implementation 'ai.libs:jaicore-ml:{{ page.version }}'
}
```

## Setting up your IDE to work with AILibs
### Eclipse
Navigate to the folder where you cloned this repository and run
```
  ./gradlew eclipse
```
This automatically creates the eclipse project files and configures the dependencies among the projects.
Then open Eclipse and go to the import menu, e.g., in the package manager. Choose to import *Existing Projects into Workspace*, select the folder where you cloned the repository, and make sure to check the *Search for nested projects* option.


## AILibs JavaDoc API

### JAICore

* [JAICore:jaicore-algorithminspector](https://javadoc.io/doc/ai.libs/jaicore-algorithminspector/)
* [JAICore:jaicore-basic](https://javadoc.io/doc/ai.libs/jaicore-basic/)
* [JAICore:jaicore-ea](https://javadoc.io/doc/ai.libs/jaicore-ea/)
* [JAICore:jaicore-experiments](https://javadoc.io/doc/ai.libs/jaicore-experiments/)
* [JAICore:jaicore-logic](https://javadoc.io/doc/ai.libs/jaicore-logic/)
* [JAICore:jaicore-math](https://javadoc.io/doc/ai.libs/jaicore-math/)
* [JAICore:jaicore-ml](https://javadoc.io/doc/ai.libs/jaicore-ml/)
* [JAICore:jaicore-planning](https://javadoc.io/doc/ai.libs/jaicore-planning/)
* [JAICore:jaicore-processes](https://javadoc.io/doc/ai.libs/jaicore-processes/)
* [JAICore:jaicore-search](https://javadoc.io/doc/ai.libs/jaicore-search/)

### Software Configuration

* [HASCO](https://javadoc.io/doc/ai.libs/hasco/)
* [ML-Plan](https://javadoc.io/doc/ai.libs/mlplan/)


## Troubleshooting

### Maven dependency resolvement problems

In some cases, maven is not able to import referenced dependencies on repositories different from the central Maven repositories, resulting in a build failure. 
To solve this problem, one might add the following repositories to the ```pom.xml``` to be able to properly execute ```maven compile``` or similar:

```
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
    <repository>
        <id>nexus.cs.upb</id>
        <url>https://nexus.cs.upb.de/repository/maven-releases/</url>
    </repository>
</repositories>
```
