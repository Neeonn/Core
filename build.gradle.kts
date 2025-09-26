plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "io.github.divinerealms.core"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/central")
    maven("https://repo.codemc.io/repository/nms")
    maven("https://jitpack.io")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://nexus.scarsz.me/content/groups/public/")
}

dependencies {
    compileOnly("org.spigotmc:spigot:1.8.8-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("org.projectlombok:lombok:1.18.38")
    compileOnly("com.github.NEZNAMY:TAB-API:5.2.5")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.discordsrv:discordsrv:1.30.0")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.3.0")

    annotationProcessor("org.projectlombok:lombok:1.18.38")

    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}