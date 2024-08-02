rootProject.name = "pack-engine"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("versions.toml"))
        }
    }
}

include("pack-engine")

sequenceOf(
    "paper",
    "velocity"
).forEach {
    include("pack-engine-$it")
}