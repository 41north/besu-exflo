
# Usage

Exflo is following the [Besu CLI Style Guide](https://wiki.hyperledger.org/display/BESU/Besu+CLI+Style+Guide) for handling CLI interactions.

In this page you will find the complete description and options that Exflo allows to be configured, it also showcases default options and
if the commands are optional or not.

## Plugins

There are two possible ways of passing options to Exflo:

- Environment variables
- Command-line arguments

Both options are supported, and the usage will depend on your necessities. Normally, if you are using docker to run Besu, environment
variables are more comfortable to use rather than a list of command-line arguments.

Keep in mind for environment variables you need to prepend `BESU_PLUGIN_EXFLO_${COMMAND}` and for command-line arguments `--plugin-exflo-${COMMAND}`
where `${COMMAND}` is the one of the possible values as described in each plugin section.

By default, plugins are disabled. So, if you want to turn one of them on, you can set `--plugin-exflo-postgres-enabled` to true. 

### Postgres

Possible command line arguments for `postgres` are described below:

| CLI                                               | Environment Variable                                 | Description                                                                                           | Default                                                                 |
| :------------------------------------------------ | :--------------------------------------------------- | :---------------------------------------------------------------------------------------------------- | :---------------------------------------------------------------------- |
| `--plugin-exflo-postgres-enabled`                 | `BESU_PLUGIN_EXFLO_POSTGRES_ENABLED`                 | Enables the postgres Exflo plugin                                                                     | false                                                                   |
| `--plugin-exflo-postgres-earliest-block-number`   | `BESU_PLUGIN_EXFLO_POSTGRES_EARLIEST_BLOCK_NUMBER`   | Earliest block number to include in the history                                                       | Genesis block                                                           |
| `--plugin-exflo-postgres-max-fork-size`           | `BESU_PLUGIN_EXFLO_POSTGRES_MAX_FORK_SIZE`           | Max no. of blocks that a fork can be comprised of. Used for resetting chain tracker's tail on restart | 192                                                                     |
| `--plugin-exflo-postgres-processing-level`        | `BESU_PLUGIN_EXFLO_POSTGRES_PROCESSING_LEVEL`        | Comma separated list of entities to include on import / ingest. Default is a predefined list          | HEADER, BODY, RECEIPTS, TRACES                                          |
| `--plugin-exflo-postgres-jdbc-url`                | `BESU_PLUGIN_EXFLO_POSTGRES_JDBC_URL`                | JDBC connection url for postgres database                                                             | jdbc:postgresql://localhost/exflo_dev?user=exflo_dev&password=exflo_dev |
| `--plugin-exflo-postgres-ignore-migrations-check` | `BESU_PLUGIN_EXFLO_POSTGRES_IGNORE_MIGRATIONS_CHECK` | Enables or disables checking migrations on the selected DB                                            | false                                                                   |
