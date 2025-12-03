all:
	javac *.java

run: all
	java -cp .:mssql-jdbc-11.2.0.jre18.jar SQLServerMain

clean:
	rm *.class
