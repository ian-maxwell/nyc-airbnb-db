# Makefile – works on Windows, Linux, macOS, aviary, AWS, everywhere

JAR = mssql-jdbc-13.2.1.jre11.jar

# Detect OS for classpath separator and clean command
ifeq ($(OS),Windows_NT)
    CPSEP = ;
    RM = del
else
    CPSEP = :
    RM = rm -f
endif

all:
	javac -cp "$(JAR)" *.java

run: all
	java -Xmx512m -cp ".$(CPSEP)$(JAR)" SQLServerMain

clean:
	$(RM) *.class

.PHONY: all run clean
