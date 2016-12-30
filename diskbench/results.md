# Test results

### mac localhost to localhost git commit 596e2c43b2b1d17f0f2013d3e58534e58f6923d1 direct buffer

* with pending = 0, throughput  1GBps, latency: 99.9% < 70 micro
* with pending >= 65, throughput 1.9GBps, latency: 99.9% < 70 micro

### mac localhost to localhost git commit 596e2c43b2b1d17f0f2013d3e58534e58f6923d1 regular buffer
* with pending = 0, throughput  1GBps, latency: 99.9% < 70 micro
* with pending >= 65, throughput 1.6GBps, latency: 99.9% < 500 micro


### The need
Just writing a small java program using maven but executing it independently without maven can be a f**ing chore.

This template does many things

* Uses maven and includes the most common libraries like guava, apache commons-lang3 and commons-io and Jackson objectmapper for json
* Log4j2 is fully configured. That itself can be quite a chore for a new project.
* A Utils class which includes only "pure" functions. No oop bullshit. reading and writing files, json, etc made easy. Dont leave home without it. Check out the `Timer` class. You can use it to sample throughput.
* When you do `mvn clean package`, all the dependencies, the project jar file and resources go into `target/dependency-jars`. This folder is everything you need to run the program without maven.
* The resources are not packaged in the jar files. Rather they are there as-is in the `dependency-jars` folder so you can just change it and run it. More details below.
* Instead of command line arguments, the main function searches for a `settings.json` in the classpath. So you can provide rich structure to command line arguments instead of flags on the command line
* Heck, I'm even throwing in a nice `.gitignore` and a `README.md` file.
*

### Configure Intellij

First run `clean.sh`. It deletes intellij metadata files, log files etc that might have been carried over from the copying.

Now a bigger problem arises. The `src/main/resources` folder (henceforth referred to as the `resources` folder) contains config files such as log4j2.xml, etc. It should be possible to change these files and rerun without doing a recompile.

The default maven behaviour makes it impossible because the resource folder gets packaged in the main jar. Another proof of the dumbassery of maven

 To workaround this, we make some changes in `pom.xml`. We exclude resource folder from getting packaged in the jar and copy the contents directly to `target/`.

 Unfortunately, Intellij starts acting up with this change and needs some hand-holding to make it see the light.

 Go to `file->project structure`. Select `modules` on the left and select the `dependencies` tab on the right. Click the `+` sign to add `Jars or Directories` and select the `resources` folder. In the `choose categories of selected files` dialog box, select `classes` and click `ok`. You'll see the resources directory added to the bottom. That is it. Now all files in `resources` directory are recognized as being in the classpath and everything in the world is happy and gay.
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
java -cp  '/home/sharath.g/code/template/:/home/sharath.g/code/template/*' sha.App
````
