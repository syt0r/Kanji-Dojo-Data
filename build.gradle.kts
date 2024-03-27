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

val radFileUrl = "http://ftp.edrdg.org/pub/Nihongo/radkfile.gz"

fun downloadFile(url: String, file: File) {
    ant.invokeMethod("get", mapOf("src" to url, "dest" to file))
}

val dataDir = File(projectDir, "parser_data")

task("downloadRadkFile") {
    doLast {
        dataDir.mkdirs()
        val radkArchive = File(dataDir, "radkfile.gz")
        val radkFile = File(dataDir, "radkfile")
        downloadFile(radFileUrl, radkArchive)
        val inputStream = resources.gzip(radkArchive).read()
        radkFile.outputStream().apply {
            write(inputStream.readAllBytes())
            close()
        }
    }
}

val leedsFreqUrl =
    "https://web.archive.org/web/20230924010025id_/http://corpus.leeds.ac.uk/frqc/internet-jp.num"

task("downloadLeedsFrequencies") {
    doLast {
        dataDir.mkdirs()
        val file = File(dataDir, "internet-jp.num")
        downloadFile(leedsFreqUrl, file)
    }
}

val jmdictFuriganaJsonUrl =
    "https://github.com/Doublevil/JmdictFurigana/releases/download/2.3.0%2B2023-08-25/JmdictFurigana.json"

task("downloadjmdictFuriganaJson") {
    doLast {
        dataDir.mkdirs()
        val file = File(dataDir, "JmdictFurigana.json")
        downloadFile(jmdictFuriganaJsonUrl, file)
    }
}
