#!/bin/bash
set -x
set -euo pipefail
echo starting deploy

projectVersion=$VERSION
cdh_version=cdh$CDH_VERSION
repoName=${projectVersion}-${cdh_version}
parcelVersion=$(make --no-print-directory --silent -C cloudera-integration/parcel version)
dist=dist$CDH_VERSION

curl -u$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -T $dist/manifest.json "https://repository.phdata.io/artifactory/$DEPLOY_REPO/phdata/pulse/${repoName}/manifest.json"

for distro in "el6" "el7" "sles12"; do
  curl -u$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -T $dist/PULSE-${parcelVersion}-el7.parcel "https://repository.phdata.io/artifactory/$DEPLOY_REPO/phdata/pulse/${repoName}/PULSE-${parcelVersion}-$distro.parcel"
  curl -u$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -T $dist/PULSE-${parcelVersion}-el7.parcel.sha "https://repository.phdata.io/artifactory/$DEPLOY_REPO/phdata/pulse/${repoName}/PULSE-${parcelVersion}-$distro.parcel.sha"
done

curl -u$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -T $dist/PULSE-${parcelVersion}.jar "https://repository.phdata.io/artifactory/$DEPLOY_REPO/phdata/pulse/${repoName}/PULSE-${parcelVersion}.jar"
curl -u$ARTIFACTORY_USER:$ARTIFACTORY_TOKEN -T $dist/PULSE-${parcelVersion}.jar.sha "https://repository.phdata.io/artifactory/$DEPLOY_REPO/phdata/pulse/${repoName}/PULSE-${parcelVersion}.jar.sha"

echo deploy success
