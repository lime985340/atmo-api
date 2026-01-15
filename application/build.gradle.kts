plugins { kotlin("jvm"); kotlin("plugin.spring") }
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
dependencies { implementation(project(":common")); implementation(project(":domain")); implementation(kotlin("stdlib")) }
