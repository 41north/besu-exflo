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
    * [How to update tests](#how-to-update-tests)
    * [Run tests](#run-tests)
    * [Formatting source](#formatting-source)

## Prerequisites

1. Java 11 or higher ([AdoptOpenJDK](https://adoptopenjdk.net/), [Zulu Community](https://www.azul.com/products/zulu-community/) or [OpenJDK](https://openjdk.java.net/))
2. [Docker](https://docs.docker.com/install/)
3. [Docker Compose](https://docs.docker.com/compose/install/)
4. [direnv](https://github.com/direnv/direnv/blob/master/docs/installation.md)
5. (Optional but highly recommendable) [IntelliJ IDEA Community or Ultimate](https://www.jetbrains.com/)

## Code organization

The codebase can be broken down as follows:

```text
├── buildSrc          # Custom gradle tasks that aids in development.
├── docker            # Custom Dockerfiles
│   ├── exflo         #   - Base Dockerfile to generate docker releases of Exflo  
│   └── flatbuffers   #   - Base Dockerfile to generate flatbuffer specific images
├── domain            # Shared domain entities across different modules (mainly related to flatbuffers)
├── gradle            # Configuration of main gradle dependencies
├── ingestion         # Core of Exflo
│   ├── base          #   - Common logic to all plugins
│   └── postgres      #   - Specific logic related to Postgres implementation
├── plugin            # Common entry point plugin definitions for Besu
├── postman           # JSON RPC Postman collection to interact with the node
├── testutil          # Test utilities and helpers related to testing Exflo
└── truffle           # Some tests require the interaction with the Blockchain, here are located Solidity tests
```

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

This `gradle` task will auto create required XMLs that are stored inside `.idea/runConfigurations` folder. If you want to customize options, you can edit the YAML [`run-configs.yaml`](../intellij/run-configs.yaml) file found on [`intellij`](../intellij) folder.

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

By default, [`ExfloPostgresPlugin`](../ingestion/postgres/src/main/kotlin/io/exflo/ingestion/postgres/ExfloPostgresPlugin.kt) will try to apply migrations automatically.

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

## How to update tests

We are using [KotlinTest](https://github.com/kotlintest/kotlintest) for verifying that everything works as intended.

Inside `testutil` module you will find a series of helper classes. Currently the majority of tests are performed on `ingestion/base` module.

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
