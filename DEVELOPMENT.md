# Development Guide

## Table of Contents

* [Development Guide](#development-guide)
    * [Prerequisites](#prerequisites)
    * [Checkout source code](#checkout-source-code)
    * [How code is organized](#how-code-is-organized)
    * [See what tasks are available](#see-what-tasks-are-available)
    * [Generating Intellij's Run Configurations](#generating-intellijs-run-configurations)
    * [Updating FlatBuffers Java entities](#updating-flatbuffers-java-entities)
    * [Updating Web3 Contract Wrappers](#updating-web3-contract-wrappers)
    * [Processing using Postgres](#processing-using-postgres)
       * [Performing Migrations](#performing-migrations)
       * [Updating Jooq Entities](#updating-jooq-entities)
    * [Processing into Kafka](#processing-into-kafka)
    * [How to update tests](#how-to-update-tests)
    * [Run tests](#run-tests)
    * [Formatting source](#formatting-source)

## Prerequisites

1. Java 11+ ([AdoptOpenJDK](https://adoptopenjdk.net/), [Zulu Community](https://www.azul.com/products/zulu-community/) or [OpenJDK](https://openjdk.java.net/))
2. [docker](https://docs.docker.com/install/)
3. [docker-compose](https://docs.docker.com/compose/install/)
4. (Optional): [IntelliJ IDEA Community or Ultimate](https://www.jetbrains.com/)

## Checkout source code

```sh
$ git clone git@github.com:41north/exflo.git
```

## How code is organized

The codebase can be broken down as follows:

- [`buildSrc`](buildSrc) - This folder includes `gradle` [custom tasks and plugins](https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources) that aids in development.
  - There are two custom made plugins focused on compiling [`flatbuffers`](buildSrc/src/main/kotlin/io/exflo/gradle/plugins/flatbuffers) and [`solidity`](buildSrc/src/main/kotlin/io/exflo/gradle/plugins/solidity) source using `docker`.
  - Several minor tasks to generate [`Kotlin` code from `Solidity's ABI`](buildSrc/src/main/kotlin/io/exflo/gradle/tasks/Web3KtCodeGenTask.kt) or for [generating Intellij's Run configurations](#generating-intellij's-run-configurations).
- [`docker`](docker) - Folder to store custom Dockerfiles with images.
- [`domain`](domain) - As the name implies, this submodule stores information related to `flatbuffer` entities for those plugins that needs a compact form of serialization (see [How to generate Flatbuffer Java entities](#updating-flatbuffers-java-entities) section).
- [`ingestion`](ingestion) - This folder is the main core of Exflo. It's composed of:
  - [`base`](ingestion/base) - Includes all the important logic for [`tracer`](ingestion/base/src/main/kotlin/io/exflo/ingestion/tracer), the [`chain tracker`](ingestion/base/src/main/kotlin/io/exflo/ingestion/tracker), [`token detector`](ingestion/base/src/main/kotlin/io/exflo/ingestion/tokens) code and base implementations for Exflo plugins.
  - [`postgres`](ingestion/postgres) - All relevant postgres implementation lies here (see [Processing using Postgres](#processing-using-postgres) section).
  - [`kafka`](ingestion/kafka) - All relevant kafka implementation lies here (see [Processing using Kafka](#processing-using-kafka) section).
- [`truffle`](truffle) - Basic tests are performed on [Truffle](https://www.trufflesuite.com/) and later exported to a private besu chain by `testutil/` utilities (see [How to update tests](#how-to-update-tests) section).
- [`testutil`](testutil) - Test utilities related to testing Exflo.
- [`intellij`](intellij) - Definitions for generating Run configuration tasks for Intellij 

## See what tasks are available

To see all of the `gradle` tasks that are available:

```
$ cd exflo
$ ./gradlew tasks
```

## Generating Intellij's Run Configurations

If you plan to use the IDE shortcuts (which we strongly recommend), after cloning the repository just issue the following on the terminal:

```sh
$ ./gradlew generateIntellijRunConfigs
```

This `gradle` task will auto create required XMLs that are stored inside `.idea/runConfigurations` folder. If you want to customize options, you can edit the YAML [`run-configs.yaml`](intellij/run-configs.yaml) file found on [`intellij`](intellij) folder.

## Updating FlatBuffers Java entities

If you want to update the `flatbuffers` entities found on `domain` submodule:

```sh
$ ./gradlew :domain:compileFlatBuffers
```

It will place updated entities on `domain/src/generated/java` folder.

## Updating Web3 Contract Wrappers

If you want to update the `flatbuffers` entities, just write the following:

```sh
$ ./gradlew generateContractWrappers
```

## Processing using Postgres

Type the following on your terminal in order to start `docker` with `postgres`:

```sh
$ docker-compose -f docker-compose.exflo-postgres.yml up -d
```

Alternatively you can do the same by running the Run configuration named `DOCKER | Postgres`.

After a couple of seconds `postgres` will be running on your machine, to verify, just navigate to [`http://localhost:8082/`](http://localhost:8082/) on your browser and `pgweb` should welcome you with an empty database.

The final step is to execute the Run configuration named `ROPSTEN | Postgres | Run` for `Ropsten` network or `DEV | Postgres | Run` for a private `dev` network.

### Performing Migrations

By default, [`ExfloPostgresPlugin`](ingestion/postgres/src/main/kotlin/io/exflo/ingestion/postgres/ExfloPostgresPlugin.kt) will try to apply migrations automatically.

If for whatever reason you want to run the migrations yourself you can use the following:

```sh
$ ./gradlew flywayMigrate
```

To drop all data and structure in the DB you can use:

```sh
$ ./gradlew flywayClean
```

We have added two Run configurations as shortcuts as well:

- `GRADLE | Postgres > FlywayMigrate`
- `GRADLE | Postgres > FlywayClean`

### Updating Jooq Entities

If you have added db migrations and applied them, you will need to regenerate the `jooq` entities for interacting with the db. In order to do so, first ensure that:

1. Postgres is running.
2. Migrations have been applied correctly.

In the terminal:

```sh
$ ./gradlew jooq-codegen-primary
```

We have added another shortcut named `GRADLE | Postgres > JooqCodeGen`.

## Processing into Kafka

The steps are performed in the same way as have been describing before for Postgres but with small variations:

```sh
$ docker-compose -f docker-compose.exflo-kafka.yml up -d
```

Alternatively you can do the same by running the Run configuration named `DOCKER | Kafka`.

After a couple of seconds, `kafka` will be running on your machine, to verify, you can check the logs using the following command:

```sh
$ docker-compose -f docker-compose.exflo-kafka.yml logs
```

The final step is to execute the Run configuration named `ROPSTEN | Kafka | Run`.

## How to update tests

We are using [KotlinTest](https://github.com/kotlintest/kotlintest) for verifying that everything works as intended.

Inside `testutil` module you will find a series of helper classes. Currently the majority of tests are performed on `ingestion/base` module (although we will cover `postgres` and `kafka` modules as well).

We are using an exported [`test.blocks`](testutil/src/main/resources/test.blocks) blockchain that the testing framework ingests in order to perform tests. That way we can combine some tests that involves the usage of Solidity code (like testing for Tokens). If you want to update some tests on `truffle/`:

```sh
$ cd truffle/
$ bin/capture-test-blocks
```

This will auto-generate the required [`test.blocks`](testutil/src/main/resources/test.blocks) and [`test-report.json`](testutil/src/main/resources/test-report.json) files.

## Run tests

All the unit tests are run as part of the build, but can be explicitly triggered with:

```sh
$ ./gradlew test
```

## Formatting source

To format the code you can run:

```sh
$ ./gradlew ktlintFormat
```