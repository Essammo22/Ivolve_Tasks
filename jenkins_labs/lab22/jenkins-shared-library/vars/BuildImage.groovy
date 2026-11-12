
def call(String imageName, String credentialsId = 'dockerhub-creds') {

    def imageTag = env.BUILD_NUMBER
    def fullImage = "${imageName}:${imageTag}"

    stage('Build Docker Image') {
        echo "Building Docker image: ${fullImage}"
        sh "docker build -t ${fullImage} ."
    }

    stage('Trivy Security Scan') {
        echo "Scanning image for HIGH and CRITICAL vulnerabilities..."
        sh """
            trivy image \\
            --timeout 15m \\
            --severity HIGH,CRITICAL \\
            --scanners vuln \\
            --exit-code 0 \\
            ${fullImage}
        """
    }

    stage('Push Docker Image') {
        echo "Pushing ${fullImage} to Docker Hub..."

        withCredentials([
            usernamePassword(
                credentialsId: credentialsId,
                usernameVariable: 'DOCKER_USER',
                passwordVariable: 'DOCKER_PASS'
            )
        ]) {
            sh """
                echo "\$DOCKER_PASS" | docker login \
                    -u "\$DOCKER_USER" \
                    --password-stdin

                docker push ${fullImage}

                docker logout
            """
        }
    }

    stage('Remove Local Image') {
        sh "docker rmi ${fullImage} || true"
    }

    echo "Docker image pipeline completed: ${fullImage}"
}

