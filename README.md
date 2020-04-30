<h1 align="center">Exflo</h1>

<p align="center">
  <a href="https://github.com/41north/exflo/workflows/KtLint/badge.svg">
    <img alt="KtLint" height="20px" src="https://github.com/41north/exflo/workflows/KtLint/badge.svg" />
  </a>
  <a href="https://img.shields.io/github/license/41north/exflo?style=flat-square">
    <img alt="Exflo License" height="20px" src="https://img.shields.io/github/license/41north/exflo?style=flat-square" />
  </a>
</p>

<p align="center">
    A plugin for the <a href="http://besu.hyperledger.org/en/stable/">Besu</a> enterprise-grade Ethereum client with the aim of making it easier to extract chain data into a variety of different data stores and processing pipelines. <br/> Written with ❤️ in <a href="https://kotlinlang.org">Kotlin</a>.
</p>

## 🗒️ Features 

**⚠️ Warning**: This project is in alpha stage, and we are working actively!

Exflo can extract the following information from a Besu archive instance into either a [Postgres](https://www.postgresql.org/) database or a [Kafka](https://kafka.apache.org/) topic:

- Block headers.
- Transactions.
- Transaction traces.
- Log events for standards-compliant [ERC20](https://eips.ethereum.org/EIPS/eip-20), [ERC721](https://eips.ethereum.org/EIPS/eip-721), [ERC777](https://eips.ethereum.org/EIPS/eip-777) and [ERC1155](https://eips.ethereum.org/EIPS/eip-1155) tokens.
- Detailed breakdown of Ether movements e.g. block rewards, tx fees, simple ether transfers and so on.
- Contract creations and self destructs.
- Per block account state changes.

## Installation

If you want to run Exflo, the approach is the same as other Besu plugins. Assuming you're using docker:

1. Go to [releases](https://github.com/41North/exflo/releases) and download the `tar` or `zip` file.
2. Extract the `tar` or the `zip` file.
3. [Besu docker image](https://hub.docker.com/r/hyperledger/besu) exposes a `/etc/besu/plugins` folder where it loads the jars.
4. Select which plugin you want to run (kafka or postgres).
5. If you want to tweak default params, we recommend you to take a look on the [usage section](#usage).

Here's an example of a possible `docker` configuration using `docker-compose` syntax:

```yaml
besu:
  image: hyperledger/besu:1.4.4
  volumes:
    - ./path/exflo-{kafka, postgres}-jar/:/etc/besu/plugins
  command: "--plugin-exflo-kafka-start-block-override=23 --plugin-exflo-kafka-max-fork-size=512"
```

## Usage

Each plugin exposes a set of command line options with sane defaults and tries to autoconfigure itself as much as possible. 
In the case of the `kafka` plugin it creates associated topics by default or for the `postgres` one it deploys automatically migrations if 
it detects that the DB is not initialized.

### Kafka

Possible command line arguments for `kafka` are described below:

| Flag                                                   |                                              Description                                              |                                      Default | Optional |
| ------------------------------------------------------ | :---------------------------------------------------------------------------------------------------: | -------------------------------------------: | -------: |
| `--plugin-exflo-kafka-start-block-override`            |                              Block number from which to start publishing                              | Genesis block or from latest published block |      Yes |
| `--plugin-exflo-kafka-max-fork-size`                   | Max no. of blocks that a fork can be comprised of. Used for resetting chain tracker's tail on restart |                                          192 |      Yes |
| `--plugin-exflo-kafka-processing-entities`             |     Comma separated list of entities to include on import / ingest. Default is a predefined list      |               HEADER, BODY, RECEIPTS, TRACES |      Yes |
| `--plugin-exflo-kafka-bootstrap-servers`               |                                     Kafka cluster to publish into                                     |                               localhost:9092 |      Yes |
| `--plugin-exflo-kafka-client-id`                       |                                 Client id to use with Kafka Publisher                                 |                                        exflo |      Yes |
| `--plugin-exflo-kafka-replication-factor`              |                                 Replication factor to use for topics                                  |                                            1 |      Yes |
| `--plugin-exflo-kafka-import-cache-topic`              |                               Topic to use for import progress tracking                               |                         \_exflo-import-cache |      Yes |
| `--plugin-exflo-kafka-blocks-topic`                    |                              Topic to use for chain tracker state store                               |                                       blocks |      Yes |
| `--plugin-exflo-kafka-blocks-topic-partitions`         |                               Num of partitions related to blocks topic                               |                                            1 |      Yes |
| `--plugin-exflo-kafka-blocks-topic-replication-factor` |                           Num of replication factor related to blocks topic                           |                                            1 |      Yes |
| `--plugin-exflo-kafka-ignore-kafka-topic-creation`     |                     Enables or disables the creation of the required Kafka topic                      |                                        false |      Yes |
| `--plugin-exflo-kafka-safe-sync-block-amount`          |                     Number of blocks to check during the initial safe sync check                      |                                          256 |      Yes |

### Postgres

Possible command line arguments for `postgres` are described below:

| Flag                                              |                                              Description                                              |                                                                 Default | Optional |
| ------------------------------------------------- | :---------------------------------------------------------------------------------------------------: | ----------------------------------------------------------------------: | -------: |
| `--plugin-exflo-postgres-start-block-override`    |                              Block number from which to start publishing                              |                            Genesis block or from latest published block |      Yes |
| `--plugin-exflo-postgres-max-fork-size`           | Max no. of blocks that a fork can be comprised of. Used for resetting chain tracker's tail on restart |                                                                     192 |      Yes |
| `--plugin-exflo-postgres-processing-level`        |     Comma separated list of entities to include on import / ingest. Default is a predefined list      |                                          HEADER, BODY, RECEIPTS, TRACES |      Yes |
| `--plugin-exflo-postgres-jdbc-url`                |                               DBC connection url for postgres database                                | jdbc:postgresql://localhost/exflo_dev?user=exflo_dev&password=exflo_dev |      Yes |
| `--plugin-exflo-postgres-ignore-migrations-check` |                      Enables or disables checking migrations on the selected DB                       |                                                                   false |      Yes |

## 💻 Contribute

We welcome any kind of contribution or support to this project but before to do so:

* Read our [development guide](/.github/DEVELOPMENT.md) to understand how to properly develop on the codebase.
* Make sure you have read the [contribution guide](/.github/CONTRIBUTING.md) for more details on how to submit a good PR (pull request).

Also, we are not only limited to technical contributions. Things that make us happy are:

* Add a [GitHub Star](https://github.com/41north/athena/stargazers) to the project.
* ETH donations to [this address](https://etherscan.io/address/0xcee9ad6d00237e25A945D7ac2f7532C602d265Df)!
* Tweet about this project.
* Write a review or tutorial.

## ❔ FAQ

### Why Besu?

We chose Besu for several reasons:

1. It is enterprise ready.
2. It is written in Java, which allows us to work in Kotlin
3. It has a modern codebase with good patterns and documentation.
4. There is a plugin system allowing for customisation and integration without the need to maintain a fork.
5. Their [community is open and welcoming](https://chat.hyperledger.org/channel/besu) (we recommend you join!).

### Why not use the Web3 interface that every Ethereum client has?

If you have ever tried this you will quickly realise that extracting even just the basic information from an Ethereum client via the Web3 
interface requires a lot of requests and some non-trivial logic to do well. On top of that, depending on the client 
(we won't name anyone in particular) you may find that under heavy load, such as when syncing for the first time, your client may become 
unstable and periodically core dump. Maximising throughput whilst keeping the client happy quickly becomes a tedious exercise.

Put simply it has been our experience that pulling via the Web3 interface is sub-optimal for a variety reasons which are better explored 
in a blog post.

## 📬 Get in touch

`Exflo` has been developed initially by [°41North](https://41north.dev). 

If you think this project would be useful for your use case and want to talk more about it you can reach out to us via our contact form 
or by sending an email to `hello@41north.dev`. We try to respond within 48 hours and look forward to hearing from you.

## ✍️ License

This project is licensed under the Apache 2.0 License - see the [LICENSE](LICENSE) file for details.
