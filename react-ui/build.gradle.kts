import com.moowork.gradle.node.npm.NpmTask
plugins {
  id("com.moowork.node") version "1.3.1"
}

tasks.register<NpmTask>("build") {
  description = "Builds the frontend"
	group = "Build"
  
  dependsOn("npmInstall")
  setArgs(listOf("run-script", "build"))
}