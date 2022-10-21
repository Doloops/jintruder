@Library('utils') _
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils

def JIRA_KEY = "JIN"
def POM_PATH = "pom.xml"
def DEVELOP_BRANCH = "develop"
def BUILD_CMD = "clean install -DskipTests -DskipITs -fn"
def TAG_VERSION = null

pipeline {
	agent {
		label 'maven'
	}
    stages {
        stage('Preparation') {
            steps {
                script {
                    env.MAVEN_HOME = tool 'maven3'
                    DEPLOY_VERSION = readMavenPom().getVersion()
    				NEW_TAG = readMavenPom().getArtifactId()
                }
            }
        }
		stage('Build Branch') {	
			when {
				not { branch "${DEVELOP_BRANCH}"}
				not { expression { BRANCH_NAME ==~ /^(release-.*)/ } }
			}
			steps {
				script {
					pipelineUtils.mavenBuild(POM_PATH, BUILD_CMD)
				}
			}
		}
		stage('Build Develop') {	
			when {
				branch "${DEVELOP_BRANCH}"
			}
			steps {
				script {
					ARTIFACTORY = "arondor-snapshot"
					pipelineUtils.mavenBuildAndPublish(POM_PATH, BUILD_CMD)
				}
			}
		}
		stage('Build Release') {	
			when {
				expression { BRANCH_NAME ==~ /^(release.*)/ }
			}
			steps {
				script {
					ARTIFACTORY = "arondor-release"
					pipelineUtils.mavenBuildAndPublish(POM_PATH, BUILD_CMD)
				}
			}
		}
    }
    post { 
        always { 
            junit allowEmptyResults: true, testResults:  '**/target/surefire-reports/TEST-*.xml'
        }
    }
    
    options {
	  buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '10')
	}
}
