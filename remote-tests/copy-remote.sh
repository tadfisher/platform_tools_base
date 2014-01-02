#########
#   This file is for testing the end to end flow on a linux system
#   It will not be used at to run tests on other operating systems but does server as an overview of what will happen
########

set -e
function build_base {
    rm -rf ../../out/host/gradle/repo/
    ./gradlew --init-script release.gradle gradle:clean prepareRepo zipArtifacts publishLocal
}

function prep_target {
    #clear last run and prep for new run
    mkdir -p $remote_dir
    rm -rf $remote_dir/*
    #../../../../../out/host/gradle/repo hardcoded in tests
    mkdir -p $remote_test_dir/build-system/{gradle,builder} $remote_dir/out/host/gradle/repo
}

function copy_binaries {
    cp -r ../../out/host/gradle/repo/* $remote_dir/out/host/gradle/repo/
    cp ../../out/host/gradle/tools/base/.gradle/wrapper/dists/gradle-${gradle_version}-bin/*/gradle-${gradle_version}-bin.zip $remote_test_dir
    cp remote-tests.zip $remote_test_dir
}

function build_in_remote {
    pushd $remote_test_dir
    unzip remote-tests.zip 
    unzip gradle-${gradle_version}-bin.zip
    ./gradle-${gradle_version}/bin/gradle disableTestFailures  extractJar check aggregateResults
}

if [[ -n "$1" ]]
then
    export remote_dir=$1
    export remote_test_dir=$remote_dir/tools/base/
    cd ../
    gradle_version=$(grep  'distributionUrl' gradle/wrapper/gradle-wrapper.properties | grep -oEi '[0-9].[0-9]+')

    build_base
    prep_target remote_dir=remote_dir remote_test_dir=remote_test_dir
    copy_binaries remote_dir=remote_dir remote_test_dir=remote_test_dir gradle_version=gradle_version 
    build_in_remote remote_dir=remote_dir remote_test_dir=remote_test_dir gradle_version=gradle_version 
else
    echo 'please specify a remote directory location to run the tests'
fi
