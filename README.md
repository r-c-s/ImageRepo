A simple application for uploading images

### API

http://ec2-3-83-1-224.compute-1.amazonaws.com/imagerepo/swagger-ui.html

##### BUILD

<pre>
mvn clean package
</pre>

##### RUN UNIT TESTS

<pre>
mvn test
</pre>

##### RUN INTEGRATION TESTS USING S3

<pre>
mvn clean test-compile failsafe:integration-test \
     -Damazon.aws.accessKey=ACCESS_KEY \
     -Damazon.aws.secretKey=SECRET_KEY \
     -Damazon.aws.region=REGION \
     -Damazon.aws.s3.bucket=BUCKET
</pre>

##### RUN INTEGRATION TESTS USING LOCAL STORAGE

<pre>
mvn clean test-compile failsafe:integration-test \
     -Dlocal.storage.dir=LOCAL_STORAGE_DIR
</pre>

##### RUN APP USING S3

<pre>
java -Djavax.net.ssl.trustStore=TRUST_STORE_FILE \
     -Djavax.net.ssl.trustStorePassword=TRUST_STORE_PASSWORD \
     -jar "ImageRepo-1.0-SNAPSHOT.jar" \
     --spring.data.mongodb.uri=MONGO_URI \
     --amazon.aws.accessKey=ACCESS_KEY \
     --amazon.aws.secretKey=SECRET_KEY \
     --amazon.aws.region=REGION \
     --amazon.aws.s3.bucket=BUCKET \
     --server.port=HOST \
     --server.port=PORT
</pre>

##### RUN APP USING LOCAL STORAGE

<pre>
java -jar "ImageRepo-1.0-SNAPSHOT.jar" \
     --spring.data.mongodb.uri=MONGO_URI \
     --local.storage.dir=LOCAL_STORAGE_DIR \
     --server.port=HOST \
     --server.port=PORT
</pre>