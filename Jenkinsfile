//def PLATFORMS = [ "ubuntu16", "windows" ]
def PLATFORMS = ["ubuntu16"]
def DEFAULT_PLATFORM = PLATFORMS[0]

pipeline {
    agent none
    options {
        timeout(time: 60, unit: 'MINUTES')
    }
    stages {
        stage('job valid?') {
            when {
                expression {
                    return _INTERNAL_OK_.toBoolean() != true
                }
            }
            steps {
                error("Exiting early as not valid run")
            }
        }
        stage('prepare and validate') {
            agent { label 'centos6||centos7||ubuntu16||ubuntu14' }
            steps {
                cleanWs()
                dir('couchbase-jvm-clients') {
                    // TODO: GERRIT
//                    checkout([$class: 'GitSCM', branches: [[name: '$SHA']], userRemoteConfigs: [[refspec: "$GERRIT_REFSPEC", url: '$REPO', poll: false]]])
                    checkout([$class: 'GitSCM', userRemoteConfigs: [[url: '$REPO', poll: false]]])


                    // TODO: METADATA UPDATES GO HERE, MAYBE COPY OVER FROM JAVA-CLIENT?
                    script {
                        if (env.IS_RELEASE.toBoolean() == true) {
                            echo "This is release, not updating metadata"
                        } else {
                            echo "This is not release, updating metadata"
                        }
                    }
                }

                stash includes: 'couchbase-jvm-clients/', name: 'couchbase-jvm-clients', useDefaultExcludes: false
            }
        }
        stage('build and test') {
            agent { label 'master' }
            steps {
                cleanWs()
                buildsAndTests(PLATFORMS)
            }
        }
        stage('package') {
            agent { label 'ubuntu14||ubuntu16||centos6||centos7' }
            when {
                expression
                        { return IS_GERRIT_TRIGGER.toBoolean() == false }
            }
            steps {
                cleanWs()
                unstash "couchbase-jvm-clients-build-${DEFAULT_PLATFORM}"
                // Unsure if anything needs to happen here, build is only done on 1 host so nothing to collect
                // and docs are generated by build too.
                stash includes: 'couchbase-jvm-clients/', name: 'couchbase-jvm-clients-package', useDefaultExcludes: false
            }

            // TODO having all kinds of issues with artifacts, commenting just to get past

//            post {
//                always {
//                    dir("couchbase-jvm-clients") {
//                        if (platform != "windows") {
//                            shWithEcho("find . -iname *.jar")
//                        }
//
//                        //archiveArtifacts artifacts: 'couchbase-jvm-clients/', fingerprint: true
////                        archiveArtifacts artifacts: 'scala-client/build/libs/*.jar', fingerprint: true
////                        archiveArtifacts artifacts: 'scala-implicits/build/libs/*.jar', fingerprint: true
////                        archiveArtifacts artifacts: 'core-io/build/libs/*.jar', fingerprint: true
////                        archiveArtifacts artifacts: 'benchmarks/build/libs/*.jar', fingerprint: true
////                        archiveArtifacts artifacts: 'kotlin-client/build/libs/*.jar', fingerprint: true
////                        archiveArtifacts artifacts: 'java-examples/build/libs/*.jar', fingerprint: true
////                        archiveArtifacts artifacts: 'java-client/build/libs/*.jar', fingerprint: true
//                    }
//                }
//            }
        }
        stage('test-integration-server') {
            agent { label 'sdk-integration-test-linux' }
            when {
                expression
                        { return IS_GERRIT_TRIGGER.toBoolean() == false }
            }
            steps {
                cleanWs()
//                build job: "couchbase-jvm-clients-test-integration", parameters: []
            }
        }
        stage('quality') {
            agent { label 'master' }
            when {
                expression
                        { return IS_GERRIT_TRIGGER.toBoolean() == false }
            }
            steps {
                cleanWs()
                // TODO: SITUATIONAL TESTING JOB WILL BE HOOKED HERE
            }
        }
        stage('snapshot') {
            agent { label 'ubuntu14||ubuntu16||centos6||centos7' }
            when {
                expression
                        { return IS_GERRIT_TRIGGER.toBoolean() == false && IS_RELEASE.toBoolean() == false }
            }
            steps {
                cleanWs()
                // TODO: upload relevant artifacts to sdk snapshots
            }
        }
        stage('approval') {
            agent none
            when {
                expression
                        { return IS_RELEASE.toBoolean() == true }
            }
            steps {
                input 'Publish?'
            }
        }
        stage('publish') {
            agent { label 'ubuntu14||ubuntu16||centos6||centos7' }
            when {
                expression
                        { return IS_RELEASE.toBoolean() == true }
            }
            steps {
                cleanWs()
                unstash "couchbase-jvm-clients-package"
                // TODO: upload relevant artifacts to maven
                // tag and push tag
            }
//            post {
//                always {
////                    archiveArtifacts artifacts: 'couchbase-jvm-clients/', fingerprint: true
//                }
//            }
        }
    }
    post {
        always {
            shWithEcho("pwd")
            shWithEcho("find .")
            // Process the Junit test results
            junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
            junit allowEmptyResults: true, testResults: 'couchbase-jvm-clients/core-io/target/surefire-reports/*.xml'
        }
    }
}

void shWithEcho(String command) {
    echo sh(script: command, returnStdout: true)
}

void batWithEcho(String command) {
    echo bat(script: command, returnStdout: true)
}

def buildsAndTests(PLATFORMS) {
    def tests = [:]
    for (j in PLATFORMS) {
        def platform = j

        tests[platform] = {
            node(platform) {
                def envStr = []
                if (platform == "windows") {
                    envStr = ["JAVA_HOME=${WORKSPACE}\\deps\\java-${JAVA_VERSION}", "PATH=${WORKSPACE}\\deps\\java-${JAVA_VERSION}\\bin:$PATH"]
                } else {
                    envStr = ["JAVA_HOME=${WORKSPACE}/deps/java-${JAVA_VERSION}", "PATH=${WORKSPACE}/deps/java-${JAVA_VERSION}/bin:$PATH"]
                }
                withEnv(envStr) {
                    stage("build-${platform}") {
                        cleanWs()
                        unstash 'couchbase-jvm-clients'
                        installJDK(platform, JAVA_VERSION)

                        if (platform == "windows") {
                            batWithEcho("dir deps")
                            dir('couchbase-jvm-clients') {
                                batWithEcho("java -version")
                                batWithEcho("make deps-only")
                                batWithEcho("mvn package -Dmaven.test.skip")
                            }
                        } else {
                            shWithEcho("dir deps")
                            dir('couchbase-jvm-clients') {
                                shWithEcho("java -version")
                                shWithEcho("make deps-only")
                                shWithEcho("mvn package -Dmaven.test.skip")
                            }
                        }
                        stash includes: 'couchbase-jvm-clients/', name: "couchbase-jvm-clients-build-${platform}", useDefaultExcludes: false
                    }
                    stage("test-${platform}") {
                        if (platform == "windows") {
                            dir('couchbase-jvm-clients') {
                                // TODO
                                batWithEcho("mvn --fail-at-end test -DuseMock=true -Dci=true")
                            }
                        } else {
                            dir('couchbase-jvm-clients') {
                                // By default Java and Scala use mock for testing
                                shWithEcho("mvn --fail-at-end test")
                            }
                        }
                        // TODO: IF YOU HAVE INTEGRATION TESTS THAT RUN AGAINST THE MOCK DO THAT HERE
                        // USING THE PACKAGE(S) CREATED ABOVE
                    }
                    post {
                        always {
                            shWithEcho("find .")
                            // Process the Junit test results
                            junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
                            junit allowEmptyResults: true, testResults: 'couchbase-jvm-clients/core-io/target/surefire-reports/*.xml'
                        }
                    }
                }
                post {
                    always {
                        shWithEcho("find .")
                        // Process the Junit test results
                        junit allowEmptyResults: true, testResults: '**/surefire-reports/*.xml'
                        junit allowEmptyResults: true, testResults: 'couchbase-jvm-clients/core-io/target/surefire-reports/*.xml'
                    }
                }
            }
        }
    }

    parallel tests
}

def installJDK(PLATFORM, JAVA_VERSION) {
    def install = false

    echo "checking install"
    if (!fileExists("deps")) {
        echo "file deps does not exist"
        install = true
    } else {
        echo "file deps does exist"
        dir("deps") {
            install = !fileExists("java-${JAVA_VERSION}")
            if (install) {
                echo "java-${JAVA_VERSION} exists"
            } else {
                echo "java-${JAVA_VERSION} does not exist"
            }
        }
    }

    if (install) {
        if (PLATFORM.contains("windows")) {
            batWithEcho("mkdir deps && mkdir deps\\java-${JAVA_VERSION}")
            batWithEcho("cbdep install -d deps java ${JAVA_VERSION}")
        } else {
            shWithEcho("mkdir deps && mkdir deps/java-${JAVA_VERSION}")
            shWithEcho("cbdep install -d deps java ${JAVA_VERSION}")
        }
    }
}
