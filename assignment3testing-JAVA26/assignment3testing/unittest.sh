# Determine path separator based on OS
if [ "$(uname)" = "Linux" ] || [ "$(uname)" = "Darwin" ]; then
    SEP=":"
else
    SEP=";"
fi

echo "Running unittests..."
java -javaagent:./lib/jacocoagent.jar -cp ./lib/org.junit4-4.3.1.jar${SEP}./bin org.junit.runner.JUnitCore ltu.PaymentTest

echo "Generating report..."
java -jar ./lib/jacococli.jar report ./jacoco.exec --classfiles ./bin --html ./coveragereport --name CodeCoverageReport --sourcefiles ./src
