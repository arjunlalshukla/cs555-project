all:
	sbt assembly 

server:
	java -cp target/scala-2.13/p1-chatserver-filesync.jar FileSync -server $(SERVER_DIR) 
client:
	sbt "run -client $(HOST) $(CLIENT_DIR)"

clean:
	sbt clean
