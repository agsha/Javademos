# A template for using java for small one-off projects

### The need
Just writing a small java program using maven but executing it independently without maven can be a f**ing chore.

This template does many things

* Uses maven and includes the most common libraries like guava, apache commons-lang3 and commons-io and Jackson objectmapper for json
* Log4j2 is fully configured. That itself can be quite a chore for a new project.
* A Utils class which includes only "pure" functions. No oop bullshit. reading and writing files, json, etc made easy. Dont leave home without it. Check out the `Timer` class. You can use it to sample throughput.
* When you do `mvn clean package`, all the dependencies, the project jar file and resources go into `target/dependency-jars`. This folder is everything you need to run the program without maven.
* The resources are not packaged in the jar files. Rather they are there as-is in the `dependency-jars` folder so you can just change it and run it.
* Instead of command line arguments, the main function searches for a `settings.json` in the classpath. So you can provide rich structure to command line arguments instead of flags on the command line
* Heck, I'm even throwing in a nice `.gitignore` and a `README.md` file.
*
### To compile
````
mvn package
````

### To copy it to a remote location (optional)
````
rsync -zaP target/dependency-jars/ 188.166.204.110:~/code/template
````

### To run it on the remote location
````
java -cp  '/home/sharath.g/code/template/:/home/sharath.g/code/template/*' sha.Client
````
