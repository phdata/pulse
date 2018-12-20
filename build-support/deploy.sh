#!/bin/bash
set -x
set -euo pipefail
echo starting deploy

projectVersion=$(make --no-print-directory --silent version)
repoName=${projectVersion}-cdh5
parcelVersion=$(make --no-print-directory --silent -C cloudera-integration/parcel version)

curl -u$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -T dist/manifest.json "https://repository.phdata.io/artifactory/$DEPLOY_REPO/phdata/pulse/${projectVersion}/manifest.json"
curl -u$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -T dist/PULSE-${parcelVersion}-el7.parcel "https://repository.phdata.io/artifactory/$DEPLOY_REPO/phdata/pulse/${projectVersion}/PULSE-${parcelVersion}-el7.parcel"
curl -u$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -T dist/PULSE-${parcelVersion}-el7.parcel.sha "https://repository.phdata.io/artifactory/$DEPLOY_REPO/phdata/pulse/${projectVersion}/PULSE-${parcelVersion}-el7.parcel.sha"

curl -u$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -T dist/PULSE-${parcelVersion}-el7.parcel "https://repository.phdata.io/artifactory/$DEPLOY_REPO/phdata/pulse/${projectVersion}/PULSE-${parcelVersion}-el6.parcel"
curl -u$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -T dist/PULSE-${parcelVersion}-el7.parcel.sha "https://repository.phdata.io/artifactory/$DEPLOY_REPO/phdata/pulse/${projectVersion}/PULSE-${parcelVersion}-el6.parcel.sha"

curl -u$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -T dist/PULSE-${parcelVersion}.jar "https://repository.phdata.io/artifactory/$DEPLOY_REPO/phdata/pulse/${projectVersion}/PULSE-${parcelVersion}.jar"
curl -u$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -T dist/PULSE-${parcelVersion}.jar.sha "https://repository.phdata.io/artifactory/$DEPLOY_REPO/phdata/pulse/${projectVersion}/PULSE-${parcelVersion}.jar.sha"

echo deploy success
