plugins {
    kotlin("jvm") version "1.8.21"
    id("app.cash.sqldelight") version "2.0.1"
    application
}

group = "ua.syt0r"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
}

sqldelight {
    databases {
        create("KanjiDojoData") {
            packageName.set("export.db")
        }
    }
}

val TaskPropertyName = "task"

application {
    if (hasProperty(TaskPropertyName)) {
        val task = properties[TaskPropertyName]
        mainClass.set("task.${task}Kt")
    }
}
