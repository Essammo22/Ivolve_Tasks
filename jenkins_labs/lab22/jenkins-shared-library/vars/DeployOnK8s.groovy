
def call(String imageName, String namespace = 'ivolve',
         String deploymentName = 'demoapp',
         String containerName = 'demoapp') {

    stage('Deploy on Kubernetes') {
        echo "Deploying ${imageName} to ${namespace}..."

        sh """
            kubectl set image deployment/${deploymentName} \
                ${containerName}=${imageName} \
                -n ${namespace}

            kubectl rollout status deployment/${deploymentName} \
                -n ${namespace} \
                --timeout=180s
        """
    }

    stage('Verify Deployment') {
        sh """
            kubectl get deployment ${deploymentName} -n ${namespace}
            kubectl get pods -n ${namespace} -l app=${deploymentName}
        """
    }
}

