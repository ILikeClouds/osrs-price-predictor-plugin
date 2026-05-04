// RuneLite Plugin build file

plugins {
    id 'java'
    id 'com.github.johnrengelman.shadow' version '8.1.1'
    id 'maven-publish'
}

group   = 'com.ilikeclouds'
version = '1.0.0'

repositories {
    mavenLocal()
    maven { url = 'https://repo.runelite.net' }
    mavenCentral()
}

dependencies {
    // RuneLite client — 'changing: true' means Gradle always checks for updates
    compileOnly(group: 'net.runelite.client', name: 'client', version: '+', changing: true)

    // Lombok — reduces boilerplate (@Slf4j, @Getter, etc.)
    compileOnly(group: 'org.projectlombok', name: 'lombok', version: '1.18.30')
    annotationProcessor(group: 'org.projectlombok', name: 'lombok', version: '1.18.30')

    // Gson is included in the RuneLite client — we declare it compileOnly
    compileOnly(group: 'com.google.code.gson', name: 'gson', version: '2.10.1')

    // OkHttp is also bundled in RuneLite
    compileOnly(group: 'com.squareup.okhttp3', name: 'okhttp', version: '4.12.0')
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

shadowJar {
    archiveClassifier = ''
}
