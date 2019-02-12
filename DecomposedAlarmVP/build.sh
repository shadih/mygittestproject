chmod -f 744 ./build.xml ./src/main/resources/valuepack/decomposedScenario/filters.xml
cp -f ./build.xml.ciena ./build.xml
cp -f ./src/main/resources/valuepack/decomposedScenario/filters.xml.ciena ./src/main/resources/valuepack/decomposedScenario/filters.xml
$UCA_EBC_DEV_HOME/3pp/ant/bin/ant clean compile package
cp -f ./build.xml.adtran ./build.xml
cp -f ./src/main/resources/valuepack/decomposedScenario/filters.xml.adtran ./src/main/resources/valuepack/decomposedScenario/filters.xml
$UCA_EBC_DEV_HOME/3pp/ant/bin/ant compile package
cp -f ./build.xml.juniper ./build.xml
cp -f ./src/main/resources/valuepack/decomposedScenario/filters.xml.juniper ./src/main/resources/valuepack/decomposedScenario/filters.xml
$UCA_EBC_DEV_HOME/3pp/ant/bin/ant compile package
