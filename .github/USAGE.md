# Usage

Exflo is following the [Besu CLI Style Guide](https://wiki.hyperledger.org/display/BESU/Besu+CLI+Style+Guide) for handling CLI interactions.

In this page you will find the complete description and options that Exflo allows to be configured, it also showcases default options and
if the commands are optional or not.

## Plugins

There are two possible ways of passing options to Exflo:

- Environment variables
- Command line arguments

Both options are supported and the usage will depend on your necessities. Normally, if you are using docker to run Besu, environment
variables are easier to use rather than a list of command line arguments.

Keep in mind that for environment variables you need to prepend `BESU_PLUGIN_EXFLO_EXFLO_${COMMAND}` and for command line arguments `--plugin-exflo-${COMMAND}`
where `${COMMAND}` is the one of the possible values as described in each plugin section.

By default, both plugins, Postgres and Kafka, are disabled. So, if you want to turn one of them on, you can set `--plugin-exflo-postgres-enabled`
or `--plugin-exflo-kafka-enabled` to true. You can even execute both at the same time!

### Postgres

Possible command line arguments for `postgres` are described below:

| CLI                                               | Environment Variable                                 | Description                                                                                           | Default                                                                 | Optional |
| :------------------------------------------------ | :--------------------------------------------------- | :---------------------------------------------------------------------------------------------------- | :---------------------------------------------------------------------- | :------- |
| `--plugin-exflo-postgres-enabled`                 | `BESU_PLUGIN_EXFLO_POSTGRES_ENABLED`                 | Enables the postgres Exflo plugin                                                                     | false                                                                   | Yes      |
| `--plugin-exflo-postgres-start-block-override`    | `BESU_PLUGIN_EXFLO_POSTGRES_START_BLOCK_OVERRIDE`    | Block number from which to start publishing                                                           | Genesis block or from latest published block                            | Yes      |
| `--plugin-exflo-postgres-max-fork-size`           | `BESU_PLUGIN_EXFLO_POSTGRES_MAX_FORK_SIZE`           | Max no. of blocks that a fork can be comprised of. Used for resetting chain tracker's tail on restart | 192                                                                     | Yes      |
| `--plugin-exflo-postgres-processing-level`        | `BESU_PLUGIN_EXFLO_POSTGRES_PROCESSING_LEVEL`        | Comma separated list of entities to include on import / ingest. Default is a predefined list          | HEADER, BODY, RECEIPTS, TRACES                                          | Yes      |
| `--plugin-exflo-postgres-jdbc-url`                | `BESU_PLUGIN_EXFLO_POSTGRES_JDBC_URL`                | JDBC connection url for postgres database                                                             | jdbc:postgresql://localhost/exflo_dev?user=exflo_dev&password=exflo_dev | Yes      |
| `--plugin-exflo-postgres-ignore-migrations-check` | `BESU_PLUGIN_EXFLO_POSTGRES_IGNORE_MIGRATIONS_CHECK` | Enables or disables checking migrations on the selected DB                                            | false                                                                   | Yes      |

### Kafka

Possible command line arguments for `kafka` are described below:

| Flag                                                   | Environment Variable                                      | Description                                                                                           | Default                                      | Optional |
| :----------------------------------------------------- | :-------------------------------------------------------- | :---------------------------------------------------------------------------------------------------- | :------------------------------------------- | :------- |
| `--plugin-exflo-kafka-enabled`                         | `BESU_PLUGIN_EXFLO_KAFKA_ENABLED`                         | Enables the kafka exflo plugin                                                                        | false                                        | Yes      |
| `--plugin-exflo-kafka-start-block-override`            | `BESU_PLUGIN_EXFLO_KAFKA_START_BLOCK_OVERRIDE`            | Block number from which to start publishing                                                           | Genesis block or from latest published block | Yes      |
| `--plugin-exflo-kafka-max-fork-size`                   | `BESU_PLUGIN_EXFLO_KAFKA_MAX_FORK_SIZE`                   | Max no. of blocks that a fork can be comprised of. Used for resetting chain tracker's tail on restart | 192                                          | Yes      |
| `--plugin-exflo-kafka-processing-entities`             | `BESU_PLUGIN_EXFLO_KAFKA_BOOTSTRAP_SERVERS`               | Comma separated list of entities to include on import / ingest. Default is a predefined list          | HEADER, BODY, RECEIPTS, TRACES               | Yes      |
| `--plugin-exflo-kafka-bootstrap-servers`               | `BESU_PLUGIN_EXFLO_KAFKA_BOOTSTRAP_SERVERS`               | Kafka cluster to publish into                                                                         | localhost:9092                               | Yes      |
| `--plugin-exflo-kafka-client-id`                       | `BESU_PLUGIN_EXFLO_KAFKA_CLIENT_ID`                       | Client id to use with Kafka Publisher                                                                 | exflo                                        | Yes      |
| `--plugin-exflo-kafka-replication-factor`              | `BESU_PLUGIN_EXFLO_KAFKA_REPLICATION_FACTOR`              | Replication factor to use for topics                                                                  | 1                                            | Yes      |
| `--plugin-exflo-kafka-import-cache-topic`              | `BESU_PLUGIN_EXFLO_KAFKA_IMPORT_CACHE_TOPIC`              | Topic to use for import progress tracking                                                             | \_exflo-import-cache                         | Yes      |
| `--plugin-exflo-kafka-blocks-topic`                    | `BESU_PLUGIN_EXFLO_KAFKA_BLOCKS_TOPIC`                    | Topic to use for chain tracker state store                                                            | blocks                                       | Yes      |
| `--plugin-exflo-kafka-blocks-topic-partitions`         | `BESU_PLUGIN_EXFLO_KAFKA_BLOCKS_TOPIC_PARTITIONS`         | Num of partitions related to blocks topic                                                             | 1                                            | Yes      |
| `--plugin-exflo-kafka-blocks-topic-replication-factor` | `BESU_PLUGIN_EXFLO_KAFKA_BLOCKS_TOPIC_REPLICATION_FACTOR` | Num of replication factor related to blocks topic                                                     | 1                                            | Yes      |
| `--plugin-exflo-kafka-ignore-kafka-topic-creation`     | `BESU_PLUGIN_EXFLO_KAFKA_IGNORE_KAFKA_TOPIC_CREATION`     | Enables or disables the creation of the required Kafka topic                                          | false                                        | Yes      |
| `--plugin-exflo-kafka-safe-sync-block-amount`          | `BESU_PLUGIN_EXFLO_KAFKA_SAFE_SYNC_BLOCK_AMOUNT`          | Number of blocks to check during the initial safe sync check                                          | 256                                          | Yes      |
