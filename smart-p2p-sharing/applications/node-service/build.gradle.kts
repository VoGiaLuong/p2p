plugins {
    application
}

application {
    mainClass.set("com.example.p2p.node.NodeServiceApp")
}

dependencies {
    implementation(project(":modules:n8n-integration"))
    implementation("org.slf4j:slf4j-simple:2.0.12")
}
