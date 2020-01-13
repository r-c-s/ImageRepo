A simple application for uploading images

### API

<pre>
http://ec2-3-83-1-224.compute-1.amazonaws.com/imagerepo/swagger-ui.html
</pre>

##### BUILD

<pre>
mvn clean package
</pre>

##### RUN UNIT TESTS

<pre>
mvn test
</pre>

##### RUN INTEGRATION TESTS

<pre>
mvn clean test-compile failsafe:integration-test -Damazon.aws.accessKey=ACCESS_KEY -Damazon.aws.secretKey=ACCESS_KEY
</pre>

##### RUN APP

<pre>
java -Djavax.net.ssl.trustStore=TRUST_STORE_FILE \
     -Djavax.net.ssl.trustStorePassword=TRUST_STORE_PASSWORD \
     -jar "ImageRepo-1.0-SNAPSHOT.jar" \
     --spring.data.mongodb.uri=MONGO_URI \
     --amazon.aws.accessKey=ACCESS_KEY \
     --amazon.aws.secretKey=SECRET_KEY \
     --amazon.aws.s3.bucket=BUCKET \
     --server.port=HOST \
     --server.port=PORT
</pre>