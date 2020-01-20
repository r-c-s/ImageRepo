## Image Repo

A simple application for uploading images

#### Dependencies
Auth microservice: https://github.com/r-c-s/Auth

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

##### Base app properties

<pre>
spring.data.mongodb.uri=MONGODB_URI
server.host=HOST
server.port=PORT
services.auth.authenticate=AUTH_SERVICE_AUTHENTICATE_URL
</pre>

One of the following two is required:

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