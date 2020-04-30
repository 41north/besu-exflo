# Postgres Plugin

Possible command line arguments for `postgres` are described below:

| Flag                                              |                                              Description                                              |                                                                 Default | Optional |
| ------------------------------------------------- | :---------------------------------------------------------------------------------------------------: | ----------------------------------------------------------------------: | -------: |
| `--plugin-exflo-postgres-start-block-override`    |                              Block number from which to start publishing                              |                            Genesis block or from latest published block |      Yes |
| `--plugin-exflo-postgres-max-fork-size`           | Max no. of blocks that a fork can be comprised of. Used for resetting chain tracker's tail on restart |                                                                     192 |      Yes |
| `--plugin-exflo-postgres-processing-level`        |     Comma separated list of entities to include on import / ingest. Default is a predefined list      |                                          HEADER, BODY, RECEIPTS, TRACES |      Yes |
| `--plugin-exflo-postgres-jdbc-url`                |                               DBC connection url for postgres database                                | jdbc:postgresql://localhost/exflo_dev?user=exflo_dev&password=exflo_dev |      Yes |
| `--plugin-exflo-postgres-ignore-migrations-check` |                      Enables or disables checking migrations on the selected DB                       |                                                                   false |      Yes |
