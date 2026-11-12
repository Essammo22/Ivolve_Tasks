def call() {

    echo "Starting Application Build..."

    stage('Unit Testing') {
        echo "Running Maven Unit Tests..."
        sh 'mvn clean test'
    }

    stage('Build Application') {
        echo "Building Application..."
        sh 'mvn package -DskipTests'
    }

    echo "Application Build Completed Successfully!"
}

