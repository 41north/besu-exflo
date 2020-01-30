/*
 * Copyright (c) 2020 41North.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.rohanprabhu.gradle.plugins.kdjooq.JooqEdition
import org.jooq.meta.jaxb.Configuration

plugins {
    `java-library`
    kotlin("jvm")
    `maven-publish`
    id("com.github.johnrengelman.shadow")
    id("org.jlleitschuh.gradle.ktlint")
    id("org.flywaydb.flyway") version "6.2.0"
    id("com.rohanprabhu.kotlin-dsl-jooq") version "0.4.4"
}

dependencies {

    implementation(project(":ingestion:base"))

    jooqGeneratorRuntime("org.postgresql:postgresql")

    implementation("org.postgresql:postgresql")
    implementation("org.jooq:jooq")
    implementation("com.zaxxer:HikariCP")
    implementation("org.flywaydb:flyway-core")

    implementation("com.beust:klaxon")

    runtimeOnly("org.apache.logging.log4j:log4j-core")

    testImplementation(project(":testutil"))
}

val postgresUrl =
    System.getenv("POSTGRES_URL") ?: "jdbc:postgresql://localhost:5432/exflo_dev?user=exflo_dev&password=exflo_dev"

flyway {
    url = postgresUrl
}

afterEvaluate {
    // jooq codegen is hardcoded into the build phase so we manually disable for now
    // so that it isn't performing unnecessary generations
    project.tasks["jooq-codegen-primary"].enabled = false
}

jooqGenerator {
    jooqEdition = JooqEdition.OpenSource
    jooqVersion = "3.12.3"

    configuration("primary", project.sourceSets["main"]) {

        configuration = Configuration()
            .apply {
                jdbc = org.jooq.meta.jaxb.Jdbc()
                    .withDriver("org.postgresql.Driver")
                    .withUrl(postgresUrl)

                generator = org.jooq.meta.jaxb.Generator()
                    .withName("org.jooq.codegen.DefaultGenerator")
                    .withStrategy(
                        org.jooq.meta.jaxb.Strategy()
                            .withName("org.jooq.codegen.DefaultGeneratorStrategy")
                    )
                    .withDatabase(
                        org.jooq.meta.jaxb.Database()
                            .withName("org.jooq.meta.postgres.PostgresDatabase")
                            .withInputSchema("public")
                    )
                    .withGenerate(
                        org.jooq.meta.jaxb.Generate()
                            .withRelations(true)
                            .withDeprecated(false)
                            .withRecords(true)
                            .withImmutablePojos(false)
                            .withFluentSetters(true)
                    )
                    .withTarget(
                        org.jooq.meta.jaxb.Target()
                            .withPackageName("io.exflo.postgres.jooq")
                            .withDirectory("src/main/java/")
                    )
            }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {

            groupId = "${rootProject.group}"
            version = "${project.version}"

            project.shadow.component(this)

            pom {
                name.set("Exflo - ${project.name}")
                url.set("https://github.com/41North/exflo")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/41North/exflo.git")
                    developerConnection.set("scm:git:ssh://github.com/41North/exflo.git")
                    url.set("https://github.com/41North/exflo")
                }
            }
        }
    }
}

val build: DefaultTask by project.tasks
build.dependsOn(tasks.shadowJar)

tasks.withType<ShadowJar> {
    archiveBaseName.set(project.name)
    archiveClassifier.set("")
    minimize()
}

tasks.withType<Test> {
    useJUnitPlatform()
}
