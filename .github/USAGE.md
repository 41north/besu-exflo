# Usage

## Postgres

Possible command line arguments for `postgres` are described below:

| Flag                                              | Description                                                                                           | Default                                                                 | Optional |
| ------------------------------------------------- | :---------------------------------------------------------------------------------------------------: | ----------------------------------------------------------------------: | -------: |
| `--plugin-exflo-postgres-enabled`                 | Enables the postgres exflo plugin                                                                     | false                                                                   | Yes      |
| `--plugin-exflo-postgres-start-block-override`    | Block number from which to start publishing                                                           | Genesis block or from latest published block                            | Yes      |
| `--plugin-exflo-postgres-max-fork-size`           | Max no. of blocks that a fork can be comprised of. Used for resetting chain tracker's tail on restart | 192                                                                     | Yes      |
| `--plugin-exflo-postgres-processing-level`        | Comma separated list of entities to include on import / ingest. Default is a predefined list          | HEADER, BODY, RECEIPTS, TRACES                                          | Yes      |
| `--plugin-exflo-postgres-jdbc-url`                | JDBC connection url for postgres database                                                             | jdbc:postgresql://localhost/exflo_dev?user=exflo_dev&password=exflo_dev | Yes      |
| `--plugin-exflo-postgres-ignore-migrations-check` | Enables or disables checking migrations on the selected DB                                            | false                                                                   | Yes      |

## Kafka

Possible command line arguments for `kafka` are described below:

| Flag                                                   | Description                                                                                           | Default                                      | Optional |
| ------------------------------------------------------ | :---------------------------------------------------------------------------------------------------: | -------------------------------------------: | -------: |
| `--plugin-exflo-kafka-enabled`                         | Enables the kafka exflo plugin                                                                        | false                                        | Yes      |
| `--plugin-exflo-kafka-start-block-override`            | Block number from which to start publishing                                                           | Genesis block or from latest published block | Yes      |
| `--plugin-exflo-kafka-max-fork-size`                   | Max no. of blocks that a fork can be comprised of. Used for resetting chain tracker's tail on restart | 192                                          | Yes      |
| `--plugin-exflo-kafka-processing-entities`             | Comma separated list of entities to include on import / ingest. Default is a predefined list          | HEADER, BODY, RECEIPTS, TRACES               | Yes      |
| `--plugin-exflo-kafka-bootstrap-servers`               | Kafka cluster to publish into                                                                         | localhost:9092                               | Yes      |
| `--plugin-exflo-kafka-client-id`                       | Client id to use with Kafka Publisher                                                                 | exflo                                        | Yes      |
| `--plugin-exflo-kafka-replication-factor`              | Replication factor to use for topics                                                                  | 1                                            | Yes      |
| `--plugin-exflo-kafka-import-cache-topic`              | Topic to use for import progress tracking                                                             | \_exflo-import-cache                         | Yes      |
| `--plugin-exflo-kafka-blocks-topic`                    | Topic to use for chain tracker state store                                                            | blocks                                       | Yes      |
| `--plugin-exflo-kafka-blocks-topic-partitions`         | Num of partitions related to blocks topic                                                             | 1                                            | Yes      |
| `--plugin-exflo-kafka-blocks-topic-replication-factor` | Num of replication factor related to blocks topic                                                     | 1                                            | Yes      |
| `--plugin-exflo-kafka-ignore-kafka-topic-creation`     | Enables or disables the creation of the required Kafka topic                                          | false                                        | Yes      |
| `--plugin-exflo-kafka-safe-sync-block-amount`          | Number of blocks to check during the initial safe sync check                                          | 256                                          | Yes      |
