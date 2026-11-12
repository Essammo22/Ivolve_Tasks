def call(String imageName,
         String manifestPath = 'jenkins_labs/lab23/manifests/deployment.yaml',
         String branch = 'main') {

    stage('Update Manifest & Push to GitHub') {
        echo "Updating ${manifestPath} to ${imageName}..."

        withCredentials([usernamePassword(
                credentialsId: 'github-token',
                usernameVariable: 'GIT_USER',
                passwordVariable: 'GIT_TOKEN')]) {

            withEnv(["IMAGE=${imageName}",
                     "MANIFEST=${manifestPath}",
                     "BRANCH=${branch}"]) {

                sh '''
                    set -e

                 
                    sed -i "s|image: .*|image: ${IMAGE}|" "${MANIFEST}"
                    grep "image:" "${MANIFEST}"

                  
                    git config user.name "Essammo22"
                    git config user.email "essammostafa2222@gmail.com"

                    
                    git add "${MANIFEST}"
                    git commit -m "Update image to ${IMAGE} (build ${BUILD_NUMBER})"

                    
                    REPO_URL="https://${GIT_USER}:${GIT_TOKEN}@github.com/Essammo22/Ivolve_Tasks.git"
                    git fetch "${REPO_URL}" "${BRANCH}"
                    git rebase FETCH_HEAD
                    git push "${REPO_URL}" HEAD:"${BRANCH}"
                '''
            }
        }
    }
}
