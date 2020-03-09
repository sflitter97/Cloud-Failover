evaluationDependsOn(":server")
evaluationDependsOn(":react-ui")

tasks.create<Copy>("copyWebUi") {
	dependsOn(":react-ui:build")
	from("/react-ui/build/")
	include("/**/*")
	into("/server/build/resources/main/static")
}

project(":server").tasks.named("bootJar") {
	dependsOn(":copyWebUi")
}

tasks.create<Copy>("copyServerJar") {
	dependsOn("server:bootJar")
	from("server/build/libs/")
	include("*.jar")
	into("/")
}

tasks.create("build") {
	description = "Builds the frontend and backend"
	group = "build"

	dependsOn("copyServerJar")
}