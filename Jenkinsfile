// Appium Java suite as a declarative Jenkins pipeline.
// It runs in Jenkinsfile Runner (ci/jenkins/Dockerfile): one throwaway Jenkins
// per build, no server. The emulator and the Appium server run on the host;
// this pipeline reaches them over the host network.
pipeline {
    agent any
    options {
        timeout(time: 30, unit: 'MINUTES')
    }
    environment {
        REPO_DIR   = '/workspace'
        OUT_DIR    = '/workspace/out'
        APPIUM_URL = 'http://127.0.0.1:4723'
    }
    stages {
        stage('Check Appium') {
            steps {
                // Fail fast with a clear message if the host side is not ready.
                sh 'curl -fsS "$APPIUM_URL/status"'
            }
        }
        stage('Appium Java tests') {
            steps {
                dir(env.REPO_DIR) {
                    // The run script writes allure-results and the status JSON
                    // and exits non-zero on test failures.
                    sh 'ci/run-appium-java.sh'
                }
            }
        }
    }
    post {
        always {
            dir(env.REPO_DIR) {
                sh 'ls -la out/status || true'
            }
        }
    }
}
