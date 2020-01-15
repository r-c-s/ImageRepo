A simple application for uploading images

<br>

### API

http://ec2-3-83-1-224.compute-1.amazonaws.com/imagerepo/swagger-ui.html

<br>

##### Build

<pre>
mvn clean package
</pre>

##### Run unit tests

<pre>
mvn test
</pre>

##### Run integration tests

<pre>
mvn clean test-compile failsafe:integration-test -Dapp.properties=APP_PROPERTIES_FILE
</pre>

##### Run application

<pre>
java -jar "ImageRepo-1.0-SNAPSHOT.jar" --app.properties=APP_PROPERTIES_FILE 
</pre>

TODO: steps to build trust store required for S3

<br>

#### APP PROPERTIES

##### Base required properties

<pre>
spring.data.mongodb.uri=MONGODB_URI
server.host=HOST
server.port=PORT
</pre>

One of the two is required:

##### S3 properties

<pre>
amazon.aws.accessKey=ACCESS_KEY
amazon.aws.secretKey=SECRET_KEY
amazon.aws.region=REGION
amazon.aws.s3.bucket=BUCKET
</pre>

##### Local storage properties

<pre>
local.storage.dir=LOCAL_STORAGE_DIR
</pre>