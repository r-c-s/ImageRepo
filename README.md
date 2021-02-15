## File Upload

A simple application for uploading files

<br>
<hr>
<br>

#### Dependencies
* [AuthApi](https://github.com/r-c-s/AuthApi)
* [MongoDB](https://docs.mongodb.com/manual/installation/)
* TODO: steps to build trust store required for S3

<br>
<hr>
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
AuthService must be running.

##### Run application

<pre>
java -jar target/ImageRepo-1.0-SNAPSHOT.jar --app.properties=APP_PROPERTIES_FILE 
</pre>

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

<br>
<hr>
<br>

##### Register on Auth service

<pre>
curl -X POST authhost:authport/auth/api/users -H "Content-type:application/json" -d "{"username":"USERNAME","password":"PASSWORD"}"
</pre>

##### Login on Auth service

<pre>
curl -X POST authhost:authport/auth/login -d "username=USERNAME&password=PASSWORD" -c cookies
</pre>

Use the cookies from above to make requests