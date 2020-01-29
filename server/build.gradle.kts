import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.2.3.RELEASE"
    id("io.spring.dependency-management") version "1.0.8.RELEASE"
    id("org.jlleitschuh.gradle.ktlint") version "9.1.1"
    kotlin("jvm") version "1.3.61"
    kotlin("plugin.spring") version "1.3.61"
}

group = "com.flitterkomskis"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

extra["azureVersion"] = "2.2.0"
extra["springCloudVersion"] = "Hoxton.SR1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("com.microsoft.azure:azure-spring-boot-starter")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.cloud:spring-cloud-gcp-starter")
    implementation("software.amazon.awssdk:ec2")
    implementation("software.amazon.awssdk:sts")
    // implementation("org.springframework.cloud:spring-cloud-starter-aws")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("de.flapdoodle.embed:de.flapdoodle.embed.mongo")
}

dependencyManagement {
    imports {
        mavenBom("com.microsoft.azure:azure-spring-boot-bom:${property("azureVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("software.amazon.awssdk:bom:2.5.29")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

sourceSets {
    create("integrationTest") {
        withConvention(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class) {
            kotlin.srcDir("src/integration/kotlin")
            resources.srcDir("src/integration/resources")
            compileClasspath += sourceSets["main"].output + configurations["testRuntimeClasspath"]
            runtimeClasspath += output + compileClasspath + sourceSets["test"].runtimeClasspath
        }
    }
}

task<Test>("integrationTest") {
    description = "Runs the integration tests"
    group = "verification"
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    mustRunAfter(tasks["test"])
    useJUnitPlatform()
}
