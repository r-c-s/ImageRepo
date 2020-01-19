A simple application for uploading images

Depends on Auth microservice, repo for that project is coming soon!

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
services.auth.authenticate=AUTH_SERVICE_AUTHENTICATE_URL
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

##### Additional test properties

<pre>
services.auth.login=AUTH_SERVICE_LOGIN_URL
userA.username=TEST_USER_A_USERNAME
userA.password=TEST_USER_A_PASSWORD
userB.username=TEST_USER_B_USERNAME
userB.password=TEST_USER_B_PASSWORD
admin.username=ADMIN_USERNAME
admin.password=ADMIN_PASSWORD
</pre>